package com.nishant.northmusic.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nishant.northmusic.util.PermissionUtils


import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.NorthWest
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nishant.northmusic.data.model.Song
import com.nishant.northmusic.navigation.BottomListScrollPadding
import com.nishant.northmusic.navigation.Screen
import com.nishant.northmusic.ui.components.AsyncSongArtwork
import com.nishant.northmusic.ui.components.SongItem
import com.nishant.northmusic.ui.theme.PillShape
import com.nishant.northmusic.ui.viewmodels.HomeViewModel
import com.nishant.northmusic.ui.viewmodels.PlayerViewModel
import com.nishant.northmusic.ui.viewmodels.normalizeForSearch

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    playerViewModel: PlayerViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToShortcut: (String) -> Unit = {},
    isSearchActive: Boolean = false,
    onSearchActiveChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val playerState by playerViewModel.playerState.collectAsState()
    val focusManager = LocalFocusManager.current

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasAudioPermission by remember { mutableStateOf(PermissionUtils.hasAudioPermission(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        hasAudioPermission = PermissionUtils.hasAudioPermission(context)
        if (hasAudioPermission) {
            homeViewModel.rescan()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = PermissionUtils.hasAudioPermission(context)
                hasAudioPermission = granted
                if (granted && uiState.yourMusic.isEmpty()) {
                    homeViewModel.rescan()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var searchCategory by remember { mutableStateOf("Songs") }
    val recentSearches = remember {
        mutableStateListOf<String>()
    }

    // Intercept back press when search mode is active
    BackHandler(enabled = isSearchActive) {
        onSearchActiveChange(false)
        searchQuery = ""
        focusManager.clearFocus()
    }

    val liveSearchResults = remember(searchQuery, searchCategory, uiState.yourMusic) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            val q = searchQuery.trim().normalizeForSearch()
            when (searchCategory) {
                "Albums" -> uiState.yourMusic.filter { it.album.normalizeForSearch().contains(q) }
                "Artists" -> uiState.yourMusic.filter { it.artist.normalizeForSearch().contains(q) }
                else -> uiState.yourMusic.filter {
                    it.title.normalizeForSearch().contains(q) ||
                            it.artist.normalizeForSearch().contains(q) ||
                            it.album.normalizeForSearch().contains(q)
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // TOP HEADER (80/20): 80% search bar + 20% settings gear icon button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 80% width search bar
            Surface(
                modifier = Modifier
                    .weight(0.80f)
                    .height(50.dp),
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isSearchActive) {
                        IconButton(
                            onClick = {
                                onSearchActiveChange(false)
                                searchQuery = ""
                                focusManager.clearFocus()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Close search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search your music",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused && !isSearchActive) {
                                        onSearchActiveChange(true)
                                    }
                                }
                        )
                    }

                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                searchQuery = ""
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // 20% width settings gear button
            Box(
                modifier = Modifier
                    .weight(0.20f)
                    .height(50.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(50.dp)
                ) {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Body Content: Switches in-place between Search Results and Normal Home Content
        if (isSearchActive) {
            // INLINE SEARCH RESULTS MODE
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = BottomListScrollPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Filter Chips (Songs, Albums, Artists)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Songs", "Albums", "Artists").forEach { cat ->
                            FilterChip(
                                selected = searchCategory == cat,
                                onClick = { searchCategory = cat },
                                label = { Text(cat) },
                                shape = PillShape,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }

                // If query is empty: Show Recent Searches list
                if (searchQuery.isBlank()) {
                    if (recentSearches.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recent Searches",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                TextButton(onClick = { recentSearches.clear() }) {
                                    Text(
                                        text = "Clear all",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        items(recentSearches) { term ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        searchQuery = term
                                        focusManager.clearFocus()
                                    }
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = term,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(
                                    onClick = { recentSearches.remove(term) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.NorthWest,
                                        contentDescription = "Search this",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Query is present: Show Live Match Count and Matching Items
                    item {
                        Text(
                            text = "${liveSearchResults.size} results found",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                        )
                    }

                    if (liveSearchResults.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No matches for \"$searchQuery\"",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(liveSearchResults, key = { it.id }) { song ->
                            SongItem(
                                song = song,
                                isPlaying = playerState.currentSong?.id == song.id && playerState.isPlaying,
                                onClick = {
                                    if (!recentSearches.contains(song.title)) {
                                        recentSearches.add(0, song.title)
                                    }
                                    playerViewModel.playSong(song, liveSearchResults)
                                },
                                onOptionsClick = { /* TODO: Song context menu */ }
                            )
                        }
                    }
                }
            }
        } else {
            // NORMAL HOME CONTENT: Shortcuts, "For you", "Listen now", and "Your music"
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = BottomListScrollPadding),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Circular shortcut buttons (56dp circle in surfaceContainer, neutral icon, label below, 1 line)
                item {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val shortcuts = listOf(
                            Triple("Top tracks", Icons.AutoMirrored.Rounded.TrendingUp, Screen.TopTracks.route),
                            Triple("Last added", Icons.Rounded.Schedule, Screen.LastAdded.route),
                            Triple("History", Icons.Rounded.History, Screen.History.route),
                            Triple("Favorites", Icons.Rounded.FavoriteBorder, Screen.Favorites.route)
                        )
                        items(shortcuts) { (title, icon, route) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(72.dp)
                                    .clickable { onNavigateToShortcut(route) }
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = title,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // 1. "For you" section: a mosaic grid of rounded album artwork tiles with one accent colored tile "Shuffle your music"
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 4.dp)
                    ) {
                        Text(
                            text = "For you",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val forYouList = uiState.forYouSongs.ifEmpty { uiState.yourMusic }
                        val tileSong1 = forYouList.getOrNull(0)
                        val tileSong2 = forYouList.getOrNull(1)
                        val tileSong3 = forYouList.getOrNull(2)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Tile 1: Accent colored tile "Shuffle your music"
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable {
                                        homeViewModel.onShuffleMixClick()
                                        playerViewModel.shuffleAll(uiState.yourMusic)
                                    },
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Rounded.Shuffle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = "Shuffle your music",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Personalized mix",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }

                            // Tile 2
                            MosaicSongTile(
                                song = tileSong1,
                                defaultTitle = "Discover music",
                                defaultArtist = "Your library",
                                defaultColorSeed = 0xFF0D9488,
                                onClick = {
                                    tileSong1?.let { playerViewModel.playSong(it, forYouList) }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Tile 3
                            MosaicSongTile(
                                song = tileSong2,
                                defaultTitle = "Recently played",
                                defaultArtist = "Quick resume",
                                defaultColorSeed = 0xFFDC2626,
                                onClick = {
                                    tileSong2?.let { playerViewModel.playSong(it, forYouList) }
                                },
                                modifier = Modifier.weight(1f)
                            )

                            // Tile 4
                            MosaicSongTile(
                                song = tileSong3,
                                defaultTitle = "Favorites",
                                defaultArtist = "Loved tracks",
                                defaultColorSeed = 0xFF15803D,
                                onClick = {
                                    tileSong3?.let { playerViewModel.playSong(it, forYouList) }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 2. "Listen now" section: horizontal album carousel (below artwork: title and artist)
                if (uiState.albums.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Listen now",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(uiState.albums, key = { it.id }) { album ->
                                    Column(
                                        modifier = Modifier
                                            .width(148.dp)
                                            .clickable {
                                                val albumSongs = uiState.yourMusic.filter {
                                                    it.albumId == album.albumId || it.album.equals(album.title, ignoreCase = true)
                                                }
                                                albumSongs.firstOrNull()?.let {
                                                    playerViewModel.playSong(it, albumSongs)
                                                }
                                            }
                                    ) {
                                        AsyncSongArtwork(
                                            albumArtUri = album.albumArtUri,
                                            colorSeed = album.colorSeed,
                                            size = 148.dp,
                                            shape = RoundedCornerShape(20.dp),
                                            iconSize = 44.dp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = album.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = album.artist,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. "Your music" section: 5-8 songs using shared SongItem component
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Your music",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        val displaySongs = uiState.yourMusic.take(8)
                        if (displaySongs.isEmpty()) {
                            if (!hasAudioPermission) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AudioFile,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Text(
                                            text = "Permission required to access music",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "North Music needs permission to scan and play audio tracks stored on this device.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                        Button(
                                            onClick = {
                                                permissionLauncher.launch(PermissionUtils.getPermissionsToRequest())
                                            },
                                            shape = PillShape,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(44.dp)
                                        ) {
                                            Text("Grant access")
                                        }
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp, horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (uiState.isScanning) "Scanning your audio files..." else "No songs found on your device yet",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            displaySongs.forEach { song ->
                                SongItem(
                                    song = song,
                                    isPlaying = playerState.currentSong?.id == song.id && playerState.isPlaying,
                                    onClick = { playerViewModel.playSong(song, uiState.yourMusic) },
                                    onOptionsClick = { /* TODO: Song context options */ }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Mosaic Song/Album Tile for "For you" section
 */
@Composable
private fun MosaicSongTile(
    song: Song?,
    defaultTitle: String,
    defaultArtist: String,
    defaultColorSeed: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(140.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            AsyncSongArtwork(
                albumArtUri = song?.albumArtUri,
                colorSeed = song?.colorSeed ?: defaultColorSeed,
                size = 46.dp,
                shape = RoundedCornerShape(12.dp),
                iconSize = 22.dp
            )

            Column {
                Text(
                    text = song?.title ?: defaultTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song?.artist ?: defaultArtist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
