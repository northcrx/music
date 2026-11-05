package com.nishant.northmusic.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nishant.northmusic.data.model.PlayerState

/**
 * Floating Mini Player Pill:
 * - 22dp corner radius, 14dp horizontal margin, levitating above bottom nav dock.
 * - Nocturnal Semi-Transparent Dark Glass aesthetic with hairline dark-supported outline.
 * - Minimalist transparent play/pause button with white icon and circular line completing as music plays.
 * - Crisp typography and controls (Title in white, artist in slate, favorite & skip).
 * - Gestures: Tap opens Now Playing; swipe left: skip next; swipe right: skip previous; swipe down hides it.
 */
@Composable
fun MiniPlayer(
    playerState: PlayerState,
    visible: Boolean,
    onTap: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onNextClick: () -> Unit = {},
    onPreviousClick: () -> Unit = {},
    onDismiss: () -> Unit = {},
    onDownloadClick: (() -> Unit)? = null,
    isDownloaded: Boolean = false,
    isDownloading: Boolean = false,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible && playerState.currentSong != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        val song = playerState.currentSong ?: return@AnimatedVisibility

        var totalDragX by remember { mutableFloatStateOf(0f) }
        var totalDragY by remember { mutableFloatStateOf(0f) }

        // Transparent dark background supported by subtle hairline border
        val miniPlayerBackground = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f)
        val frostedBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, bottom = 6.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(22.dp),
                    ambientColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.12f),
                    spotColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.18f)
                )
                .clip(RoundedCornerShape(22.dp))
                .clickable(onClick = onTap)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            totalDragX += dragAmount.x
                            totalDragY += dragAmount.y
                        },
                        onDragEnd = {
                            if (totalDragY < -50f && kotlin.math.abs(totalDragY) > kotlin.math.abs(totalDragX)) {
                                onTap() // Swipe up opens Now Playing
                            } else if (totalDragY > 70f && kotlin.math.abs(totalDragY) > kotlin.math.abs(totalDragX)) {
                                onDismiss()
                            } else if (totalDragX < -70f) {
                                onNextClick()
                            } else if (totalDragX > 70f) {
                                onPreviousClick()
                            }
                            totalDragX = 0f
                            totalDragY = 0f
                        },
                        onDragCancel = {
                            totalDragX = 0f
                            totalDragY = 0f
                        }
                    )
                },
            shape = RoundedCornerShape(22.dp),
            color = miniPlayerBackground,
            border = frostedBorder,
            tonalElevation = 0.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 10.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 44dp rounded artwork with async loading and fallback
                    AsyncSongArtwork(
                        albumArtUri = song.albumArtUri,
                        size = 44.dp,
                        shape = RoundedCornerShape(10.dp),
                        colorSeed = song.colorSeed
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.5.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    val isOnline = song.filePath.startsWith("http://") || song.filePath.startsWith("https://") || song.id.startsWith("online_")
                    if (isOnline && !isDownloaded) {
                        if (isDownloading) {
                            Box(
                                modifier = Modifier.size(38.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        } else {
                            IconButton(
                                onClick = { onDownloadClick?.invoke() },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Download,
                                    contentDescription = "Download song",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    } else {
                        // Favorite Button for downloaded / local song
                        IconButton(
                            onClick = onFavoriteClick,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = if (playerState.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (playerState.isFavorite) Color(0xFFFF3B5C) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Play / Pause Button: Transparent background, white icon, circular progress line
                    ProgressRingIconButton(
                        progress = playerState.progress,
                        icon = if (playerState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                        onClick = onPlayPauseClick,
                        size = 46.dp,
                        iconSize = 24.dp,
                        strokeWidth = 2.5.dp,
                        containerColor = Color.Transparent,
                        iconTint = MaterialTheme.colorScheme.onSurface,
                        ringColor = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Skip Next Button
                    IconButton(
                        onClick = onNextClick,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
