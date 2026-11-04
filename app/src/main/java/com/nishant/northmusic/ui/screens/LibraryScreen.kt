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
import com.nishant.northmusic.data.model.Song
import com.nishant.northmusic.navigation.BottomListScrollPadding
import com.nishant.northmusic.ui.components.AsyncSongArtwork
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
        animationSpec = tween(durationMillis = 250),
        label = "railAlpha"
    )

    fun scrollToLetter(letter: String) {
        val targetIndex = if (letter == "#") {
            sortedSongs.indexOfFirst { song ->
                val firstChar = song.title.firstOrNull()?.uppercaseChar() ?: '#'
                firstChar !in 'A'..'Z'
            }
        } else {
            sortedSongs.indexOfFirst { song ->
                song.title.startsWith(letter, ignoreCase = true)
            }
        }
        if (targetIndex >= 0) {
            coroutineScope.launch {
                listState.scrollToItem(targetIndex)
            }
        }
    }

    val highlightedLetter = remember(sortedSongs, listState) {
        derivedStateOfLetter(sortedSongs, listState)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Sticky Top App Bar
        TopAppBar(
            windowInsets = WindowInsets(0.dp),
            title = {
                if (uiState.isSearchActive) {
                    TextField(
                        value = uiState.searchQuery,
                        onValueChange = { libraryViewModel.updateSearchQuery(it) },
                        placeholder = {
                            Text(
                                "Filter in ${uiState.selectedFilter.label.lowercase()}...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { libraryViewModel.updateSearchQuery("") }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Clear,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = "Library",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            actions = {
                IconButton(onClick = { libraryViewModel.toggleSearch() }) {
                    Icon(
                        imageVector = if (uiState.isSearchActive) Icons.Rounded.Clear else Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (uiState.selectedFilter == LibraryFilter.SONGS) {
                    Box {
                        IconButton(onClick = { sortMenuExpanded = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Sort,
                                contentDescription = "Sort",
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
                                    trailingIcon = if (selectedSort == sortOption) {
                                        {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    } else null,
                                    onClick = {
                                        selectedSort = sortOption
                                        sortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Scrollable Tab Row with indicator
        val filters = LibraryFilter.entries.toTypedArray()
        val selectedIndex = filters.indexOf(uiState.selectedFilter)

        ScrollableTabRow(
            selectedTabIndex = selectedIndex,
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                if (selectedIndex < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                        color = MaterialTheme.colorScheme.primary,
                        height = 3.dp
                    )
                }
            },
            divider = {}
        ) {
            filters.forEachIndexed { index, filter ->
                val isSelected = index == selectedIndex
                Tab(
                    selected = isSelected,
                    onClick = { libraryViewModel.selectFilter(filter) },
                    text = {
                        Text(
                            text = filter.label,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Content Area by Selected Tab
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
                                    onClick = { playerViewModel.playSong(song, sortedSongs) },
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
                                                else -> 0.9f
                                            }
                                        } else {
                                            if (isSelected) 1.25f else 1.0f
                                        },
                                        animationSpec = spring(stiffness = 800f),
                                        label = "railCharScale"
                                    )

                                    Text(
                                        text = letter,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        },
                                        modifier = Modifier
                                            .scale(scale)
                                            .padding(vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        // Large Floating Alphabet Preview Bubble (56dp circle) shown during touch drag
                        val showBubble = isDraggingRail && activeRailIndex in alphabet.indices
                        if (showBubble) {
                            val activeLetter = if (activeRailIndex in alphabet.indices) alphabet[activeRailIndex] else "A"
                            val bubbleTop by animateDpAsState(
                                targetValue = if (activeRailIndex >= 0) {
                                    val fraction = activeRailIndex.toFloat() / alphabet.size.toFloat()
                                    (fraction * 450).dp
                                } else 200.dp,
                                animationSpec = spring(stiffness = 900f),
                                label = "bubbleOffset"
                            )

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-46).dp, y = bubbleTop.coerceAtLeast(0.dp))
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
                    albums = uiState.albums,
                    onAlbumClick = { album ->
                        val matchingSongs = uiState.songs.filter {
                            it.albumId == album.albumId || it.album.equals(album.title, ignoreCase = true)
                        }
                        matchingSongs.firstOrNull()?.let {
                            playerViewModel.playSong(it, matchingSongs)
                        }
                    }
                )
            }

            LibraryFilter.ARTISTS -> {
                ArtistsTabContent(
                    artists = uiState.artists,
                    onArtistClick = { artist ->
                        val matchingSongs = uiState.songs.filter {
                            it.artistId.toString() == artist.id || it.artist.contains(artist.name, ignoreCase = true)
                        }
                        matchingSongs.firstOrNull()?.let {
                            playerViewModel.playSong(it, matchingSongs)
                        }
                    }
                )
            }

            LibraryFilter.PLAYLISTS -> {
                PlaylistsTabContent(
                    playlists = uiState.playlists,
                    onPlaylistClick = {
                        uiState.songs.firstOrNull()?.let {
                            playerViewModel.playSong(it, uiState.songs)
                        }
                    }
                )
            }

            LibraryFilter.FOLDERS -> {
                FoldersTabContent(
                    folders = uiState.folders,
                    onFolderClick = { folder ->
                        val matchingSongs = uiState.songs.filter {
                            it.relativeFolder == folder.path || it.filePath.contains(folder.name)
                        }
                        matchingSongs.firstOrNull()?.let {
                            playerViewModel.playSong(it, matchingSongs)
                        }
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
    if (albums.isEmpty()) {
        EmptyCategoryNotice(message = "No albums found in your library")
        return
    }

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
                AsyncSongArtwork(
                    albumArtUri = album.albumArtUri,
                    colorSeed = album.colorSeed,
                    size = 150.dp,
                    shape = RoundedCornerShape(18.dp),
                    iconSize = 44.dp,
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
    if (artists.isEmpty()) {
        EmptyCategoryNotice(message = "No artists found in your library")
        return
    }

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
    if (playlists.isEmpty()) {
        EmptyCategoryNotice(message = "No playlists created yet")
        return
    }

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
    if (folders.isEmpty()) {
        EmptyCategoryNotice(message = "No audio folders found")
        return
    }

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
private fun EmptyCategoryNotice(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
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
