package com.hasad.app.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import com.hasad.app.data.Db
import com.hasad.app.data.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** يقرأ من قاعدة البيانات ويعيد القراءة تلقائياً كل ما تغيّر شي */
@Composable
fun <T> dbState(initial: T, vararg keys: Any?, block: (Db) -> T): State<T> {
    val ctx = LocalContext.current
    val db = Db.get(ctx)
    val version by db.version.collectAsState()
    return produceState(initial, version, *keys) {
        value = withContext(Dispatchers.IO) { block(db) }
    }
}

fun openFile(ctx: Context, f: FileItem) {
    val uri = f.uri ?: return
    val i = Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(uri), f.mime.ifBlank { "*/*" })
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    try { ctx.startActivity(i) } catch (_: ActivityNotFoundException) {
        try { ctx.startActivity(Intent.createChooser(i.setDataAndType(Uri.parse(uri), "*/*"), f.displayName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        catch (_: Exception) { Toast.makeText(ctx, "ما فيه تطبيق يفتح هذا الملف", Toast.LENGTH_SHORT).show() }
    }
}

fun shareFile(ctx: Context, f: FileItem) {
    val uri = f.uri ?: return
    val i = Intent(Intent.ACTION_SEND).setType(f.mime.ifBlank { "*/*" }).putExtra(Intent.EXTRA_STREAM, Uri.parse(uri))
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    ctx.startActivity(Intent.createChooser(i, f.displayName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

fun openUrl(ctx: Context, url: String) {
    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
