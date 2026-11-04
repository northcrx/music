package com.nishant.northmusic.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nishant.northmusic.data.model.LyricLine
import com.nishant.northmusic.data.model.RepeatMode
import com.nishant.northmusic.data.model.Song
import com.nishant.northmusic.ui.components.ArtworkPlaceholder
import com.nishant.northmusic.ui.components.SongItem
import com.nishant.northmusic.ui.theme.PillShape
import com.nishant.northmusic.ui.theme.SquircleLargeArtworkShape
import com.nishant.northmusic.ui.viewmodels.PlayerViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    playerViewModel: PlayerViewModel,
    onDismiss: () -> Unit,
    onNavigateToEqualizer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val playerState by playerViewModel.playerState.collectAsState()
    val queue by playerViewModel.queue.collectAsState()
    val lyrics by playerViewModel.lyrics.collectAsState()
    val song = playerState.currentSong ?: return

    var sliderPosition by remember(playerState.currentPositionSeconds) {
        mutableFloatStateOf(playerState.currentPositionSeconds.toFloat())
    }

    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedSheetTab by remember { mutableIntStateOf(0) } // 0 = Lyrics, 1 = Up Next
    var showMoreMenu by remember { mutableStateOf(false) }

    // Fixed page layout: No verticalScroll on outer column so the player is always stable
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top App Bar (0.dp insets to sit ~16dp below system bar)
        TopAppBar(
            windowInsets = WindowInsets(0.dp),
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "PLAYING FROM PLAYLIST",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = song.album,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(32.dp)
                    )
                }
            },
            actions = {
                Box {
                    IconButton(
                        onClick = { showMoreMenu = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        if (onNavigateToEqualizer != null) {
                            DropdownMenuItem(
                                text = { Text("Equalizer") },
                                onClick = {
                                    showMoreMenu = false
                                    onNavigateToEqualizer()
                                },
                                leadingIcon = {
                                    Icon(Icons.Rounded.GraphicEq, contentDescription = null)
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("View album") },
                            onClick = {
                                showMoreMenu = false
                                /* TODO: Navigate to album details */
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("View artist") },
                            onClick = {
                                showMoreMenu = false
                                /* TODO: Navigate to artist details */
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (lyrics.isEmpty()) "Load demo lyrics" else "Simulate no lyrics") },
                            onClick = {
                                showMoreMenu = false
                                playerViewModel.setEmptyLyrics(lyrics.isNotEmpty())
                            }
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Artwork (fills vertical space proportionally via weight, keeping everything on screen)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            ArtworkPlaceholder(
                size = 320.dp,
                shape = SquircleLargeArtworkShape,
                colorSeed = song.colorSeed,
                iconSize = 88.dp,
                modifier = Modifier
                    .aspectRatio(1f, matchHeightConstraintsFirst = true)
                    .shadow(
                        elevation = 20.dp,
                        shape = SquircleLargeArtworkShape,
                        ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    )
            )
        }

        // Song Title, Artist, Badges, and Quick Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // High Quality Audio Badge
                Surface(
                    shape = PillShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.HighQuality,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "High quality",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = song.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(
                    onClick = { playerViewModel.toggleFavorite() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (playerState.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (playerState.isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
                IconButton(
                    onClick = { /* TODO: Add to playlist */ },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                        contentDescription = "Add to playlist",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Progress Slider with thick 8dp track & round thumb
        Slider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            onValueChangeFinished = { playerViewModel.seekTo(sliderPosition.toInt()) },
            valueRange = 0f..song.durationSeconds.toFloat(),
            thumb = {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(8.dp),
                    colors = SliderDefaults.colors(
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                    thumbTrackGapSize = 0.dp,
                    drawStopIndicator = null
                )
            },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val currentMinutes = sliderPosition.toInt() / 60
            val currentSeconds = sliderPosition.toInt() % 60
            Text(
                text = "%d:%02d".format(currentMinutes, currentSeconds),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = song.formattedDuration,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Transport Controls (Play 80dp, Prev/Next 56dp, Shuffle/Repeat 48dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle
            IconButton(
                onClick = { playerViewModel.toggleShuffle() },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (playerState.isShuffle) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                    )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (playerState.isShuffle) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Previous
            IconButton(
                onClick = { playerViewModel.skipPrevious() },
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = "Previous",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Play / Pause Hero Button
            FilledIconButton(
                onClick = { playerViewModel.togglePlayPause() },
                modifier = Modifier
                    .size(80.dp)
                    .shadow(
                        elevation = 14.dp,
                        shape = RoundedCornerShape(26.dp),
                        ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                    ),
                shape = RoundedCornerShape(26.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = if (playerState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(40.dp)
                )
            }

            // Next
            IconButton(
                onClick = { playerViewModel.skipNext() },
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = "Next",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Repeat
            IconButton(
                onClick = { playerViewModel.cycleRepeatMode() },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (playerState.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                    )
            ) {
                Icon(
                    imageVector = if (playerState.repeatMode == RepeatMode.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    contentDescription = "Repeat",
                    tint = if (playerState.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Bottom Auxiliary Buttons (Lyrics & Up Next - open ModalBottomSheet)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        selectedSheetTab = 0
                        showBottomSheet = true
                    },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lyrics,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Lyrics",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        selectedSheetTab = 1
                        showBottomSheet = true
                    },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Up Next (${queue.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    // ModalBottomSheet covering lower section of Now Playing screen with internal scrolling
    if (showBottomSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        val scope = rememberCoroutineScope()

        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(560.dp)
            ) {
                // Two tabs: Lyrics and Up Next
                PrimaryTabRow(
                    selectedTabIndex = selectedSheetTab,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedSheetTab == 0,
                        onClick = { selectedSheetTab = 0 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.Lyrics, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Lyrics")
                            }
                        }
                    )
                    Tab(
                        selected = selectedSheetTab == 1,
                        onClick = { selectedSheetTab = 1 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Up Next (${queue.size})")
                            }
                        }
                    )
                }

                // Internal scrollable content based on active tab
                if (selectedSheetTab == 0) {
                    LyricsTabContent(
                        lyrics = lyrics,
                        currentPositionSeconds = playerState.currentPositionSeconds,
                        onSeekToLine = { time ->
                            playerViewModel.seekTo(time)
                            // TODO: Real seek in playback engine
                        },
                        onSearchOnline = {
                            playerViewModel.searchLyricsOnline()
                            // TODO: Query online lyrics provider
                        }
                    )
                } else {
                    UpNextTabContent(
                        currentSong = song,
                        queue = queue,
                        onSongClick = { selectedSong ->
                            playerViewModel.playSong(selectedSong)
                            scope.launch { sheetState.hide() }
                            showBottomSheet = false
                        },
                        onRemoveFromQueue = { songId ->
                            playerViewModel.removeFromQueue(songId)
                            // TODO: Remove song from ExoPlayer queue
                        },
                        onMoveQueueItem = { from, to ->
                            playerViewModel.moveQueueItem(from, to)
                            // TODO: Reorder items in ExoPlayer timeline
                        },
                        onClearQueue = {
                            playerViewModel.clearQueue()
                            // TODO: Clear playback queue
                        }
                    )
                }
            }
        }
    }
}

/**
 * Lyrics Tab:
 * Displays synchronized lyrics with current line highlighted and dimmed surrounding lines.
 * Auto-scrolls to the current line as song position progresses.
 * Tap a line to seek to that timestamp.
 * Includes empty state with "Search online" button when no lyrics exist.
 */
@Composable
fun LyricsTabContent(
    lyrics: List<LyricLine>,
    currentPositionSeconds: Int,
    onSeekToLine: (Int) -> Unit,
    onSearchOnline: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (lyrics.isEmpty()) {
        // Empty State: "No lyrics found" + "Search online" button
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Lyrics,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "No lyrics found",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Synchronized lyrics are not available for this track yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onSearchOnline,
                shape = PillShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Search online")
            }
            // Architecture Note: Real lyrics later will read embedded ID3 USLT/SYLT tags or local .lrc files
        }
    } else {
        val listState = rememberLazyListState()

        // Identify current active lyric line
        val activeIndex = remember(lyrics, currentPositionSeconds) {
            val idx = lyrics.indexOfLast { it.timeSeconds <= currentPositionSeconds }
            if (idx == -1) 0 else idx
        }

        // Auto-scroll so current lyric line stays centered
        LaunchedEffect(activeIndex) {
            if (lyrics.isNotEmpty() && activeIndex in lyrics.indices) {
                listState.animateScrollToItem(
                    index = activeIndex,
                    scrollOffset = -150
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 80.dp, bottom = 120.dp, start = 24.dp, end = 24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            itemsIndexed(lyrics) { index, line ->
                val isCurrent = index == activeIndex

                val textColor by animateColorAsState(
                    targetValue = if (isCurrent) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                    },
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    label = "lyricColor"
                )

                Text(
                    text = line.text,
                    style = if (isCurrent) {
                        MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            lineHeight = 28.sp
                        )
                    } else {
                        MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Normal,
                            fontSize = 17.sp,
                            lineHeight = 24.sp
                        )
                    },
                    color = textColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            onSeekToLine(line.timeSeconds)
                            // TODO: Seek playback to synchronized line timestamp
                        }
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Up Next Tab:
 * Queue list using the shared SongItem with drag handles to reorder, swipe to remove,
 * current song at top, and a "Clear queue" action.
 */
@Composable
fun UpNextTabContent(
    currentSong: Song,
    queue: List<Song>,
    onSongClick: (Song) -> Unit,
    onRemoveFromQueue: (String) -> Unit,
    onMoveQueueItem: (from: Int, to: Int) -> Unit,
    onClearQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Queue Summary & Clear Queue Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${queue.size} songs in queue",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            TextButton(
                onClick = onClearQueue,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ClearAll,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Clear queue")
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Section 1: Currently Playing Song
            item {
                Text(
                    text = "NOW PLAYING",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )

                SongItem(
                    song = currentSong,
                    isPlaying = true,
                    onClick = { /* Already playing */ },
                    onOptionsClick = { /* TODO: Song options */ }
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Section 2: Queue items with SwipeToDismissBox & Drag Handles
            item {
                Text(
                    text = "UP NEXT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }

            itemsIndexed(
                items = queue.filter { it.id != currentSong.id },
                key = { _, song -> song.id }
            ) { index, song ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart || value == SwipeToDismissBoxValue.StartToEnd) {
                            onRemoveFromQueue(song.id)
                            true
                        } else false
                    }
                )

                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Remove from queue",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerLow),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            SongItem(
                                song = song,
                                isPlaying = false,
                                onClick = { onSongClick(song) },
                                onOptionsClick = { /* TODO: Options */ }
                            )
                        }

                        // Reorder Handle
                        IconButton(
                            onClick = {
                                val actualIdx = queue.indexOfFirst { it.id == song.id }
                                if (actualIdx > 1) {
                                    onMoveQueueItem(actualIdx, actualIdx - 1)
                                }
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DragHandle,
                                contentDescription = "Drag to reorder",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
