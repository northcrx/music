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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.ManageSearch
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nishant.northmusic.data.model.Album
import com.nishant.northmusic.data.model.Artist
import com.nishant.northmusic.data.model.FolderItem
import com.nishant.northmusic.data.model.LibraryFilter
import com.nishant.northmusic.data.model.Playlist
import com.nishant.northmusic.data.model.SampleData
import com.nishant.northmusic.data.model.Song
import com.nishant.northmusic.navigation.BottomListScrollPadding
import com.nishant.northmusic.ui.components.ArtworkPlaceholder
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
    val highlightedLetter = remember {
        derivedStateOfLetter(sortedSongs, listState)
    }

    // Scroll to letter helper
    val scrollToLetter: (String) -> Unit = { letter ->
        val targetIndex = if (letter == "#") {
            sortedSongs.indexOfFirst { it.title.firstOrNull()?.isLetter() == false }
        } else {
            sortedSongs.indexOfFirst { it.title.startsWith(letter, ignoreCase = true) }
        }
        if (targetIndex != -1) {
            coroutineScope.launch {
                listState.scrollToItem(targetIndex)
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

        // Library Top Tabs: Songs, Albums, Artists, Playlists, Folders (ScrollableTabRow)
        val tabEntries = LibraryFilter.entries.toTypedArray()
        val selectedTabIndex = tabEntries.indexOf(uiState.selectedFilter).coerceAtLeast(0)

        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                if (selectedTabIndex < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            divider = {}
        ) {
            tabEntries.forEachIndexed { index, filter ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { libraryViewModel.onFilterSelected(filter) },
                    text = {
                        Text(
                            text = filter.label,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTabIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Main Tab Content Area
        when (uiState.selectedFilter) {
            LibraryFilter.SONGS -> {
                if (uiState.isLibraryEmpty) {
                    EmptyLibraryContent(
                        isScanning = uiState.isScanning,
                        onScanClick = { libraryViewModel.scanMusic() }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Song list spanning full width
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 4.dp, bottom = BottomListScrollPadding)
                        ) {
                            items(sortedSongs, key = { it.id }) { song ->
                                val isCurrentSong = playerState.currentSong?.id == song.id
                                val isPlaying = isCurrentSong && playerState.isPlaying

                                SongItem(
                                    song = song,
                                    isPlaying = isPlaying,
                                    onClick = { playerViewModel.playSong(song) },
                                    onOptionsClick = { /* TODO: Song context menu */ }
                                )
                            }
                        }

                        // A-Z Side Rail: Draggable Overlay Strip (24dp)
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .width(28.dp)
                                .padding(end = 4.dp, top = 8.dp, bottom = BottomListScrollPadding)
                                .alpha(railAlpha)
                                .onSizeChanged { size ->
                                    if (size.height > 0) railHeightPx = size.height.toFloat()
                                }
                                .pointerInput(alphabet) {
                                    detectTapGestures(
                                        onPress = { offset ->
                                            isDraggingRail = true
                                            val index = ((offset.y / railHeightPx) * alphabet.size)
                                                .toInt()
                                                .coerceIn(0, alphabet.lastIndex)
                                            activeRailIndex = index
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            scrollToLetter(alphabet[index])
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
                                            val index = ((offset.y / railHeightPx) * alphabet.size)
                                                .toInt()
                                                .coerceIn(0, alphabet.lastIndex)
                                            if (index != activeRailIndex) {
                                                activeRailIndex = index
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                scrollToLetter(alphabet[index])
                                            }
                                        },
                                        onVerticalDrag = { change, _ ->
                                            change.consume()
                                            val y = change.position.y
                                            val index = ((y / railHeightPx) * alphabet.size)
                                                .toInt()
                                                .coerceIn(0, alphabet.lastIndex)
                                            if (index != activeRailIndex) {
                                                activeRailIndex = index
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                scrollToLetter(alphabet[index])
                                            }
                                        },
                                        onDragEnd = {
                                            isDraggingRail = false
                                            activeRailIndex = -1
                                        },
                                        onDragCancel = {
                                            isDraggingRail = false
                                            activeRailIndex = -1
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier.fillMaxHeight(),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                alphabet.forEachIndexed { index, letter ->
                                    val isSelected = (isDraggingRail && activeRailIndex == index) ||
                                            (!isDraggingRail && highlightedLetter.value == letter)

                                    val scale by animateFloatAsState(
                                        targetValue = if (isDraggingRail && activeRailIndex != -1) {
                                            val diff = abs(index - activeRailIndex)
                                            when (diff) {
                                                0 -> 2.0f
                                                1 -> 1.5f
                                                2 -> 1.2f
                                                else -> 1.0f
                                            }
                                        } else 1.0f,
                                        animationSpec = spring(stiffness = 500f),
                                        label = "fisheyeScale"
                                    )

                                    val shiftX by animateDpAsState(
                                        targetValue = if (isDraggingRail && activeRailIndex != -1) {
                                            val diff = abs(index - activeRailIndex)
                                            when (diff) {
                                                0 -> (-14).dp
                                                1 -> (-8).dp
                                                2 -> (-4).dp
                                                else -> 0.dp
                                            }
                                        } else 0.dp,
                                        animationSpec = spring(stiffness = 500f),
                                        label = "fisheyeShift"
                                    )

                                    Text(
                                        text = letter,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            lineHeight = 10.sp
                                        ),
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                        color = when {
                                            isDraggingRail && activeRailIndex == index -> MaterialTheme.colorScheme.primary
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        },
                                        modifier = Modifier
                                            .offset(x = shiftX)
                                            .scale(scale)
                                    )
                                }
                            }
                        }

                        // Big Floating Letter Bubble Indicator while dragging
                        if (isDraggingRail && activeRailIndex in alphabet.indices) {
                            val activeLetter = alphabet[activeRailIndex]
                            val bubbleFraction = (activeRailIndex + 0.5f) / alphabet.size
                            val bubbleTop = (railHeightPx * bubbleFraction) - 28.dp.value

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-46).dp, y = bubbleTop.coerceAtLeast(0f).dp)
                                    .size(56.dp)
                                    .shadow(8.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = activeLetter,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }

            LibraryFilter.ALBUMS -> {
                AlbumsTabContent(
                    albums = SampleData.albums,
                    onAlbumClick = { album ->
                        // Play album's tracks
                        val matchingSong = SampleData.songs.firstOrNull { it.album == album.title } ?: SampleData.songs.first()
                        playerViewModel.playSong(matchingSong)
                    }
                )
            }

            LibraryFilter.ARTISTS -> {
                ArtistsTabContent(
                    artists = SampleData.artists,
                    onArtistClick = { artist ->
                        val matchingSong = SampleData.songs.firstOrNull { it.artist.contains(artist.name) } ?: SampleData.songs.first()
                        playerViewModel.playSong(matchingSong)
                    }
                )
            }

            LibraryFilter.PLAYLISTS -> {
                PlaylistsTabContent(
                    playlists = SampleData.playlists,
                    onPlaylistClick = { playlist ->
                        val firstSong = SampleData.songs.first()
                        playerViewModel.playSong(firstSong)
                    }
                )
            }

            LibraryFilter.FOLDERS -> {
                FoldersTabContent(
                    folders = SampleData.folders,
                    onFolderClick = { folder ->
                        val firstSong = SampleData.songs.first()
                        playerViewModel.playSong(firstSong)
                    }
                )
            }
        }
    }
}

@Composable
private fun AlbumsTabContent(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = BottomListScrollPadding),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(albums, key = { it.id }) { album ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onAlbumClick(album) }
            ) {
                ArtworkPlaceholder(
                    colorSeed = album.colorSeed,
                    size = 150.dp,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = album.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${album.artist} • ${album.year}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ArtistsTabContent(
    artists: List<Artist>,
    onArtistClick: (Artist) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = BottomListScrollPadding)
    ) {
        items(artists, key = { it.id }) { artist ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onArtistClick(artist) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Circular artist avatar
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(artist.colorSeed)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = artist.name.take(1),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = artist.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${artist.songCount} tracks • ${artist.albumCount} albums",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun PlaylistsTabContent(
    playlists: List<Playlist>,
    onPlaylistClick: (Playlist) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = BottomListScrollPadding)
    ) {
        items(playlists, key = { it.id }) { playlist ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlaylistClick(playlist) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(playlist.colorSeed),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.QueueMusic,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playlist.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${playlist.songCount} tracks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { /* TODO: Playlist menu */ },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FoldersTabContent(
    folders: List<FolderItem>,
    onFolderClick: (FolderItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = BottomListScrollPadding)
    ) {
        items(folders, key = { it.path }) { folder ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onFolderClick(folder) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${folder.path} • ${folder.songCount} songs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyLibraryContent(
    isScanning: Boolean,
    onScanClick: () -> Unit
) {
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
                    text = "We couldn't detect any audio tracks in your designated storage directories.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onScanClick,
                    enabled = !isScanning,
                    shape = PillShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scanning directories...")
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scan music")
                    }
                }
            }
        }
    }
}

private fun derivedStateOfLetter(
    sortedSongs: List<Song>,
    listState: androidx.compose.foundation.lazy.LazyListState
): androidx.compose.runtime.State<String> {
    return androidx.compose.runtime.derivedStateOf {
        val firstVisible = listState.firstVisibleItemIndex
        if (sortedSongs.isNotEmpty() && firstVisible in sortedSongs.indices) {
            val title = sortedSongs[firstVisible].title
            val char = title.firstOrNull()?.uppercaseChar() ?: '#'
            if (char in 'A'..'Z') char.toString() else "#"
        } else {
            "#"
        }
    }
}
