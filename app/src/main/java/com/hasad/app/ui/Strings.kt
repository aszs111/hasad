package com.hasad.app.ui

import androidx.compose.runtime.compositionLocalOf
import com.hasad.app.data.Kind

/** نصوص الواجهة بالعربي والإنجليزي (زر EN / ع) */
class S(val ar: Boolean) {
    private fun t(a: String, e: String) = if (ar) a else e

    val appName = t("حصاد", "Hasad")
    val tagline = t("تجمع ملفات تيليجرام على جوالك", "Collect Telegram files on your phone")
    val channels = t("القنوات", "Channels")
    val library = t("المكتبة", "Library")
    val settings = t("الإعدادات", "Settings")
    val addChannel = t("أضف قناة", "Add channel")
    val addChannelHint = t("الصق رابط قناة عامة أو أي منشور فيها، أو رابط دعوة لقناتك الخاصة.", "Paste a public channel link, any post in it, or an invite link.")
    val scan = t("افحص", "Scan")
    val scanning = t("نفحص القناة…", "Scanning channel…")
    val filesFound = t("ملف مكتشف", "files found")
    val sizeLbl = t("الحجم", "Size")
    val since = t("من سنة", "Since")
    val subscribers = t("مشترك", "subscribers")
    val publicChannel = t("قناة عامة", "Public channel")
    val whatToDownload = t("وش تبي تنزّل؟", "What to download?")
    fun blockedNote(n: Int) = t("وفيه $n برامج (apk, exe) ما نجمعها، عشان سلامة جهازك", "$n programs (apk, exe) skipped for your device's safety")
    val saveLocation = t("مكان الحفظ", "Save location")
    val change = t("تغيير", "Change")
    val folderPerSection = t("مجلد لكل قسم (من هاشتاقات القناة وأسماء ملفاتها)", "Folder per section (from channel hashtags)")
    val folderPerYear = t("مجلد لكل سنة", "Folder per year")
    val renameFromCaption = t("إذا اسم الملف عام، سمّه من وصف الرسالة", "Name generic files from the message caption")
    val autoUpdate = t("حدّثها لحالها كل يوم", "Update automatically every day")
    val filter = t("فلتر (اختياري)", "Filter (optional)")
    val keywordHint = t("كلمات لازم تكون في الاسم أو الوصف (افصل بفاصلة)", "Words that must appear (comma separated)")
    val fromYear = t("من سنة", "From year")
    val start = t("ابدأ التجميع", "Start collecting")
    fun startN(n: Int, size: String) = t("جمّع $n ملف · $size", "Collect $n files · $size")
    val protectedNote = t("صاحبها مانع الحفظ — تيليجرام ما يسمح بتنزيل ملفاتها", "Saving is restricted by the owner")
    fun ofFiles(n: String) = t("من $n ملف", "of $n files")
    val filesOnShelf = t("ملف على رفّك", "files on your shelf")
    val canClose = t("تقدر تسكّر التطبيق، والتنزيل يكمل. حصاد يبقى في الإشعارات.", "You can close the app; downloading continues in the background.")
    fun removedNote(n: Int) = t("$n ملفات حذفتها أو نقلتها من المجلد، وما نرجّعها إلا إذا طلبت.", "$n files you removed won't come back unless you ask.")
    val restore = t("رجّعها", "Restore")
    fun failedNote(n: Int) = t("$n ملفات ما نزلت", "$n files failed")
    val retry = t("أعد المحاولة", "Retry")
    val pause = t("إيقاف مؤقت", "Pause")
    val resume = t("كمّل", "Resume")
    val updateNow = t("حدّث الحين", "Update now")
    val delete = t("حذف القناة", "Remove channel")
    val deleteConfirm = t("نشيل القناة من حصاد؟ الملفات المنزّلة تبقى في مجلدها.", "Remove the channel? Downloaded files stay in their folder.")
    val cancel = t("إلغاء", "Cancel")
    val ok = t("تمام", "OK")
    val edit = t("تعديل الاختيارات", "Edit options")
    val save = t("حفظ", "Save")
    val today = t("تحدّثت اليوم", "Updated today")
    val notYet = t("ما تحدّثت", "Not updated")
    val paused = t("متوقفة", "Paused")
    val downloading = t("ينزّل الحين", "Downloading")
    val searchHint = t("ابحث باسم الملف أو بكلمة من وصفه", "Search by file name or caption")
    val myShelves = t("رفوفك الخاصة", "Your shelves")
    val favorites = t("المفضلة", "Favorites")
    val newShelf = t("رف جديد", "New shelf")
    val shelfName = t("اسم الرف", "Shelf name")
    val sections = t("الأقسام", "Sections")
    val other = t("أخرى", "Other")
    fun libSummary(n: String, ch: Int, size: String) = t("$n ملف على رفّك من $ch قنوات · $size", "$n files from $ch channels · $size")
    val open = t("فتح", "Open")
    val share = t("مشاركة", "Share")
    val addToShelf = t("أضف لرف", "Add to shelf")
    val emptyChannels = t("ما أضفت قنوات للحين.\nاضغط \"أضف قناة\" والصق الرابط.", "No channels yet.\nTap \"Add channel\" and paste a link.")
    val emptyLibrary = t("المكتبة فاضية. أول ما تنزل الملفات تلقاها هنا.", "Library is empty. Downloaded files will show here.")
    val noResults = t("ما لقينا شي", "Nothing found")
    val all = t("الكل", "All")

