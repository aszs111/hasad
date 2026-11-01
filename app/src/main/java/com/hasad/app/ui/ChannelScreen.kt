package com.hasad.app.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hasad.app.data.Channel
import com.hasad.app.data.ChannelStats
import com.hasad.app.data.Db
import com.hasad.app.data.FileItem
import com.hasad.app.data.FileStatus
import com.hasad.app.data.Kind
import com.hasad.app.data.Library
import com.hasad.app.util.Fmt
import com.hasad.app.work.Jobs
import com.hasad.app.work.Progress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ChannelScreen(id: Long, onBack: () -> Unit) {
    val s = LocalS.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val ch by dbState<Channel?>(null, id) { it.channel(id) }
    val st by dbState(ChannelStats(0, 0, 0, 0, 0, 0), id) { it.stats(id) }
    val files by dbState(emptyList<FileItem>(), id) { it.recentFiles(id) }
    val progress by Progress.map.collectAsState()
    var confirmDelete by remember { mutableStateOf(false) }
    var editKinds by remember { mutableStateOf(false) }
    var syncing by remember { mutableStateOf(false) }

    // أول ما تفتح القناة: نتأكد هل المستخدم حذف ملفات من المجلد
    LaunchedEffect(id) { withContext(Dispatchers.IO) { Library.checkRemoved(ctx, id) } }

    val c = ch ?: return
    val db = Db.get(ctx)

    var picking by remember { mutableStateOf(false) }
    if (picking) {
        val all by dbState<List<FileItem>?>(null, id, "all") { it.allFiles(id) }
        val list = all ?: return
        val items = remember(list) { list.map {
            com.hasad.app.data.ScannedFile(it.messageId, it.kind, it.origName, it.caption, it.section, it.year, it.date, it.size, it.mime)
        } }
        val names = remember(list) { list.associate { it.messageId to it.displayName } }
        val done = remember(list) { list.filter { it.status == FileStatus.DONE }.map { it.messageId }.toSet() }
        val initial = remember(list) { list.filter { it.status != FileStatus.SKIPPED && it.status != FileStatus.REMOVED }.map { it.messageId }.toSet() }
        FilePicker(
            title = c.title, items = items, initial = initial, done = done,
            nameOf = { names[it.messageId] ?: it.origName },
            confirmLabel = { n, size -> s.saveSelection(n, size) },
            onBack = { picking = false },
            onConfirm = { sel ->
                picking = false
                scope.launch(Dispatchers.IO) { db.applySelection(id, sel); Jobs.startDownloads(ctx) }
            },
        )
        return
    }

    LazyColumn(Modifier.fillMaxSize(), state = rememberLazyListState()) {
        item {
            Row(Modifier.padding(top = 6.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null, tint = C.text) }
                Column(Modifier.weight(1f)) {
                    Text(c.title, color = C.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(listOfNotNull(c.username.takeIf { it.isNotBlank() }?.let { "@$it" },
                        if (c.members > 0) "${Fmt.num(c.members)} ${s.subscribers}" else null).joinToString(" · "), color = C.sub, fontSize = 11.sp)
                }
                Avatar(c.title, 42.dp)
            }
        }
        item {
            Column(Modifier.padding(horizontal = 18.dp)) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    if (st.done < st.total) BigNumber(Fmt.num(st.done), s.ofFiles(Fmt.num(st.total)))
                    else BigNumber(Fmt.num(st.done), s.filesOnShelf)
                    Spacer(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(s.sizeLbl, color = C.sub, fontSize = 10.sp)
                        Text(Fmt.size(st.bytesDone, s.ar), color = C.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Bookshelf(st.total, st.done, height = 120.dp, maxBooks = 44)
                Spacer(Modifier.height(10.dp))
                if (st.done < st.total && !c.paused && !c.isProtected) Note(Icons.Filled.Info, s.canClose)
                if (st.removed > 0) Note(Icons.Filled.Restore, s.removedNote(st.removed), s.restore) {
                    db.restoreRemoved(id); Jobs.startDownloads(ctx)
                }
                if (st.failed > 0) Note(Icons.Filled.ErrorOutline, s.failedNote(st.failed), s.retry) {
                    db.retryFailed(id); Jobs.startDownloads(ctx)
                }
                if (c.isProtected) Note(Icons.Filled.Info, s.protectedNote)

                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (c.paused) Pill(s.resume, icon = Icons.Filled.PlayArrow) { db.setPaused(id, false); Jobs.startDownloads(ctx) }
                    else Pill(s.pause, icon = Icons.Filled.Pause) { db.setPaused(id, true) }
                    Pill(if (syncing) "…" else s.updateNow, icon = Icons.Filled.Sync) {
                        if (!syncing) { syncing = true
                            scope.launch {
                                runCatching { withContext(Dispatchers.IO) { Library.sync(ctx, c) } }
                                Jobs.startDownloads(ctx); syncing = false
                            }
                        }
                    }
                    Pill(s.pickFilesShort, icon = Icons.Filled.Checklist) { picking = true }
                    Pill(s.edit, icon = Icons.Filled.Tune) { editKinds = true }
                    Pill(s.delete, icon = Icons.Filled.Delete) { confirmDelete = true }
                }
                HorizontalDivider(color = C.line)
            }
        }
        items(files, key = { it.id }) { f ->
            Column(Modifier.padding(horizontal = 14.dp)) {
                FileRow(f, s, progress = progress[f.id], onClick = { if (f.status == FileStatus.DONE) openFile(ctx, f) })
                HorizontalDivider(color = C.line.copy(alpha = 0.5f))
            }
        }
    }

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        text = { Text(s.deleteConfirm) },
        confirmButton = { TextButton(onClick = {
            confirmDelete = false
            scope.launch {
                withContext(Dispatchers.IO) { db.deleteChannel(id) }
                onBack()
            }
        }) { Text(s.delete, color = C.red) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(s.cancel) } },
        containerColor = C.card,
    )

    if (editKinds) EditKindsDialog(c, onDismiss = { editKinds = false }) { kinds ->
        editKinds = false
        scope.launch(Dispatchers.IO) {
            db.upsertChannel(c.copy(kinds = kinds))
            db.reapplyKinds(id, kinds)
            Jobs.startDownloads(ctx)
        }
    }
}

@Composable
private fun Note(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(15.dp), tint = C.sub)
        Spacer(Modifier.width(8.dp))
        Text(text, color = C.sub, fontSize = 12.sp, modifier = Modifier.weight(1f))
        if (action != null) TextButton(onClick = onAction) { Text(action, color = C.amber, fontSize = 12.sp) }
    }
}

@Composable
private fun EditKindsDialog(c: Channel, onDismiss: () -> Unit, onSave: (Set<Kind>) -> Unit) {
    val s = LocalS.current
    var kinds by remember { mutableStateOf(c.kinds) }
    val counts by dbState(emptyMap<Kind, Int>(), c.id) { db ->
        db.readableDatabase.rawQuery("SELECT kind, COUNT(*) FROM files WHERE channelId=? GROUP BY kind", arrayOf(c.id.toString())).use { cur ->
            buildMap { while (cur.moveToNext()) put(Kind.of(cur.getString(0)), cur.getInt(1)) }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.whatToDownload) },
        text = {
            Column {
                for (k in Kind.selectable) KindRow(s.kind(k), counts[k] ?: 0, "", k in kinds) { on -> kinds = if (on) kinds + k else kinds - k }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(kinds) }) { Text(s.save, color = C.amber) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
        containerColor = C.card,
    )
}
