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
    background = Color(0xFF000000),
    bgRaised = Color(0xFF1E1E22),
    bgHover = Color(0xFF27272A),
    bgElevated = Color(0xFF1E1E22),
    line = Color(0xFF27272A),
    lineStrong = Color(0xFF3F3F46),
    textHi = Color(0xFFF4F4F5),
    text = Color(0xFFE4E4E7),
    textMid = Color(0xFFA1A1AA),
    textLow = Color(0xFF71717A),
    accent = Color(0xFFF4F4F5),
    accentSoft = Color(0x1AF4F4F5),
    azure = Color(0xFF60A5FA),
    azureSoft = Color(0x1A60A5FA),
    code = Color(0xFFF4F4F5),
    codeSoft = Color(0x1AF4F4F5),
    onAccent = Color(0xFF000000),
    pageBackground = Color(0xFF000000),
    sidebarBackground = Color(0xFF121212),
    composerShell = Color(0xFF1E1E22),
    menuBackground = Color(0xFF1E1E22),
    primaryButton = Color(0xFF1E1E22),
    primaryButtonHover = Color(0xFF27272A),
    mint = Color(0xFFF4F4F5),
    userBubble = Color(0x1AF4F4F5),
    danger = Color(0xFFEF5350),
    dangerSoft = Color(0x1AEF5350),
    amber = Color(0xFFFBBF24),
    amberSoft = Color(0x1AFBBF24),
    lightStatusBar = false,
    systemBarColor = Color(0xFF000000),
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
    val colors = DarkMinoColors
    CompositionLocalProvider(LocalMinoColors provides colors) {
        MaterialTheme(
            colorScheme = schemeFor(colors),
            typography = MinoTypography,
            content = content,
        )
    }
}

/** Spec §5.4 — appearance sets system bars to dark #000000 / light #fbf9f2. */
fun systemBarColors(dark: Boolean): Pair<Color, Boolean> =
    if (dark) Color(0xFF000000) to false else Color(0xFFFBF9F2) to true
