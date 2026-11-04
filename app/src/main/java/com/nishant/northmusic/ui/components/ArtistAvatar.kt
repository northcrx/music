package com.nishant.northmusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.nishant.northmusic.data.model.Artist

/**
 * ArtistAvatar displays an artist's image following a strict fallback chain:
 * 1. Saved artist photo (online download or user-assigned photo)
 * 2. 2x2 collage (if >= 4 distinct album arts) or first album art
 * 3. Letter avatar with deterministic colorSeed background
 */
@Composable
fun ArtistAvatar(
    artist: Artist,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp
) {
    val context = LocalContext.current
    val savedPhoto = artist.savedPhotoUri
    val albumArts = artist.albumArtUris

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Fallback Letter Avatar rendered at the base layer
        LetterAvatar(
            name = artist.name,
            colorSeed = artist.colorSeed,
            size = size
        )

        when {
            // Level 1: Saved artist photo
            !savedPhoto.isNullOrBlank() -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(savedPhoto)
                        .crossfade(true)
                        .build(),
                    contentDescription = artist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            // Level 2a: 2x2 collage when >= 4 distinct album covers
            albumArts.size >= 4 -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(albumArts[0]).crossfade(true).build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(albumArts[1]).crossfade(true).build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(albumArts[2]).crossfade(true).build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(albumArts[3]).crossfade(true).build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
            // Level 2b: Single first album cover when 1 to 3 album arts
            albumArts.isNotEmpty() -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(albumArts.first())
                        .crossfade(true)
                        .build(),
                    contentDescription = artist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun LetterAvatar(
    name: String,
    colorSeed: Long,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .background(Color(colorSeed)),
        contentAlignment = Alignment.Center
    ) {
        val letter = name.trim().take(1).uppercase().ifBlank { "?" }
        Text(
            text = letter,
            style = if (size < 40.dp) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
