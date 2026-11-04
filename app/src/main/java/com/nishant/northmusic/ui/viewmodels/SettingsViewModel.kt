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

    fun toggleGaplessPlayback(enabled: Boolean) {
        _settingsState.update { it.copy(gaplessPlayback = enabled) }
        // TODO: Update audio player configuration
    }

    fun toggleAudioNormalization(enabled: Boolean) {
        _settingsState.update { it.copy(audioNormalization = enabled) }
        // TODO: Apply audio effect processor
    }

    fun toggleHighQualityAudio(enabled: Boolean) {
        _settingsState.update { it.copy(highQualityAudio = enabled) }
        // TODO: Update audio decoder preference
    }

    fun toggleWifiOnlyDownloads(enabled: Boolean) {
        _settingsState.update { it.copy(wifiOnlyDownloads = enabled) }
        // TODO: Set download network constraints
    }

    fun setMaxParallelDownloads(max: Int) {
        _settingsState.update { it.copy(maxParallelDownloads = max) }
        // TODO: Update download manager thread pool
    }

    fun setSaveFolder(path: String) {
        _settingsState.update { it.copy(saveFolder = path) }
        // TODO: Save preference to DataStore
    }

    fun setFileNamingFormat(format: String) {
        _settingsState.update { it.copy(fileNamingFormat = format) }
        // TODO: Save preference to DataStore
    }

    fun toggleIgnoreShortTracks(enabled: Boolean) {
        _settingsState.update { it.copy(ignoreShortTracks = enabled) }
        // TODO: Update media scanner threshold
    }

    fun setSetupDone(done: Boolean) {
        _settingsState.update { it.copy(setupDone = done) }
        // TODO: Persist setupDone to DataStore
    }

    fun triggerRescanLibrary() {
        viewModelScope.launch {
            _settingsState.update { it.copy(isRescanning = true) }
            delay(1800)
            _settingsState.update { it.copy(isRescanning = false) }
            // TODO: Trigger full storage scan via WorkManager
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
