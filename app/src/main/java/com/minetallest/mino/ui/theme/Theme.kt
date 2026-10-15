package com.minetallest.mino.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
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

val LocalMinoColors = staticCompositionLocalOf { DarkMinoColors }

private fun schemeFor(colors: MinoColors) = darkColorScheme(
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

@Composable
fun MinoTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalMinoColors provides DarkMinoColors) {
        MaterialTheme(
            colorScheme = schemeFor(DarkMinoColors),
            typography = MinoTypography,
            content = content,
        )
    }
}
