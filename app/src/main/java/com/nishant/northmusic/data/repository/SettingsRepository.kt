package com.nishant.northmusic.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.nishant.northmusic.data.model.AccentColor
import com.nishant.northmusic.data.model.SettingsState
import com.nishant.northmusic.data.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Singleton repository caching application settings with persistent SharedPreferences backing.
 * Saves accent colors, theme modes, navigation routes, and setup completion permanently.
 */
object SettingsRepository {

    private const val PREFS_NAME = "north_music_settings_prefs"
    private var prefs: SharedPreferences? = null

    private val _settingsState = MutableStateFlow(SettingsState())
    val settingsState: StateFlow<SettingsState> = _settingsState.asStateFlow()

    private val _themeMode = MutableStateFlow(_settingsState.value.themeMode)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _dynamicColor = MutableStateFlow(_settingsState.value.dynamicColor)
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    private val _accentColor = MutableStateFlow(_settingsState.value.accentColor)
    val accentColor: StateFlow<AccentColor> = _accentColor.asStateFlow()

    val current: SettingsState
        get() = _settingsState.value

    @Synchronized
    fun init(context: Context) {
        if (prefs != null) return
        val sp = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sp

        val savedAccent = try {
            val name = sp.getString("accentColor", AccentColor.TEAL.name) ?: AccentColor.TEAL.name
            AccentColor.valueOf(name)
        } catch (_: Exception) {
            AccentColor.TEAL
        }

        val savedTheme = try {
            val name = sp.getString("themeMode", ThemeMode.DARK.name) ?: ThemeMode.DARK.name
            ThemeMode.valueOf(name)
        } catch (_: Exception) {
            ThemeMode.DARK
        }

        val savedDynamic = sp.getBoolean("dynamicColor", false)
        val savedSetupDone = sp.getBoolean("setupDone", false)
        val savedHoldTab = sp.getBoolean("holdTabToSearch", false)
        val savedRememberLast = sp.getBoolean("rememberLastPage", true)
        val savedLastRoute = sp.getString("lastOpenedRoute", "home") ?: "home"
        val savedMinDuration = sp.getInt("minSongDurationSeconds", 15)
        val savedIgnoreShort = sp.getBoolean("ignoreShortTracks", true)
        val savedGapless = sp.getBoolean("gaplessPlayback", true)
        val savedNormalization = sp.getBoolean("audioNormalization", true)
        val savedHighQuality = sp.getBoolean("highQualityAudio", true)
        val savedAudioFocus = sp.getBoolean("audioFocus", true)
        val savedSplitAmpersand = sp.getBoolean("splitArtistsOnAmpersand", false)
        val savedDontSplitSet = sp.getStringSet("dontSplitArtistNames", null)
        val savedDontSplit = savedDontSplitSet?.toList() ?: listOf("AC/DC", "Guns N' Roses", "Simon & Garfunkel", "Earth, Wind & Fire")

        val loaded = SettingsState(
            accentColor = savedAccent,
            themeMode = savedTheme,
            dynamicColor = savedDynamic,
            setupDone = savedSetupDone,
            holdTabToSearch = savedHoldTab,
            rememberLastPage = savedRememberLast,
            lastOpenedRoute = savedLastRoute,
            minSongDurationSeconds = savedMinDuration,
            ignoreShortTracks = savedIgnoreShort,
            gaplessPlayback = savedGapless,
            audioNormalization = savedNormalization,
            highQualityAudio = savedHighQuality,
            audioFocus = savedAudioFocus,
            splitArtistsOnAmpersand = savedSplitAmpersand,
            dontSplitArtistNames = savedDontSplit
        )

        _settingsState.value = loaded
        _accentColor.value = savedAccent
        _themeMode.value = savedTheme
        _dynamicColor.value = savedDynamic
    }

    fun update(transform: (SettingsState) -> SettingsState) {
        _settingsState.update { current ->
            val updated = transform(current)
            if (updated.themeMode != _themeMode.value) {
                _themeMode.value = updated.themeMode
            }
            if (updated.dynamicColor != _dynamicColor.value) {
                _dynamicColor.value = updated.dynamicColor
            }
            if (updated.accentColor != _accentColor.value) {
                _accentColor.value = updated.accentColor
            }

            prefs?.edit()?.apply {
                putString("accentColor", updated.accentColor.name)
                putString("themeMode", updated.themeMode.name)
                putBoolean("dynamicColor", updated.dynamicColor)
                putBoolean("setupDone", updated.setupDone)
                putBoolean("holdTabToSearch", updated.holdTabToSearch)
                putBoolean("rememberLastPage", updated.rememberLastPage)
                putString("lastOpenedRoute", updated.lastOpenedRoute)
                putInt("minSongDurationSeconds", updated.minSongDurationSeconds)
                putBoolean("ignoreShortTracks", updated.ignoreShortTracks)
                putBoolean("gaplessPlayback", updated.gaplessPlayback)
                putBoolean("audioNormalization", updated.audioNormalization)
                putBoolean("highQualityAudio", updated.highQualityAudio)
                putBoolean("audioFocus", updated.audioFocus)
                putBoolean("splitArtistsOnAmpersand", updated.splitArtistsOnAmpersand)
                putStringSet("dontSplitArtistNames", updated.dontSplitArtistNames.toSet())
                apply()
            }

            updated
        }
    }
}
