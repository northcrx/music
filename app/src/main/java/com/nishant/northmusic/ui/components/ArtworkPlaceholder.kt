package com.nishant.northmusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nishant.northmusic.ui.theme.SquircleArtworkShape

@Composable
fun ArtworkPlaceholder(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    shape: Shape = SquircleArtworkShape,
    colorSeed: Long = 0xFF0D9488,
    iconSize: Dp = size * 0.45f
) {
    val baseColor = Color(colorSeed)
    val highlightColor = baseColor.copy(alpha = 0.5f)
    val darkColor = baseColor.copy(alpha = 0.85f)

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        darkColor,
                        baseColor,
                        highlightColor
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Inner subtle disc pattern
        Box(
            modifier = Modifier
                .fillMaxSize(0.85f)
                .clip(RoundedCornerShape(percent = 50))
                .background(Color.Black.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = "Artwork",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(iconSize)
            )
        }
    }
}
