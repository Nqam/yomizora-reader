package com.yomizora.reader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.yomizora.reader.domain.model.ThemeMode

data class YomizoraSemanticColors(
    val completed: Color,
    val completedBg: Color,
    val inProgress: Color,
    val inProgressBg: Color,
    val locked: Color,
    val lockedBg: Color
)

val LocalSemanticColors = staticCompositionLocalOf {
    YomizoraSemanticColors(
        completed = StatusCompletedLight,
        completedBg = StatusCompletedBgLight,
        inProgress = StatusInProgressLight,
        inProgressBg = StatusInProgressBgLight,
        locked = StatusLockedLight,
        lockedBg = StatusLockedBgLight
    )
}

private val LightColorScheme = lightColorScheme(
    primary = VermilionPrimary,
    onPrimary = Color.White,
    primaryContainer = VermilionContainer,
    onPrimaryContainer = VermilionPrimary,
    secondary = IndigoSecondary,
    onSecondary = Color.White,
    secondaryContainer = IndigoContainer,
    onSecondaryContainer = IndigoSecondary,
    background = WashiBackground,
    onBackground = SumiText,
    surface = WashiSurface,
    onSurface = SumiText,
    surfaceVariant = WashiSurfaceVariant,
    onSurfaceVariant = SumiTextMuted,
    outline = WashiOutline
)

private val DarkColorScheme = darkColorScheme(
    primary = SoftVermilionPrimary,
    onPrimary = Color(0xFF2B100C),
    primaryContainer = SoftVermilionContainer,
    onPrimaryContainer = SoftVermilionPrimary,
    secondary = SoftIndigoSecondary,
    onSecondary = Color(0xFF0E1D2A),
    secondaryContainer = SoftIndigoContainer,
    onSecondaryContainer = SoftIndigoSecondary,
    background = SumiDarkBackground,
    onBackground = SoftWashiText,
    surface = SumiDarkSurface,
    onSurface = SoftWashiText,
    surfaceVariant = SumiDarkSurfaceVariant,
    onSurfaceVariant = SoftWashiMuted,
    outline = SumiDarkOutline
)

@Composable
fun YomizoraTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val semanticColors = if (isDark) {
        YomizoraSemanticColors(
            completed = StatusCompletedDark,
            completedBg = StatusCompletedBgDark,
            inProgress = StatusInProgressDark,
            inProgressBg = StatusInProgressBgDark,
            locked = StatusLockedDark,
            lockedBg = StatusLockedBgDark
        )
    } else {
        YomizoraSemanticColors(
            completed = StatusCompletedLight,
            completedBg = StatusCompletedBgLight,
            inProgress = StatusInProgressLight,
            inProgressBg = StatusInProgressBgLight,
            locked = StatusLockedLight,
            lockedBg = StatusLockedBgLight
        )
    }

    CompositionLocalProvider(LocalSemanticColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = YomizoraTypography,
            content = content
        )
    }
}
