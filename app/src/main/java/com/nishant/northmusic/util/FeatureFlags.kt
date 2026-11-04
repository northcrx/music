package com.nishant.northmusic.util

import com.nishant.northmusic.BuildConfig

/**
 * Feature flags for Version 1.0 release.
 * Hides unfinished/in-progress features cleanly while keeping all underlying code intact.
 */
object FeatureFlags {
    // Hide Downloads tab and Add URL button for v1
    const val SHOW_DOWNLOADS: Boolean = false

    // Equalizer navigation and menus enabled with real hardware audio effects
    const val SHOW_EQUALIZER: Boolean = true

    // Tag editor & Network settings enabled
    const val SHOW_TAG_EDITOR: Boolean = true
    const val SHOW_NETWORK_SETTINGS: Boolean = true
}
