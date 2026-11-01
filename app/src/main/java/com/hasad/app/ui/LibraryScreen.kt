package com.hasad.app.ui

import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hasad.app.data.Channel
import com.hasad.app.data.ChannelStats
import com.hasad.app.data.Db
import com.hasad.app.data.FileItem
import com.hasad.app.data.Shelf
import com.hasad.app.util.Fmt

@Composable
fun LibraryScreen() {
    val s = LocalS.current
    val ctx = LocalContext.current
    val db = Db.get(ctx)

    var q by remember { mutableStateOf("") }
    var section by remember { mutableStateOf<String?>(null) }
    var channelId by remember { mutableStateOf<Long?>(null) }
    var shelfId by remember { mutableStateOf<Long?>(null) }
    var favs by remember { mutableStateOf(false) }
    var newShelf by remember { mutableStateOf(false) }

    val totals by dbState(Triple(0, 0L, 0)) { it.libraryTotals() }
    val sections by dbState(emptyList<Pair<String, Int>>()) { it.sections() }
    val shelves by dbState(emptyList<Shelf>()) { it.shelves() }
    val favCount by dbState(0) { it.favoritesCount() }
    val channels by dbState(emptyList<Pair<Channel, ChannelStats>>()) { d -> d.channels().map { it to d.stats(it.id) } }
    val results by dbState(emptyList<FileItem>(), q, section, channelId, shelfId, favs) { it.search(q, section, channelId, shelfId, favs) }

    val fieldColors = OutlinedTextFieldDefaults.colors(focusedBorderColor = C.amber, unfocusedBorderColor = C.amber.copy(alpha = 0.5f), cursorColor = C.amber)
    val filtered = q.isNotBlank() || section != null || channelId != null || shelfId != null || favs

    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)) {
        item {
            AppHeader(s.library, sub = s.libSummary(Fmt.num(totals.first), totals.third, Fmt.size(totals.second, s.ar)))
            OutlinedTextField(
                q, { q = it }, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), singleLine = true,
                placeholder = { Text(s.searchHint, color = C.dim) }, colors = fieldColors, shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = C.amber) },
                trailingIcon = { if (q.isNotEmpty()) IconButton(onClick = { q = "" }) { Icon(Icons.Filled.Close, null, tint = C.sub) } },
            )
        }
        item {
            Column(Modifier.padding(horizontal = 14.dp)) {
                SectionTitle(s.myShelves)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { Pill("${s.favorites} $favCount", favs, Icons.Filled.Star) { favs = !favs; shelfId = null } }
                    items(shelves, key = { it.id }) { sh -> Pill("${sh.name} ${sh.count}", shelfId == sh.id, Icons.Filled.Bookmark) { shelfId = if (shelfId == sh.id) null else sh.id; favs = false } }
                    item { Pill(s.newShelf, icon = Icons.Filled.Add) { newShelf = true } }
                }
                if (sections.isNotEmpty()) {
                    SectionTitle(s.sections)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(sections) { (name, n) ->
                            Pill("${name.ifBlank { s.other }} ${Fmt.num(n)}", section == name) { section = if (section == name) null else name }
                        }
                    }
                }
                if (channels.size > 1) {
                    SectionTitle(s.channels)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(channels, key = { it.first.id }) { (ch, st) ->
                            val sel = channelId == ch.id
                            Card(Modifier.width(150.dp).then(if (sel) Modifier.border(1.5.dp, C.amber, RoundedCornerShape(14.dp)) else Modifier)
                                .clickable { channelId = if (sel) null else ch.id }, padding = 10.dp) {
                                Column {
                                    Bookshelf(st.total, st.done, height = 40.dp, maxBooks = 16)
                                    Spacer(Modifier.height(6.dp))
                                    Text(ch.title, color = C.text, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${Fmt.num(st.done)} · ${Fmt.size(st.bytesDone, s.ar)}", color = C.sub, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = C.line)
            }
        }
        if (results.isEmpty()) item {
            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                Text(if (filtered) s.noResults else s.emptyLibrary, color = C.sub, textAlign = TextAlign.Center)
            }
        }
        items(results, key = { it.id }) { f ->
            Column(Modifier.padding(horizontal = 14.dp)) {
                FileRow(f, s, highlight = q, showChannel = true, onClick = { openFile(ctx, f) }) {
                    var menu by remember { mutableStateOf(false) }
                    IconButton(onClick = { db.toggleFavorite(f.id) }) {
                        Icon(if (f.favorite) Icons.Filled.Star else Icons.Filled.StarBorder, null, tint = if (f.favorite) C.amber else C.sub, modifier = Modifier.size(20.dp))
                    }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Filled.BookmarkBorder, null, tint = C.sub, modifier = Modifier.size(20.dp)) }
                        DropdownMenu(menu, { menu = false }, containerColor = C.card) {
                            val mine = remember(menu) { db.shelvesOf(f.id) }
                            shelves.forEach { sh ->
                                DropdownMenuItem(text = { Text((if (sh.id in mine) "✓ " else "") + sh.name) }, onClick = { db.toggleShelfItem(sh.id, f.id); menu = false })
                            }
                            DropdownMenuItem(text = { Text(s.share) }, leadingIcon = { Icon(Icons.Filled.Share, null) }, onClick = { menu = false; shareFile(ctx, f) })
                        }
                    }
                }
                HorizontalDivider(color = C.line.copy(alpha = 0.5f))
            }
        }
    }

    if (newShelf) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { newShelf = false },
            title = { Text(s.newShelf) },
            text = { OutlinedTextField(name, { name = it }, label = { Text(s.shelfName) }, singleLine = true, colors = fieldColors) },
            confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { db.addShelf(name.trim()); newShelf = false }) { Text(s.save, color = C.amber) } },
            dismissButton = { TextButton(onClick = { newShelf = false }) { Text(s.cancel) } },
            containerColor = C.card,
        )
    }
}
