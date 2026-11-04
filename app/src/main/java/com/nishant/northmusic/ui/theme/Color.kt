package com.nishant.northmusic.ui.theme

import androidx.compose.ui.graphics.Color
import com.nishant.northmusic.data.model.AccentColor

// Neutral Near-Black Dark Surfaces (zero purple tint)
val NeutralDarkBackground = Color(0xFF0F0F10)
val NeutralDarkSurface = Color(0xFF0F0F10)
val NeutralDarkSurfaceDim = Color(0xFF0B0B0C)
val NeutralDarkSurfaceBright = Color(0xFF333336)
val NeutralDarkSurfaceContainerLowest = Color(0xFF0A0A0B)
val NeutralDarkSurfaceContainerLow = Color(0xFF141416)
val NeutralDarkSurfaceContainer = Color(0xFF1A1A1C)
val NeutralDarkSurfaceContainerHigh = Color(0xFF222225)
val NeutralDarkSurfaceContainerHighest = Color(0xFF2A2A2D)

val NeutralDarkOnSurface = Color(0xFFEDEDED)
val NeutralDarkOnSurfaceVariant = Color(0xFFA1A1AA)
val NeutralDarkOutline = Color(0xFF52525B)
val NeutralDarkOutlineVariant = Color(0xFF3F3F46)

// Neutral Light Surfaces
val NeutralLightBackground = Color(0xFFF9F9FB)
val NeutralLightSurface = Color(0xFFF9F9FB)
val NeutralLightSurfaceDim = Color(0xFFE4E4E7)
val NeutralLightSurfaceBright = Color(0xFFFFFFFF)
val NeutralLightSurfaceContainerLowest = Color(0xFFFFFFFF)
val NeutralLightSurfaceContainerLow = Color(0xFFF3F3F6)
val NeutralLightSurfaceContainer = Color(0xFFEAEAEC)
val NeutralLightSurfaceContainerHigh = Color(0xFFE2E2E5)
val NeutralLightSurfaceContainerHighest = Color(0xFFD4D4D8)

val NeutralLightOnSurface = Color(0xFF18181B)
val NeutralLightOnSurfaceVariant = Color(0xFF52525B)
val NeutralLightOutline = Color(0xFF71717A)
val NeutralLightOutlineVariant = Color(0xFFA1A1AA)

// Accent Color Definitions
data class AccentPalette(
    val primaryDark: Color,
    val onPrimaryDark: Color,
    val primaryContainerDark: Color,
    val onPrimaryContainerDark: Color,
    val primaryLight: Color,
    val onPrimaryLight: Color,
    val primaryContainerLight: Color,
    val onPrimaryContainerLight: Color
)

fun getAccentPalette(accent: AccentColor): AccentPalette {
    return when (accent) {
        AccentColor.TEAL -> AccentPalette(
            primaryDark = Color(0xFF2DD4BF),
            onPrimaryDark = Color(0xFF003731),
            primaryContainerDark = Color(0xFF0D4F46),
            onPrimaryContainerDark = Color(0xFF99F6E4),
            primaryLight = Color(0xFF006A60),
            onPrimaryLight = Color(0xFFFFFFFF),
            primaryContainerLight = Color(0xFF70F7E6),
            onPrimaryContainerLight = Color(0xFF00201D)
        )
        AccentColor.BLUE -> AccentPalette(
            primaryDark = Color(0xFF60A5FA),
            onPrimaryDark = Color(0xFF1E3A8A),
            primaryContainerDark = Color(0xFF1D4ED8),
            onPrimaryContainerDark = Color(0xFFDBEAFE),
            primaryLight = Color(0xFF2563EB),
            onPrimaryLight = Color(0xFFFFFFFF),
            primaryContainerLight = Color(0xFFDBEAFE),
            onPrimaryContainerLight = Color(0xFF1E40AF)
        )
        AccentColor.GREEN -> AccentPalette(
            primaryDark = Color(0xFF4ADE80),
            onPrimaryDark = Color(0xFF052E16),
            primaryContainerDark = Color(0xFF166534),
            onPrimaryContainerDark = Color(0xFFDCFCE7),
            primaryLight = Color(0xFF16A34A),
            onPrimaryLight = Color(0xFFFFFFFF),
            primaryContainerLight = Color(0xFFDCFCE7),
            onPrimaryContainerLight = Color(0xFF14532D)
        )
        AccentColor.ORANGE -> AccentPalette(
            primaryDark = Color(0xFFFB923C),
            onPrimaryDark = Color(0xFF431407),
            primaryContainerDark = Color(0xFF9A3412),
            onPrimaryContainerDark = Color(0xFFFFEDD5),
            primaryLight = Color(0xFFEA580C),
            onPrimaryLight = Color(0xFFFFFFFF),
            primaryContainerLight = Color(0xFFFFEDD5),
            onPrimaryContainerLight = Color(0xFF7C2D12)
        )
        AccentColor.ROSE -> AccentPalette(
            primaryDark = Color(0xFFFB7185),
            onPrimaryDark = Color(0xFF4C0519),
            primaryContainerDark = Color(0xFF9F1239),
            onPrimaryContainerDark = Color(0xFFFFE4E6),
            primaryLight = Color(0xFFE11D48),
            onPrimaryLight = Color(0xFFFFFFFF),
            primaryContainerLight = Color(0xFFFFE4E6),
            onPrimaryContainerLight = Color(0xFF881337)
        )
        AccentColor.RED -> AccentPalette(
            primaryDark = Color(0xFFF87171),
            onPrimaryDark = Color(0xFF450A0A),
            primaryContainerDark = Color(0xFF991B1B),
            onPrimaryContainerDark = Color(0xFFFEE2E2),
            primaryLight = Color(0xFFDC2626),
            onPrimaryLight = Color(0xFFFFFFFF),
            primaryContainerLight = Color(0xFFFEE2E2),
            onPrimaryContainerLight = Color(0xFF7F1D1D)
        )
        AccentColor.PURPLE -> AccentPalette(
            primaryDark = Color(0xFFA78BFA),
            onPrimaryDark = Color(0xFF2E1065),
            primaryContainerDark = Color(0xFF5B21B6),
            onPrimaryContainerDark = Color(0xFFEDE9FE),
            primaryLight = Color(0xFF7C3AED),
            onPrimaryLight = Color(0xFFFFFFFF),
            primaryContainerLight = Color(0xFFEDE9FE),
            onPrimaryContainerLight = Color(0xFF4C1D95)
        )
    }
}
