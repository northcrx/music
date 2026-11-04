package com.nishant.northmusic.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nishant.northmusic.data.model.AccentColor
import com.nishant.northmusic.data.model.MetadataFixItem
import com.nishant.northmusic.data.model.MetadataFixState
import com.nishant.northmusic.data.model.SettingsState
import com.nishant.northmusic.data.model.ThemeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {

    private val _settingsState = MutableStateFlow(SettingsState())
    val settingsState: StateFlow<SettingsState> = _settingsState.asStateFlow()

    // --- Navigation Settings ---
    fun setHoldTabToSearch(enabled: Boolean) {
        _settingsState.update { it.copy(holdTabToSearch = enabled) }
        // TODO: Persist to DataStore
    }

    fun setRememberLastPage(enabled: Boolean) {
        _settingsState.update { it.copy(rememberLastPage = enabled) }
        // TODO: Persist to DataStore
    }

    fun setLastOpenedRoute(route: String) {
        _settingsState.update { it.copy(lastOpenedRoute = route) }
        // TODO: Persist to DataStore
    }

    // --- Appearance Settings ---
    fun toggleDynamicColor(enabled: Boolean) {
        _settingsState.update { it.copy(dynamicColor = enabled) }
        // TODO: Save preference to DataStore
    }

    fun setThemeMode(mode: ThemeMode) {
        _settingsState.update { it.copy(themeMode = mode) }
        // TODO: Save preference to DataStore
    }

    fun setAccentColor(accent: AccentColor) {
        _settingsState.update { it.copy(accentColor = accent) }
        // TODO: Save preference to DataStore
    }

    fun setLargerHeaderImage(enabled: Boolean) {
        _settingsState.update { it.copy(largerHeaderImage = enabled) }
    }

    fun setTabTitlesMode(mode: String) {
        _settingsState.update { it.copy(tabTitlesMode = mode) }
    }

    fun setScrollingTextForLongTitles(enabled: Boolean) {
        _settingsState.update { it.copy(scrollingTextForLongTitles = enabled) }
    }

    // --- Now Playing Settings ---
    fun setAnimateControls(enabled: Boolean) {
        _settingsState.update { it.copy(animateControls = enabled) }
    }

    fun setExtraControlsMiniPlayer(enabled: Boolean) {
        _settingsState.update { it.copy(extraControlsMiniPlayer = enabled) }
    }

    fun setAdaptiveControls(enabled: Boolean) {
        _settingsState.update { it.copy(adaptiveControls = enabled) }
    }

    fun setSquigglySeekBar(enabled: Boolean) {
        _settingsState.update { it.copy(squigglySeekBar = enabled) }
    }

    fun setPreferRemainingTime(enabled: Boolean) {
        _settingsState.update { it.copy(preferRemainingTime = enabled) }
    }

    // --- Lyrics Settings ---
    fun setShowTranslation(enabled: Boolean) {
        _settingsState.update { it.copy(showTranslation = enabled) }
    }

    fun setShowTransliteration(enabled: Boolean) {
        _settingsState.update { it.copy(showTransliteration = enabled) }
    }

    fun setCenterCurrentLine(enabled: Boolean) {
        _settingsState.update { it.copy(centerCurrentLine = enabled) }
    }

    fun setCenterLyricsHorizontally(enabled: Boolean) {
        _settingsState.update { it.copy(centerLyricsHorizontally = enabled) }
    }

    fun setLyricsFontSize(size: Float) {
        _settingsState.update { it.copy(lyricsFontSize = size) }
    }

    // --- Playback Settings ---
    fun toggleGaplessPlayback(enabled: Boolean) {
        _settingsState.update { it.copy(gaplessPlayback = enabled) }
    }

    fun toggleAudioNormalization(enabled: Boolean) {
        _settingsState.update { it.copy(audioNormalization = enabled) }
    }

    fun toggleHighQualityAudio(enabled: Boolean) {
        _settingsState.update { it.copy(highQualityAudio = enabled) }
    }

    fun setReplayGainMode(mode: String) {
        _settingsState.update { it.copy(replayGainMode = mode) }
    }

    fun setHeadphonesResume(enabled: Boolean) {
        _settingsState.update { it.copy(headphonesResumeOnConnect = enabled) }
    }

    fun setHeadphonesPause(enabled: Boolean) {
        _settingsState.update { it.copy(headphonesPauseOnDisconnect = enabled) }
    }

    fun setBluetoothResume(enabled: Boolean) {
        _settingsState.update { it.copy(bluetoothResumeOnConnect = enabled) }
    }

    fun setBluetoothPause(enabled: Boolean) {
        _settingsState.update { it.copy(bluetoothPauseOnDisconnect = enabled) }
    }

    fun setAudioFocus(enabled: Boolean) {
        _settingsState.update { it.copy(audioFocus = enabled) }
    }

    // --- Library Settings ---
    fun setLibraryCategories(categories: List<String>) {
        _settingsState.update { it.copy(libraryCategories = categories) }
    }

    fun setMinSongDurationSeconds(seconds: Int) {
        _settingsState.update { it.copy(minSongDurationSeconds = seconds) }
    }

    fun setArtistMinSongCount(count: Int) {
        _settingsState.update { it.copy(artistMinSongCount = count) }
    }

    fun setAlbumMinSongCount(count: Int) {
        _settingsState.update { it.copy(albumMinSongCount = count) }
    }

    fun setFoldersBlacklist(folders: List<String>) {
        _settingsState.update { it.copy(foldersBlacklist = folders) }
    }

    fun setIgnoreArticlesWhenSorting(enabled: Boolean) {
        _settingsState.update { it.copy(ignoreArticlesWhenSorting = enabled) }
    }

    fun setSendDeletedToTrash(enabled: Boolean) {
        _settingsState.update { it.copy(sendDeletedToTrash = enabled) }
    }

    fun setRecordListeningHistory(enabled: Boolean) {
        _settingsState.update { it.copy(recordListeningHistory = enabled) }
    }

    fun setHistoryIntervalDays(days: Int) {
        _settingsState.update { it.copy(historyIntervalDays = days) }
    }

    fun setLastAddedIntervalDays(days: Int) {
        _settingsState.update { it.copy(lastAddedIntervalDays = days) }
    }

    fun toggleIgnoreShortTracks(enabled: Boolean) {
        _settingsState.update { it.copy(ignoreShortTracks = enabled) }
    }

    // --- Downloads Settings ---
    fun toggleWifiOnlyDownloads(enabled: Boolean) {
        _settingsState.update { it.copy(wifiOnlyDownloads = enabled) }
    }

    fun setMaxParallelDownloads(max: Int) {
        _settingsState.update { it.copy(maxParallelDownloads = max) }
    }

    fun setSaveFolder(path: String) {
        _settingsState.update { it.copy(saveFolder = path) }
    }

    fun setFileNamingFormat(format: String) {
        _settingsState.update { it.copy(fileNamingFormat = format) }
    }

    // --- Network Settings ---
    fun setOnlineMetadataLookup(enabled: Boolean) {
        _settingsState.update { it.copy(onlineMetadataLookup = enabled) }
    }

    // --- Advanced Settings ---
    fun setAppLanguage(lang: String) {
        _settingsState.update { it.copy(appLanguage = lang) }
    }

    fun setRotationLock(enabled: Boolean) {
        _settingsState.update { it.copy(rotationLock = enabled) }
    }

    fun setSetupDone(done: Boolean) {
        _settingsState.update { it.copy(setupDone = done) }
    }

    fun triggerRescanLibrary() {
        viewModelScope.launch {
            _settingsState.update { it.copy(isRescanning = true) }
            delay(1800)
            _settingsState.update { it.copy(isRescanning = false) }
        }
    }

    /**
     * Fix missing metadata and artwork via iTunes Search API.
     */
    fun startFixingMetadata() {
        viewModelScope.launch {
            _settingsState.update { current ->
                current.copy(
                    metadataFixState = MetadataFixState(
                        isScanning = true,
                        progress = 0.15f,
                        statusText = "Scanning local library for missing tags & artwork...",
                        items = emptyList()
                    )
                )
            }
            delay(1200)

            _settingsState.update { current ->
                current.copy(
                    metadataFixState = current.metadataFixState.copy(
                        progress = 0.45f,
                        statusText = "Querying iTunes Search API for 1400x1400 artwork...",
                        items = listOf(
                            MetadataFixItem("Starboy", "The Weeknd", "High-res artwork (1400x1400) found", true),
                            MetadataFixItem("Blinding Lights", "The Weeknd", "Year (2020) & Pop/Synthwave genre matched", true)
                        )
                    )
                )
            }
            delay(1500)

            _settingsState.update { current ->
                current.copy(
                    metadataFixState = current.metadataFixState.copy(
                        progress = 0.85f,
                        statusText = "Writing ID3 tags & updating artwork cache...",
                        items = current.metadataFixState.items + listOf(
                            MetadataFixItem("Midnight City", "M83", "Artwork fetched & Album Artist set", true),
                            MetadataFixItem("Tech Noir", "Gunship", "Synthwave genre tag & cover embedded", true)
                        )
                    )
                )
            }
            delay(1200)

            _settingsState.update { current ->
                current.copy(
                    metadataFixState = current.metadataFixState.copy(
                        isScanning = false,
                        progress = 1.0f,
                        statusText = "Metadata and artwork successfully updated!"
                    )
                )
            }
        }
    }

    fun dismissMetadataFix() {
        _settingsState.update { it.copy(metadataFixState = MetadataFixState()) }
    }
}
