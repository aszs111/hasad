package com.hasad.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hasad.app.data.Channel
import com.hasad.app.data.ChannelStats
import com.hasad.app.util.Fmt
import com.hasad.app.work.Progress

@Composable
fun AppHeader(title: String, onToggleLang: (() -> Unit)? = null, sub: String? = null) {
    val s = LocalS.current
    Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 8.dp, top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = C.text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            if (sub != null) Text(sub, color = C.sub, fontSize = 12.sp)
        }
        if (onToggleLang != null) TextButton(onClick = onToggleLang) { Text(if (s.ar) "EN" else "ع", color = C.sub) }
    }
}

@Composable
fun ChannelsScreen(onOpen: (Long) -> Unit, onAdd: () -> Unit, onToggleLang: () -> Unit) {
    val s = LocalS.current
    val data by dbState(emptyList<Pair<Channel, ChannelStats>>()) { db -> db.channels().map { it to db.stats(it.id) } }
    val progress by Progress.map.collectAsState()

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.padding(start = 18.dp, end = 8.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(s.appName, color = C.amber, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(8.dp))
                Bookshelf(6, 6, Modifier.width(34.dp), height = 26.dp, maxBooks = 6)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onToggleLang) { Text(if (s.ar) "EN" else "ع", color = C.sub) }
            }
            if (data.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Bookshelf(10, 0, Modifier.width(160.dp), height = 70.dp, maxBooks = 10)
                        Spacer(Modifier.height(16.dp))
                        Text(s.emptyChannels, color = C.sub, textAlign = TextAlign.Center)
                    }
                }
            } else {
                val totalDone = data.sumOf { it.second.done }
                val totalBytes = data.sumOf { it.second.bytesDone }
                Column(Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
                    BigNumber(Fmt.num(totalDone), s.filesOnShelf)
                    Text(Fmt.size(totalBytes, s.ar), color = C.sub, fontSize = 12.sp)
                }
                LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp, 8.dp, 14.dp, 90.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(data, key = { it.first.id }) { (ch, st) ->
                        val active = progress.keys.isNotEmpty() && st.done < st.total && !ch.paused
                        ChannelCard(ch, st, active, s) { onOpen(ch.id) }
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = onAdd, containerColor = C.amber, contentColor = C.bg,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            icon = { Icon(Icons.Filled.Add, null) }, text = { Text(s.addChannel, fontWeight = FontWeight.Bold) },
        )
    }
}

@Composable
private fun ChannelCard(ch: Channel, st: ChannelStats, active: Boolean, s: S, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(ch.title, 40.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(ch.title, color = C.text, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val status = when {
                        ch.isProtected -> s.protectedNote
                        ch.paused -> s.paused
                        active -> s.downloading
                        System.currentTimeMillis() - ch.lastSync < 86_400_000 -> s.today
                        else -> s.notYet
                    }
                    Text("${Fmt.num(st.done)} / ${Fmt.num(st.total)} · ${Fmt.size(st.bytesDone, s.ar)} · $status",
                        color = if (active) C.amber else C.sub, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (ch.isProtected) Icon(Icons.Filled.Lock, null, Modifier.size(16.dp), tint = C.sub)
            }
            Spacer(Modifier.height(10.dp))
            Bookshelf(st.total, st.done, height = 48.dp, maxBooks = 34)
        }
    }
}
