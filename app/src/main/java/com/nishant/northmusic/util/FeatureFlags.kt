package com.nishant.northmusic.util

import com.nishant.northmusic.BuildConfig

/**
 * Feature flags for North Music.
 */
object FeatureFlags {
    // Enable Downloads tab and Add URL functionality
    const val SHOW_DOWNLOADS: Boolean = true

    // Equalizer navigation and menus enabled with real hardware audio effects
    const val SHOW_EQUALIZER: Boolean = true

    // Tag editor & Network settings enabled
    const val SHOW_TAG_EDITOR: Boolean = true
    const val SHOW_NETWORK_SETTINGS: Boolean = true
}