    // تسجيل الدخول
    val loginTitle = t("سجّل دخولك في تيليجرام", "Sign in to Telegram")
    val loginNote = t("حصاد يقرأ القنوات بحسابك عشان يقدر ينزّل ملفاتها. ما نحفظ شي خارج جوالك.", "Hasad reads channels with your account. Nothing leaves your phone.")
    val phone = t("رقم الجوال مع مفتاح الدولة", "Phone number with country code")
    val code = t("الكود اللي وصلك في تيليجرام", "Code sent to your Telegram")
    val password = t("كلمة مرور التحقق بخطوتين", "Two-step verification password")
    val next = t("التالي", "Next")
    val keysTitle = t("مفاتيح تطبيق تيليجرام", "Telegram API keys")
    val keysNote = t("مرة وحدة فقط: ادخل my.telegram.org بحسابك ← API development tools ← أنشئ تطبيق، وانسخ api_id و api_hash هنا.",
        "One time: open my.telegram.org → API development tools → create an app, then copy api_id and api_hash here.")
    val openSite = t("افتح my.telegram.org", "Open my.telegram.org")
    val logout = t("تسجيل الخروج", "Log out")
    val account = t("الحساب", "Account")
    val language = t("اللغة", "Language")
    val wifiOnly = t("نزّل على الواي فاي فقط", "Download on Wi-Fi only")
    val defaultFolder = t("مجلد الحفظ الافتراضي", "Default save folder")
    val reset = t("رجّع الافتراضي", "Reset")
    val about = t("حصاد: يجمع ملفات قنوات تيليجرام (PDF وكتب وصوتيات وصور وفيديو)، يسمّيها صح، ويرتبها بأقسام، وتبحث فيها مثل المكتبة.",
        "Hasad collects Telegram channel files, names them properly, sorts them into sections, and lets you search them like a library.")
    val syncAll = t("حدّث كل القنوات الحين", "Sync all channels now")

    // اختيار الملفات
    val pickFiles = t("اختر الملفات بنفسك (بحث وفرز)", "Pick files yourself (search & sort)")
    val pickFilesShort = t("اختر الملفات", "Pick files")
    fun selectedSummary(n: String, size: String, total: String) = t("مختار $n من $total · $size", "$n of $total selected · $size")
    val pickSearchHint = t("ابحث بكلمة من الاسم أو الوصف", "Search name or caption")
    fun shownCount(n: String) = t("الظاهر: $n", "Shown: $n")
    val selectShown = t("اختر الظاهر", "Select shown")
    val clearShown = t("ألغِ الظاهر", "Clear shown")
    val onlySelected = t("المختار فقط", "Selected only")
    val alreadyDone = t("نزل قبل", "Downloaded")
    fun downloadSelected(n: Int, size: String) = t("نزّل المختار: $n ملف · $size", "Download selected: $n · $size")
    fun saveSelection(n: Int, size: String) = t("اعتمد الاختيار: $n ملف · $size", "Apply: $n files · $size")
    fun customSelection(n: String) = t("اخترت بنفسك $n ملف", "You picked $n files")
    val resetSelection = t("رجّع الاختيار حسب النوع", "Back to type-based selection")
    fun sortLabel(o: SortBy) = when (o) {
        SortBy.NEWEST -> t("الأحدث", "Newest"); SortBy.OLDEST -> t("الأقدم", "Oldest")
        SortBy.LARGEST -> t("الأكبر حجماً", "Largest"); SortBy.SMALLEST -> t("الأصغر حجماً", "Smallest")
        SortBy.NAME -> t("بالاسم", "By name")
    }
    fun sizeBucket(i: Int) = when (i) {
        0 -> t("أقل من 1 ميجا", "< 1 MB"); 1 -> t("1–10 ميجا", "1–10 MB")
        2 -> t("10–100 ميجا", "10–100 MB"); else -> t("أكبر من 100 ميجا", "> 100 MB")
    }
    fun kindShort(k: Kind) = when (k) {
        Kind.PDF -> "PDF"; Kind.WORD -> t("وورد", "Word"); Kind.EXCEL -> t("إكسل", "Excel")
        Kind.BOOK -> t("كتب", "Books"); Kind.AUDIO -> t("صوتيات", "Audio"); Kind.VOICE -> t("بصمات", "Voice")
        Kind.IMAGE -> t("صور", "Images"); Kind.VIDEO -> t("فيديو", "Video"); Kind.OTHER -> t("أخرى", "Other")
        Kind.BLOCKED -> t("برامج", "Programs")
    }

    fun kind(k: Kind) = when (k) {
        Kind.PDF -> t("ملفات PDF", "PDF files")
        Kind.WORD -> t("ملفات وورد", "Word files")
        Kind.EXCEL -> t("جداول إكسل", "Excel sheets")
        Kind.BOOK -> t("كتب إلكترونية", "E-books")
        Kind.AUDIO -> t("صوتيات ومقاطع", "Audio")
        Kind.VOICE -> t("بصمات", "Voice notes")
        Kind.IMAGE -> t("صور", "Images")
        Kind.VIDEO -> t("فيديو", "Video")
        Kind.OTHER -> t("ملفات ثانية", "Other files")
        Kind.BLOCKED -> t("برامج", "Programs")
    }

    fun badge(k: Kind, name: String) = when (k) {
        Kind.PDF -> "PDF"; Kind.WORD -> "DOC"; Kind.EXCEL -> "XLS"; Kind.IMAGE -> "IMG"; Kind.VIDEO -> "VID"
        Kind.AUDIO -> "MP3"; Kind.VOICE -> "VOC"
        else -> name.substringAfterLast('.', "FILE").uppercase().take(4)
    }
}

val LocalS = compositionLocalOf { S(true) }
