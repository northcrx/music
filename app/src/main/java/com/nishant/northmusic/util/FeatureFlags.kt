package com.nishant.northmusic.util

import com.nishant.northmusic.BuildConfig

/**
 * Feature flags for Version 1.0 release.
 * Hides unfinished/in-progress features cleanly while keeping all underlying code intact.
 */
object FeatureFlags {
    // Hide Downloads tab and Add URL button for v1
    const val SHOW_DOWNLOADS: Boolean = false

    // Hide Equalizer navigation and menus until real hardware audio effects are bound
    const val SHOW_EQUALIZER: Boolean = false

    // Hide Tag editor & Network settings while not working
    const val SHOW_TAG_EDITOR: Boolean = false
    const val SHOW_NETWORK_SETTINGS: Boolean = false
}
