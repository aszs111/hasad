package com.hasad.app.util

import com.hasad.app.data.Kind
import java.util.Locale

object Arabic {
    private val tashkeel = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u0640]")

    /** توحيد الحروف عشان البحث يلقى "الأحوال" لو كتبت "الاحوال" */
    fun normalize(s: String): String = s.lowercase(Locale.ROOT)
        .replace(tashkeel, "")
        .replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا').replace('ٱ', 'ا')
        .replace('ى', 'ي').replace('ة', 'ه').replace('ؤ', 'و').replace('ئ', 'ي')
        .replace('_', ' ')
        .replace(Regex("\\s+"), " ").trim()
}

object Naming {
    private val illegal = Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]")
    private val hashtag = Regex("#([\\p{L}\\p{N}_]+)")
    private val urls = Regex("https?://\\S+|t\\.me/\\S+|@\\w+")

    fun hashtags(text: String): List<String> = hashtag.findAll(text).map { it.groupValues[1].replace('_', ' ').trim() }.toList()

    fun sanitize(name: String, max: Int = 120): String {
        var n = name.replace(illegal, " ").replace(Regex("\\s+"), " ").trim().trim('.')
        if (n.length > max) n = n.take(max).trim()
        return n
    }

    fun ext(fileName: String): String = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT).take(8)

    /** اسم الملف من وصف الرسالة: أول سطر مفيد بدون هاشتاقات وروابط */
    fun nameFromCaption(caption: String): String? {
        val line = caption.lines()
            .map { it.replace(hashtag, " ").replace(urls, " ").replace(Regex("[\\p{So}\\p{Cn}]"), " ").trim(' ', '-', '–', '—', '|', '•', '*', ':', '.') }
            .firstOrNull { it.count { ch -> ch.isLetter() } >= 3 } ?: return null
        return sanitize(line, 100).ifBlank { null }
    }

    fun buildName(orig: String, caption: String, kind: Kind, rename: Boolean, fallback: String): String {
        val e = ext(orig).ifBlank { defaultExt(kind) }
        val base = (if (rename) nameFromCaption(caption) else null)
            ?: sanitize(orig.substringBeforeLast('.', orig)).ifBlank { fallback }
        return if (e.isBlank()) base else "$base.$e"
    }

    private fun defaultExt(k: Kind) = when (k) {
        Kind.IMAGE -> "jpg"; Kind.VIDEO -> "mp4"; Kind.VOICE -> "ogg"; Kind.AUDIO -> "mp3"; Kind.PDF -> "pdf"; else -> ""
    }
}

object Kinds {
    private val word = setOf("doc", "docx", "rtf", "odt", "dot", "dotx", "pages")
    private val excel = setOf("xls", "xlsx", "xlsm", "csv", "ods", "numbers")
    private val book = setOf("epub", "mobi", "azw", "azw3", "djvu", "fb2", "chm", "bok", "cbz")
    private val audio = setOf("mp3", "m4a", "aac", "wav", "ogg", "opus", "flac", "amr", "wma")
    private val video = setOf("mp4", "mkv", "mov", "avi", "webm", "3gp", "m4v", "wmv")
    private val image = setOf("jpg", "jpeg", "png", "gif", "webp", "heic", "bmp", "tif", "tiff")

    /** ملفات تنفيذية ما نجمعها أبداً — لسلامة الجهاز */
    val blocked = setOf("apk", "apks", "xapk", "aab", "exe", "msi", "bat", "cmd", "com", "scr", "jar", "vbs", "ps1",
        "dmg", "pkg", "app", "sh", "deb", "rpm", "dll", "js", "hta", "lnk", "reg")

    fun ofDocument(fileName: String, mime: String): Kind {
        val e = Naming.ext(fileName)
        return when {
            e in blocked || mime.contains("android.package-archive") || mime.contains("msdownload") -> Kind.BLOCKED
            e == "pdf" || mime == "application/pdf" -> Kind.PDF
            e in word || mime.contains("msword") || mime.contains("wordprocessing") -> Kind.WORD
            e in excel || mime.contains("spreadsheet") || mime.contains("ms-excel") -> Kind.EXCEL
            e in book || mime.contains("epub") -> Kind.BOOK
            e in audio || mime.startsWith("audio/") -> Kind.AUDIO
            e in video || mime.startsWith("video/") -> Kind.VIDEO
            e in image || mime.startsWith("image/") -> Kind.IMAGE
            else -> Kind.OTHER
        }
    }
}

