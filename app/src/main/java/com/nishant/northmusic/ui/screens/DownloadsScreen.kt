package com.nishant.northmusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddLink
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nishant.northmusic.data.model.DownloadStatus
import com.nishant.northmusic.navigation.BottomListScrollPadding
import com.nishant.northmusic.ui.components.DownloadCard
import com.nishant.northmusic.ui.theme.PillShape
import com.nishant.northmusic.ui.viewmodels.DownloadsTab
import com.nishant.northmusic.ui.viewmodels.DownloadsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    downloadsViewModel: DownloadsViewModel,
    saveFolder: String = "/storage/emulated/0/Music/Downloads",
    modifier: Modifier = Modifier
) {
    val uiState by downloadsViewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    var showAddUrlSheet by remember { mutableStateOf(false) }
    var urlInputText by remember { mutableStateOf("") }

    val detectedLinks = remember(urlInputText) {
        urlInputText.lines().map { it.trim() }.filter { it.startsWith("http://") || it.startsWith("https://") }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar: Normal vs Selection Mode
            if (uiState.isSelectionMode) {
                TopAppBar(
                    windowInsets = WindowInsets(0.dp),
                    title = {
                        Text(
                            text = "${uiState.selectedCount} selected",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { downloadsViewModel.exitSelectionMode() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Cancel selection",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { downloadsViewModel.selectAll() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SelectAll,
                                contentDescription = "Select all",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { downloadsViewModel.pauseSelected() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Pause,
                                contentDescription = "Pause selected",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { downloadsViewModel.resumeSelected() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Resume selected",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { downloadsViewModel.deleteSelected() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Delete selected",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            } else {
                TopAppBar(
                    windowInsets = WindowInsets(0.dp),
                    title = {
                        Text(
                            text = "Downloads",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            // Material Primary Tabs (Equal Width, One Line, Counts)
            PrimaryTabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = uiState.selectedTab == DownloadsTab.DOWNLOADING,
                    onClick = { downloadsViewModel.onTabSelected(DownloadsTab.DOWNLOADING) },
                    text = {
                        Text(
                            text = "Downloading (${uiState.downloadingItems.size})",
                            maxLines = 1,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                )
                Tab(
                    selected = uiState.selectedTab == DownloadsTab.COMPLETED,
                    onClick = { downloadsViewModel.onTabSelected(DownloadsTab.COMPLETED) },
                    text = {
                        Text(
                            text = "Completed (${uiState.completedItems.size})",
                            maxLines = 1,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                )
                Tab(
                    selected = uiState.selectedTab == DownloadsTab.FAILED,
                    onClick = { downloadsViewModel.onTabSelected(DownloadsTab.FAILED) },
                    text = {
                        Text(
                            text = "Failed (${uiState.failedItems.size})",
                            maxLines = 1,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                )
            }

            // Top Summary Bar: "142 of 200 done · 38 MB/s" with one overall progress line and control buttons
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${uiState.completedCount} of ${uiState.totalCount} done • ${uiState.networkSpeedText}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Action Buttons based on context
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            when (uiState.selectedTab) {
                                DownloadsTab.DOWNLOADING -> {
                                    OutlinedButton(
                                        onClick = { downloadsViewModel.pauseAll() },
                                        shape = PillShape,
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Pause all", style = MaterialTheme.typography.labelSmall)
                                    }
                                    FilledTonalButton(
                                        onClick = { downloadsViewModel.resumeAll() },
                                        shape = PillShape,
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Resume all", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                DownloadsTab.COMPLETED -> {
                                    OutlinedButton(
                                        onClick = { downloadsViewModel.clearCompleted() },
                                        shape = PillShape,
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Clear completed", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                DownloadsTab.FAILED -> {
                                    FilledTonalButton(
                                        onClick = { downloadsViewModel.retryAllFailed() },
                                        shape = PillShape,
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Retry all failed", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Overall 4dp Progress Bar
                    val overallProgress = uiState.completedCount.toFloat() / uiState.totalCount.toFloat()
                    LinearProgressIndicator(
                        progress = { overallProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        drawStopIndicator = {}
                    )
                }
            }

            // List of flat 56dp download items (Performance optimized for 100-200 items)
            if (uiState.currentList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.DownloadForOffline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = when (uiState.selectedTab) {
                                DownloadsTab.DOWNLOADING -> "No active downloads"
                                DownloadsTab.COMPLETED -> "No completed downloads yet"
                                DownloadsTab.FAILED -> "No failed downloads"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap 'Add URL' below to download audio streams directly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = BottomListScrollPadding + 72.dp)
                ) {
                    items(
                        items = uiState.currentList,
                        key = { it.id }
                    ) { item ->
                        DownloadCard(
                            item = item,
                            isSelectionMode = uiState.isSelectionMode,
                            onItemClick = {
                                if (uiState.isSelectionMode) {
                                    downloadsViewModel.toggleItemSelection(item.id)
                                }
                            },
                            onItemLongClick = {
                                downloadsViewModel.startSelectionMode(item.id)
                            },
                            onPauseResumeClick = {
                                downloadsViewModel.togglePauseResume(item.id)
                            },
                            onRetryClick = {
                                downloadsViewModel.togglePauseResume(item.id)
                            },
                            onDeleteClick = {
                                downloadsViewModel.toggleItemSelection(item.id)
                                downloadsViewModel.deleteSelected()
                            }
                        )
                    }
                }
            }
        }

        // ADD URL BUTTON: ExtendedFloatingActionButton above mini-player and bottom bar
        if (!uiState.isSelectionMode) {
            ExtendedFloatingActionButton(
                onClick = { showAddUrlSheet = true },
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.AddLink,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                },
                text = { Text("Add URL") },
                shape = PillShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = BottomListScrollPadding + 8.dp)
            )
        }
    }

    // ModalBottomSheet for Adding URLs
    if (showAddUrlSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showAddUrlSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Download URLs",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedButton(
                        onClick = {
                            val clipText = clipboardManager.getText()?.text
                            if (!clipText.isNullOrBlank()) {
                                urlInputText = if (urlInputText.isBlank()) clipText else "$urlInputText\n$clipText"
                            }
                        },
                        shape = PillShape,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentPaste,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Paste from clipboard", style = MaterialTheme.typography.labelSmall)
                    }
                }

                // Multi-line Text Field (one link per line)
                OutlinedTextField(
                    value = urlInputText,
                    onValueChange = { urlInputText = it },
                    placeholder = {
                        Text(
                            text = "https://example.com/audio1.mp3\nhttps://example.com/stream2.flac",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    shape = RoundedCornerShape(16.dp)
                )

                // Live status line: "N links detected"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${detectedLinks.size} links detected",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (detectedLinks.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Save folder text
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = saveFolder.substringAfterLast("/"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Add to Queue Button
                Button(
                    onClick = {
                        if (detectedLinks.isNotEmpty()) {
                            downloadsViewModel.addUrls(detectedLinks)
                            urlInputText = ""
                            showAddUrlSheet = false
                        }
                    },
                    enabled = detectedLinks.isNotEmpty(),
                    shape = PillShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add to queue (${detectedLinks.size})", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
