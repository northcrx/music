package com.nishant.northmusic.navigation

import androidx.annotation.DrawableRes
import com.nishant.northmusic.R

sealed class Screen(
    val route: String,
    val title: String,
    @param:DrawableRes val filledIconRes: Int = R.drawable.ic_nav_home_filled,
    @param:DrawableRes val outlinedIconRes: Int = R.drawable.ic_nav_home_outlined
) {
    data object Home : Screen(
        route = "home",
        title = "Home",
        filledIconRes = R.drawable.ic_nav_home_filled,
        outlinedIconRes = R.drawable.ic_nav_home_outlined
    )
    data object Library : Screen(
        route = "library",
        title = "Library",
        filledIconRes = R.drawable.ic_nav_library_music_filled,
        outlinedIconRes = R.drawable.ic_nav_library_music_outlined
    )
    data object Downloads : Screen(
        route = "downloads",
        title = "Downloads",
        filledIconRes = R.drawable.ic_nav_download_filled,
        outlinedIconRes = R.drawable.ic_nav_download_outlined
    )
    data object Settings : Screen(
        route = "settings",
        title = "Settings",
        filledIconRes = R.drawable.ic_nav_settings_filled,
        outlinedIconRes = R.drawable.ic_nav_settings_outlined
    )
    data object NowPlaying : Screen(
        route = "now_playing",
        title = "Now Playing"
    )
    data object Equalizer : Screen(
        route = "equalizer",
        title = "Equalizer"
    )
    data object SettingsAppearance : Screen(
        route = "settings_appearance",
        title = "Appearance"
    )
    data object SettingsPlayback : Screen(
        route = "settings_playback",
        title = "Playback"
    )
    data object SettingsDownloads : Screen(
        route = "settings_downloads",
        title = "Downloads"
    )
    data object SettingsLibrary : Screen(
        route = "settings_library",
        title = "Library"
    )
    data object SettingsAbout : Screen(
        route = "settings_about",
        title = "About"
    )
    data object SetupWizard : Screen(
        route = "setup_wizard",
        title = "Welcome"
    )
}

val BottomNavItems = listOf(
    Screen.Home,
    Screen.Library,
    Screen.Downloads,
    Screen.Settings
)
