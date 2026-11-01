package com.hasad.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hasad.app.data.FileItem
import com.hasad.app.data.FileStatus
import com.hasad.app.util.Arabic
import com.hasad.app.util.Fmt
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * رف الكتب: كل عمود يمثّل مجموعة ملفات. الكهرماني = نزل، الباهت = باقي.
 * [total] عدد الملفات، [done] المنزّل.
 */
@Composable
fun Bookshelf(total: Int, done: Int, modifier: Modifier = Modifier, height: Dp = 110.dp, maxBooks: Int = 46) {
    val n = if (total <= 0) maxBooks else min(maxBooks, max(total, 8))
    val target = if (total <= 0) 0f else done.toFloat() / total
    val frac by animateFloatAsState(target, tween(700), label = "shelf")
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Canvas(modifier.fillMaxWidth().height(height)) {
        val shelfH = 5.dp.toPx()
        val gap = 2.dp.toPx()
        val bw = (size.width - gap * (n - 1)) / n
        val baseY = size.height - shelfH
        val filled = (frac * n).let { if (done > 0 && it < 1f) 1f else it }
        for (i in 0 until n) {
            val h = baseY * (0.55f + 0.42f * pseudo(i))
            val idx = if (rtl) n - 1 - i else i
            val x = idx * (bw + gap)
            val isDone = i < filled
            val partial = !isDone && i < filled + 1 && filled > 0f && filled < n
            val col = when {
                isDone -> listOf(C.amber, C.amberDeep, C.amberSoft)[i % 3]
                else -> C.dim.copy(alpha = 0.55f)
            }
            if (isDone) {
                drawRoundRect(col, Offset(x, baseY - h), Size(bw, h), CornerRadius(2.dp.toPx()))
                drawRect(Color.Black.copy(alpha = 0.18f), Offset(x + bw * 0.15f, baseY - h * 0.82f), Size(bw * 0.7f, 1.5.dp.toPx()))
                drawRect(Color.Black.copy(alpha = 0.18f), Offset(x + bw * 0.15f, baseY - h * 0.2f), Size(bw * 0.7f, 1.5.dp.toPx()))
            } else {
                drawRoundRect(col, Offset(x, baseY - h), Size(bw, h), CornerRadius(2.dp.toPx()), style = Stroke(1.dp.toPx()))
                if (partial) {
                    val p = filled - filled.toInt()
                    drawRoundRect(C.amber.copy(alpha = 0.7f), Offset(x, baseY - h * p), Size(bw, h * p), CornerRadius(2.dp.toPx()))
                }
            }
        }
        drawRoundRect(Color(0xFF3A3150), Offset(0f, baseY), Size(size.width, shelfH), CornerRadius(2.dp.toPx()))
    }
}

private fun pseudo(i: Int): Float {
    val x = ((i * 2654435761L) xor 0x5bd1e995L) and 0xFFFF
    return (abs(x.toInt()) % 1000) / 1000f
}

@Composable
fun Avatar(title: String, size: Dp = 44.dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(Color(0xFF2B2216)).border(1.5.dp, C.amber, CircleShape),
        contentAlignment = Alignment.Center,
    ) { Text(Fmt.initials(title), color = C.amber, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.36f).sp) }
}

@Composable
fun Card(modifier: Modifier = Modifier, padding: Dp = 14.dp, content: @Composable () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(14.dp)).background(C.card).border(1.dp, C.line, RoundedCornerShape(14.dp)).padding(padding)) { content() }
}

@Composable
fun Pill(text: String, selected: Boolean = false, icon: ImageVector? = null, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(if (selected) C.amber.copy(alpha = 0.18f) else C.surface)
            .border(1.dp, if (selected) C.amber else C.line, RoundedCornerShape(50))
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { Icon(icon, null, Modifier.size(15.dp), tint = if (selected) C.amber else C.sub); Spacer(Modifier.width(5.dp)) }
        Text(text, color = if (selected) C.amber else C.text, fontSize = 13.sp)
    }
}

