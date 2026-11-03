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

/** خط «مركزي» (Markazi Text) — رخصة SIL OFL — لجميع نصوص التطبيق. */
val Markazi = androidx.compose.ui.text.font.FontFamily(
    androidx.compose.ui.text.font.Font(com.hasad.app.R.font.markazi_regular, androidx.compose.ui.text.font.FontWeight.Normal),
    androidx.compose.ui.text.font.Font(com.hasad.app.R.font.markazi_medium, androidx.compose.ui.text.font.FontWeight.Medium),
    androidx.compose.ui.text.font.Font(com.hasad.app.R.font.markazi_semibold, androidx.compose.ui.text.font.FontWeight.SemiBold),
    androidx.compose.ui.text.font.Font(com.hasad.app.R.font.markazi_bold, androidx.compose.ui.text.font.FontWeight.Bold),
)

private fun androidx.compose.material3.Typography.withFamily(f: androidx.compose.ui.text.font.FontFamily) = androidx.compose.material3.Typography(
    displayLarge = displayLarge.copy(fontFamily = f), displayMedium = displayMedium.copy(fontFamily = f), displaySmall = displaySmall.copy(fontFamily = f),
    headlineLarge = headlineLarge.copy(fontFamily = f), headlineMedium = headlineMedium.copy(fontFamily = f), headlineSmall = headlineSmall.copy(fontFamily = f),
    titleLarge = titleLarge.copy(fontFamily = f), titleMedium = titleMedium.copy(fontFamily = f), titleSmall = titleSmall.copy(fontFamily = f),
    bodyLarge = bodyLarge.copy(fontFamily = f), bodyMedium = bodyMedium.copy(fontFamily = f), bodySmall = bodySmall.copy(fontFamily = f),
    labelLarge = labelLarge.copy(fontFamily = f), labelMedium = labelMedium.copy(fontFamily = f), labelSmall = labelSmall.copy(fontFamily = f),
)

private val AppTypography = androidx.compose.material3.Typography().withFamily(Markazi)

/** حروف «مركزي» أصغر من غيرها بالحجم نفسه، فنكبّرها قليلًا. */
@Composable
private fun MarkaziScale(content: @Composable () -> Unit) {
    val d = androidx.compose.ui.platform.LocalDensity.current
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(d.density, d.fontScale * 1.15f),
        content = content,
    )
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
        typography = AppTypography,
    ) { MarkaziScale(content) }
}
