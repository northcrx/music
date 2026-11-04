package com.nishant.northmusic.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ManageSearch
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nishant.northmusic.data.model.LibraryFilter
import com.nishant.northmusic.data.model.Song
import com.nishant.northmusic.navigation.BottomListScrollPadding
import com.nishant.northmusic.ui.components.SongItem
import com.nishant.northmusic.ui.theme.PillShape
import com.nishant.northmusic.ui.viewmodels.LibraryViewModel
import com.nishant.northmusic.ui.viewmodels.PlayerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    libraryViewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by libraryViewModel.uiState.collectAsState()
    val playerState by playerViewModel.playerState.collectAsState()

    val alphabet = remember {
        listOf("#", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z")
    }

    var sortMenuExpanded by remember { mutableStateOf(false) }
    var selectedSort by remember { mutableStateOf("A-Z") }

    val sortedSongs = remember(uiState.songs, selectedSort) {
        when (selectedSort) {
            "Recently added" -> uiState.songs.reversed()
            "Most played" -> uiState.songs.sortedByDescending { it.durationSeconds }
            "Duration" -> uiState.songs.sortedBy { it.durationSeconds }
            else -> uiState.songs.sortedBy { it.title }
        }
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var isDraggingRail by remember { mutableStateOf(false) }
    var activeRailIndex by remember { mutableIntStateOf(-1) }
    var railHeightPx by remember { mutableFloatStateOf(1f) }

    // Auto fade in / fade out timer for the rail overlay (1.5 seconds inactivity)
    var isRailVisible by remember { mutableStateOf(false) }

    LaunchedEffect(listState.isScrollInProgress, isDraggingRail) {
        if (listState.isScrollInProgress || isDraggingRail) {
            isRailVisible = true
        } else {
            delay(1500)
            isRailVisible = false
        }
    }

    val railAlpha by animateFloatAsState(
        targetValue = if (isRailVisible || isDraggingRail) 1f else 0f,
        animationSpec = tween(300),
        label = "railAlpha"
    )

    // Highlight letter of the first visible song during normal scrolling
    val currentVisibleLetter = remember(listState.firstVisibleItemIndex, sortedSongs) {
        if (sortedSongs.isNotEmpty()) {
            val idx = listState.firstVisibleItemIndex.coerceIn(0, sortedSongs.lastIndex)
            val c = sortedSongs[idx].title.firstOrNull()?.uppercaseChar() ?: '#'
            if (c in 'A'..'Z') c.toString() else "#"
        } else "#"
    }

    // Touch handler calculating index exclusively from touch position INSIDE rail size
    fun onRailTouch(touchY: Float) {
        if (railHeightPx <= 0f) return
        val fraction = (touchY / railHeightPx).coerceIn(0f, 0.999f)
        val index = (fraction * alphabet.size).toInt().coerceIn(0, alphabet.lastIndex)

        if (index != activeRailIndex) {
            activeRailIndex = index
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

            // Scroll to the first song starting with that letter (or the next available letter)
            var targetSongIndex = -1
            for (i in index until alphabet.size) {
                val l = alphabet[i]
                val found = sortedSongs.indexOfFirst { song ->
                    val c = song.title.firstOrNull()?.uppercaseChar() ?: '#'
                    if (l == "#") !c.isLetter() else c.toString().equals(l, ignoreCase = true)
                }
                if (found != -1) {
                    targetSongIndex = found
                    break
                }
            }

            if (targetSongIndex == -1 && sortedSongs.isNotEmpty()) {
                targetSongIndex = sortedSongs.lastIndex
            }

            if (targetSongIndex != -1) {
                coroutineScope.launch {
                    listState.scrollToItem(targetSongIndex)
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Top App Bar
        TopAppBar(
            windowInsets = WindowInsets(0.dp),
            title = {
                Text(
                    text = "Library",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            actions = {
                IconButton(
                    onClick = { libraryViewModel.toggleSearch() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search library",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box {
                    IconButton(
                        onClick = { sortMenuExpanded = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Sort,
                            contentDescription = "Sort library",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        listOf("A-Z", "Recently added", "Most played", "Duration").forEach { sortOption ->
                            DropdownMenuItem(
                                text = { Text(sortOption) },
                                onClick = {
                                    selectedSort = sortOption
                                    sortMenuExpanded = false
                                },
                                leadingIcon = if (selectedSort == sortOption) {
                                    {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else null
                            )
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Search Bar (Expandable)
        AnimatedVisibility(
            visible = uiState.isSearchActive,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                TextField(
                    value = uiState.searchQuery,
                    onValueChange = { libraryViewModel.onSearchQueryChange(it) },
                    placeholder = {
                        Text(
                            text = "Filter library tracks...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { libraryViewModel.onSearchQueryChange("") },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Clear,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = PillShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Filter Chips Row (Songs, Albums, Artists, Folders)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(LibraryFilter.entries.toTypedArray()) { filter ->
                val isSelected = filter == uiState.selectedFilter
                FilterChip(
                    selected = isSelected,
                    onClick = { libraryViewModel.onFilterSelected(filter) },
                    leadingIcon = {
                        val icon = when (filter) {
                            LibraryFilter.SONGS -> if (isSelected) Icons.Rounded.Check else Icons.Rounded.MusicNote
                            LibraryFilter.ALBUMS -> Icons.Rounded.Album
                            LibraryFilter.ARTISTS -> Icons.Rounded.Person
                            LibraryFilter.FOLDERS -> Icons.Rounded.Folder
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = {
                        Text(
                            text = filter.label,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    shape = PillShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = null
                )
            }

            // Preview Empty State Switch Chip
            item {
                Surface(
                    shape = PillShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier
                        .clip(PillShape)
                        .clickable { libraryViewModel.toggleEmptyStateForPreview() }
                ) {
                    Text(
                        text = if (uiState.isLibraryEmpty) "Show Songs" else "Preview Empty State",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Main Content Area: Song List spanning full width + Overlay A-Z Rail
        if (uiState.isLibraryEmpty) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ManageSearch,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(38.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Looking for local music?",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Index audio files directly from your internal storage, downloads folder, or external SD card.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { libraryViewModel.scanMusic() },
                            enabled = !uiState.isScanning,
                            shape = PillShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            if (uiState.isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Scanning device storage...")
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ManageSearch,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Scan music",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                // List of songs: Full width, no extra end padding, 3-dot menu at far right (16dp margin)
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = BottomListScrollPadding)
                ) {
                    items(
                        items = sortedSongs,
                        key = { it.id }
                    ) { song ->
                        SongItem(
                            song = song,
                            isPlaying = playerState.currentSong?.id == song.id && playerState.isPlaying,
                            onClick = { playerViewModel.playSong(song) }
                        )
                    }
                }

                // Overlay A-Z Side Rail: 24dp wide on the right edge with fade in/out and fisheye magnify
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(28.dp)
                        .alpha(railAlpha)
                        .onSizeChanged { railHeightPx = it.height.toFloat() }
                        .pointerInput(alphabet) {
                            detectTapGestures(
                                onPress = { offset ->
                                    isDraggingRail = true
                                    onRailTouch(offset.y)
                                    tryAwaitRelease()
                                    isDraggingRail = false
                                    activeRailIndex = -1
                                }
                            )
                        }
                        .pointerInput(alphabet) {
                            detectVerticalDragGestures(
                                onDragStart = { offset ->
                                    isDraggingRail = true
                                    onRailTouch(offset.y)
                                },
                                onDragEnd = {
                                    isDraggingRail = false
                                    activeRailIndex = -1
                                },
                                onDragCancel = {
                                    isDraggingRail = false
                                    activeRailIndex = -1
                                },
                                onVerticalDrag = { change, _ ->
                                    change.consume()
                                    onRailTouch(change.position.y)
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(vertical = 12.dp)
                    ) {
                        alphabet.forEachIndexed { index, letter ->
                            val isUnderFinger = isDraggingRail && index == activeRailIndex
                            val distance = if (isDraggingRail && activeRailIndex != -1) {
                                abs(index - activeRailIndex)
                            } else 100

                            // Fisheye smooth magnify curve over 3 letters on each side
                            val targetScale = when (distance) {
                                0 -> 2.2f
                                1 -> 1.6f
                                2 -> 1.3f
                                3 -> 1.15f
                                else -> 1.0f
                            }
                            val targetShiftLeft = when (distance) {
                                0 -> (-20).dp
                                1 -> (-12).dp
                                2 -> (-6).dp
                                3 -> (-2).dp
                                else -> 0.dp
                            }

                            val scale by animateFloatAsState(targetValue = targetScale, animationSpec = spring(), label = "scale")
                            val shiftLeft by animateDpAsState(targetValue = targetShiftLeft, animationSpec = spring(), label = "shift")

                            val isHighlighted = isUnderFinger || (activeRailIndex == -1 && letter == currentVisibleLetter)

                            Text(
                                text = letter,
                                fontSize = 10.sp,
                                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
                                color = if (isHighlighted) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                },
                                modifier = Modifier
                                    .offset(x = shiftLeft)
                                    .scale(scale)
                            )
                        }
                    }
                }

                // Big circle bubble with the current letter to the left of the rail while dragging
                if (isDraggingRail && activeRailIndex in alphabet.indices) {
                    val activeLetter = alphabet[activeRailIndex]
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 44.dp)
                            .size(60.dp)
                            .shadow(16.dp, CircleShape)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = activeLetter,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