@Composable
fun Badge(text: String) {
    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF3A2A16)).padding(horizontal = 6.dp, vertical = 3.dp)) {
        Text(text, color = C.amber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ToggleRow(icon: ImageVector, text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(18.dp), tint = C.sub)
        Spacer(Modifier.width(10.dp))
        Text(text, Modifier.weight(1f), color = C.text, fontSize = 14.sp)
        Switch(checked, onChange, colors = SwitchDefaults.colors(checkedTrackColor = C.amber, checkedThumbColor = Color.White))
    }
}

@Composable
fun KindRow(label: String, count: Int, size: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled && count > 0) { onChange(!checked) }.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked, { onChange(it) }, enabled = enabled && count > 0,
            colors = CheckboxDefaults.colors(checkedColor = C.amber, checkmarkColor = Color(0xFF231A0C), uncheckedColor = C.dim))
        Text(label, Modifier.weight(1f), color = if (count > 0) C.text else C.dim, fontSize = 14.sp)
        if (count > 0) Text(size, color = C.sub, fontSize = 11.sp, modifier = Modifier.padding(end = 10.dp))
        Text(Fmt.num(count), color = if (checked && count > 0) C.amber else C.sub, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

/** سطر ملف: الشارة، الاسم (مع تظليل كلمة البحث)، التفاصيل، وشريط التقدّم إن كان ينزل */
@Composable
fun FileRow(
    f: FileItem, s: S, highlight: String = "", progress: Pair<Long, Long>? = null,
    showChannel: Boolean = false, onClick: () -> Unit = {}, trailing: @Composable () -> Unit = {},
) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 4.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Badge(s.badge(f.kind, f.displayName))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(highlighted(f.displayName.substringBeforeLast('.'), highlight), color = when (f.status) {
                FileStatus.FAILED -> C.red; FileStatus.REMOVED -> C.dim; else -> C.text
            }, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            val meta = buildList {
                if (showChannel && f.channelTitle.isNotBlank()) add(f.channelTitle)
                add(Fmt.date(f.date, s.ar)); add(Fmt.size(f.size, s.ar))
                if (f.section.isNotBlank()) add(f.section)
            }.joinToString(" · ")
            Text(meta, color = C.sub, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (progress != null && progress.second > 0) {
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { (progress.first.toFloat() / progress.second).coerceIn(0f, 1f) },
                        modifier = Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)), color = C.amber, trackColor = C.line,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("${(progress.first * 100 / progress.second)}%", color = C.sub, fontSize = 10.sp)
                }
            }
        }
        trailing()
    }
}

fun highlighted(text: String, q: String) = buildAnnotatedString {
    val words = Arabic.normalize(q).split(' ').filter { it.length >= 2 }
    if (words.isEmpty()) { append(text); return@buildAnnotatedString }
    // نطابق على النص الموحّد ونلوّن نفس المواقع في النص الأصلي (الطول ثابت لأن التوحيد حرف بحرف تقريباً)
    val marks = BooleanArray(text.length)
    val lowered = text.map { ch -> Arabic.normalize(ch.toString()).firstOrNull() ?: ' ' }.joinToString("")
    for (w in words) {
        var i = lowered.indexOf(w)
        while (i >= 0) { for (k in i until min(i + w.length, text.length)) marks[k] = true; i = lowered.indexOf(w, i + 1) }
    }
    var i = 0
    while (i < text.length) {
        val m = marks[i]; var j = i
        while (j < text.length && marks[j] == m) j++
        if (m) withStyle(SpanStyle(background = C.amber.copy(alpha = 0.35f), color = Color.White)) { append(text.substring(i, j)) }
        else append(text.substring(i, j))
        i = j
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, color = C.sub, fontSize = 12.sp, modifier = Modifier.padding(top = 14.dp, bottom = 8.dp))
}

@Composable
fun BigNumber(value: String, caption: String) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(value, color = C.amber, fontSize = 52.sp, fontWeight = FontWeight.Black, lineHeight = 54.sp)
        Text(caption, color = C.text, fontSize = 16.sp, modifier = Modifier.padding(bottom = 10.dp))
    }
}