object Fmt {
    fun num(n: Number): String = String.format(Locale.US, "%,d", n.toLong())

    fun size(b: Long, ar: Boolean): String {
        val kb = 1024.0; val mb = kb * 1024; val gb = mb * 1024
        return when {
            b >= gb -> String.format(Locale.US, "%.1f %s", b / gb, if (ar) "جيجا" else "GB")
            b >= mb -> String.format(Locale.US, "%.1f %s", b / mb, if (ar) "ميجا" else "MB")
            b >= kb -> String.format(Locale.US, "%.0f %s", b / kb, if (ar) "كيلو" else "KB")
            else -> "$b B"
        }
    }

    /** الرقم الكبير بدون الوحدة + الوحدة منفصلة (للعرض الكبير) */
    fun sizeParts(b: Long, ar: Boolean): Pair<String, String> {
        val s = size(b, ar); val i = s.lastIndexOf(' ')
        return s.substring(0, i) to s.substring(i + 1)
    }

    private val arMonths = arrayOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر")

    fun date(epochSec: Long, ar: Boolean): String {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = epochSec * 1000 }
        val d = cal.get(java.util.Calendar.DAY_OF_MONTH); val m = cal.get(java.util.Calendar.MONTH); val y = cal.get(java.util.Calendar.YEAR)
        return if (ar) "$d ${arMonths[m]} $y" else String.format(Locale.US, "%d/%d/%d", d, m + 1, y)
    }

    fun yearOf(epochSec: Long): Int = java.util.Calendar.getInstance().apply { timeInMillis = epochSec * 1000 }.get(java.util.Calendar.YEAR)

    /** حرفين من اسم القناة للصورة الرمزية */
    fun initials(title: String): String {
        val words = title.split(' ', '·', '-', '|').filter { w -> w.any { it.isLetter() } }
        if (words.isEmpty()) return "؟"
        val w0 = words[0].filter { it.isLetter() }
        return if (words.size >= 2) "${w0.removePrefix("ال").firstOrNull() ?: w0.first()}${words[1].filter { it.isLetter() }.removePrefix("ال").firstOrNull() ?: ""}"
        else w0.removePrefix("ال").take(1).ifEmpty { w0.take(1) }
    }
}

/** تحليل رابط تيليجرام بأي شكل */
sealed class TgLink {
    data class Public(val username: String) : TgLink()
    data class Invite(val link: String) : TgLink()
    data class PrivatePost(val url: String) : TgLink()
    data object Invalid : TgLink()

    companion object {
        fun parse(raw: String): TgLink {
            val s = raw.trim().removePrefix("https://").removePrefix("http://").removePrefix("www.")
            if (s.startsWith("@")) return s.drop(1).takeWhile { it.isLetterOrDigit() || it == '_' }.let { if (it.length >= 4) Public(it) else Invalid }
            val host = listOf("t.me/", "telegram.me/", "telegram.dog/").firstOrNull { s.startsWith(it) }
            if (host == null) {
                if (s.startsWith("tg://resolve?domain=")) return Public(s.substringAfter("domain=").substringBefore('&'))
                if (Regex("^[A-Za-z][A-Za-z0-9_]{3,31}$").matches(s)) return Public(s)
                return Invalid
            }
            val path = s.removePrefix(host).substringBefore('?').trimEnd('/')
            val seg = path.split('/')
            return when {
                seg[0].startsWith("+") || seg[0] == "joinchat" -> Invite("https://t.me/$path")
                seg[0] == "c" -> PrivatePost("https://t.me/$path")
                seg[0] == "s" && seg.size > 1 -> Public(seg[1])
                seg[0].length >= 4 -> Public(seg[0])
                else -> Invalid
            }
        }
    }
}
