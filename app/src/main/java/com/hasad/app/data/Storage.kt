package com.hasad.app.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.hasad.app.util.Naming
import java.io.File

/**
 * الحفظ في مكان يشوفه المستخدم:
 *  - الافتراضي: مجلد التنزيلات / حصاد / القناة / القسم / السنة (عن طريق MediaStore بدون صلاحيات)
 *  - أو مجلد يختاره المستخدم بنفسه (SAF)
 */
object Storage {
    const val ROOT = "حصاد"

    fun dirsFor(ch: Channel, f: FileItem): List<String> = buildList {
        add(Naming.sanitize(ch.title, 60).ifBlank { "قناة" })
        if (ch.byHashtag) add(Naming.sanitize(f.section, 60).ifBlank { "أخرى" })
        if (ch.byYear && f.year > 0) add(f.year.toString())
    }

    fun describe(ctx: Context, uri: String?): String =
        if (uri == null) "التنزيلات › $ROOT"
        else DocumentFile.fromTreeUri(ctx, Uri.parse(uri))?.name?.let { "$it" } ?: "مجلد مختار"

    /** ينسخ الملف ويرجع الرابط النهائي */
    fun save(ctx: Context, src: File, storageUri: String?, dirs: List<String>, name: String, mime: String): Uri {
        return if (storageUri == null) saveMediaStore(ctx, src, dirs, name, mime) else saveSaf(ctx, src, Uri.parse(storageUri), dirs, name, mime)
    }

    private fun saveMediaStore(ctx: Context, src: File, dirs: List<String>, name: String, mime: String): Uri {
        val rel = Environment.DIRECTORY_DOWNLOADS + "/" + (listOf(ROOT) + dirs).joinToString("/") + "/"
        val r = ctx.contentResolver
        val v = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime.ifBlank { "application/octet-stream" })
            put(MediaStore.MediaColumns.RELATIVE_PATH, rel)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = r.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v) ?: error("تعذّر إنشاء الملف")
        try {
            r.openOutputStream(uri)!!.use { out -> src.inputStream().use { it.copyTo(out, 256 * 1024) } }
            r.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        } catch (t: Throwable) {
            runCatching { r.delete(uri, null, null) }; throw t
        }
        return uri
    }

    private fun saveSaf(ctx: Context, src: File, tree: Uri, dirs: List<String>, name: String, mime: String): Uri {
        var dir = DocumentFile.fromTreeUri(ctx, tree) ?: error("ما نقدر نوصل للمجلد المختار")
        for (d in dirs) dir = dir.findFile(d)?.takeIf { it.isDirectory } ?: dir.createDirectory(d) ?: error("تعذّر إنشاء مجلد $d")
        var finalName = name
        var i = 2
        while (dir.findFile(finalName) != null) {
            val base = name.substringBeforeLast('.', name); val ext = name.substringAfterLast('.', "")
            finalName = if (ext.isEmpty()) "$base ($i)" else "$base ($i).$ext"; i++
        }
        val doc = dir.createFile(mime.ifBlank { "application/octet-stream" }, finalName) ?: error("تعذّر إنشاء الملف")
        ctx.contentResolver.openOutputStream(doc.uri)!!.use { out -> src.inputStream().use { it.copyTo(out, 256 * 1024) } }
        return doc.uri
    }

    /** هل الملف لسه موجود؟ (لو المستخدم حذفه نعلّمه "محذوف" وما نرجّعه) */
    fun exists(ctx: Context, uri: String?): Boolean {
        if (uri == null) return false
        return try {
            ctx.contentResolver.query(Uri.parse(uri), arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)
                ?.use { it.moveToFirst() } ?: false
        } catch (_: Exception) { false }
    }
}
