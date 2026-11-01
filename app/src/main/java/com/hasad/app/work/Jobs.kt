package com.hasad.app.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.hasad.app.R
import com.hasad.app.data.Db
import com.hasad.app.data.FileItem
import com.hasad.app.data.FileStatus
import com.hasad.app.data.Library
import com.hasad.app.data.Prefs
import com.hasad.app.data.Storage
import com.hasad.app.td.AuthState
import com.hasad.app.td.Td
import com.hasad.app.td.TgRepo
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object Jobs {
    private const val DL = "hasad-download"
    private const val SYNC = "hasad-daily"

    fun startDownloads(ctx: Context) {
        val req = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(
                if (Prefs.wifiOnly(ctx)) NetworkType.UNMETERED else NetworkType.CONNECTED).build())
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(ctx).enqueueUniqueWork(DL, ExistingWorkPolicy.KEEP, req)
    }

    fun scheduleDaily(ctx: Context) {
        val req = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(SYNC, ExistingPeriodicWorkPolicy.KEEP, req)
    }

    fun syncNow(ctx: Context) {
        WorkManager.getInstance(ctx).enqueue(OneTimeWorkRequestBuilder<SyncWorker>()
            .setInputData(androidx.work.workDataOf("all" to true))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build())
    }

    suspend fun awaitReady(ctx: Context): Boolean {
        Td.start(ctx)
        return withTimeoutOrNull(45_000) { Td.auth.first { it == AuthState.Ready || it is AuthState.Failed || it == AuthState.NeedKeys || it == AuthState.WaitPhone } } == AuthState.Ready
    }

    fun notifChannel(ctx: Context): String {
        val id = "downloads"
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = ctx.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(id, "التنزيلات", NotificationManager.IMPORTANCE_LOW))
        }
        return id
    }
}

/** تقدّم التنزيل الحالي للعرض في الواجهة: رقم الملف ← (المنزّل، الحجم) */
object Progress {
    private val _map = MutableStateFlow<Map<Long, Pair<Long, Long>>>(emptyMap())
    val map: StateFlow<Map<Long, Pair<Long, Long>>> = _map
    private val tdToItem = ConcurrentHashMap<Int, Long>()

    fun bind(tdFileId: Int, itemId: Long, total: Long) { tdToItem[tdFileId] = itemId; _map.value = _map.value + (itemId to (0L to total)) }
    fun update(f: org.drinkless.tdlib.TdApi.File) {
        val item = tdToItem[f.id] ?: return
        val total = if (f.size > 0) f.size else f.expectedSize
        _map.value = _map.value + (item to (f.local.downloadedSize to total))
    }
    fun clear(tdFileId: Int) { tdToItem.remove(tdFileId)?.let { _map.value = _map.value - it } }
}

class DownloadWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    private fun info(text: String): ForegroundInfo {
        val n = NotificationCompat.Builder(applicationContext, Jobs.notifChannel(applicationContext))
            .setSmallIcon(R.drawable.ic_launcher_fg)
            .setContentTitle("حصاد يجمع الملفات")
            .setContentText(text)
            .setOngoing(true)
            .setSilent(true)
            .build()
        return if (Build.VERSION.SDK_INT >= 29) ForegroundInfo(42, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC) else ForegroundInfo(42, n)
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = info("جاري التجهيز…")

    override suspend fun doWork(): Result = coroutineScope {
        val ctx = applicationContext
        runCatching { setForeground(info("جاري التجهيز…")) }
        if (!Jobs.awaitReady(ctx)) return@coroutineScope Result.retry()
        val db = Db.get(ctx)
        db.resetDownloading()
        val listener = launch { Td.fileUpdates.collect { Progress.update(it) } }
        val done = java.util.concurrent.atomic.AtomicInteger(0)
        try {
            while (true) {
                val batch = db.nextPending(2)
                if (batch.isEmpty()) break
                runCatching { setForeground(info("تم ${done.get()} ملف · جاري: ${batch.first().displayName}")) }
                batch.map { f -> async { if (downloadOne(f)) done.incrementAndGet(); Unit } }.awaitAll()
                if (isStopped) break
            }
        } finally { listener.cancel() }
        Result.success()
    }

    private suspend fun downloadOne(f: FileItem): Boolean {
        val ctx = applicationContext
        val db = Db.get(ctx)
        val ch = db.channel(f.channelId) ?: return false
        db.setStatus(f.id, FileStatus.DOWNLOADING)
        var tdId = 0
        return try {
            val file = TgRepo.fileOf(f.chatId, f.messageId) ?: run { db.setStatus(f.id, FileStatus.FAILED); return false }
            tdId = file.id
            Progress.bind(tdId, f.id, if (file.size > 0) file.size else file.expectedSize)
            val res = TgRepo.download(file.id)
            val path = res.local.path
            if (path.isNullOrBlank() || !res.local.isDownloadingCompleted) error("لم يكتمل التنزيل")
            val uri = Storage.save(ctx, File(path), ch.storageUri, Storage.dirsFor(ch, f), f.displayName, f.mime)
            TgRepo.deleteCached(file.id)
            db.setStatus(f.id, FileStatus.DONE, uri.toString())
            true
        } catch (t: Throwable) {
            if (isStopped) db.setStatus(f.id, FileStatus.PENDING) else db.setStatus(f.id, FileStatus.FAILED)
            false
        } finally {
            if (tdId != 0) Progress.clear(tdId)
        }
    }
}

/** التحديث اليومي: يجيب الملفات الجديدة من القنوات اللي فعّلت لها "حدّثها لحالها" */
class SyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        if (!Jobs.awaitReady(ctx)) return Result.retry()
        val manual = inputData.getBoolean("all", false)
        for (ch in Db.get(ctx).channels()) {
            if (!ch.autoUpdate && !manual) continue
            if (ch.isProtected) continue
            runCatching { Library.sync(ctx, ch) }
        }
        Jobs.startDownloads(ctx)
        return Result.success()
    }
}
