package com.nishant.northmusic.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nishant.northmusic.data.model.SampleData
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
import com.nishant.northmusic.ui.screens.settings.LibrarySettingsScreen
import com.nishant.northmusic.ui.screens.settings.LyricsSettingsScreen
import com.nishant.northmusic.ui.screens.settings.NetworkSettingsScreen
import com.nishant.northmusic.ui.screens.settings.NowPlayingSettingsScreen
import com.nishant.northmusic.ui.screens.settings.PlaybackSettingsScreen
import com.nishant.northmusic.ui.screens.setup.SetupWizardScreen
import com.nishant.northmusic.ui.viewmodels.DownloadsViewModel
import com.nishant.northmusic.ui.viewmodels.EqualizerViewModel
import com.nishant.northmusic.ui.viewmodels.HomeViewModel
import com.nishant.northmusic.ui.viewmodels.LibraryViewModel
import com.nishant.northmusic.ui.viewmodels.PlayerViewModel
import com.nishant.northmusic.ui.viewmodels.SettingsViewModel

val BottomListScrollPadding = 88.dp

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
    val downloadsUiState by downloadsViewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.settingsState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

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
            currentRoute == Screen.TopTracks.route ||
            currentRoute == Screen.LastAdded.route ||
            currentRoute == Screen.History.route ||
            currentRoute == Screen.Favorites.route

    // Mini-player is hidden during selection mode, full-screen route, active search, or when dismissed via swipe down
    val isMiniPlayerVisible = !isFullScreenRoute && !downloadsUiState.isSelectionMode && !isSearchActive && !isMiniPlayerDismissed
    val showBottomNav = !isFullScreenRoute && !isSearchActive

    val initialStartDestination = if (!settingsState.setupDone) {
        Screen.SetupWizard.route
    } else if (settingsState.rememberLastPage && settingsState.lastOpenedRoute.isNotBlank()) {
        settingsState.lastOpenedRoute
    } else {
        Screen.Home.route
    }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
            ) {
                // Persistent Mini Player levitating above the Navigation Bar
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
                    onNextClick = { playerViewModel.skipNext() },
                    onDismiss = { isMiniPlayerDismissed = true }
                )

                AnimatedVisibility(
                    visible = showBottomNav,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    // Navigation Bar (3 items: Home, Library, Downloads)
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 4.dp
                    ) {
                        BottomNavItems.forEach { screen ->
                            val selected = currentRoute == screen.route
                            val iconRes = if (selected) screen.filledIconRes else screen.outlinedIconRes

                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    isSearchActive = false
                                    if (currentRoute != screen.route) {
                                        if (settingsState.rememberLastPage) {
                                            settingsViewModel.setLastOpenedRoute(screen.route)
                                        }
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        painter = painterResource(id = iconRes),
                                        contentDescription = screen.title
                                    )
                                },
                                label = {
                                    Text(
                                        text = screen.title,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = initialStartDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    homeViewModel = homeViewModel,
                    playerViewModel = playerViewModel,
                    onNavigateToSettings = {
                        isSearchActive = false
                        navController.navigate(Screen.Settings.route)
                    },
                    onNavigateToShortcut = { route ->
                        isSearchActive = false
                        navController.navigate(route)
                    },
                    isSearchActive = isSearchActive,
                    onSearchActiveChange = { isSearchActive = it }
                )
            }
            composable(Screen.Library.route) {
                LibraryScreen(
                    libraryViewModel = libraryViewModel,
                    playerViewModel = playerViewModel
                )
            }
            composable(Screen.Downloads.route) {
                DownloadsScreen(
                    downloadsViewModel = downloadsViewModel,
                    saveFolder = settingsState.saveFolder
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
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
                    songs = SampleData.songs.sortedByDescending { it.durationSeconds },
                    playerViewModel = playerViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.LastAdded.route) {
                SongListSubScreen(
                    title = "Last added",
                    songs = SampleData.songs.reversed(),
                    playerViewModel = playerViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.History.route) {
                SongListSubScreen(
                    title = "History",
                    songs = SampleData.songs,
                    playerViewModel = playerViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Favorites.route) {
                SongListSubScreen(
                    title = "Favorites",
                    songs = SampleData.songs.filter { it.isFavorite },
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
                    onDismiss = { navController.popBackStack() },
                    onNavigateToEqualizer = { navController.navigate(Screen.Equalizer.route) }
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
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.SetupWizard.route) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
