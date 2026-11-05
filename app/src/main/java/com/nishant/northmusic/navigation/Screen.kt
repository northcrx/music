package com.nishant.northmusic.navigation

import androidx.annotation.DrawableRes
import com.nishant.northmusic.R
import com.nishant.northmusic.util.FeatureFlags

sealed class Screen(
    val route: String,
    val title: String,
    @param:DrawableRes val filledIconRes: Int = R.drawable.ic_nav_home_filled,
    @param:DrawableRes val outlinedIconRes: Int = R.drawable.ic_nav_home_outlined
) {
    val selectedIconRes: Int get() = filledIconRes
    val unselectedIconRes: Int get() = outlinedIconRes
    val label: String get() = title

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

    // Home 4 Shortcut List Pages
    data object TopTracks : Screen(
        route = "top_tracks",
        title = "Top tracks"
    )
    data object LastAdded : Screen(
        route = "last_added",
        title = "Last added"
    )
    data object History : Screen(
        route = "history",
        title = "History"
    )
    data object Favorites : Screen(
        route = "favorites",
        title = "Favorites"
    )

    // Settings Sub-screens
    data object SettingsAppearance : Screen(
        route = "settings_appearance",
        title = "Appearance"
    )
    data object SettingsHomeSections : Screen(
        route = "settings_home_sections",
        title = "Home sections"
    )
    data object SettingsNowPlaying : Screen(
        route = "settings_now_playing",
        title = "Now playing"
    )
    data object SettingsLyrics : Screen(
        route = "settings_lyrics",
        title = "Lyrics"
    )
    data object SettingsPlayback : Screen(
        route = "settings_playback",
        title = "Playback"
    )
    data object SettingsLibrary : Screen(
        route = "settings_library",
        title = "Library"
    )
    data object SettingsDownloads : Screen(
        route = "settings_downloads",
        title = "Downloads"
    )
    data object CategoryRules : Screen(
        route = "category_rules",
        title = "Category rules"
    )
    data object SettingsNetwork : Screen(
        route = "settings_network",
        title = "Network"
    )
    data object SettingsPermissionsSetup : Screen(
        route = "settings_permissions_setup",
        title = "Permissions & setup"
    )
    data object SettingsAdvanced : Screen(
        route = "settings_advanced",
        title = "Advanced"
    )
    data object SettingsAbout : Screen(
        route = "settings_about",
        title = "About"
    )
    data object FixMetadata : Screen(
        route = "fix_metadata",
        title = "Fix metadata and artwork"
    )
    data object SetupWizard : Screen(
        route = "setup_wizard",
        title = "Welcome"
    )
}

val BottomNavItems: List<Screen>
    get() = if (FeatureFlags.SHOW_DOWNLOADS) {
        listOf(
            Screen.Home,
            Screen.Library,
            Screen.Downloads
        )
    } else {
        listOf(
            Screen.Home,
            Screen.Library
        )
    }
