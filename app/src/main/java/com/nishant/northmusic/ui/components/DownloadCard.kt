package com.nishant.northmusic.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nishant.northmusic.data.model.DownloadItem
import com.nishant.northmusic.data.model.DownloadStatus

/**
 * Flat 56dp Download Row:
 * No card background, full-width, separated by thin dividers.
 * 40dp artwork, 1-line title, "artist · 8.2 MB" subtitle.
 * 3dp thin progress line at bottom (no end dot).
 * Percent text, pause/resume button, and 3-dot context menu at far right.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DownloadCard(
    item: DownloadItem,
    isSelectionMode: Boolean,
    onItemClick: () -> Unit,
    onItemLongClick: () -> Unit,
    onPauseResumeClick: () -> Unit,
    onRetryClick: () -> Unit,
    onDeleteClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .combinedClickable(
                    onClick = onItemClick,
                    onLongClick = onItemLongClick
                ),
            color = if (item.isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            } else {
                Color.Transparent
            }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedVisibility(
                        visible = isSelectionMode,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Checkbox(
                            checked = item.isSelected,
                            onCheckedChange = { onItemClick() },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }

                    // 40dp artwork
                    ArtworkPlaceholder(
                        size = 40.dp,
                        colorSeed = item.colorSeed,
                        shape = RoundedCornerShape(10.dp),
                        iconSize = 18.dp
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Title & "artist · 8.2 MB" Subtitle
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (item.status == DownloadStatus.FAILED) {
                                "${item.artist} • Failed"
                            } else {
                                "${item.artist} • %.1f MB".format(item.totalSizeMb)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (item.status == DownloadStatus.FAILED) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Status / Percentage text
                    when (item.status) {
                        DownloadStatus.DOWNLOADING -> {
                            val percent = (item.progress * 100).toInt()
                            Text(
                                text = if (item.isPaused) "Paused" else "$percent%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (item.isPaused) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                            )

                            // Pause / Resume icon button inside row end
                            IconButton(
                                onClick = onPauseResumeClick,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (item.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                    contentDescription = if (item.isPaused) "Resume" else "Pause",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        DownloadStatus.COMPLETED -> {
                            Text(
                                text = "100%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Done",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DownloadStatus.FAILED -> {
                            IconButton(
                                onClick = onRetryClick,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "Retry",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // 3-dot menu at far right
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "Options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            if (item.status == DownloadStatus.DOWNLOADING) {
                                DropdownMenuItem(
                                    text = { Text(if (item.isPaused) "Resume" else "Pause") },
                                    onClick = {
                                        menuExpanded = false
                                        onPauseResumeClick()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            if (item.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                            contentDescription = null
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Cancel") },
                                    onClick = {
                                        menuExpanded = false
                                        onDeleteClick()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Rounded.Close, contentDescription = null)
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Add to playlist") },
                                onClick = { menuExpanded = false },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Add to queue") },
                                onClick = { menuExpanded = false },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Play next") },
                                onClick = { menuExpanded = false },
                                leadingIcon = {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share") },
                                onClick = { menuExpanded = false },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Share, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Details") },
                                onClick = { menuExpanded = false },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Info, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteClick()
                                },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                }
                            )
                        }
                    }
                }

                // 3dp thin progress line at the bottom of the row (no end dot)
                if (item.status == DownloadStatus.DOWNLOADING) {
                    LinearProgressIndicator(
                        progress = { item.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.BottomCenter),
                        color = if (item.isPaused) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent,
                        drawStopIndicator = {}
                    )
                }
            }
        }

        // Thin horizontal divider between rows
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f)
        )
    }
}
