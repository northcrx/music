package com.nishant.northmusic.ui.screens

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.text.style.TextOverflow
import com.nishant.northmusic.data.network.CdnMusicService
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material3.CircularProgressIndicator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.material3.TextButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.nishant.northmusic.data.network.CategoryPromptData
import com.nishant.northmusic.navigation.BottomListScrollPadding
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddLink
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.sp
import com.nishant.northmusic.ui.components.AppBottomSheet
import com.nishant.northmusic.ui.dialogs.OrganizeMusicDialog
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import com.nishant.northmusic.ui.components.DownloadCard
import com.nishant.northmusic.ui.theme.PillShape
import com.nishant.northmusic.ui.viewmodels.DownloadsTab
import com.nishant.northmusic.ui.viewmodels.DownloadsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    downloadsViewModel: DownloadsViewModel,
    saveFolder: String = "/storage/emulated/0/Music/Downloads",
    onNavigateToLibrary: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by downloadsViewModel.uiState.collectAsState()
    val playlistPreview by downloadsViewModel.playlistPreview.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    var showAddUrlSheet by remember { mutableStateOf(false) }
    var urlInputText by remember { mutableStateOf("") }
    var topMenuExpanded by remember { mutableStateOf(false) }
    var showOrganizeDialog by remember { mutableStateOf(false) }

    val categoryPrompt by downloadsViewModel.categoryPromptFlow.collectAsState(initial = null)
    var activePrompt by remember { mutableStateOf<CategoryPromptData?>(null) }
    var selectedCategory by remember { mutableStateOf("Hip-Hop") }
    var rememberForArtist by remember { mutableStateOf(true) }

    LaunchedEffect(categoryPrompt) {
        categoryPrompt?.let {
            activePrompt = it
            selectedCategory = "Hip-Hop"
            rememberForArtist = true
        }
    }

    val detectedLinks = remember(urlInputText) {
        urlInputText.lines()
            .mapNotNull { line ->
                val trimmed = line.trim()
                if (trimmed.isBlank()) null
                else CdnMusicService.extractFirstUrl(trimmed) ?: if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else null
            }
            .distinct()
    }

    val listState = rememberLazyListState()
    var isScrollingDown by remember { mutableStateOf(false) }
    var lastFirstVisibleItemIndex by remember { mutableIntStateOf(0) }
    var lastFirstVisibleItemScrollOffset by remember { mutableIntStateOf(0) }

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        val currentIndex = listState.firstVisibleItemIndex
        val currentOffset = listState.firstVisibleItemScrollOffset
        if (currentIndex > lastFirstVisibleItemIndex || (currentIndex == lastFirstVisibleItemIndex && currentOffset > lastFirstVisibleItemScrollOffset + 10)) {
            isScrollingDown = true
        } else if (currentIndex < lastFirstVisibleItemIndex || (currentIndex == lastFirstVisibleItemIndex && currentOffset < lastFirstVisibleItemScrollOffset - 10)) {
            isScrollingDown = false
        }
        lastFirstVisibleItemIndex = currentIndex
        lastFirstVisibleItemScrollOffset = currentOffset
    }

    var totalDragX by remember { mutableStateOf(0f) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(uiState.isSelectionMode) {
                    if (!uiState.isSelectionMode) {
                        detectHorizontalDragGestures(
                            onDragStart = { totalDragX = 0f },
                            onDragEnd = {
                                if (totalDragX > 70f) {
                                    onNavigateToLibrary()
                                }
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                totalDragX += dragAmount
                            }
                        )
                    }
                }
        ) {
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
                    actions = {
                        IconButton(
                            onClick = { showAddUrlSheet = true },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AddLink,
                                contentDescription = "Add URL",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Box {
                            IconButton(
                                onClick = { topMenuExpanded = true },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = "Menu",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DropdownMenu(
                                expanded = topMenuExpanded,
                                onDismissRequest = { topMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Pause all") },
                                    onClick = {
                                        topMenuExpanded = false
                                        downloadsViewModel.pauseAll()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Rounded.Pause,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Resume all") },
                                    onClick = {
                                        topMenuExpanded = false
                                        downloadsViewModel.resumeAll()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Rounded.PlayArrow,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Clear completed") },
                                    onClick = {
                                        topMenuExpanded = false
                                        downloadsViewModel.clearCompleted()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Rounded.ClearAll,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Organize into Music folder") },
                                    onClick = {
                                        topMenuExpanded = false
                                        showOrganizeDialog = true
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.AutoMirrored.Rounded.DriveFileMove,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Retry all failed") },
                                    onClick = {
                                        topMenuExpanded = false
                                        downloadsViewModel.retryAllFailed()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Rounded.Refresh,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            // Summary card (same style as Home tiles: surfaceContainer, 24dp corners, 16dp padding)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "${uiState.completedCount}",
                                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 32.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "/ ${uiState.totalCount} files",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${uiState.networkSpeedText} · about 2 min left",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Toggle FilledTonalButton on the right between "Pause all" and "Resume all"
                        val allPaused = uiState.downloadingItems.isNotEmpty() && uiState.downloadingItems.all { it.isPaused }
                        FilledTonalButton(
                            onClick = {
                                if (allPaused) downloadsViewModel.resumeAll() else downloadsViewModel.pauseAll()
                            },
                            shape = PillShape,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (allPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (allPaused) "Resume all" else "Pause all",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Thick 6dp rounded progress bar
                    val overallProgress = if (uiState.totalCount > 0) {
                        uiState.completedCount.toFloat() / uiState.totalCount.toFloat()
                    } else 0f
                    LinearProgressIndicator(
                        progress = { overallProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        drawStopIndicator = {}
                    )
                }
            }

            // Material Primary Tabs: Active (58), Completed (142), Failed (28)
            PrimaryTabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                val tabs = listOf(
                    DownloadsTab.ACTIVE to "Active (${uiState.downloadingCount})",
                    DownloadsTab.COMPLETED to "Completed (${uiState.completedCount})",
                    DownloadsTab.FAILED to "Failed (${uiState.failedCount})"
                )
                tabs.forEach { (tab, title) ->
                    val selected = uiState.selectedTab == tab
                    Tab(
                        selected = selected,
                        onClick = { downloadsViewModel.onTabSelected(tab) },
                        text = {
                            Text(
                                text = title,
                                maxLines = 1,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            // List of flat download items
            if (uiState.currentList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 32.dp, end = 32.dp, top = 16.dp, bottom = BottomListScrollPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .size(64.dp)
                                .clickable { showAddUrlSheet = true }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.DownloadForOffline,
                                    contentDescription = "Add URL",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = when (uiState.selectedTab) {
                                DownloadsTab.ACTIVE -> "No active downloads"
                                DownloadsTab.COMPLETED -> "No completed downloads yet"
                                DownloadsTab.FAILED -> "No failed downloads"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap the link button in the top bar to download audio streams directly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = BottomListScrollPadding)
                ) {
                    if (uiState.selectedTab == DownloadsTab.COMPLETED && uiState.completedItems.isNotEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .clickable { showOrganizeDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.DriveFileMove,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Organize downloads",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Sort songs into Category / Artist folders in your Music directory",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
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
                                downloadsViewModel.retry(item.id)
                            },
                            onDeleteClick = {
                                downloadsViewModel.cancel(item.id)
                            }
                        )
                    }
                }
            }
        }


    }

    // ModalBottomSheet for Adding URLs
    if (showAddUrlSheet) {
        val isAnyPlaylist = detectedLinks.any { CdnMusicService.isPlaylistUrl(it) }
        val actionText = if (isAnyPlaylist) "Analyze Playlist" else if (detectedLinks.isNotEmpty()) "Start Download (${detectedLinks.size})" else "Start Download"
        AppBottomSheet(
            onDismissRequest = { showAddUrlSheet = false },
            title = "Add Download URLs",
            subtitle = "Save directory: $saveFolder",
            icon = Icons.Rounded.DownloadForOffline,
            dismissButtonText = "Cancel",
            confirmButtonText = actionText,
            isConfirmEnabled = detectedLinks.isNotEmpty(),
            onConfirm = {
                if (detectedLinks.isNotEmpty()) {
                    downloadsViewModel.addUrls(detectedLinks)
                    urlInputText = ""
                    showAddUrlSheet = false
                }
            },
            onDismiss = { showAddUrlSheet = false }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    FilledTonalButton(
                        onClick = {
                            val clipText = clipboardManager.getText()?.text
                            if (!clipText.isNullOrBlank()) {
                                urlInputText = if (urlInputText.isBlank()) clipText else "$urlInputText\n$clipText"
                            }
                        },
                        shape = PillShape,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentPaste,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Paste", style = MaterialTheme.typography.labelSmall)
                    }
                }

                OutlinedTextField(
                    value = urlInputText,
                    onValueChange = { urlInputText = it },
                    label = { Text("Paste Spotify, YouTube, or JioSaavn links") },
                    placeholder = { Text("https://open.spotify.com/track/...\nhttps://youtube.com/watch?v=...\nhttps://www.jiosaavn.com/song/...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    shape = RoundedCornerShape(16.dp),
                    maxLines = 5
                )

                if (detectedLinks.isNotEmpty()) {
                    Text(
                        text = "Detected ${detectedLinks.size} valid link${if (detectedLinks.size > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }


    // Ask Category when unknown BottomSheet (Requirement 3)
    if (activePrompt != null) {
        val prompt = activePrompt!!
        AppBottomSheet(
            onDismissRequest = { activePrompt = null },
            title = "Choose Category",
            subtitle = "${prompt.title} • ${prompt.artist}",
            confirmButtonText = "Save",
            onConfirm = {
                downloadsViewModel.applyCategoryChoice(prompt, selectedCategory, rememberForArtist)
                activePrompt = null
            },
            dismissButtonText = "Skip",
            onDismiss = { activePrompt = null }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "No genre category matched for this track. Select where it belongs:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val categories = listOf("Hip-Hop", "Pop", "Bollywood", "English", "Rock", "Electronic", "Classical", "Jazz", "Chill", "Other")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) },
                            shape = PillShape
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { rememberForArtist = !rememberForArtist }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = rememberForArtist,
                        onCheckedChange = { rememberForArtist = it }
                    )
                    Text(
                        text = "Remember for ${prompt.artist}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }


    // Playlist Preview & Batch Download BottomSheet
    if (playlistPreview != null) {
        val preview = playlistPreview!!
        AppBottomSheet(
            onDismissRequest = { downloadsViewModel.dismissPlaylistPreview() },
            title = if (preview.isLoading) "Analyzing Playlist..." else preview.title,
            subtitle = if (preview.isLoading) "Extracting songs & tags..." else "${preview.author} • ${preview.source}",
            icon = Icons.AutoMirrored.Rounded.QueueMusic,
            confirmButtonText = if (preview.isLoading || preview.errorMessage != null) null else "Download Selected (${preview.selectedCount})",
            isConfirmEnabled = preview.selectedCount > 0 && !preview.isLoading,
            onConfirm = {
                downloadsViewModel.downloadSelectedPlaylistTracks()
            },
            dismissButtonText = "Cancel",
            onDismiss = { downloadsViewModel.dismissPlaylistPreview() }
        ) {
            if (preview.isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Analyzing playlist contents...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (preview.errorMessage != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = preview.errorMessage ?: "Failed to load playlist",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Header row: Select all checkbox & counter
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    downloadsViewModel.selectAllPlaylistTracks(!preview.allSelected)
                                }
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                        ) {
                            Checkbox(
                                checked = preview.allSelected,
                                onCheckedChange = { downloadsViewModel.selectAllPlaylistTracks(it) }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (preview.allSelected) "Deselect All" else "Select All",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = "${preview.selectedCount} / ${preview.totalCount} selected",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Track items list
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 340.dp)
                    ) {
                        itemsIndexed(preview.tracks) { index, track ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        downloadsViewModel.togglePlaylistTrack(index)
                                    }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = track.isSelected,
                                    onCheckedChange = { downloadsViewModel.togglePlaylistTrack(index) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = track.artist,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (track.durationSeconds > 0) {
                                    val minutes = track.durationSeconds / 60
                                    val seconds = track.durationSeconds % 60
                                    Text(
                                        text = String.format("%d:%02d", minutes, seconds),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
