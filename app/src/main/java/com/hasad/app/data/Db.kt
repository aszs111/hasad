package com.hasad.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.hasad.app.util.Arabic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * قاعدة بيانات بسيطة (SQLite) للقنوات والملفات والرفوف.
 * أي تعديل يرفع [version] عشان الشاشات تعيد التحميل.
 */
class Db private constructor(ctx: Context) : SQLiteOpenHelper(ctx, "hasad.db", null, 1) {

    companion object {
        @Volatile private var inst: Db? = null
        fun get(c: Context): Db = inst ?: synchronized(this) { inst ?: Db(c.applicationContext).also { inst = it } }
    }

    private val _version = MutableStateFlow(0L)
    val version: StateFlow<Long> = _version
    fun bump() { _version.value = _version.value + 1 }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE channels(
            id INTEGER PRIMARY KEY AUTOINCREMENT, chatId INTEGER UNIQUE, title TEXT, username TEXT, members INTEGER,
            link TEXT, kinds TEXT, byHashtag INTEGER, byYear INTEGER, rename INTEGER, autoUpdate INTEGER,
            keyword TEXT, fromYear INTEGER, storage TEXT, protected INTEGER, lastMsgId INTEGER, blocked INTEGER,
            paused INTEGER, createdAt INTEGER, lastSync INTEGER)""")
        db.execSQL("""CREATE TABLE files(
            id INTEGER PRIMARY KEY AUTOINCREMENT, channelId INTEGER, chatId INTEGER, messageId INTEGER, kind TEXT,
            origName TEXT, displayName TEXT, caption TEXT, section TEXT, year INTEGER, date INTEGER, size INTEGER,
            mime TEXT, status INTEGER, uri TEXT, favorite INTEGER DEFAULT 0, norm TEXT,
            UNIQUE(chatId, messageId))""")
        db.execSQL("CREATE INDEX files_ch ON files(channelId, status)")
        db.execSQL("CREATE TABLE shelves(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT)")
        db.execSQL("CREATE TABLE shelf_items(shelfId INTEGER, fileId INTEGER, PRIMARY KEY(shelfId, fileId))")
        db.execSQL("INSERT INTO shelves(name) VALUES('بحثي')")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    // ───────── القنوات ─────────

    fun upsertChannel(ch: Channel): Long {
        val v = ContentValues().apply {
            put("chatId", ch.chatId); put("title", ch.title); put("username", ch.username)
            put("members", ch.members); put("link", ch.link)
            put("kinds", ch.kinds.joinToString(",") { it.key })
            put("byHashtag", ch.byHashtag.i); put("byYear", ch.byYear.i); put("rename", ch.renameFromCaption.i)
            put("autoUpdate", ch.autoUpdate.i); put("keyword", ch.keyword); put("fromYear", ch.fromYear)
            put("storage", ch.storageUri); put("protected", ch.isProtected.i); put("lastMsgId", ch.lastMsgId)
            put("blocked", ch.blockedCount); put("paused", ch.paused.i)
            put("createdAt", ch.createdAt); put("lastSync", ch.lastSync)
        }
        val existing = channelByChat(ch.chatId)
        val id = if (existing != null) {
            writableDatabase.update("channels", v, "id=?", arrayOf(existing.id.toString())); existing.id
        } else writableDatabase.insert("channels", null, v)
        bump(); return id
    }

    fun channels(): List<Channel> = readableDatabase.rawQuery("SELECT * FROM channels ORDER BY createdAt", null).use { c ->
        buildList { while (c.moveToNext()) add(c.toChannel()) }
    }

    fun channel(id: Long): Channel? = readableDatabase.rawQuery("SELECT * FROM channels WHERE id=?", arrayOf(id.toString())).use {
        if (it.moveToFirst()) it.toChannel() else null
    }

    fun channelByChat(chatId: Long): Channel? = readableDatabase.rawQuery("SELECT * FROM channels WHERE chatId=?", arrayOf(chatId.toString())).use {
        if (it.moveToFirst()) it.toChannel() else null
    }

    fun setPaused(id: Long, paused: Boolean) {
        writableDatabase.execSQL("UPDATE channels SET paused=? WHERE id=?", arrayOf(paused.i, id)); bump()
    }

    fun setLastSync(id: Long, lastMsgId: Long) {
        writableDatabase.execSQL("UPDATE channels SET lastSync=?, lastMsgId=MAX(lastMsgId, ?) WHERE id=?",
            arrayOf(System.currentTimeMillis(), lastMsgId, id)); bump()
    }

    fun addBlocked(id: Long, n: Int) {
        if (n > 0) writableDatabase.execSQL("UPDATE channels SET blocked=blocked+? WHERE id=?", arrayOf(n, id))
    }

    fun deleteChannel(id: Long) {
        writableDatabase.execSQL("DELETE FROM shelf_items WHERE fileId IN (SELECT id FROM files WHERE channelId=?)", arrayOf(id))
        writableDatabase.delete("files", "channelId=?", arrayOf(id.toString()))
        writableDatabase.delete("channels", "id=?", arrayOf(id.toString()))
        bump()
    }

    fun stats(channelId: Long): ChannelStats = readableDatabase.rawQuery(
        """SELECT COUNT(*),
           SUM(CASE WHEN status=${FileStatus.DONE} THEN 1 ELSE 0 END),
           SUM(CASE WHEN status=${FileStatus.FAILED} THEN 1 ELSE 0 END),
           SUM(CASE WHEN status=${FileStatus.REMOVED} THEN 1 ELSE 0 END),
           SUM(CASE WHEN status=${FileStatus.DONE} THEN size ELSE 0 END),
           SUM(size)
           FROM files WHERE channelId=? AND status<>${FileStatus.SKIPPED}""", arrayOf(channelId.toString())
    ).use { c ->
        c.moveToFirst()
        ChannelStats(c.getInt(0), c.getInt(1), c.getInt(2), c.getInt(3), c.getLong(4), c.getLong(5))
    }

    // ───────── الملفات ─────────

    /** يضيف الملفات الجديدة فقط (ما يلمس الموجود) ويرجع عدد المضاف */
    fun insertFiles(channelId: Long, chatId: Long, items: List<ScannedFile>, statusFor: (ScannedFile) -> Int,
                    nameFor: (ScannedFile) -> String): Int {
        var added = 0
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (f in items) {
                val name = nameFor(f)
                val v = ContentValues().apply {
                    put("channelId", channelId); put("chatId", chatId); put("messageId", f.messageId)
                    put("kind", f.kind.key); put("origName", f.origName); put("displayName", name)
                    put("caption", f.caption); put("section", f.section); put("year", f.year); put("date", f.date)
                    put("size", f.size); put("mime", f.mime); put("status", statusFor(f))
                    put("norm", Arabic.normalize("$name ${f.caption} ${f.origName}"))
                }
                if (db.insertWithOnConflict("files", null, v, SQLiteDatabase.CONFLICT_IGNORE) != -1L) added++
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
        bump(); return added
    }

    /** يعيد تطبيق الأنواع المختارة على ملفات قناة (لو المستخدم غيّر الاختيار) */
    fun reapplyKinds(channelId: Long, kinds: Set<Kind>) {
        val inList = kinds.joinToString(",") { "'${it.key}'" }.ifEmpty { "''" }
        writableDatabase.execSQL("UPDATE files SET status=${FileStatus.PENDING} WHERE channelId=? AND status=${FileStatus.SKIPPED} AND kind IN ($inList)", arrayOf(channelId))
        writableDatabase.execSQL("UPDATE files SET status=${FileStatus.SKIPPED} WHERE channelId=? AND status IN (${FileStatus.PENDING},${FileStatus.FAILED}) AND kind NOT IN ($inList)", arrayOf(channelId))
        bump()
    }

    fun nextPending(limit: Int): List<FileItem> = query(
        """SELECT f.*, c.title AS chTitle FROM files f JOIN channels c ON c.id=f.channelId
           WHERE f.status IN (${FileStatus.PENDING}, ${FileStatus.DOWNLOADING}) AND c.paused=0 AND c.protected=0
           ORDER BY c.createdAt, f.messageId LIMIT $limit""", emptyArray())

    fun hasPending(): Boolean = readableDatabase.rawQuery(
        """SELECT 1 FROM files f JOIN channels c ON c.id=f.channelId
           WHERE f.status IN (${FileStatus.PENDING}, ${FileStatus.DOWNLOADING}) AND c.paused=0 AND c.protected=0 LIMIT 1""", null
    ).use { it.moveToFirst() }

    fun setStatus(fileId: Long, status: Int, uri: String? = null) {
        val v = ContentValues().apply { put("status", status); if (uri != null) put("uri", uri) }
        writableDatabase.update("files", v, "id=?", arrayOf(fileId.toString())); bump()
    }

    fun setStatusQuiet(fileId: Long, status: Int) {
        writableDatabase.execSQL("UPDATE files SET status=? WHERE id=?", arrayOf(status, fileId))
    }

    fun retryFailed(channelId: Long) {
        writableDatabase.execSQL("UPDATE files SET status=${FileStatus.PENDING} WHERE channelId=? AND status=${FileStatus.FAILED}", arrayOf(channelId)); bump()
    }

    fun restoreRemoved(channelId: Long) {
        writableDatabase.execSQL("UPDATE files SET status=${FileStatus.PENDING}, uri=NULL WHERE channelId=? AND status=${FileStatus.REMOVED}", arrayOf(channelId)); bump()
    }

    fun resetDownloading() {
        writableDatabase.execSQL("UPDATE files SET status=${FileStatus.PENDING} WHERE status=${FileStatus.DOWNLOADING}")
    }

    fun doneFiles(channelId: Long): List<FileItem> =
        query("SELECT f.*, '' AS chTitle FROM files f WHERE channelId=? AND status=${FileStatus.DONE}", arrayOf(channelId.toString()))

    fun recentFiles(channelId: Long, limit: Int = 300): List<FileItem> = query(
        """SELECT f.*, '' AS chTitle FROM files f WHERE channelId=? AND status<>${FileStatus.SKIPPED}
           ORDER BY CASE status WHEN ${FileStatus.DOWNLOADING} THEN 0 WHEN ${FileStatus.DONE} THEN 2 ELSE 1 END, date DESC LIMIT $limit""",
        arrayOf(channelId.toString()))

    /** شكل الرف: حالة كل ملف بالترتيب (للرسم) */
    fun shelfShape(channelId: Long): List<Pair<Long, Int>> = readableDatabase.rawQuery(
        "SELECT id, status FROM files WHERE channelId=? AND status<>${FileStatus.SKIPPED} ORDER BY messageId", arrayOf(channelId.toString())
    ).use { c -> buildList { while (c.moveToNext()) add(c.getLong(0) to c.getInt(1)) } }

    fun toggleFavorite(fileId: Long) {
        writableDatabase.execSQL("UPDATE files SET favorite=1-favorite WHERE id=?", arrayOf(fileId)); bump()
    }

    // ───────── المكتبة والبحث ─────────

    fun search(q: String, section: String?, channelId: Long?, shelfId: Long?, favorites: Boolean, limit: Int = 500): List<FileItem> {
        val where = mutableListOf("f.status=${FileStatus.DONE}")
        val args = mutableListOf<String>()
        Arabic.normalize(q).split(' ').filter { it.isNotBlank() }.forEach { w -> where += "f.norm LIKE ?"; args += "%$w%" }
        if (section != null) { where += "f.section=?"; args += section }
        if (channelId != null) { where += "f.channelId=?"; args += channelId.toString() }
        if (favorites) where += "f.favorite=1"
        if (shelfId != null) { where += "f.id IN (SELECT fileId FROM shelf_items WHERE shelfId=?)"; args += shelfId.toString() }
        return query("""SELECT f.*, c.title AS chTitle FROM files f JOIN channels c ON c.id=f.channelId
            WHERE ${where.joinToString(" AND ")} ORDER BY f.date DESC LIMIT $limit""", args.toTypedArray())
    }

    fun sections(): List<Pair<String, Int>> = readableDatabase.rawQuery(
        "SELECT section, COUNT(*) n FROM files WHERE status=${FileStatus.DONE} GROUP BY section ORDER BY n DESC", null
    ).use { c -> buildList { while (c.moveToNext()) add(c.getString(0) to c.getInt(1)) } }

    fun libraryTotals(): Triple<Int, Long, Int> = readableDatabase.rawQuery(
        "SELECT COUNT(*), IFNULL(SUM(size),0), COUNT(DISTINCT channelId) FROM files WHERE status=${FileStatus.DONE}", null
    ).use { it.moveToFirst(); Triple(it.getInt(0), it.getLong(1), it.getInt(2)) }

    fun favoritesCount(): Int = readableDatabase.rawQuery(
        "SELECT COUNT(*) FROM files WHERE favorite=1 AND status=${FileStatus.DONE}", null).use { it.moveToFirst(); it.getInt(0) }

    fun shelves(): List<Shelf> = readableDatabase.rawQuery(
        "SELECT s.id, s.name, (SELECT COUNT(*) FROM shelf_items i WHERE i.shelfId=s.id) FROM shelves s ORDER BY s.id", null
    ).use { c -> buildList { while (c.moveToNext()) add(Shelf(c.getLong(0), c.getString(1), c.getInt(2))) } }

    fun addShelf(name: String) { writableDatabase.execSQL("INSERT INTO shelves(name) VALUES(?)", arrayOf(name)); bump() }
    fun deleteShelf(id: Long) {
        writableDatabase.execSQL("DELETE FROM shelf_items WHERE shelfId=?", arrayOf(id))
        writableDatabase.execSQL("DELETE FROM shelves WHERE id=?", arrayOf(id)); bump()
    }
    fun toggleShelfItem(shelfId: Long, fileId: Long) {
        val db = writableDatabase
        val exists = db.rawQuery("SELECT 1 FROM shelf_items WHERE shelfId=? AND fileId=?", arrayOf("$shelfId", "$fileId")).use { it.moveToFirst() }
        if (exists) db.execSQL("DELETE FROM shelf_items WHERE shelfId=? AND fileId=?", arrayOf(shelfId, fileId))
        else db.execSQL("INSERT INTO shelf_items VALUES(?,?)", arrayOf(shelfId, fileId))
        bump()
    }
    fun shelvesOf(fileId: Long): Set<Long> = readableDatabase.rawQuery(
        "SELECT shelfId FROM shelf_items WHERE fileId=?", arrayOf("$fileId")).use { c -> buildSet { while (c.moveToNext()) add(c.getLong(0)) } }

    // ───────── أدوات ─────────

    private fun query(sql: String, args: Array<String>): List<FileItem> = readableDatabase.rawQuery(sql, args).use { c ->
        buildList { while (c.moveToNext()) add(c.toFile()) }
    }

    private val Boolean.i get() = if (this) 1 else 0
    private fun Cursor.s(n: String) = getString(getColumnIndexOrThrow(n)) ?: ""
    private fun Cursor.l(n: String) = getLong(getColumnIndexOrThrow(n))
    private fun Cursor.n(n: String) = getInt(getColumnIndexOrThrow(n))

    private fun Cursor.toChannel() = Channel(
        id = l("id"), chatId = l("chatId"), title = s("title"), username = s("username"), members = n("members"),
        link = s("link"), kinds = s("kinds").split(',').filter { it.isNotBlank() }.map { Kind.of(it) }.toSet(),
        byHashtag = n("byHashtag") == 1, byYear = n("byYear") == 1, renameFromCaption = n("rename") == 1,
        autoUpdate = n("autoUpdate") == 1, keyword = s("keyword"), fromYear = n("fromYear"),
        storageUri = getString(getColumnIndexOrThrow("storage")), isProtected = n("protected") == 1,
        lastMsgId = l("lastMsgId"), blockedCount = n("blocked"), paused = n("paused") == 1,
        createdAt = l("createdAt"), lastSync = l("lastSync"),
    )

    private fun Cursor.toFile() = FileItem(
        id = l("id"), channelId = l("channelId"), chatId = l("chatId"), messageId = l("messageId"),
        kind = Kind.of(s("kind")), origName = s("origName"), displayName = s("displayName"), caption = s("caption"),
        section = s("section"), year = n("year"), date = l("date"), size = l("size"), mime = s("mime"),
        status = n("status"), uri = getString(getColumnIndexOrThrow("uri")), favorite = n("favorite") == 1,
        channelTitle = getColumnIndex("chTitle").let { if (it >= 0) getString(it) ?: "" else "" },
    )
}
