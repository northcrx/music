package com.nishant.northmusic.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.nishant.northmusic.util.PermissionUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nishant.northmusic.ui.components.MiniPlayer
import com.nishant.northmusic.ui.screens.DownloadsScreen
import com.nishant.northmusic.ui.screens.EqualizerScreen
import com.nishant.northmusic.ui.screens.HomeScreen
import com.nishant.northmusic.ui.screens.LibraryScreen
import com.nishant.northmusic.ui.screens.NowPlayingScreen
import com.nishant.northmusic.ui.screens.SettingsScreen
import com.nishant.northmusic.ui.screens.SongListSubScreen
import com.nishant.northmusic.ui.screens.settings.AboutSettingsScreen
import com.nishant.northmusic.ui.screens.settings.AdvancedSettingsScreen
import com.nishant.northmusic.ui.screens.settings.AppearanceSettingsScreen
import com.nishant.northmusic.ui.screens.settings.DownloadsSettingsScreen
import com.nishant.northmusic.ui.screens.settings.FixMetadataScreen
import com.nishant.northmusic.ui.screens.settings.LibrarySettingsScreen
import com.nishant.northmusic.ui.screens.settings.LyricsSettingsScreen
import com.nishant.northmusic.ui.screens.settings.NetworkSettingsScreen
import com.nishant.northmusic.ui.screens.settings.NowPlayingSettingsScreen
import com.nishant.northmusic.ui.screens.settings.PlaybackSettingsScreen
import com.nishant.northmusic.ui.screens.setup.SetupWizardScreen
import com.nishant.northmusic.ui.theme.PillShape
import com.nishant.northmusic.ui.viewmodels.DownloadsViewModel
import com.nishant.northmusic.ui.viewmodels.EqualizerViewModel
import com.nishant.northmusic.ui.viewmodels.HomeViewModel
import com.nishant.northmusic.ui.viewmodels.LibraryViewModel
import com.nishant.northmusic.ui.viewmodels.PlayerViewModel
import com.nishant.northmusic.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.launch

val BottomListScrollPadding = 180.dp

