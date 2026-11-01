package com.hasad.app.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hasad.app.data.Kind
import com.hasad.app.data.ScannedFile
import com.hasad.app.util.Arabic
import com.hasad.app.util.Fmt

enum class SortBy { NEWEST, OLDEST, LARGEST, SMALLEST, NAME }

/** شرائح الحجم للفلترة */
private enum class SizeBucket(val min: Long, val max: Long) {
    TINY(0, 1L shl 20), SMALL(1L shl 20, 10L shl 20), MEDIUM(10L shl 20, 100L shl 20), LARGE(100L shl 20, Long.MAX_VALUE)
}

/**
 * شاشة اختيار الملفات قبل التنزيل: بحث بالكلمات، فرز بالتاريخ/الحجم/الاسم،
 * فلترة بالنوع والحجم والسنة والقسم، واختيار ملف ملف أو الكل.
 * [done] ملفات نزلت قبل (تظهر مقفلة).
 */
@Composable
fun FilePicker(
    title: String,
    items: List<ScannedFile>,
    initial: Set<Long>,
    done: Set<Long> = emptySet(),
    nameOf: (ScannedFile) -> String,
    confirmLabel: (Int, String) -> String,
    onBack: () -> Unit,
    onConfirm: (Set<Long>) -> Unit,
) {
    val s = LocalS.current
    BackHandler { onBack() }

    var selected by remember { mutableStateOf(initial) }
    var q by remember { mutableStateOf("") }
    var kindF by remember { mutableStateOf(emptySet<Kind>()) }
    var sizeF by remember { mutableStateOf<SizeBucket?>(null) }
    var yearF by remember { mutableStateOf<Int?>(null) }
    var sectionF by remember { mutableStateOf<String?>(null) }
    var onlySelected by remember { mutableStateOf(false) }
    var sort by remember { mutableStateOf(SortBy.NEWEST) }
    var sortMenu by remember { mutableStateOf(false) }

    // النص الموحّد لكل ملف للبحث السريع
    val norm = remember(items) { items.associate { it.messageId to Arabic.normalize("${nameOf(it)} ${it.origName} ${it.caption}") } }
    val kinds = remember(items) { items.groupingBy { it.kind }.eachCount() }
    val years = remember(items) { items.map { it.year }.filter { it > 0 }.distinct().sortedDescending() }
    val sections = remember(items) { items.filter { it.section.isNotBlank() }.groupingBy { it.section }.eachCount().entries.sortedByDescending { it.value } }

    val words = Arabic.normalize(q).split(' ').filter { it.isNotBlank() }
    val visible = remember(items, q, kindF, sizeF, yearF, sectionF, onlySelected, sort, if (onlySelected) selected else null) {
        items.asSequence()
            .filter { f -> words.all { w -> norm[f.messageId]!!.contains(w) } }
            .filter { kindF.isEmpty() || it.kind in kindF }
            .filter { sizeF == null || it.size in sizeF!!.min until sizeF!!.max }
            .filter { yearF == null || it.year == yearF }
            .filter { sectionF == null || it.section == sectionF }
            .filter { !onlySelected || it.messageId in selected }
            .let { seq ->
                when (sort) {
                    SortBy.NEWEST -> seq.sortedByDescending { it.date }
                    SortBy.OLDEST -> seq.sortedBy { it.date }
                    SortBy.LARGEST -> seq.sortedByDescending { it.size }
                    SortBy.SMALLEST -> seq.sortedBy { it.size }
                    SortBy.NAME -> seq.sortedBy { nameOf(it) }
                }
            }.toList()
    }
    val byId = remember(items) { items.associateBy { it.messageId } }
    val selCount = selected.count { it !in done }
    val selBytes = selected.filter { it !in done }.sumOf { byId[it]?.size ?: 0L }
    val visibleIds = visible.map { it.messageId }.filter { it !in done }

    val fieldColors = OutlinedTextFieldDefaults.colors(focusedBorderColor = C.amber, unfocusedBorderColor = C.line, cursorColor = C.amber)

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(top = 6.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null, tint = C.text) }
            Column(Modifier.weight(1f)) {
                Text(title, color = C.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(s.selectedSummary(Fmt.num(selCount), Fmt.size(selBytes, s.ar), Fmt.num(items.size)), color = C.amber, fontSize = 12.sp)
            }
        }
        OutlinedTextField(
            q, { q = it }, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), singleLine = true,
            placeholder = { Text(s.pickSearchHint, color = C.dim) }, colors = fieldColors, shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Filled.Search, null, tint = C.amber) },
            trailingIcon = { if (q.isNotEmpty()) IconButton(onClick = { q = "" }) { Icon(Icons.Filled.Close, null, tint = C.sub) } },
        )
        // الفلاتر
        LazyRow(Modifier.padding(top = 8.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                Box {
                    Pill(s.sortLabel(sort), true, Icons.Filled.Sort) { sortMenu = true }
                    DropdownMenu(sortMenu, { sortMenu = false }, containerColor = C.card) {
                        SortBy.entries.forEach { o -> DropdownMenuItem(text = { Text((if (o == sort) "✓ " else "") + s.sortLabel(o)) }, onClick = { sort = o; sortMenu = false }) }
                    }
                }
            }
            items(Kind.selectable.filter { (kinds[it] ?: 0) > 0 }) { k ->
                Pill("${s.kindShort(k)} ${kinds[k]}", k in kindF) { kindF = if (k in kindF) kindF - k else kindF + k }
            }
        }
        LazyRow(Modifier.padding(top = 6.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item { Pill(s.onlySelected, onlySelected) { onlySelected = !onlySelected } }
            items(SizeBucket.entries) { b -> Pill(s.sizeBucket(b.ordinal), sizeF == b) { sizeF = if (sizeF == b) null else b } }
            if (years.size > 1) items(years) { y -> Pill(y.toString(), yearF == y) { yearF = if (yearF == y) null else y } }
            items(sections.take(30)) { (name, n) -> Pill("#$name $n", sectionF == name) { sectionF = if (sectionF == name) null else name } }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(s.shownCount(Fmt.num(visible.size)), color = C.sub, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Pill(s.selectShown, icon = Icons.Filled.CheckCircle) { selected = selected + visibleIds }
            Spacer(Modifier.width(6.dp))
            Pill(s.clearShown, icon = Icons.Filled.RemoveCircleOutline) { selected = selected - visibleIds.toSet() }
        }
        HorizontalDivider(color = C.line)

        LazyColumn(Modifier.weight(1f)) {
            items(visible, key = { it.messageId }) { f ->
                val isDone = f.messageId in done
                val on = isDone || f.messageId in selected
                Row(
                    Modifier.fillMaxWidth().clickable(enabled = !isDone) { selected = if (on) selected - f.messageId else selected + f.messageId }
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(on, { selected = if (it) selected + f.messageId else selected - f.messageId }, enabled = !isDone,
                        colors = CheckboxDefaults.colors(checkedColor = C.amber, checkmarkColor = Color(0xFF231A0C), uncheckedColor = C.dim,
                            disabledCheckedColor = C.green.copy(alpha = 0.6f)))
                    val name = nameOf(f)
                    Badge(s.badge(f.kind, name))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(highlighted(name.substringBeforeLast('.'), q), color = if (on) C.text else C.sub, fontSize = 14.sp,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(2.dp))
                        Text(listOfNotNull(Fmt.date(f.date, s.ar), Fmt.size(f.size, s.ar), f.section.ifBlank { null },
                            if (isDone) s.alreadyDone else null).joinToString(" · "),
                            color = if (isDone) C.green else C.sub, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                HorizontalDivider(color = C.line.copy(alpha = 0.4f), modifier = Modifier.padding(start = 56.dp))
            }
        }
        Button(
            onClick = { onConfirm(selected) }, colors = ButtonDefaults.buttonColors(C.amber), shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(12.dp).height(52.dp),
        ) { Text(confirmLabel(selCount, Fmt.size(selBytes, s.ar)), fontWeight = FontWeight.Bold, fontSize = 15.sp) }
        Spacer(Modifier.size(0.dp))
    }
}
