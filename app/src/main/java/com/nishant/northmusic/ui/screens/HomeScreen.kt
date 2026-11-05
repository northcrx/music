package com.nishant.northmusic.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.input.pointer.pointerInput
import com.nishant.northmusic.data.network.OnlineAlbum
import com.nishant.northmusic.data.network.OnlineArtist
import com.nishant.northmusic.ui.components.ShimmerAlbumCard
import com.nishant.northmusic.ui.components.ShimmerTrackRow
import com.nishant.northmusic.ui.dialogs.AlbumDetailBottomSheet

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.NorthWest
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nishant.northmusic.data.model.Song
import com.nishant.northmusic.data.network.CdnMusicService
import com.nishant.northmusic.data.network.OnlineTrack
import com.nishant.northmusic.data.network.ResolvedTrackInfo
import com.nishant.northmusic.data.repository.normalizeText
import com.nishant.northmusic.navigation.BottomListScrollPadding
import com.nishant.northmusic.navigation.Screen
import com.nishant.northmusic.ui.components.AppLogo
import com.nishant.northmusic.ui.components.AsyncSongArtwork
import com.nishant.northmusic.ui.components.SongItem
import com.nishant.northmusic.ui.theme.PillShape
import com.nishant.northmusic.ui.viewmodels.DownloadsViewModel
import com.nishant.northmusic.ui.viewmodels.HomeViewModel
import com.nishant.northmusic.ui.viewmodels.PlayerViewModel
import com.nishant.northmusic.ui.viewmodels.normalizeForSearch
import com.nishant.northmusic.util.PermissionUtils
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    playerViewModel: PlayerViewModel,
    downloadsViewModel: DownloadsViewModel? = null,
    onNavigateToSettings: () -> Unit,
    onNavigateToLibrary: () -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    onNavigateToShortcut: (String) -> Unit = {},
    onNavigateToNowPlaying: () -> Unit = {},
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
    var searchCategory by remember { mutableStateOf("All") }
    val downloadsState by downloadsViewModel?.uiState?.collectAsState() ?: remember { mutableStateOf(null) }
    var onlineTracks by remember { mutableStateOf<List<OnlineTrack>>(emptyList()) }
    var onlineAlbums by remember { mutableStateOf<List<OnlineAlbum>>(emptyList()) }
    var onlineArtists by remember { mutableStateOf<List<OnlineArtist>>(emptyList()) }
    var selectedAlbumForDetail by remember { mutableStateOf<OnlineAlbum?>(null) }
    var isOnlineSearching by remember { mutableStateOf(false) }
    var resolvedUrlTrack by remember { mutableStateOf<ResolvedTrackInfo?>(null) }
    var isResolvingUrl by remember { mutableStateOf(false) }
    var currentTrackPage by remember { mutableIntStateOf(1) }
    var isPaginating by remember { mutableStateOf(false) }
    var canPaginate by remember { mutableStateOf(true) }
    val searchListState = rememberLazyListState()

    // Pagination for online tracks when scrolling to bottom of search list
    LaunchedEffect(searchListState.canScrollForward, isPaginating, canPaginate) {
        if (!searchListState.canScrollForward && canPaginate && !isPaginating && !isOnlineSearching && searchQuery.isNotBlank() && (searchCategory == "All" || searchCategory == "Tracks")) {
            isPaginating = true
            val nextPage = currentTrackPage + 1
            val nextResults = CdnMusicService.searchTracks(searchQuery.trim(), page = nextPage, limit = 15)
            if (nextResults.isEmpty()) {
                canPaginate = false
            } else {
                onlineTracks = (onlineTracks + nextResults).distinctBy { it.id }
                currentTrackPage = nextPage
            }
            isPaginating = false
        }
    }

    LaunchedEffect(searchQuery, searchCategory) {
        val q = searchQuery.trim()
        if (q.isBlank()) {
            onlineTracks = emptyList()
            onlineAlbums = emptyList()
            onlineArtists = emptyList()
            resolvedUrlTrack = null
            isOnlineSearching = false
            isResolvingUrl = false
            currentTrackPage = 1
            canPaginate = false
            return@LaunchedEffect
        }

        if (CdnMusicService.isUrl(q)) {
            isResolvingUrl = true
            resolvedUrlTrack = CdnMusicService.resolveUrl(q)
            isResolvingUrl = false
        } else {
            delay(300)
            isOnlineSearching = true
            currentTrackPage = 1
            canPaginate = true
            when (searchCategory) {
                "All" -> {
                    val tracks = CdnMusicService.searchTracks(q, page = 1, limit = 15)
                    val albums = CdnMusicService.searchAlbums(q, page = 1, limit = 8)
                    val artists = CdnMusicService.searchArtists(q, page = 1, limit = 8)
                    onlineTracks = tracks
                    onlineAlbums = albums
                    onlineArtists = artists
                    canPaginate = tracks.size >= 12
                }
                "Tracks" -> {
                    val tracks = CdnMusicService.searchTracks(q, page = 1, limit = 20)
                    onlineTracks = tracks
                    onlineAlbums = emptyList()
                    onlineArtists = emptyList()
                    canPaginate = tracks.size >= 15
                }
                "Albums" -> {
                    onlineAlbums = CdnMusicService.searchAlbums(q, page = 1, limit = 20)
                    onlineTracks = emptyList()
                    onlineArtists = emptyList()
                    canPaginate = false
                }
                "Artists" -> {
                    onlineArtists = CdnMusicService.searchArtists(q, page = 1, limit = 20)
                    onlineTracks = emptyList()
                    onlineAlbums = emptyList()
                    canPaginate = false
                }
            }
            isOnlineSearching = false
        }
    }

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
                "Tracks" -> uiState.yourMusic.filter { it.title.normalizeForSearch().contains(q) }
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
        // TOP HEADER: 3-line hamburger menu + search bar + settings gear icon button (Top Zone Gestures)
        var totalHeaderDragX by remember { mutableStateOf(0f) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { totalHeaderDragX = 0f },
                        onDragEnd = {
                            if (totalHeaderDragX > 45f) {
                                onOpenDrawer()
                            } else if (totalHeaderDragX < -45f) {
                                onNavigateToSettings()
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            totalHeaderDragX += dragAmount
                        }
                    )
                },
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isSearchActive) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(44.dp)
                ) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Menu,
                            contentDescription = "Open navigation menu",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Search bar
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(22.dp),
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
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search music",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 15.sp,
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

            if (!isSearchActive) {
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
            LazyColumn(
                state = searchListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = BottomListScrollPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Filter Chips
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("All", "Tracks", "Artists", "Albums").forEach { cat ->
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
                                        imageVector = Icons.Rounded.History,
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
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // 1. Resolved URL Track
                    if (isResolvingUrl) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Analyzing link...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (resolvedUrlTrack != null) {
                        val track = resolvedUrlTrack!!
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            ) {
                                if (track.title.isNotBlank()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        AsyncSongArtwork(
                                            model = track.artworkUrl,
                                            size = 54.dp,
                                            shape = RoundedCornerShape(12.dp)
                                        )

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Link detected (${track.source})",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = track.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
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

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            FilledTonalButton(
                                                onClick = {
                                                    downloadsViewModel?.enqueue(
                                                        track.title,
                                                        track.artist,
                                                        track.album,
                                                        track.downloadUrl,
                                                        track.artworkUrl
                                                    )
                                                },
                                                shape = PillShape
                                            ) {
                                                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Download")
                                            }
                                        }
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(Icons.Rounded.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Text("Paste detected. Tap search to resolve.", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }

                    // 2. Shimmer Skeleton Loading Waves
                    if (isOnlineSearching) {
                        if (searchCategory == "All" || searchCategory == "Albums") {
                            item {
                                Text(
                                    text = "ALBUMS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                                )
                            }
                            item {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(3) {
                                        ShimmerAlbumCard(modifier = Modifier.width(130.dp))
                                    }
                                }
                            }
                        }

                        if (searchCategory == "All" || searchCategory == "Tracks") {
                            item {
                                Text(
                                    text = "TRACKS (320k Audio)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                                )
                            }
                            items(5) {
                                ShimmerTrackRow()
                            }
                        }

                        if (searchCategory == "Artists") {
                            items(4) {
                                ShimmerTrackRow()
                            }
                        }
                    } else {
                        // 3. Online Artists Section
                        if (onlineArtists.isNotEmpty() && (searchCategory == "All" || searchCategory == "Artists")) {
                            item {
                                Text(
                                    text = "ARTISTS (${onlineArtists.size})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                                )
                            }
                            item {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(onlineArtists, key = { it.id }) { artist ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .width(84.dp)
                                                .clickable {
                                                    searchQuery = artist.name
                                                    searchCategory = "All"
                                                }
                                        ) {
                                            AsyncSongArtwork(
                                                model = artist.artworkUrl,
                                                size = 72.dp,
                                                shape = CircleShape
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = artist.name,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center
                                            )
                                            Text(
                                                text = artist.role,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Online Albums Section
                        if (onlineAlbums.isNotEmpty() && (searchCategory == "All" || searchCategory == "Albums")) {
                            item {
                                Text(
                                    text = "ALBUMS (${onlineAlbums.size} • Tap to view & download)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                                )
                            }

                            if (searchCategory == "All") {
                                item {
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(onlineAlbums, key = { it.id }) { album ->
                                            Column(
                                                modifier = Modifier
                                                    .width(136.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .clickable {
                                                        focusManager.clearFocus()
                                                        selectedAlbumForDetail = album
                                                    }
                                                    .padding(4.dp)
                                            ) {
                                                AsyncSongArtwork(
                                                    model = album.artworkUrl,
                                                    size = 128.dp,
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = album.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
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
                                                if (album.year.isNotBlank()) {
                                                    Text(
                                                        text = album.year,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                items(onlineAlbums, key = { it.id }) { album ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                            focusManager.clearFocus()
                                            selectedAlbumForDetail = album
                                        }
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        AsyncSongArtwork(
                                            model = album.artworkUrl,
                                            size = 56.dp,
                                            shape = RoundedCornerShape(10.dp)
                                        )

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = album.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${album.artist}${if (album.year.isNotBlank()) " • ${album.year}" else ""}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        FilledTonalButton(
                                            onClick = { selectedAlbumForDetail = album },
                                            shape = PillShape
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Download,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("View Album")
                                        }
                                    }
                                }
                            }
                        }

                        // 5. Online Tracks Section
                        if (onlineTracks.isNotEmpty() && (searchCategory == "All" || searchCategory == "Tracks")) {
                            item {
                                Text(
                                    text = "TRACKS (320k Audio • Tap to stream)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                                )
                            }

                            items(onlineTracks, key = { it.id }) { track ->
                                val isDownloaded = downloadsState?.completedItems?.any { it.title.equals(track.title, ignoreCase = true) } == true
                                val isDownloading = downloadsState?.downloadingItems?.any { it.title.equals(track.title, ignoreCase = true) } == true
                                val isCurrentPlaying = playerState.currentSong?.title.equals(track.title, ignoreCase = true)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            focusManager.clearFocus()
                                            val localMatch = uiState.yourMusic.find { it.title.equals(track.title, ignoreCase = true) }
                                            if (localMatch != null) {
                                                playerViewModel.playSong(localMatch, source = "Downloaded")
                                            } else {
                                                playerViewModel.playSong(track.toSong(), source = "Online Stream (320k)")
                                            }
                                        }
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    AsyncSongArtwork(
                                        model = track.artworkUrl,
                                        size = 48.dp,
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = track.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = if (isCurrentPlaying) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isCurrentPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "${track.artist} • ${track.album.ifEmpty { "Single" }}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "320k",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Dedicated Download Button
                                    IconButton(
                                        onClick = {
                                            if (!isDownloaded && !isDownloading) {
                                                downloadsViewModel?.enqueue(track.title, track.artist, track.album, track.downloadUrl, track.artworkUrl)
                                                // download queued silently
                                            }
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        when {
                                            isDownloaded -> {
                                                Icon(
                                                    imageVector = Icons.Rounded.CheckCircle,
                                                    contentDescription = "Downloaded",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                            isDownloading -> {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(20.dp),
                                                    strokeWidth = 2.5.dp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            else -> {
                                                Icon(
                                                    imageVector = Icons.Rounded.Download,
                                                    contentDescription = "Download",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Pagination Loading Indicator
                            if (isPaginating) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Loading more tracks...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // 6. Local Library Results
                        if (liveSearchResults.isNotEmpty()) {
                            item {
                                Text(
                                    text = "LOCAL LIBRARY (${liveSearchResults.size})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                                )
                            }

                            items(liveSearchResults, key = { it.id }) { song ->
                                SongItem(
                                    song = song,
                                    isPlaying = playerState.currentSong?.id == song.id && playerState.isPlaying,
                                    onClick = {
                                        if (!recentSearches.contains(song.title)) {
                                            recentSearches.add(0, song.title)
                                        }
                                        playerViewModel.playSong(song, liveSearchResults, source = "Search results")
                                    },
                                    onOptionsClick = { }
                                )
                            }
                        } else if (onlineTracks.isEmpty() && onlineAlbums.isEmpty() && onlineArtists.isEmpty() && !CdnMusicService.isUrl(searchQuery)) {
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
                        }
                    }
                }
            }
        } else {
            // NORMAL HOME CONTENT: Shortcuts, "For you", "Listen now", and "From your library" (Middle Zone Swipe)
            var totalContentDragX by remember { mutableStateOf(0f) }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { totalContentDragX = 0f },
                            onDragEnd = {
                                if (totalContentDragX < -60f) {
                                    onNavigateToLibrary()
                                }
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                totalContentDragX += dragAmount
                            }
                        )
                    },
                contentPadding = PaddingValues(bottom = BottomListScrollPadding),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 4 Centered Shortcut buttons sticking across mobile breadth
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val shortcuts = listOf(
                            Triple("Top tracks", Icons.AutoMirrored.Rounded.TrendingUp, Screen.TopTracks.route),
                            Triple("Last added", Icons.Rounded.Schedule, Screen.LastAdded.route),
                            Triple("History", Icons.Rounded.History, Screen.History.route),
                            Triple("Favorites", Icons.Rounded.FavoriteBorder, Screen.Favorites.route)
                        )
                        shortcuts.forEach { (title, icon, route) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
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
                                        playerViewModel.shuffleAll(uiState.yourMusic, source = "Personalized mix")
                                    },
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Shuffle,
                                        contentDescription = "Shuffle",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Shuffle your music",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Personalized mix",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            // Tile 2: Album art tile
                            MosaicSongTile(
                                song = tileSong1,
                                defaultTitle = "Discover",
                                defaultArtist = "Start listening",
                                defaultColorSeed = 101L,
                                onClick = {
                                    tileSong1?.let {
                                        playerViewModel.playSong(it, forYouList, source = "For you")
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Tile 3: Album art tile
                            MosaicSongTile(
                                song = tileSong2,
                                defaultTitle = "Favorites",
                                defaultArtist = "Your top rotation",
                                defaultColorSeed = 202L,
                                onClick = {
                                    tileSong2?.let {
                                        playerViewModel.playSong(it, forYouList, source = "For you")
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )

                            // Tile 4: Album art tile
                            MosaicSongTile(
                                song = tileSong3,
                                defaultTitle = "Recently Added",
                                defaultArtist = "Fresh picks",
                                defaultColorSeed = 303L,
                                onClick = {
                                    tileSong3?.let {
                                        playerViewModel.playSong(it, forYouList, source = "For you")
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 2. "Listen now" section: Carousel of Albums
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
                                                    it.albumId == album.albumId || normalizeText(it.album) == normalizeText(album.title)
                                                }
                                                albumSongs.firstOrNull()?.let {
                                                    playerViewModel.playSong(it, albumSongs, source = "Album ${album.title}")
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

                // 3. "From your library" section: Shows curated sample of local songs (up to 20) with direct jump to Library tab
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "From your library",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (uiState.yourMusic.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                                    ) {
                                        Text(
                                            text = "${uiState.yourMusic.size}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            TextButton(onClick = onNavigateToLibrary) {
                                Text(
                                    text = "View all",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (uiState.yourMusic.isEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AudioFile,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text(
                                        text = "Your library is empty",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Scan your device or download songs to begin listening.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    FilledTonalButton(
                                        onClick = onNavigateToLibrary,
                                        shape = PillShape
                                    ) {
                                        Text("Go to Library")
                                    }
                                }
                            }
                        } else {
                            val sampleSongs = uiState.yourMusic.take(20)
                            sampleSongs.forEach { song ->
                                SongItem(
                                    song = song,
                                    isPlaying = playerState.currentSong?.id == song.id && playerState.isPlaying,
                                    onClick = { playerViewModel.playSong(song, uiState.yourMusic, source = "Library") },
                                    onOptionsClick = { }
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                OutlinedButton(
                                    onClick = onNavigateToLibrary,
                                    shape = PillShape,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("View all ${uiState.yourMusic.size} songs in library →")
                                }
                            }
                        }
                    }
                }
            }
        }

        if (selectedAlbumForDetail != null) {
            AlbumDetailBottomSheet(
                album = selectedAlbumForDetail!!,
                playerViewModel = playerViewModel,
                downloadsViewModel = downloadsViewModel,
                onDismiss = { selectedAlbumForDetail = null }
            )
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
