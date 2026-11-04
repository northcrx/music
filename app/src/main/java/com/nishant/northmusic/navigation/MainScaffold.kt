package com.nishant.northmusic.navigation

import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
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
import com.nishant.northmusic.ui.screens.settings.AboutSettingsScreen
import com.nishant.northmusic.ui.screens.settings.AppearanceSettingsScreen
import com.nishant.northmusic.ui.screens.settings.DownloadsSettingsScreen
import com.nishant.northmusic.ui.screens.settings.LibrarySettingsScreen
import com.nishant.northmusic.ui.screens.settings.PlaybackSettingsScreen
import com.nishant.northmusic.ui.screens.setup.SetupWizardScreen
import com.nishant.northmusic.ui.viewmodels.DownloadsViewModel
import com.nishant.northmusic.ui.viewmodels.EqualizerViewModel
import com.nishant.northmusic.ui.viewmodels.HomeViewModel
import com.nishant.northmusic.ui.viewmodels.LibraryViewModel
import com.nishant.northmusic.ui.viewmodels.PlayerViewModel
import com.nishant.northmusic.ui.viewmodels.SettingsViewModel

val BottomListScrollPadding = 16.dp

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

    val isFullScreenRoute = currentRoute == Screen.NowPlaying.route ||
            currentRoute == Screen.Equalizer.route ||
            currentRoute == Screen.SetupWizard.route ||
            currentRoute == Screen.SettingsAppearance.route ||
            currentRoute == Screen.SettingsPlayback.route ||
            currentRoute == Screen.SettingsDownloads.route ||
            currentRoute == Screen.SettingsLibrary.route ||
            currentRoute == Screen.SettingsAbout.route

    // Mini-player is hidden during selection mode on Downloads screen or when full screen is open
    val isMiniPlayerVisible = !isFullScreenRoute && !downloadsUiState.isSelectionMode

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        bottomBar = {
            if (!isFullScreenRoute) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    // Persistent Mini Player above the Navigation Bar
                    MiniPlayer(
                        playerState = playerState,
                        visible = isMiniPlayerVisible,
                        onTap = {
                            navController.navigate(Screen.NowPlaying.route) {
                                launchSingleTop = true
                            }
                        },
                        onPlayPauseClick = { playerViewModel.togglePlayPause() },
                        onFavoriteClick = { playerViewModel.toggleFavorite() }
                    )

                    // Navigation Bar (100% opaque surfaceContainer, neutral near-black)
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
                                    if (currentRoute != screen.route) {
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
            startDestination = if (settingsState.setupDone) Screen.Home.route else Screen.SetupWizard.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    homeViewModel = homeViewModel,
                    playerViewModel = playerViewModel
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
                    onNavigateToPlayback = { navController.navigate(Screen.SettingsPlayback.route) },
                    onNavigateToDownloads = { navController.navigate(Screen.SettingsDownloads.route) },
                    onNavigateToLibrary = { navController.navigate(Screen.SettingsLibrary.route) },
                    onNavigateToAbout = { navController.navigate(Screen.SettingsAbout.route) }
                )
            }
            composable(Screen.SettingsAppearance.route) {
                AppearanceSettingsScreen(
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
            composable(Screen.SettingsDownloads.route) {
                DownloadsSettingsScreen(
                    settingsViewModel = settingsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.SettingsLibrary.route) {
                LibrarySettingsScreen(
                    settingsViewModel = settingsViewModel,
                    onNavigateBack = { navController.popBackStack() }
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
