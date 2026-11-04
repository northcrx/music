package com.nishant.northmusic.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.nishant.northmusic.data.model.AccentColor

fun buildDarkColorScheme(accent: AccentColor): ColorScheme {
    val palette = getAccentPalette(accent)
    return darkColorScheme(
        primary = palette.primaryDark,
        onPrimary = palette.onPrimaryDark,
        primaryContainer = palette.primaryContainerDark,
        onPrimaryContainer = palette.onPrimaryContainerDark,
        secondary = palette.primaryDark,
        onSecondary = palette.onPrimaryDark,
        secondaryContainer = palette.primaryContainerDark,
        onSecondaryContainer = palette.onPrimaryContainerDark,
        tertiary = palette.primaryDark,
        onTertiary = palette.onPrimaryDark,
        background = NeutralDarkBackground,
        onBackground = NeutralDarkOnSurface,
        surface = NeutralDarkSurface,
        onSurface = NeutralDarkOnSurface,
        surfaceVariant = NeutralDarkSurfaceContainerHighest,
        onSurfaceVariant = NeutralDarkOnSurfaceVariant,
        surfaceDim = NeutralDarkSurfaceDim,
        surfaceBright = NeutralDarkSurfaceBright,
        surfaceContainerLowest = NeutralDarkSurfaceContainerLowest,
        surfaceContainerLow = NeutralDarkSurfaceContainerLow,
        surfaceContainer = NeutralDarkSurfaceContainer,
        surfaceContainerHigh = NeutralDarkSurfaceContainerHigh,
        surfaceContainerHighest = NeutralDarkSurfaceContainerHighest,
        outline = NeutralDarkOutline,
        outlineVariant = NeutralDarkOutlineVariant,
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6)
    )
}

fun buildLightColorScheme(accent: AccentColor): ColorScheme {
    val palette = getAccentPalette(accent)
    return lightColorScheme(
        primary = palette.primaryLight,
        onPrimary = palette.onPrimaryLight,
        primaryContainer = palette.primaryContainerLight,
        onPrimaryContainer = palette.onPrimaryContainerLight,
        secondary = palette.primaryLight,
        onSecondary = palette.onPrimaryLight,
        secondaryContainer = palette.primaryContainerLight,
        onSecondaryContainer = palette.onPrimaryContainerLight,
        tertiary = palette.primaryLight,
        onTertiary = palette.onPrimaryLight,
        background = NeutralLightBackground,
        onBackground = NeutralLightOnSurface,
        surface = NeutralLightSurface,
        onSurface = NeutralLightOnSurface,
        surfaceVariant = NeutralLightSurfaceContainerHighest,
        onSurfaceVariant = NeutralLightOnSurfaceVariant,
        surfaceDim = NeutralLightSurfaceDim,
        surfaceBright = NeutralLightSurfaceBright,
        surfaceContainerLowest = NeutralLightSurfaceContainerLowest,
        surfaceContainerLow = NeutralLightSurfaceContainerLow,
        surfaceContainer = NeutralLightSurfaceContainer,
        surfaceContainerHigh = NeutralLightSurfaceContainerHigh,
        surfaceContainerHighest = NeutralLightSurfaceContainerHighest,
        outline = NeutralLightOutline,
        outlineVariant = NeutralLightOutlineVariant,
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002)
    )
}

@Composable
fun NorthMusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    accentColor: AccentColor = AccentColor.TEAL,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> buildDarkColorScheme(accentColor)
        else -> buildLightColorScheme(accentColor)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = ExpressiveShapes,
        content = content
    )
}
