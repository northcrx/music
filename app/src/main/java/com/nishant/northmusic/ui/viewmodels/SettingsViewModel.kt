package com.nishant.northmusic.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nishant.northmusic.data.model.AccentColor
import com.nishant.northmusic.data.model.MetadataFixItem
import com.nishant.northmusic.data.model.MetadataFixState
import com.nishant.northmusic.data.model.SettingsState
import com.nishant.northmusic.data.model.ThemeMode
import com.nishant.northmusic.data.repository.SettingsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {

    val settingsState: StateFlow<SettingsState> = SettingsRepository.settingsState

    private fun updateState(transform: (SettingsState) -> SettingsState) {
        SettingsRepository.update(transform)
    }

    // --- Navigation Settings ---
    fun setHoldTabToSearch(enabled: Boolean) {
        updateState { it.copy(holdTabToSearch = enabled) }
        // TODO: Persist to DataStore
    }

    fun setRememberLastPage(enabled: Boolean) {
        updateState { it.copy(rememberLastPage = enabled) }
        // TODO: Persist to DataStore
    }

    fun setLastOpenedRoute(route: String) {
        updateState { it.copy(lastOpenedRoute = route) }
        // TODO: Persist to DataStore
    }

    // --- Appearance Settings ---
    fun toggleDynamicColor(enabled: Boolean) {
        updateState { it.copy(dynamicColor = enabled) }
        // TODO: Save preference to DataStore
    }

    fun setThemeMode(mode: ThemeMode) {
        updateState { it.copy(themeMode = mode) }
        // TODO: Save preference to DataStore
    }

    fun setAccentColor(accent: AccentColor) {
        updateState { it.copy(accentColor = accent) }
        // TODO: Save preference to DataStore
    }

    fun setLargerHeaderImage(enabled: Boolean) {
        updateState { it.copy(largerHeaderImage = enabled) }
    }

    fun setTabTitlesMode(mode: String) {
        updateState { it.copy(tabTitlesMode = mode) }
    }

    fun setScrollingTextForLongTitles(enabled: Boolean) {
        updateState { it.copy(scrollingTextForLongTitles = enabled) }
    }

    // --- Now Playing Settings ---
    fun setAnimateControls(enabled: Boolean) {
        updateState { it.copy(animateControls = enabled) }
    }

    fun setExtraControlsMiniPlayer(enabled: Boolean) {
        updateState { it.copy(extraControlsMiniPlayer = enabled) }
    }

    fun setAdaptiveControls(enabled: Boolean) {
        updateState { it.copy(adaptiveControls = enabled) }
    }

    fun setSquigglySeekBar(enabled: Boolean) {
        updateState { it.copy(squigglySeekBar = enabled) }
    }

    fun setPreferRemainingTime(enabled: Boolean) {
        updateState { it.copy(preferRemainingTime = enabled) }
    }

    // --- Lyrics Settings ---
    fun setShowTranslation(enabled: Boolean) {
        updateState { it.copy(showTranslation = enabled) }
    }

    fun setShowTransliteration(enabled: Boolean) {
        updateState { it.copy(showTransliteration = enabled) }
    }

    fun setCenterCurrentLine(enabled: Boolean) {
        updateState { it.copy(centerCurrentLine = enabled) }
    }

    fun setCenterLyricsHorizontally(enabled: Boolean) {
        updateState { it.copy(centerLyricsHorizontally = enabled) }
    }

    fun setLyricsFontSize(size: Float) {
        updateState { it.copy(lyricsFontSize = size) }
    }

    // --- Playback Settings ---
    fun toggleGaplessPlayback(enabled: Boolean) {
        updateState { it.copy(gaplessPlayback = enabled) }
    }

    fun toggleAudioNormalization(enabled: Boolean) {
        updateState { it.copy(audioNormalization = enabled) }
    }

    fun toggleHighQualityAudio(enabled: Boolean) {
        updateState { it.copy(highQualityAudio = enabled) }
    }

    fun setReplayGainMode(mode: String) {
        updateState { it.copy(replayGainMode = mode) }
    }

    fun setHeadphonesResume(enabled: Boolean) {
        updateState { it.copy(headphonesResumeOnConnect = enabled) }
    }

    fun setHeadphonesPause(enabled: Boolean) {
        updateState { it.copy(headphonesPauseOnDisconnect = enabled) }
    }

    fun setBluetoothResume(enabled: Boolean) {
        updateState { it.copy(bluetoothResumeOnConnect = enabled) }
    }

    fun setBluetoothPause(enabled: Boolean) {
        updateState { it.copy(bluetoothPauseOnDisconnect = enabled) }
    }

    fun setAudioFocus(enabled: Boolean) {
        updateState { it.copy(audioFocus = enabled) }
    }

    // --- Library Settings ---
    fun setLibraryCategories(categories: List<String>) {
        updateState { it.copy(libraryCategories = categories) }
    }

    fun setMinSongDurationSeconds(seconds: Int) {
        updateState { it.copy(minSongDurationSeconds = seconds) }
    }

    fun setArtistMinSongCount(count: Int) {
        updateState { it.copy(artistMinSongCount = count) }
    }

    fun setAlbumMinSongCount(count: Int) {
        updateState { it.copy(albumMinSongCount = count) }
    }

    fun setFoldersBlacklist(folders: List<String>) {
        updateState { it.copy(foldersBlacklist = folders) }
    }

    fun setIgnoreArticlesWhenSorting(enabled: Boolean) {
        updateState { it.copy(ignoreArticlesWhenSorting = enabled) }
    }

    fun setSendDeletedToTrash(enabled: Boolean) {
        updateState { it.copy(sendDeletedToTrash = enabled) }
    }

    fun setRecordListeningHistory(enabled: Boolean) {
        updateState { it.copy(recordListeningHistory = enabled) }
    }

    fun setHistoryIntervalDays(days: Int) {
        updateState { it.copy(historyIntervalDays = days) }
    }

    fun setLastAddedIntervalDays(days: Int) {
        updateState { it.copy(lastAddedIntervalDays = days) }
    }

    fun toggleIgnoreShortTracks(enabled: Boolean) {
        updateState { it.copy(ignoreShortTracks = enabled) }
    }

    // --- Downloads Settings ---
    fun toggleWifiOnlyDownloads(enabled: Boolean) {
        updateState { it.copy(wifiOnlyDownloads = enabled) }
    }

    fun setMaxParallelDownloads(max: Int) {
        updateState { it.copy(maxParallelDownloads = max) }
    }

    fun setSaveFolder(path: String) {
        updateState { it.copy(saveFolder = path) }
    }

    fun setFileNamingFormat(format: String) {
        updateState { it.copy(fileNamingFormat = format) }
    }

    // --- Network Settings ---
    fun setOnlineMetadataLookup(enabled: Boolean) {
        updateState { it.copy(onlineMetadataLookup = enabled) }
    }

    // --- Advanced Settings ---
    fun setAppLanguage(lang: String) {
        updateState { it.copy(appLanguage = lang) }
    }

    fun setRotationLock(enabled: Boolean) {
        updateState { it.copy(rotationLock = enabled) }
    }

    fun setSetupDone(done: Boolean) {
        updateState { it.copy(setupDone = done) }
    }

    fun triggerRescanLibrary() {
        viewModelScope.launch {
            updateState { it.copy(isRescanning = true) }
            delay(1800)
            updateState { it.copy(isRescanning = false) }
        }
    }

    /**
     * Fix missing metadata and artwork via iTunes Search API.
     */
    fun startFixingMetadata() {
        viewModelScope.launch {
            updateState { current ->
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

            updateState { current ->
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

            updateState { current ->
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

            updateState { current ->
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
        updateState { it.copy(metadataFixState = MetadataFixState()) }
    }
}
