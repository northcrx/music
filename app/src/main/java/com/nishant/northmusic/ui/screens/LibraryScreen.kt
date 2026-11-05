package com.nishant.northmusic.ui.screens

import androidx.compose.material.icons.rounded.AutoFixHigh

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nishant.northmusic.util.PermissionUtils
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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
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
import androidx.compose.runtime.DisposableEffect
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
import com.nishant.northmusic.data.repository.SettingsRepository
import com.nishant.northmusic.data.repository.normalizeText
import com.nishant.northmusic.data.repository.splitArtistNames
import com.nishant.northmusic.navigation.BottomListScrollPadding
import com.nishant.northmusic.ui.components.AsyncSongArtwork
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Refresh
import com.nishant.northmusic.data.network.ArtistPhotoDownloader
import java.io.FileOutputStream
import java.io.File
import kotlinx.coroutines.Dispatchers
import com.nishant.northmusic.data.local.LocalStatsDatabase
import com.nishant.northmusic.ui.components.ArtistAvatar
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.CallMerge
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.PlayArrow
import com.nishant.northmusic.ui.dialogs.BatchTagEditorDialog
import com.nishant.northmusic.ui.dialogs.MergeAlbumsDialog
import com.nishant.northmusic.ui.dialogs.RenameArtistDialog
import com.nishant.northmusic.ui.dialogs.TagEditorDialog
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
    onNavigateToFixMetadata: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by libraryViewModel.uiState.collectAsState()
    val playerState by playerViewModel.playerState.collectAsState()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember { mutableStateOf(PermissionUtils.hasAudioPermission(context)) }
    var permissionDeniedOnce by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        val granted = PermissionUtils.hasAudioPermission(context)
        hasPermission = granted
        if (granted) {
            libraryViewModel.scanMusic()
        } else {
            permissionDeniedOnce = true
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = PermissionUtils.hasAudioPermission(context)
                hasPermission = granted
                if (granted && uiState.isLibraryEmpty) {
                    libraryViewModel.scanMusic()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }


    val alphabet = remember {
        listOf("#", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z")
    }

    var sortMenuExpanded by remember { mutableStateOf(false) }
    var selectedSort by remember { mutableStateOf("A-Z") }

    var songToEditTags by remember { mutableStateOf<Song?>(null) }
    var albumToMerge by remember { mutableStateOf<Album?>(null) }
    var albumToBatchEdit by remember { mutableStateOf<Album?>(null) }
    var artistToRename by remember { mutableStateOf<Artist?>(null) }
    var artistToBatchEdit by remember { mutableStateOf<Artist?>(null) }

    var selectedAlbumDetail by remember { mutableStateOf<Album?>(null) }
    var selectedArtistDetail by remember { mutableStateOf<Artist?>(null) }
    var selectedPlaylistDetail by remember { mutableStateOf<Playlist?>(null) }
    var selectedFolderDetail by remember { mutableStateOf<FolderItem?>(null) }

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

    if (selectedAlbumDetail != null) {
        val album = selectedAlbumDetail!!
        val albumSongs = remember(album, uiState.songs) {
            uiState.songs.filter {
                it.albumId == album.albumId || normalizeText(it.album) == normalizeText(album.title)
            }
        }
        LibraryDetailSubScreen(
            title = album.title,
            subtitle = if (album.year > 0) "${album.artist} • ${album.year}" else album.artist,
            artworkUri = album.albumArtUri,
            colorSeed = album.colorSeed,
            songs = albumSongs,
            playerViewModel = playerViewModel,
            onBack = { selectedAlbumDetail = null },
            onOptionsClick = { albumToBatchEdit = album },
            onEditSongTags = { songToEditTags = it },
            modifier = modifier
        )
    } else if (selectedArtistDetail != null) {
        val artist = selectedArtistDetail!!
        val settings = SettingsRepository.current
        val artistSongs = remember(artist, uiState.songs) {
            uiState.songs.filter { s ->
                val split = splitArtistNames(s.artist, settings.splitArtistsOnAmpersand, settings.dontSplitArtistNames)
                split.any { it.equals(artist.name, ignoreCase = true) }
            }
        }
        val firstSongArtwork = artistSongs.firstOrNull { it.albumArtUri != null }?.albumArtUri
        LibraryDetailSubScreen(
            title = artist.name,
            subtitle = "${artist.albumCount} albums • ${artist.songCount} songs",
            artworkUri = artist.savedPhotoUri?.let { android.net.Uri.parse(it) } ?: artist.firstAlbumArtUri ?: firstSongArtwork,
            colorSeed = artist.colorSeed,
            songs = artistSongs,
            playerViewModel = playerViewModel,
            onBack = { selectedArtistDetail = null },
            onOptionsClick = { artistToBatchEdit = artist },
            onEditSongTags = { songToEditTags = it },
            modifier = modifier
        )
    } else if (selectedPlaylistDetail != null) {
        val playlist = selectedPlaylistDetail!!
        var playlistSongIds by remember(playlist.id) { mutableStateOf<List<String>>(emptyList()) }
        LaunchedEffect(playlist.id) {
            if (playlist.id != "fav") {
                playlistSongIds = LocalStatsDatabase.getInstance(context).getPlaylistSongIds(playlist.id)
            }
        }
        val playlistSongs = remember(playlist.id, playlistSongIds, uiState.songs) {
            if (playlist.id == "fav") {
                uiState.songs.filter { it.isFavorite }
            } else {
                val map = uiState.songs.associateBy { it.id }
                playlistSongIds.mapNotNull { map[it] }
            }
        }
        val firstSongArtwork = playlistSongs.firstOrNull { it.albumArtUri != null }?.albumArtUri
        LibraryDetailSubScreen(
            title = playlist.title,
            subtitle = "${playlistSongs.size} songs",
            artworkUri = firstSongArtwork,
            colorSeed = playlist.colorSeed,
            songs = playlistSongs,
            playerViewModel = playerViewModel,
            onBack = { selectedPlaylistDetail = null },
            onEditSongTags = { songToEditTags = it },
            modifier = modifier
        )
    } else if (selectedFolderDetail != null) {
        val folder = selectedFolderDetail!!
        val folderSongs = remember(folder, uiState.songs) {
            uiState.songs.filter {
                it.relativeFolder == folder.path || it.filePath.contains(folder.name)
            }
        }
        val firstSongArtwork = folderSongs.firstOrNull { it.albumArtUri != null }?.albumArtUri
        LibraryDetailSubScreen(
            title = folder.name,
            subtitle = folder.path,
            artworkUri = firstSongArtwork,
            colorSeed = (folder.name.hashCode().toLong() and 0xFFFFFFL) or 0xFF000000L,
            songs = folderSongs,
            playerViewModel = playerViewModel,
            onBack = { selectedFolderDetail = null },
            onEditSongTags = { songToEditTags = it },
            modifier = modifier
        )
    } else {
        var totalDragX by remember { mutableStateOf(0f) }
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { totalDragX = 0f },
                        onDragEnd = {
                            if (totalDragX > 70f) {
                                onNavigateToHome()
                            } else if (totalDragX < -70f) {
                                onNavigateToDownloads()
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            totalDragX += dragAmount
                        }
                    )
                }
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

        if (uiState.categoryFilter != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = PillShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.clickable { libraryViewModel.clearCategoryFilter() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Category: ${uiState.categoryFilter}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                TextButton(onClick = { libraryViewModel.clearCategoryFilter() }) {
                    Text(
                        text = "Show all",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Content Area by Selected Tab
        when (uiState.selectedFilter) {
            LibraryFilter.SONGS -> {
                if (uiState.isLibraryEmpty) {
                    EmptyLibraryContent(
                        isScanning = uiState.isScanning,
                        hasPermission = hasPermission,
                        permissionDeniedOnce = permissionDeniedOnce,
                        onRequestPermission = {
                            permissionLauncher.launch(PermissionUtils.getPermissionsToRequest())
                        },
                        onOpenSettings = {
                            PermissionUtils.openAppSettings(context)
                        },
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
                                    onClick = { playerViewModel.playSong(song, sortedSongs, source = "Songs") },
                                    onOptionsClick = { /* Song context menu */ },
                                    onEditTags = { songToEditTags = song }
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
                        selectedAlbumDetail = album
                    },
                    onMergeAlbum = { albumToMerge = it },
                    onBatchEditAlbum = { albumToBatchEdit = it }
                )
            }

            LibraryFilter.ARTISTS -> {
                ArtistsTabContent(
                    artists = uiState.artists,
                    onArtistClick = { artist ->
                        selectedArtistDetail = artist
                    },
                    onRenameArtist = { artistToRename = it },
                    onBatchEditArtist = { artistToBatchEdit = it }
                )
            }

            LibraryFilter.PLAYLISTS -> {
                PlaylistsTabContent(
                    playlists = uiState.playlists,
                    onPlaylistClick = { playlist ->
                        selectedPlaylistDetail = playlist
                    }
                )
            }

            LibraryFilter.FOLDERS -> {
                FoldersTabContent(
                    folders = uiState.folders,
                    onFolderClick = { folder ->
                        selectedFolderDetail = folder
                    }
                )
            }

            LibraryFilter.DOWNLOADED -> {
                val downloaded = remember(sortedSongs) {
                    sortedSongs.filter {
                        it.filePath.contains("NorthMusic", ignoreCase = true) ||
                        it.filePath.contains("Download", ignoreCase = true)
                    }
                }
                if (downloaded.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Download,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "No downloaded tracks",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Songs downloaded from online search or links will appear here organized in your folder structure.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = BottomListScrollPadding)
                    ) {
                        items(downloaded, key = { it.id }) { song ->
                            val isCurrentSong = playerState.currentSong?.id == song.id
                            val isPlaying = isCurrentSong && playerState.isPlaying

                            SongItem(
                                song = song,
                                isPlaying = isPlaying,
                                onClick = { playerViewModel.playSong(song, downloaded, source = "Downloaded") },
                                onOptionsClick = { },
                                onEditTags = { songToEditTags = song }
                            )
                        }
                    }
                }
            }
        }
    }
}

    if (songToEditTags != null) {
        TagEditorDialog(
            song = songToEditTags!!,
            onDismiss = {
                songToEditTags = null
                libraryViewModel.scanMusic()
            },
            playerViewModel = playerViewModel
        )
    }

    if (albumToMerge != null) {
        MergeAlbumsDialog(
            sourceAlbum = albumToMerge!!,
            availableAlbums = uiState.albums,
            songs = uiState.songs,
            onDismiss = {
                albumToMerge = null
                libraryViewModel.scanMusic()
            }
        )
    }

    if (albumToBatchEdit != null) {
        val albumSongs = uiState.songs.filter {
            it.albumId == albumToBatchEdit!!.albumId || normalizeText(it.album) == normalizeText(albumToBatchEdit!!.title)
        }
        BatchTagEditorDialog(
            songs = albumSongs,
            onDismiss = {
                albumToBatchEdit = null
                libraryViewModel.scanMusic()
            }
        )
    }

    if (artistToRename != null) {
        RenameArtistDialog(
            artist = artistToRename!!,
            songs = uiState.songs,
            onDismiss = {
                artistToRename = null
                libraryViewModel.scanMusic()
            }
        )
    }

    if (artistToBatchEdit != null) {
        val settings = SettingsRepository.current
        val artistSongs = uiState.songs.filter { s ->
            val split = splitArtistNames(s.artist, settings.splitArtistsOnAmpersand, settings.dontSplitArtistNames)
            split.any { it.equals(artistToBatchEdit!!.name, ignoreCase = true) }
        }
        BatchTagEditorDialog(
            songs = artistSongs,
            onDismiss = {
                artistToBatchEdit = null
                libraryViewModel.scanMusic()
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumsTabContent(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit,
    onMergeAlbum: (Album) -> Unit,
    onBatchEditAlbum: (Album) -> Unit,
    modifier: Modifier = Modifier
) {
    if (albums.isEmpty()) {
        EmptyCategoryNotice(message = "No albums found in your library")
        return
    }

    var selectedAlbumForMenu by remember { mutableStateOf<Album?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = BottomListScrollPadding),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(albums, key = { it.id }) { album ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .combinedClickable(
                        onClick = { onAlbumClick(album) },
                        onLongClick = {
                            selectedAlbumForMenu = album
                            showMenu = true
                        }
                    )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
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
                    val albumSubtitle = if (album.year > 0) "${album.artist} • ${album.year}" else album.artist
                    Text(
                        text = albumSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (showMenu && selectedAlbumForMenu?.id == album.id) {
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = {
                            showMenu = false
                            selectedAlbumForMenu = null
                        }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Play album") },
                            leadingIcon = {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            onClick = {
                                showMenu = false
                                val target = selectedAlbumForMenu
                                selectedAlbumForMenu = null
                                if (target != null) onAlbumClick(target)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Merge albums...") },
                            leadingIcon = {
                                Icon(Icons.Rounded.CallMerge, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            onClick = {
                                showMenu = false
                                val target = selectedAlbumForMenu
                                selectedAlbumForMenu = null
                                if (target != null) onMergeAlbum(target)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit album tags") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            onClick = {
                                showMenu = false
                                val target = selectedAlbumForMenu
                                selectedAlbumForMenu = null
                                if (target != null) onBatchEditAlbum(target)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ArtistsTabContent(
    artists: List<Artist>,
    onArtistClick: (Artist) -> Unit,
    onRenameArtist: (Artist) -> Unit = {},
    onBatchEditArtist: (Artist) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (artists.isEmpty()) {
        EmptyCategoryNotice(message = "No artists found in your library")
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedArtistForMenu by remember { mutableStateOf<Artist?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val targetArtist = selectedArtistForMenu
        if (uri != null && targetArtist != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val dir = File(context.filesDir, "artist_images").apply { if (!exists()) mkdirs() }
                    val fileHash = ArtistPhotoDownloader.getHash(targetArtist.name)
                    val destFile = File(dir, "$fileHash.jpg")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    LocalStatsDatabase.getInstance(context).saveArtistImage(targetArtist.name, destFile.absolutePath, "user")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = BottomListScrollPadding)
    ) {
        items(artists, key = { it.id }) { artist ->
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onArtistClick(artist) },
                            onLongClick = {
                                selectedArtistForMenu = artist
                                showMenu = true
                            }
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Circular artist avatar with fallback chain: saved -> collage/album art -> letter
                    ArtistAvatar(
                        artist = artist,
                        size = 52.dp
                    )

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
                        val trackText = if (artist.songCount == 1) "1 track" else "${artist.songCount} tracks"
                        val albumText = if (artist.albumCount == 1) "1 album" else "${artist.albumCount} albums"
                        Text(
                            text = "$trackText • $albumText",
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

                if (showMenu && selectedArtistForMenu?.id == artist.id) {
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = {
                            showMenu = false
                            selectedArtistForMenu = null
                        }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Change photo") },
                            leadingIcon = {
                                Icon(Icons.Rounded.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            onClick = {
                                showMenu = false
                                galleryPicker.launch("image/*")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Remove photo") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            onClick = {
                                val target = selectedArtistForMenu
                                showMenu = false
                                selectedArtistForMenu = null
                                if (target != null) {
                                    scope.launch(Dispatchers.IO) {
                                        LocalStatsDatabase.getInstance(context).removeArtistImage(target.name)
                                    }
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Rename artist...") },
                            leadingIcon = {
                                Icon(Icons.Rounded.DriveFileRenameOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            onClick = {
                                val target = selectedArtistForMenu
                                showMenu = false
                                selectedArtistForMenu = null
                                if (target != null) onRenameArtist(target)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit all tracks") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            onClick = {
                                val target = selectedArtistForMenu
                                showMenu = false
                                selectedArtistForMenu = null
                                if (target != null) onBatchEditArtist(target)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Search again") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            onClick = {
                                val target = selectedArtistForMenu
                                showMenu = false
                                selectedArtistForMenu = null
                                if (target != null) {
                                    scope.launch(Dispatchers.IO) {
                                        ArtistPhotoDownloader.fetchAndSaveArtistPhoto(context, target.name, forceRefresh = true)
                                    }
                                }
                            }
                        )
                    }
                }
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
                    shape = RoundedCornerShape(12.dp),
                    color = Color(playlist.colorSeed),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
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
                    val trackText = if (playlist.songCount == 1) "1 track" else "${playlist.songCount} tracks"
                    Text(
                        text = trackText,
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
                    val trackText = if (folder.songCount == 1) "1 track" else "${folder.songCount} tracks"
                    Text(
                        text = "${folder.path} • $trackText",
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
    hasPermission: Boolean,
    permissionDeniedOnce: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
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
                        imageVector = if (!hasPermission) Icons.Rounded.AudioFile else Icons.AutoMirrored.Rounded.ManageSearch,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = if (!hasPermission) "Music permission required" else "Looking for local music?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (!hasPermission) {
                        "North Music needs permission to read audio files so you can listen to your local tracks."
                    } else {
                        "We couldn't detect any audio tracks in your designated storage directories."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (!hasPermission) {
                    Button(
                        onClick = onRequestPermission,
                        shape = PillShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AudioFile,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Grant access")
                    }

                    if (permissionDeniedOnce) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onOpenSettings,
                            shape = PillShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Text("Open app settings")
                        }
                    }
                } else {
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
