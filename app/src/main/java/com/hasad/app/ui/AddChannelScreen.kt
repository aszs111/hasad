package com.hasad.app.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hasad.app.data.Channel
import com.hasad.app.data.Kind
import com.hasad.app.data.Library
import com.hasad.app.data.Prefs
import com.hasad.app.data.ScanResult
import com.hasad.app.data.Storage
import com.hasad.app.td.TgRepo
import com.hasad.app.util.Fmt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AddChannelScreen(initialLink: String, onBack: () -> Unit, onDone: (Long) -> Unit) {
    val s = LocalS.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    var link by remember { mutableStateOf(initialLink) }
    var scanning by remember { mutableStateOf(false) }
    var found by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<ScanResult?>(null) }

    // الخيارات
    var kinds by remember { mutableStateOf(Kind.defaults) }
    var storage by remember { mutableStateOf(Prefs.defaultStorage(ctx)) }
    var byHashtag by remember { mutableStateOf(false) }
    var byYear by remember { mutableStateOf(true) }
    var rename by remember { mutableStateOf(true) }
    var auto by remember { mutableStateOf(true) }
    var showFilter by remember { mutableStateOf(false) }
    var keyword by remember { mutableStateOf("") }
    var fromYear by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            storage = uri.toString()
        }
    }

    fun doScan() {
        if (link.isBlank() || scanning) return
        scanning = true; error = null; result = null; found = 0
        scope.launch {
            try {
                val chat = TgRepo.resolve(link)
                val r = TgRepo.scan(chat) { n -> found = n }
                result = r
                kinds = Kind.defaults.filter { (r.countByKind[it] ?: 0) > 0 }.toSet()
                    .ifEmpty { Kind.selectable.filter { (r.countByKind[it] ?: 0) > 0 }.toSet() }
                byHashtag = r.sections.size >= 2
            } catch (e: Exception) {
                error = e.message ?: "تعذّر الفحص"
            }
            scanning = false
        }
    }

    LaunchedEffect(Unit) { if (initialLink.isNotBlank()) doScan() }

    val fieldColors = OutlinedTextFieldDefaults.colors(focusedBorderColor = C.amber, unfocusedBorderColor = C.line, cursorColor = C.amber)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null, tint = C.text) }
            Text(s.addChannel, color = C.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Text(s.addChannelHint, color = C.sub, fontSize = 12.sp, modifier = Modifier.padding(start = 12.dp, bottom = 10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                link, { link = it }, modifier = Modifier.weight(1f), singleLine = true, colors = fieldColors,
                placeholder = { Text("https://t.me/…", color = C.dim) },
                textStyle = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.Ltr, color = C.text, fontSize = 14.sp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { doScan() }),
                trailingIcon = {
                    IconButton(onClick = { clipboard.getText()?.text?.let { link = it.trim() } }) { Icon(Icons.Filled.ContentPaste, null, tint = C.sub) }
                },
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { doScan() }, enabled = !scanning && link.isNotBlank(), colors = ButtonDefaults.buttonColors(C.amber),
                shape = RoundedCornerShape(12.dp), modifier = Modifier.height(54.dp)) { Text(s.scan, fontWeight = FontWeight.Bold) }
        }

        if (scanning) {
            Row(Modifier.padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp), color = C.amber, strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("${s.scanning} ${if (found > 0) Fmt.num(found) + " " + s.filesFound else ""}", color = C.sub, fontSize = 13.sp)
            }
        }
        error?.let { Text(it, color = C.red, modifier = Modifier.padding(vertical = 14.dp)) }

        val r = result ?: return@Column
        Spacer(Modifier.height(16.dp))

        // ─── بطاقة القناة ───
        Card(Modifier.fillMaxWidth()) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(r.title, 46.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(r.title, color = C.text, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        val sub = listOfNotNull(
                            r.username.takeIf { it.isNotBlank() }?.let { "@$it" },
                            if (r.username.isNotBlank()) s.publicChannel else null,
                            if (r.members > 0) "${Fmt.num(r.members)} ${s.subscribers}" else null,
                        ).joinToString(" · ")
                        Text(sub, color = C.sub, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    Stat(Fmt.num(r.totalFiles), s.filesFound, highlight = true)
                    val (v, u) = Fmt.sizeParts(r.totalBytes, s.ar)
                    Stat(v, u)
                    if (r.sinceYear > 0) Stat(r.sinceYear.toString(), s.since)
                }
            }
        }

        if (r.isProtected) {
            Spacer(Modifier.height(12.dp))
            Card(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Lock, null, tint = C.amber, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp)); Text(s.protectedNote, color = C.text, fontSize = 13.sp)
                }
            }
            return@Column
        }

        // ─── وش تبي تنزّل؟ ───
        SectionTitle(s.whatToDownload)
        Card(Modifier.fillMaxWidth(), padding = 6.dp) {
            Column {
                for (k in Kind.selectable) {
                    KindRow(s.kind(k), r.countByKind[k] ?: 0, Fmt.size(r.bytesByKind[k] ?: 0, s.ar), k in kinds) { on ->
                        kinds = if (on) kinds + k else kinds - k
                    }
                }
                if (r.blocked > 0) Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Shield, null, Modifier.size(14.dp), tint = C.sub); Spacer(Modifier.width(6.dp))
                    Text(s.blockedNote(r.blocked), color = C.sub, fontSize = 11.sp)
                }
            }
        }

        // ─── مكان الحفظ والخيارات ───
        SectionTitle(s.saveLocation)
        Card(Modifier.fillMaxWidth()) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Folder, null, Modifier.size(18.dp), tint = C.sub); Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(Storage.describe(ctx, storage) + " › " + r.title, color = C.text, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    TextButton(onClick = { picker.launch(null) }) { Text(s.change, color = C.amber) }
                }
                ToggleRow(Icons.Filled.Tag, s.folderPerSection + if (r.sections.isNotEmpty()) " (${r.sections.size})" else "", byHashtag) { byHashtag = it }
                ToggleRow(Icons.Filled.CalendarMonth, s.folderPerYear, byYear) { byYear = it }
                ToggleRow(Icons.Filled.DriveFileRenameOutline, s.renameFromCaption, rename) { rename = it }
                ToggleRow(Icons.Filled.Sync, s.autoUpdate, auto) { auto = it }
            }
        }

        // ─── فلتر ───
        Row(Modifier.fillMaxWidth().clickable { showFilter = !showFilter }.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.FilterList, null, Modifier.size(18.dp), tint = C.sub); Spacer(Modifier.width(8.dp))
            Text(s.filter, color = C.sub, fontSize = 13.sp)
        }
        if (showFilter) {
            OutlinedTextField(keyword, { keyword = it }, label = { Text(s.keywordHint) }, modifier = Modifier.fillMaxWidth(), colors = fieldColors, singleLine = true)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(fromYear, { fromYear = it.filter { c -> c.isDigit() }.take(4) }, label = { Text(s.fromYear) },
                modifier = Modifier.fillMaxWidth(), colors = fieldColors, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Spacer(Modifier.height(8.dp))
        }

        val selCount = kinds.sumOf { r.countByKind[it] ?: 0 }
        val selBytes = kinds.sumOf { r.bytesByKind[it] ?: 0L }
        Button(
            onClick = {
                val opts = Channel(
                    chatId = r.chatId, title = r.title, username = r.username, members = r.members, link = link.trim(),
                    kinds = kinds, byHashtag = byHashtag, byYear = byYear, renameFromCaption = rename, autoUpdate = auto,
                    keyword = keyword.trim(), fromYear = fromYear.toIntOrNull() ?: 0, storageUri = storage,
                    isProtected = false, lastMsgId = 0, blockedCount = 0, paused = false, createdAt = 0, lastSync = 0,
                )
                scope.launch {
                    val id = withContext(Dispatchers.IO) { Library.addChannel(ctx, link.trim(), r, opts) }
                    onDone(id)
                }
            },
            enabled = kinds.isNotEmpty(), colors = ButtonDefaults.buttonColors(C.amber), shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text(s.startN(selCount, Fmt.size(selBytes, s.ar)), fontWeight = FontWeight.Bold, fontSize = 15.sp) }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun Stat(value: String, label: String, highlight: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = if (highlight) C.amber else C.text, fontSize = 26.sp, fontWeight = FontWeight.Black)
        Text(label, color = C.sub, fontSize = 11.sp)
    }
}
