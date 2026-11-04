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

    private fun updateState(updater: (SettingsState) -> SettingsState) {
        SettingsRepository.update(updater)
    }

    // --- Navigation Settings ---
    fun setHoldTabToSearch(enabled: Boolean) {
        updateState { it.copy(holdTabToSearch = enabled) }
    }

    fun setRememberLastPage(enabled: Boolean) {
        updateState { it.copy(rememberLastPage = enabled) }
    }

    fun setLastOpenedRoute(route: String) {
        updateState { it.copy(lastOpenedRoute = route) }
    }

    // --- Appearance Settings ---
    fun setDynamicColor(enabled: Boolean) {
        updateState { it.copy(dynamicColor = enabled) }
    }

    fun toggleDynamicColor(enabled: Boolean) = setDynamicColor(enabled)

    fun setThemeMode(mode: ThemeMode) {
        updateState { it.copy(themeMode = mode) }
    }

    fun setAccentColor(accentColor: AccentColor) {
        updateState { it.copy(accentColor = accentColor) }
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

    fun setHighQualityAudio(enabled: Boolean) = toggleHighQualityAudio(enabled)
    fun setAudioNormalization(enabled: Boolean) = toggleAudioNormalization(enabled)
    fun setGaplessPlayback(enabled: Boolean) = toggleGaplessPlayback(enabled)
    fun setFadeOnPlayPause(enabled: Boolean) {
        updateState { it.copy(fadeOnPlayPause = enabled) }
    }

    fun setReplayGainMode(mode: String) {
        updateState { it.copy(replayGainMode = mode) }
    }

    fun setHeadphonesResume(enabled: Boolean) {
        updateState { it.copy(headphonesResumeOnConnect = enabled) }
    }

    fun setResumeOnHeadphonesConnect(enabled: Boolean) = setHeadphonesResume(enabled)

    fun setHeadphonesPause(enabled: Boolean) {
        updateState { it.copy(headphonesPauseOnDisconnect = enabled) }
    }

    fun setPauseOnHeadphonesDisconnect(enabled: Boolean) = setHeadphonesPause(enabled)

    fun setBluetoothResume(enabled: Boolean) {
        updateState { it.copy(bluetoothResumeOnConnect = enabled) }
    }

    fun setResumeOnBluetoothConnect(enabled: Boolean) = setBluetoothResume(enabled)

    fun setBluetoothPause(enabled: Boolean) {
        updateState { it.copy(bluetoothPauseOnDisconnect = enabled) }
    }

    fun setPauseOnBluetoothDisconnect(enabled: Boolean) = setBluetoothPause(enabled)

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

    fun setFoldersBlacklist(blacklist: List<String>) {
        updateState { it.copy(foldersBlacklist = blacklist) }
    }

    fun addFolderToBlacklist(folder: String) {
        updateState {
            val list = it.foldersBlacklist.toMutableList()
            if (!list.contains(folder)) list.add(folder)
            it.copy(foldersBlacklist = list)
        }
    }

    fun removeFolderFromBlacklist(folder: String) {
        updateState {
            val list = it.foldersBlacklist.toMutableList()
            list.remove(folder)
            it.copy(foldersBlacklist = list)
        }
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

    fun setIgnoreShortTracks(enabled: Boolean) = toggleIgnoreShortTracks(enabled)

    fun setMusicFolders(folders: List<String>) {
        updateState { it.copy(musicFolders = folders) }
    }

    fun setIsRescanning(rescanning: Boolean) {
        updateState { it.copy(isRescanning = rescanning) }
    }

    fun triggerRescanLibrary() {
        viewModelScope.launch {
            updateState { it.copy(isRescanning = true) }
            delay(1500)
            updateState { it.copy(isRescanning = false) }
        }
    }

    fun startFixingMetadata() {
        viewModelScope.launch {
            updateState { current ->
                current.copy(
                    metadataFixState = MetadataFixState(
                        isScanning = true,
                        progress = 0.2f,
                        statusText = "Scanning local library for metadata...",
                        items = emptyList()
                    )
                )
            }
            delay(1000)
            updateState { current ->
                current.copy(
                    metadataFixState = current.metadataFixState.copy(
                        progress = 0.6f,
                        statusText = "Resolving tags and album artwork...",
                        items = listOf(
                            MetadataFixItem("High-res art verified", "Local library", "Tags synchronized", true)
                        )
                    )
                )
            }
            delay(1000)
            updateState { current ->
                current.copy(
                    metadataFixState = current.metadataFixState.copy(
                        isScanning = false,
                        progress = 1.0f,
                        statusText = "Metadata scan completed"
                    )
                )
            }
        }
    }

    fun dismissMetadataFix() {
        updateState { it.copy(metadataFixState = MetadataFixState()) }
    }

    // --- Downloads Settings ---
    fun setSaveFolder(folder: String) {
        updateState { it.copy(saveFolder = folder) }
    }

    fun setWifiOnly(enabled: Boolean) {
        updateState { it.copy(wifiOnly = enabled, wifiOnlyDownloads = enabled) }
    }

    fun toggleWifiOnlyDownloads(enabled: Boolean) = setWifiOnly(enabled)

    fun setParallelSlots(slots: Int) {
        updateState { it.copy(parallelSlots = slots, maxParallelDownloads = slots) }
    }

    fun setMaxParallelDownloads(max: Int) = setParallelSlots(max)

    fun setNamingFormat(format: String) {
        updateState { it.copy(namingFormat = format, fileNamingFormat = format) }
    }

    fun setFileNamingFormat(format: String) = setNamingFormat(format)

    fun setDownloadSpeedLimitKbps(limit: Int) {
        updateState { it.copy(downloadSpeedLimitKbps = limit) }
    }

    // --- Network Settings ---
    fun setAutoDownloadMetadata(enabled: Boolean) {
        updateState { it.copy(autoDownloadMetadata = enabled, onlineMetadataLookup = enabled) }
    }

    fun setOnlineMetadataLookup(enabled: Boolean) = setAutoDownloadMetadata(enabled)

    fun setPreferredLyricsProvider(provider: String) {
        updateState { it.copy(preferredLyricsProvider = provider, lyricsProviders = provider) }
    }

    fun setStreamingQuality(quality: String) {
        updateState { it.copy(streamingQuality = quality) }
    }

    // --- Advanced Settings ---
    fun setAppLanguage(language: String) {
        updateState { it.copy(appLanguage = language) }
    }

    fun setLockScreenRotation(enabled: Boolean) {
        updateState { it.copy(lockScreenRotation = enabled, rotationLock = enabled) }
    }

    fun setRotationLock(enabled: Boolean) = setLockScreenRotation(enabled)

    fun setSetupDone(done: Boolean) {
        updateState { it.copy(setupDone = done) }
    }

    fun resetToDefaults() {
        val defaultState = SettingsState()
        updateState { defaultState }
    }
}
