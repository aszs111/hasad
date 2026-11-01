package com.hasad.app.data

import android.content.Context
import com.hasad.app.td.Td
import com.hasad.app.td.TgRepo
import com.hasad.app.util.Arabic
import com.hasad.app.util.Naming
import com.hasad.app.work.Jobs
import org.drinkless.tdlib.TdApi

/** منطق القنوات: إضافة، تحديث، مراجعة الملفات المحذوفة */
object Library {

    fun statusFor(ch: Channel, f: ScannedFile): Int {
        if (f.kind !in ch.kinds) return FileStatus.SKIPPED
        if (ch.fromYear > 0 && f.year < ch.fromYear) return FileStatus.SKIPPED
        if (ch.keyword.isNotBlank()) {
            val hay = Arabic.normalize("${f.origName} ${f.caption}")
            val words = ch.keyword.split(',', '،').map { Arabic.normalize(it) }.filter { it.isNotBlank() }
            if (words.isNotEmpty() && words.none { hay.contains(it) }) return FileStatus.SKIPPED
        }
        return FileStatus.PENDING
    }

    fun nameFor(ch: Channel, f: ScannedFile) =
        Naming.buildName(f.origName, f.caption, f.kind, ch.renameFromCaption, "ملف_${f.messageId}")

    /** حفظ القناة بعد الفحص وبدء التجميع */
    /** [selected] أرقام الرسائل اللي اختارها المستخدم بنفسه؛ لو null نختار حسب النوع والفلتر */
    fun addChannel(ctx: Context, link: String, scan: ScanResult, opts: Channel, selected: Set<Long>? = null): Long {
        val db = Db.get(ctx)
        val ch = opts.copy(
            chatId = scan.chatId, title = scan.title, username = scan.username, members = scan.members, link = link,
            isProtected = scan.isProtected, lastMsgId = scan.maxMsgId, blockedCount = scan.blocked,
            createdAt = db.channelByChat(scan.chatId)?.createdAt ?: System.currentTimeMillis(), lastSync = System.currentTimeMillis(),
        )
        val id = db.upsertChannel(ch)
        val chosen = selected ?: scan.items.filter { statusFor(ch, it) == FileStatus.PENDING }.map { it.messageId }.toSet()
        db.insertFiles(id, scan.chatId, scan.items,
            { if (it.messageId in chosen) FileStatus.PENDING else FileStatus.SKIPPED }, { nameFor(ch, it) })
        db.applySelection(id, chosen)
        Jobs.startDownloads(ctx)
        return id
    }

    /** يجيب الجديد من القناة (يُستدعى يومياً أو يدوياً) */
    suspend fun sync(ctx: Context, ch: Channel): Int {
        val db = Db.get(ctx)
        val chat = Td.send(TdApi.GetChat().apply { chatId = ch.chatId })
        val scan = TgRepo.scan(chat, afterMsgId = ch.lastMsgId)
        db.addBlocked(ch.id, scan.blocked)
        val added = db.insertFiles(ch.id, ch.chatId, scan.items, { statusFor(ch, it) }, { nameFor(ch, it) })
        db.setLastSync(ch.id, scan.maxMsgId)
        checkRemoved(ctx, ch.id)
        return added
    }

    /** الملفات اللي حذفها المستخدم أو نقلها: نعلّمها ولا نعيد تنزيلها */
    fun checkRemoved(ctx: Context, channelId: Long): Int {
        val db = Db.get(ctx)
        var n = 0
        for (f in db.doneFiles(channelId)) {
            if (!Storage.exists(ctx, f.uri)) { db.setStatusQuiet(f.id, FileStatus.REMOVED); n++ }
        }
        if (n > 0) db.bump()
        return n
    }
}
