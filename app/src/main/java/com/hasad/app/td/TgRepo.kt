package com.hasad.app.td

import com.hasad.app.data.Kind
import com.hasad.app.data.ScanResult
import com.hasad.app.data.ScannedFile
import com.hasad.app.util.Fmt
import com.hasad.app.util.Kinds
import com.hasad.app.util.Naming
import com.hasad.app.util.TgLink
import org.drinkless.tdlib.TdApi

/** كل العمليات اللي تكلّم تيليجرام: فتح الرابط، فحص القناة، جلب الملف */
object TgRepo {

    class ResolveException(msg: String) : Exception(msg)

    suspend fun resolve(link: String, allowJoin: Boolean = true): TdApi.Chat {
        return when (val l = TgLink.parse(link)) {
            is TgLink.Public -> try {
                Td.send(TdApi.SearchPublicChat().apply { username = l.username })
            } catch (e: TdException) {
                throw ResolveException("ما لقينا قناة بهذا الاسم (@${l.username})")
            }
            is TgLink.PrivatePost -> {
                val info = Td.send(TdApi.GetMessageLinkInfo().apply { url = l.url })
                if (info.chatId == 0L) throw ResolveException("لازم تكون عضو في هذي القناة الخاصة")
                Td.send(TdApi.GetChat().apply { chatId = info.chatId })
            }
            is TgLink.Invite -> {
                var info = Td.send(TdApi.CheckChatInviteLink().apply { inviteLink = l.link })
                if (info.chatId == 0L || info.accessibleFor > 0) {
                    if (!allowJoin) throw ResolveException("رابط دعوة: لازم ننضم للقناة أولاً")
                    val r = Td.send(TdApi.JoinChatByInviteLink().apply { inviteLink = l.link })
                    if (r is TdApi.ChatJoinResultSuccess) return Td.send(TdApi.GetChat().apply { chatId = r.chatId })
                    info = Td.send(TdApi.CheckChatInviteLink().apply { inviteLink = l.link })
                    if (info.chatId == 0L) throw ResolveException("تم إرسال طلب انضمام، انتظر موافقة المشرف")
                }
                Td.send(TdApi.GetChat().apply { chatId = info.chatId })
            }
            TgLink.Invalid -> throw ResolveException("الرابط غير صحيح. مثال: https://t.me/اسم_القناة")
        }
    }

    data class ChatMeta(val username: String, val members: Int)

    suspend fun meta(chat: TdApi.Chat): ChatMeta {
        val t = chat.type
        if (t is TdApi.ChatTypeSupergroup) {
            val sg = runCatching { Td.send(TdApi.GetSupergroup().apply { supergroupId = t.supergroupId }) }.getOrNull()
            val full = runCatching { Td.send(TdApi.GetSupergroupFullInfo().apply { supergroupId = t.supergroupId }) }.getOrNull()
            val user = sg?.usernames?.activeUsernames?.firstOrNull() ?: sg?.usernames?.editableUsername ?: ""
            return ChatMeta(user, full?.memberCount?.takeIf { it > 0 } ?: sg?.memberCount ?: 0)
        }
        return ChatMeta("", 0)
    }

    private val filters: List<() -> TdApi.SearchMessagesFilter> = listOf(
        { TdApi.SearchMessagesFilterDocument() },
        { TdApi.SearchMessagesFilterAudio() },
        { TdApi.SearchMessagesFilterVoiceNote() },
        { TdApi.SearchMessagesFilterPhoto() },
        { TdApi.SearchMessagesFilterVideo() },
    )