@Composable
fun MainScaffold(
    settingsViewModel: SettingsViewModel,
    playerViewModel: PlayerViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel(),
    libraryViewModel: LibraryViewModel = viewModel(),
    downloadsViewModel: DownloadsViewModel = viewModel(),
    equalizerViewModel: EqualizerViewModel = viewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val playerState by playerViewModel.playerState.collectAsState()
    val homeUiState by homeViewModel.uiState.collectAsState()
    val downloadsUiState by downloadsViewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.settingsState.collectAsState()

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        if (PermissionUtils.hasAudioPermission(context)) {
            homeViewModel.rescan()
            libraryViewModel.scanMusic()
        }
    }

    LaunchedEffect(settingsState.setupDone) {
        if (settingsState.setupDone && !PermissionUtils.hasAudioPermission(context)) {
            permissionLauncher.launch(PermissionUtils.getPermissionsToRequest())
        }
    }

    LaunchedEffect(Unit) {
        DownloadsViewModel.sharedUrlFlow.collect {
            if (currentRoute != Screen.Downloads.route) {
                navController.navigate(Screen.Downloads.route) {
                    launchSingleTop = true
                }
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var isSearchActive by remember { mutableStateOf(false) }
    var isMiniPlayerDismissed by remember { mutableStateOf(false) }

    // Reset dismissal when new song begins playing
    LaunchedEffect(playerState.currentSong?.id) {
        isMiniPlayerDismissed = false
    }

    val isFullScreenRoute = currentRoute == Screen.NowPlaying.route ||
            currentRoute == Screen.Equalizer.route ||
            currentRoute == Screen.SetupWizard.route ||
            currentRoute == Screen.Settings.route ||
            currentRoute == Screen.SettingsAppearance.route ||
            currentRoute == Screen.SettingsNowPlaying.route ||
            currentRoute == Screen.SettingsLyrics.route ||
            currentRoute == Screen.SettingsPlayback.route ||
            currentRoute == Screen.SettingsDownloads.route ||
            currentRoute == Screen.SettingsLibrary.route ||
            currentRoute == Screen.SettingsNetwork.route ||
            currentRoute == Screen.SettingsAdvanced.route ||
            currentRoute == Screen.SettingsAbout.route ||
            currentRoute == Screen.FixMetadata.route ||
            currentRoute == Screen.TopTracks.route ||
            currentRoute == Screen.LastAdded.route ||
            currentRoute == Screen.History.route ||
            currentRoute == Screen.Favorites.route

    // Mini-player is hidden during selection mode, full-screen route, or when dismissed via swipe down (remains visible during search)
    val isMiniPlayerVisible = !isFullScreenRoute && !downloadsUiState.isSelectionMode && !isMiniPlayerDismissed
    val showBottomNav = !isFullScreenRoute && !downloadsUiState.isSelectionMode

    val initialStartDestination = if (!settingsState.setupDone) {
        Screen.SetupWizard.route
    } else if (settingsState.rememberLastPage && settingsState.lastOpenedRoute.isNotBlank()) {
        settingsState.lastOpenedRoute
    } else {
        Screen.Home.route
    }

    val tabIndices = remember {
        mapOf(
            Screen.Home.route to 0,
            Screen.Library.route to 1,
            Screen.Downloads.route to 2
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                // Drawer Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.GraphicEq,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "NorthMusic",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Offline & Hi-Res Audio",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Navigation Items
                NavigationDrawerItem(
                    label = { Text("Home", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Rounded.Home, contentDescription = null) },
                    selected = currentRoute == Screen.Home.route,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        if (currentRoute != Screen.Home.route) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Library", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null) },
                    selected = currentRoute == Screen.Library.route,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        if (currentRoute != Screen.Library.route) {
                            navController.navigate(Screen.Library.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Downloads", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Rounded.Download, contentDescription = null) },
                    selected = currentRoute == Screen.Downloads.route,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        if (currentRoute != Screen.Downloads.route) {
                            navController.navigate(Screen.Downloads.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Equalizer", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Rounded.GraphicEq, contentDescription = null) },
                    selected = currentRoute == Screen.Equalizer.route,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate(Screen.Equalizer.route)
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                NavigationDrawerItem(
                    label = { Text("Fix metadata & tags", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Rounded.AutoFixHigh, contentDescription = null) },
                    selected = currentRoute == Screen.FixMetadata.route,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate(Screen.FixMetadata.route)
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                    selected = currentRoute == Screen.Settings.route,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate(Screen.Settings.route)
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                NavigationDrawerItem(
                    label = { Text("About NorthMusic", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Rounded.Info, contentDescription = null) },
                    selected = currentRoute == Screen.SettingsAbout.route,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate(Screen.SettingsAbout.route)
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets.statusBars,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding())
            ) {
                // Edge-to-edge NavHost with directional tab transitions and vertical NowPlaying animations
                NavHost(
                    navController = navController,
                    startDestination = initialStartDestination,
                    modifier = Modifier.fillMaxSize(),
                    enterTransition = {
                        if (targetState.destination.route == Screen.NowPlaying.route) {
                            slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) + fadeIn(tween(200))
                        } else {
                            val fromIdx = tabIndices[initialState.destination.route]
                            val toIdx = tabIndices[targetState.destination.route]
                            if (fromIdx != null && toIdx != null) {
                                if (toIdx > fromIdx) {
                                    slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(240)) + fadeIn(tween(200))
                                } else {
                                    slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(240)) + fadeIn(tween(200))
                                }
                            } else {
                                slideInHorizontally(initialOffsetX = { 300 }, animationSpec = tween(220)) + fadeIn(tween(200))
                            }
                        }
                    },
                    exitTransition = {
                        if (initialState.destination.route == Screen.NowPlaying.route) {
                            slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) + fadeOut(tween(200))
                        } else {
                            val fromIdx = tabIndices[initialState.destination.route]
                            val toIdx = tabIndices[targetState.destination.route]
                            if (fromIdx != null && toIdx != null) {
                                if (toIdx > fromIdx) {
                                    slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(240)) + fadeOut(tween(200))
                                } else {
                                    slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(240)) + fadeOut(tween(200))
                                }
                            } else {
                                slideOutHorizontally(targetOffsetX = { -300 }, animationSpec = tween(220)) + fadeOut(tween(200))
                            }
                        }
                    },
                    popEnterTransition = {
                        val fromIdx = tabIndices[initialState.destination.route]
                        val toIdx = tabIndices[targetState.destination.route]
                        if (fromIdx != null && toIdx != null) {
                            if (toIdx > fromIdx) {
                                slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(240)) + fadeIn(tween(200))
                            } else {
                                slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(240)) + fadeIn(tween(200))
                            }
                        } else if (initialState.destination.route == Screen.NowPlaying.route) {
                            fadeIn(tween(200))
                        } else {
                            slideInHorizontally(initialOffsetX = { -300 }, animationSpec = tween(220)) + fadeIn(tween(200))
                        }
                    },
                    popExitTransition = {
                        if (initialState.destination.route == Screen.NowPlaying.route) {
                            slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) + fadeOut(tween(200))
                        } else {
                            val fromIdx = tabIndices[initialState.destination.route]
                            val toIdx = tabIndices[targetState.destination.route]
                            if (fromIdx != null && toIdx != null) {
                                if (toIdx > fromIdx) {
                                    slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(240)) + fadeOut(tween(200))
                                } else {
                                    slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(240)) + fadeOut(tween(200))
                                }
                            } else {
                                slideOutHorizontally(targetOffsetX = { 300 }, animationSpec = tween(220)) + fadeOut(tween(200))
                            }
                        }
                    }
                ) {
                    // Main Bottom Bar Screens
                    composable(Screen.Home.route) {
                        HomeScreen(
                            homeViewModel = homeViewModel,
                            playerViewModel = playerViewModel,
                            downloadsViewModel = downloadsViewModel,
                            onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                            onNavigateToLibrary = {
                                navController.navigate(Screen.Library.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onOpenDrawer = {
                                coroutineScope.launch { drawerState.open() }
                            },
                            onNavigateToShortcut = { route -> navController.navigate(route) },
                            onNavigateToNowPlaying = {
                                navController.navigate(Screen.NowPlaying.route) {
                                    launchSingleTop = true
                                }
                            },
                            isSearchActive = isSearchActive,
                            onSearchActiveChange = { isSearchActive = it }
                        )
                    }
                    composable(Screen.Library.route) {
                        LibraryScreen(
                            libraryViewModel = libraryViewModel,
                            playerViewModel = playerViewModel,
                            onNavigateToFixMetadata = { navController.navigate(Screen.FixMetadata.route) },
                            onNavigateToHome = {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onNavigateToDownloads = {
                                navController.navigate(Screen.Downloads.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                    composable(Screen.Downloads.route) {
                        DownloadsScreen(
                            downloadsViewModel = downloadsViewModel,
                            onNavigateToLibrary = {
                                navController.navigate(Screen.Library.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }

                    // Settings Hub Screen
                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToAppearance = { navController.navigate(Screen.SettingsAppearance.route) },
                            onNavigateToNowPlaying = { navController.navigate(Screen.SettingsNowPlaying.route) },
                            onNavigateToLyrics = { navController.navigate(Screen.SettingsLyrics.route) },
                            onNavigateToPlayback = { navController.navigate(Screen.SettingsPlayback.route) },
                            onNavigateToLibrary = { navController.navigate(Screen.SettingsLibrary.route) },
                            onNavigateToDownloads = { navController.navigate(Screen.SettingsDownloads.route) },
                            onNavigateToNetwork = { navController.navigate(Screen.SettingsNetwork.route) },
                            onNavigateToAdvanced = { navController.navigate(Screen.SettingsAdvanced.route) },
                            onNavigateToAbout = { navController.navigate(Screen.SettingsAbout.route) }
                        )
                    }

                    // 4 Home Shortcuts
                    composable(Screen.TopTracks.route) {
                        SongListSubScreen(
                            title = "Top tracks",
                            songs = homeUiState.topTracks,
                            playerViewModel = playerViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.LastAdded.route) {
                        SongListSubScreen(
                            title = "Last added",
                            songs = homeUiState.lastAdded,
                            playerViewModel = playerViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.History.route) {
                        SongListSubScreen(
                            title = "History",
                            songs = homeUiState.recentlyPlayed,
                            playerViewModel = playerViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.Favorites.route) {
                        SongListSubScreen(
                            title = "Favorites",
                            songs = homeUiState.favorites,
                            playerViewModel = playerViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    // 9 Settings Sub-screens
                    composable(Screen.SettingsAppearance.route) {
                        AppearanceSettingsScreen(
                            settingsViewModel = settingsViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.SettingsNowPlaying.route) {
                        NowPlayingSettingsScreen(
                            settingsViewModel = settingsViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.SettingsLyrics.route) {
                        LyricsSettingsScreen(
                            settingsViewModel = settingsViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.SettingsPlayback.route) {
                        PlaybackSettingsScreen(
                            settingsViewModel = settingsViewModel,
                            onNavigateToEqualizer = { navController.navigate(Screen.Equalizer.route) },
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.SettingsLibrary.route) {
                        LibrarySettingsScreen(
                            settingsViewModel = settingsViewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToFixMetadata = { navController.navigate(Screen.FixMetadata.route) }
                        )
                    }
                    composable(Screen.FixMetadata.route) {
                        FixMetadataScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.SettingsDownloads.route) {
                        DownloadsSettingsScreen(
                            settingsViewModel = settingsViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.SettingsNetwork.route) {
                        NetworkSettingsScreen(
                            settingsViewModel = settingsViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.SettingsAdvanced.route) {
                        AdvancedSettingsScreen(
                            settingsViewModel = settingsViewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onRunSetupAgain = { navController.navigate(Screen.SetupWizard.route) }
                        )
                    }
                    composable(Screen.SettingsAbout.route) {
                        AboutSettingsScreen(
                            onRunSetupAgain = { navController.navigate(Screen.SetupWizard.route) },
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.NowPlaying.route) {
                        NowPlayingScreen(
                            playerViewModel = playerViewModel,
                            downloadsViewModel = downloadsViewModel,
                            onDismiss = { navController.popBackStack() },
                            onNavigateToEqualizer = { navController.navigate(Screen.Equalizer.route) },
                            onNavigateToAlbum = { albumName ->
                                navController.popBackStack()
                                navController.navigate(Screen.Library.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                                libraryViewModel.selectFilter(com.nishant.northmusic.data.model.LibraryFilter.ALBUMS)
                                libraryViewModel.updateSearchQuery(albumName)
                            },
                            onNavigateToArtist = { artistName ->
                                navController.popBackStack()
                                navController.navigate(Screen.Library.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                                libraryViewModel.selectFilter(com.nishant.northmusic.data.model.LibraryFilter.ARTISTS)
                                libraryViewModel.updateSearchQuery(artistName)
                            },
                            onNavigateToFolder = { folderName ->
                                navController.popBackStack()
                                navController.navigate(Screen.Library.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                                libraryViewModel.selectFilter(com.nishant.northmusic.data.model.LibraryFilter.FOLDERS)
                                libraryViewModel.updateSearchQuery(folderName)
                            }
                        )
                    }

                    composable(Screen.Equalizer.route) {
                        EqualizerScreen(
                            equalizerViewModel = equalizerViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.SetupWizard.route) {
                        SetupWizardScreen(
                            settingsViewModel = settingsViewModel,
                            onComplete = {
                                settingsViewModel.setSetupDone(true)
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.SetupWizard.route) { inclusive = true }
                                }
                            }
                        )
                    }
                }

                // Floating Bottom Dock overlaying the edge-to-edge NavHost
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.70f),
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                                )
                            )
                        )
                ) {
                    val currentSong = playerState.currentSong
                    val isCurrentDownloaded = currentSong != null && (
                        downloadsUiState.completedItems.any { it.title.equals(currentSong.title, ignoreCase = true) } ||
                        homeUiState.yourMusic.any { it.title.equals(currentSong.title, ignoreCase = true) }
                    )
                    val isCurrentDownloading = currentSong != null && downloadsUiState.downloadingItems.any {
                        it.title.equals(currentSong.title, ignoreCase = true)
                    }

                    // Mini Player levitating right above bottom dock
                    MiniPlayer(
                        playerState = playerState,
                        visible = isMiniPlayerVisible,
                        onTap = {
                            navController.navigate(Screen.NowPlaying.route) {
                                launchSingleTop = true
                            }
                        },
                        onPlayPauseClick = { playerViewModel.togglePlayPause() },
                        onFavoriteClick = { playerViewModel.toggleFavorite() },
                        onNextClick = { playerViewModel.skipToNext() },
                        onPreviousClick = { playerViewModel.skipToPrevious() },
                        onDismiss = { isMiniPlayerDismissed = true },
                        isDownloaded = isCurrentDownloaded,
                        isDownloading = isCurrentDownloading,
                        onDownloadClick = {
                            val s = playerState.currentSong ?: return@MiniPlayer
                            downloadsViewModel.enqueue(
                                title = s.title,
                                artist = s.artist,
                                album = s.album,
                                streamUrl = s.filePath,
                                artworkUrl = s.customArtUri?.toString() ?: "",
                                durationSeconds = s.durationSeconds,
                                tags = listOf(s.genre).filter { it.isNotBlank() }
                            )
                        }
                    )

                // Sleek Full-Width Semi-Transparent Bottom Navigation Bar
                AnimatedVisibility(
                    visible = showBottomNav,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(durationMillis = 200)
                    ) + fadeIn(animationSpec = tween(durationMillis = 150)),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(durationMillis = 180)
                    ) + fadeOut(animationSpec = tween(durationMillis = 120))
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.88f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                        tonalElevation = 0.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BottomNavItems.forEach { item ->
                                    val isSelected = currentRoute == item.route
                                    val showLabel = settingsState.tabTitlesMode == "Always" ||
                                            (settingsState.tabTitlesMode == "Selected" && isSelected)

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable {
                                                if (currentRoute != item.route) {
                                                    if (settingsState.rememberLastPage) {
                                                        settingsViewModel.setLastOpenedRoute(item.route)
                                                    }
                                                    navController.navigate(item.route) {
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            },
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        // Pill indicator behind icon for active tab
                                        Box(
                                            modifier = Modifier
                                                .size(width = 56.dp, height = 30.dp)
                                                .clip(PillShape)
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else Color.Transparent
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(
                                                    id = if (isSelected) item.selectedIconRes else item.unselectedIconRes
                                                ),
                                                contentDescription = item.label,
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        if (showLabel && settingsState.tabTitlesMode != "Never") {
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = item.label,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
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
    }
}

}