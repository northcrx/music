package com.nishant.northmusic.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
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
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

/**
 * Asynchronous Artwork Loader using Coil:
 * - Attempts to load real album art thumbnail from MediaStore content Uri
 * - Automatically falls back to deterministic ArtworkPlaceholder if no art exists
 */
@Composable
fun AsyncSongArtwork(
    artworkUri: Uri? = null,
    albumArtUri: Uri? = artworkUri,
    colorSeed: Long = 0xFF0D9488,
    size: Dp = 48.dp,
    shape: Shape = SquircleArtworkShape,
    iconSize: Dp = size * 0.45f,
    modifier: Modifier = Modifier,
    contentDescription: String? = "Artwork"
) {
    val context = LocalContext.current
    val targetUri = albumArtUri ?: artworkUri

    if (targetUri != null) {
        Box(
            modifier = modifier
                .size(size)
                .clip(shape)
        ) {
            // Background fallback always rendered
            ArtworkPlaceholder(
                size = size,
                shape = shape,
                colorSeed = colorSeed,
                iconSize = iconSize,
                modifier = Modifier.fillMaxSize()
            )

            // Real image layered on top with crossfade
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(targetUri)
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        ArtworkPlaceholder(
            size = size,
            shape = shape,
            colorSeed = colorSeed,
            iconSize = iconSize,
            modifier = modifier
        )
    }
}
