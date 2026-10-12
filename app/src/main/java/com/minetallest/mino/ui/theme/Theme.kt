package com.minetallest.mino.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Spec §5.1 colour tokens — dark (default) and light ("warm off-white paper"). */
data class MinoColors(
    val background: Color,
    val bgRaised: Color,
    val bgHover: Color,
    val bgElevated: Color,
    val line: Color,
    val lineStrong: Color,
    val textHi: Color,
    val text: Color,
    val textMid: Color,
    val textLow: Color,
    val accent: Color,
    val accentSoft: Color,
    val azure: Color,
    val azureSoft: Color,
    val code: Color,
    val codeSoft: Color,
    val onAccent: Color,
    // Hard-coded brand surfaces (spec §5.1 table)
    val pageBackground: Color,
    val sidebarBackground: Color,
    val composerShell: Color,
    val menuBackground: Color,
    val primaryButton: Color,
    val primaryButtonHover: Color,
    val mint: Color,
    val userBubble: Color,
    val danger: Color,
    val dangerSoft: Color,
    val amber: Color,
    val amberSoft: Color,
    val lightStatusBar: Boolean,
    val systemBarColor: Color,
)

val DarkMinoColors = MinoColors(
    background = Color(0xFF0B1310),
    bgRaised = Color(0xFF111A16),
    bgHover = Color(0xFF17221C),
    bgElevated = Color(0xFF1A2520),
    line = Color(0x14E9F5EC),
    lineStrong = Color(0x24E9F5EC),
    textHi = Color(0xFFEEF5F0),
    text = Color(0xFFCDDFD4),
    textMid = Color(0xFF8BA396),
    textLow = Color(0xFF5D7167),
    accent = Color(0xFF6EC294),
    accentSoft = Color(0x266EC294),
    azure = Color(0xFF5B9BD5),
    azureSoft = Color(0x265B9BD5),
    code = Color(0xFFE08E6B),
    codeSoft = Color(0x26E08E6B),
    onAccent = Color(0xFF0B1310),
    pageBackground = Color(0xFF060A08),
    sidebarBackground = Color(0xFF080D0A),
    composerShell = Color(0xF21A2620),
    menuBackground = Color(0xFA141F1A),
    primaryButton = Color(0xFF2A6142),
    primaryButtonHover = Color(0xFF35744F),
    mint = Color(0xFFA9D8BB),
    userBubble = Color(0x14FFFFFF),
    danger = Color(0xFFF87171),
    dangerSoft = Color(0x1AF87171),
    amber = Color(0xFFFBBF24),
    amberSoft = Color(0x1AFBBF24),
    lightStatusBar = false,
    systemBarColor = Color(0xFF0B1310),
)

val LightMinoColors = MinoColors(
    background = Color(0xFFF4F1E9),
    bgRaised = Color(0xFFFFFDF7),
    bgHover = Color(0xFFEAE5D9),
    bgElevated = Color(0xFFFFFFFF),
    line = Color(0x1C2E3E30),
    lineStrong = Color(0x332E3E30),
    textHi = Color(0xFF16281C),
    text = Color(0xFF33453A),
    textMid = Color(0xFF6B7D71),
    textLow = Color(0xFF9BA89E),
    accent = Color(0xFF2F6B48),
    accentSoft = Color(0x1F2F6B48),
    azure = Color(0xFF2C5AA0),
    azureSoft = Color(0x1F2C5AA0),
    code = Color(0xFF8A4F2A),
    codeSoft = Color(0x1F8A4F2A),
    onAccent = Color(0xFFF7F5EE),
    pageBackground = Color(0xFFFBF9F2),
    sidebarBackground = Color(0xFFF1EDE3),
    composerShell = Color(0xF2FFFDF7),
    menuBackground = Color(0xFAFFFDF7),
    primaryButton = Color(0xFF2F6B48),
    primaryButtonHover = Color(0xFF35744F),
    mint = Color(0xFF2F6B48),
    userBubble = Color(0x0D2E3E30),
    danger = Color(0xFFB91C1C),
    dangerSoft = Color(0x14B91C1C),
    amber = Color(0xFFB45309),
    amberSoft = Color(0x14B45309),
    lightStatusBar = true,
    systemBarColor = Color(0xFFFBF9F2),
)

val LocalMinoColors = staticCompositionLocalOf { DarkMinoColors }

private fun schemeFor(colors: MinoColors) = if (colors.lightStatusBar) {
    lightColorScheme(
        primary = colors.accent,
        onPrimary = colors.onAccent,
        background = colors.background,
        onBackground = colors.text,
        surface = colors.background,
        onSurface = colors.text,
        surfaceVariant = colors.bgRaised,
        onSurfaceVariant = colors.textMid,
        outline = colors.lineStrong,
        error = colors.danger,
    )
} else {
    darkColorScheme(
        primary = colors.accent,
        onPrimary = colors.onAccent,
        background = colors.background,
        onBackground = colors.text,
        surface = colors.background,
        onSurface = colors.text,
        surfaceVariant = colors.bgRaised,
        onSurfaceVariant = colors.textMid,
        outline = colors.lineStrong,
        error = colors.danger,
    )
}

@Composable
fun MinoTheme(
    dark: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = if (dark) DarkMinoColors else LightMinoColors
    CompositionLocalProvider(LocalMinoColors provides colors) {
        MaterialTheme(
            colorScheme = schemeFor(colors),
            typography = MinoTypography,
            content = content,
        )
    }
}

/** Spec §5.4 — appearance sets system bars to dark #0b1310 / light #fbf9f2. */
fun systemBarColors(dark: Boolean): Pair<Color, Boolean> =
    if (dark) Color(0xFF0B1310) to false else Color(0xFFFBF9F2) to true