    /**
     * يمرّ على كل رسائل الملفات في القناة. لو [afterMsgId] > 0 يوقف عند الرسائل القديمة (تحديث يومي).
     * [onProgress] يستقبل عدد الملفات اللي انقرت لين الحين.
     */
    suspend fun scan(chat: TdApi.Chat, afterMsgId: Long = 0, onProgress: (Int) -> Unit = {}): ScanResult {
        val meta = meta(chat)
        val items = LinkedHashMap<Long, ScannedFile>()
        var blocked = 0
        var maxId = afterMsgId
        for (mk in filters) {
            var from = 0L
            loop@ while (true) {
                val req = TdApi.SearchChatMessages().apply {
                    chatId = chat.id; query = ""; fromMessageId = from; offset = 0; limit = 100; filter = mk()
                }
                val res = Td.send(req)
                if (res.messages.isEmpty()) break
                for (m in res.messages) {
                    if (m.id <= afterMsgId) break@loop
                    if (m.id > maxId) maxId = m.id
                    val f = parse(m) ?: continue
                    if (f.kind == Kind.BLOCKED) { if (!items.containsKey(m.id)) blocked++; continue }
                    items[m.id] = f
                }
                onProgress(items.size)
                from = if (res.nextFromMessageId != 0L) res.nextFromMessageId else res.messages.last().id
                if (res.nextFromMessageId == 0L && res.messages.size < 100) break
            }
        }
        val list = items.values.sortedBy { it.messageId }
        val byKind = list.groupingBy { it.kind }.eachCount()
        val bytesByKind = list.groupBy { it.kind }.mapValues { (_, v) -> v.sumOf { it.size } }
        val sections = list.filter { it.section.isNotBlank() }.groupingBy { it.section }.eachCount()
        return ScanResult(
            chatId = chat.id, title = chat.title, username = meta.username, members = meta.members,
            isProtected = chat.hasProtectedContent, totalFiles = list.size, totalBytes = list.sumOf { it.size },
            sinceYear = list.minOfOrNull { it.year } ?: 0, countByKind = byKind, bytesByKind = bytesByKind,
            blocked = blocked, sections = sections, items = list, maxMsgId = maxId,
        )
    }

    private fun parse(m: TdApi.Message): ScannedFile? {
        val c = m.content
        var caption = ""
        val (kind, name, file, mime) = when (c) {
            is TdApi.MessageDocument -> { caption = c.caption?.text ?: ""
                Quad(Kinds.ofDocument(c.document.fileName ?: "", c.document.mimeType ?: ""), c.document.fileName ?: "", c.document.document, c.document.mimeType ?: "") }
            is TdApi.MessageAudio -> { caption = c.caption?.text ?: ""
                val n = c.audio.fileName?.takeIf { it.isNotBlank() }
                    ?: listOf(c.audio.performer, c.audio.title).filter { !it.isNullOrBlank() }.joinToString(" - ").ifBlank { "صوتية_${m.id}" } + ".mp3"
                Quad(Kind.AUDIO, n, c.audio.audio, c.audio.mimeType ?: "audio/mpeg") }
            is TdApi.MessageVoiceNote -> { caption = c.caption?.text ?: ""
                Quad(Kind.VOICE, "بصمة_${m.id}.ogg", c.voiceNote.voice, c.voiceNote.mimeType ?: "audio/ogg") }
            is TdApi.MessagePhoto -> { caption = c.caption?.text ?: ""
                val best = c.photo.sizes.maxByOrNull { it.width * it.height } ?: return null
                Quad(Kind.IMAGE, "صورة_${m.id}.jpg", best.photo, "image/jpeg") }
            is TdApi.MessageVideo -> { caption = c.caption?.text ?: ""
                Quad(Kind.VIDEO, c.video.fileName?.takeIf { it.isNotBlank() } ?: "فيديو_${m.id}.mp4", c.video.video, c.video.mimeType ?: "video/mp4") }
            else -> return null
        }
        val size = if (file.size > 0) file.size else file.expectedSize
        val date = m.date.toLong()
        return ScannedFile(
            messageId = m.id, kind = kind, origName = name, caption = caption,
            section = Naming.hashtags(caption).firstOrNull() ?: "", year = Fmt.yearOf(date), date = date,
            size = size, mime = mime,
        )
    }

    private data class Quad(val kind: Kind, val name: String, val file: TdApi.File, val mime: String)

    /** يجيب ملف الرسالة من جديد (معرّفات الملفات ممكن تتغير) */
    suspend fun fileOf(chatId: Long, messageId: Long): TdApi.File? {
        val m = Td.send(TdApi.GetMessage().apply { this.chatId = chatId; this.messageId = messageId })
        return when (val c = m.content) {
            is TdApi.MessageDocument -> c.document.document
            is TdApi.MessageAudio -> c.audio.audio
            is TdApi.MessageVoiceNote -> c.voiceNote.voice
            is TdApi.MessagePhoto -> c.photo.sizes.maxByOrNull { it.width * it.height }?.photo
            is TdApi.MessageVideo -> c.video.video
            else -> null
        }
    }

    suspend fun download(fileId: Int): TdApi.File =
        Td.send(TdApi.DownloadFile().apply { this.fileId = fileId; priority = 16; offset = 0; limit = 0; synchronous = true })

    suspend fun deleteCached(fileId: Int) { runCatching { Td.send(TdApi.DeleteFile().apply { this.fileId = fileId }) } }
    suspend fun cancel(fileId: Int) { runCatching { Td.send(TdApi.CancelDownloadFile().apply { this.fileId = fileId; onlyIfPending = false }) } }
}
