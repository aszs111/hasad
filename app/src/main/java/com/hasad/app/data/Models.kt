package com.hasad.app.data

/** أنواع الملفات كما تظهر في شاشة "وش تبي تنزّل؟" */
enum class Kind(val key: String) {
    PDF("pdf"), WORD("word"), EXCEL("excel"), BOOK("book"), AUDIO("audio"), VOICE("voice"),
    IMAGE("image"), VIDEO("video"), OTHER("other"), BLOCKED("blocked");

    companion object {
        fun of(key: String?) = entries.firstOrNull { it.key == key } ?: OTHER
        val selectable = listOf(PDF, WORD, EXCEL, BOOK, AUDIO, VOICE, IMAGE, VIDEO, OTHER)
        val defaults = setOf(PDF, WORD, EXCEL, BOOK)
    }
}

object FileStatus {
    const val PENDING = 0
    const val DOWNLOADING = 1
    const val DONE = 2
    const val FAILED = 3
    const val REMOVED = 4   // المستخدم حذفه أو نقله من المجلد — ما نرجّعه إلا إذا طلب
    const val SKIPPED = 5   // نوع غير مختار أو مستبعد بالفلتر
}

data class Channel(
    val id: Long = 0,
    val chatId: Long,
    val title: String,
    val username: String,
    val members: Int,
    val link: String,
    val kinds: Set<Kind>,
    val byHashtag: Boolean,
    val byYear: Boolean,
    val renameFromCaption: Boolean,
    val autoUpdate: Boolean,
    val keyword: String,
    val fromYear: Int,
    val storageUri: String?,
    val isProtected: Boolean,
    val lastMsgId: Long,
    val blockedCount: Int,
    val paused: Boolean,
    val createdAt: Long,
    val lastSync: Long,
)

data class ChannelStats(
    val total: Int,        // الملفات المطلوبة (غير المتخطاة)
    val done: Int,
    val failed: Int,
    val removed: Int,
    val bytesDone: Long,
    val bytesTotal: Long,
)

data class FileItem(
    val id: Long,
    val channelId: Long,
    val chatId: Long,
    val messageId: Long,
    val kind: Kind,
    val origName: String,
    val displayName: String,
    val caption: String,
    val section: String,
    val year: Int,
    val date: Long,
    val size: Long,
    val mime: String,
    val status: Int,
    val uri: String?,
    val favorite: Boolean,
    val channelTitle: String = "",
)

data class Shelf(val id: Long, val name: String, val count: Int)

/** نتيجة فحص القناة قبل التنزيل */
data class ScanResult(
    val chatId: Long,
    val title: String,
    val username: String,
    val members: Int,
    val isProtected: Boolean,
    val totalFiles: Int,
    val totalBytes: Long,
    val sinceYear: Int,
    val countByKind: Map<Kind, Int>,
    val bytesByKind: Map<Kind, Long>,
    val blocked: Int,
    val sections: Map<String, Int>,
    val items: List<ScannedFile>,
    val maxMsgId: Long,
)

data class ScannedFile(
    val messageId: Long,
    val kind: Kind,
    val origName: String,
    val caption: String,
    val section: String,
    val year: Int,
    val date: Long,
    val size: Long,
    val mime: String,
)
