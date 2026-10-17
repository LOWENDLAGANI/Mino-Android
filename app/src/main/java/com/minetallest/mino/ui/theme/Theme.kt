package com.minetallest.mino.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Apple-inspired design tokens: system colours, grouped backgrounds and
 * label hierarchy for both light and dark appearance.
 */
data class MinoColors(
    val isDark: Boolean,
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
    val pageBackground: Color,
    val sidebarBackground: Color,
    val composerShell: Color,
    val menuBackground: Color,
    val primaryButton: Color,
    val primaryButtonHover: Color,
    val mint: Color,
    val userBubble: Color,
    val userBubbleText: Color,
    val danger: Color,
    val dangerSoft: Color,
    val amber: Color,
    val amberSoft: Color,
    val groupedBackground: Color,
    val fill: Color,
    val switchTrack: Color,
    val lightStatusBar: Boolean,
    val systemBarColor: Color,
) {
    companion object {
        /** iOS system palette. */
        val SystemBlue = Color(0xFF007AFF)
        val SystemGreen = Color(0xFF34C759)
        val SystemOrange = Color(0xFFFF9500)
        val SystemRed = Color(0xFFFF3B30)
        val SystemPurple = Color(0xFFAF52DE)
        val SystemTeal = Color(0xFF30B0C7)
    }
}

val LightMinoColors = MinoColors(
    isDark = false,
    background = Color(0xFFF2F2F7),
    bgRaised = Color(0xFFFFFFFF),
    bgHover = Color(0xFFE5E5EA),
    bgElevated = Color(0xFFFFFFFF),
    line = Color(0xFFE5E5EA),
    lineStrong = Color(0xFFC6C6C8),
    textHi = Color(0xFF000000),
    text = Color(0xFF1C1C1E),
    textMid = Color(0xFF8E8E93),
    textLow = Color(0xFFAEAEB2),
    accent = MinoColors.SystemBlue,
    accentSoft = Color(0xFFE5F1FF),
    azure = Color(0xFF32ADE6),
    azureSoft = Color(0xFFE8F6FD),
    code = MinoColors.SystemPurple,
    codeSoft = Color(0xFFF4EAFB),
    onAccent = Color.White,
    pageBackground = Color(0xFFFFFFFF),
    sidebarBackground = Color(0xFFF2F2F7),
    composerShell = Color(0xFFFFFFFF),
    menuBackground = Color(0xFFFFFFFF),
    primaryButton = MinoColors.SystemBlue,
    primaryButtonHover = Color(0xFF0A84FF),
    mint = MinoColors.SystemGreen,
    userBubble = MinoColors.SystemBlue,
    userBubbleText = Color.White,
    danger = MinoColors.SystemRed,
    dangerSoft = Color(0xFFFFEBEA),
    amber = MinoColors.SystemOrange,
    amberSoft = Color(0xFFFFF4E5),
    groupedBackground = Color(0xFFF2F2F7),
    fill = Color(0xFF767680).copy(alpha = 0.12f),
    switchTrack = MinoColors.SystemGreen,
    lightStatusBar = true,
    systemBarColor = Color.White,
)

val DarkMinoColors = MinoColors(
    isDark = true,
    background = Color(0xFF000000),
    bgRaised = Color(0xFF1C1C1E),
    bgHover = Color(0xFF2C2C2E),
    bgElevated = Color(0xFF1C1C1E),
    line = Color(0xFF38383A),
    lineStrong = Color(0xFF545458),
    textHi = Color(0xFFFFFFFF),
    text = Color(0xFFEBEBF5),
    textMid = Color(0xFF8E8E93),
    textLow = Color(0xFF636366),
    accent = Color(0xFF0A84FF),
    accentSoft = Color(0xFF0A84FF).copy(alpha = 0.16f),
    azure = Color(0xFF64D2FF),
    azureSoft = Color(0xFF64D2FF).copy(alpha = 0.16f),
    code = Color(0xFFBF5AF2),
    codeSoft = Color(0xFFBF5AF2).copy(alpha = 0.16f),
    onAccent = Color.White,
    pageBackground = Color(0xFF000000),
    sidebarBackground = Color(0xFF000000),
    composerShell = Color(0xFF1C1C1E),
    menuBackground = Color(0xFF1C1C1E),
    primaryButton = Color(0xFF0A84FF),
    primaryButtonHover = Color(0xFF409CFF),
    mint = Color(0xFF30D158),
    userBubble = Color(0xFF0A84FF),
    userBubbleText = Color.White,
    danger = Color(0xFFFF453A),
    dangerSoft = Color(0xFFFF453A).copy(alpha = 0.16f),
    amber = Color(0xFFFF9F0A),
    amberSoft = Color(0xFFFF9F0A).copy(alpha = 0.16f),
    groupedBackground = Color(0xFF000000),
    fill = Color(0xFF767680).copy(alpha = 0.24f),
    switchTrack = Color(0xFF30D158),
    lightStatusBar = false,
    systemBarColor = Color.Black,
)

val LocalMinoColors = staticCompositionLocalOf { DarkMinoColors }

private fun schemeFor(colors: MinoColors) = if (colors.isDark) {
    darkColorScheme(
        primary = colors.accent,
        onPrimary = colors.onAccent,
        secondary = colors.textMid,
        background = colors.background,
        onBackground = colors.text,
        surface = colors.bgRaised,
        onSurface = colors.text,
        surfaceVariant = colors.bgHover,
        onSurfaceVariant = colors.textMid,
        outline = colors.lineStrong,
        error = colors.danger,
    )
} else {
    lightColorScheme(
        primary = colors.accent,
        onPrimary = colors.onAccent,
        secondary = colors.textMid,
        background = colors.background,
        onBackground = colors.text,
        surface = colors.bgRaised,
        onSurface = colors.text,
        surfaceVariant = colors.bgHover,
        onSurfaceVariant = colors.textMid,
        outline = colors.lineStrong,
        error = colors.danger,
    )
}

@Composable
fun MinoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkMinoColors else LightMinoColors
    CompositionLocalProvider(LocalMinoColors provides colors) {
        MaterialTheme(
            colorScheme = schemeFor(colors),
            typography = MinoTypography,
            content = content,
        )
    }
}
