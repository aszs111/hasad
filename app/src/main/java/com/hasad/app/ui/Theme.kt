package com.hasad.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** ألوان داكنة بنفسجية مع كهرماني — نفس روح التطبيق الأصلي */
object C {
    val bg = Color(0xFF15121C)
    val surface = Color(0xFF1C1826)
    val card = Color(0xFF221D2E)
    val line = Color(0xFF2E2840)
    val amber = Color(0xFFF0A23B)
    val amberDeep = Color(0xFFD9862A)
    val amberSoft = Color(0xFFF4B860)
    val text = Color(0xFFEDE8F5)
    val sub = Color(0xFF9A91AE)
    val dim = Color(0xFF4A4260)
    val green = Color(0xFF6CC28A)
    val red = Color(0xFFE0736A)
}

@Composable
fun HasadTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = C.amber, onPrimary = Color(0xFF231A0C),
            secondary = C.amberSoft, background = C.bg, surface = C.surface,
            surfaceVariant = C.card, onBackground = C.text, onSurface = C.text, onSurfaceVariant = C.sub,
            outline = C.line, error = C.red,
            surfaceContainer = C.surface, surfaceContainerHigh = C.card, surfaceContainerHighest = C.card,
        ),
        content = content,
    )
}
