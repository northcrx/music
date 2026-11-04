package com.nishant.northmusic.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.nishant.northmusic.data.model.AudioOutputDevice
import com.nishant.northmusic.data.model.EqualizerPreset
import com.nishant.northmusic.data.model.EqualizerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class EqualizerViewModel : ViewModel() {

    private val _equalizerState = MutableStateFlow(EqualizerState())
    val equalizerState: StateFlow<EqualizerState> = _equalizerState.asStateFlow()

    /**
     * Master Equalizer switch (On/Off).
     * Connects to:
     * android.media.audiofx.Equalizer.setEnabled(enabled)
     * android.media.audiofx.BassBoost.setEnabled(enabled)
     * android.media.audiofx.Virtualizer.setEnabled(enabled)
     */
    fun toggleMaster(enabled: Boolean) {
        _equalizerState.update { it.copy(isMasterEnabled = enabled) }
        // TODO: Apply enable state to active AudioEffect instances for the player's audioSessionId
    }

    /**
     * Switch output device profile (Speaker, Wired, Bluetooth).
     * Saved per output device.
     * In real app: Detected via AudioManager.registerAudioDeviceCallback / AudioDeviceInfo.
     */
    fun selectOutputDevice(device: AudioOutputDevice) {
        _equalizerState.update { it.copy(currentDevice = device) }
        // TODO: Load stored EQ preset / bands for this device from DataStore
    }

    /**
     * AUTO mode switch (default ON).
     * When ON, dynamically selects best compensation curve based on:
     * 1) Current output audio device acoustic response
     * 2) Song genre read from ID3 metadata (TCON tag)
     */
    fun toggleAutoMode(enabled: Boolean) {
        _equalizerState.update { current ->
            val activeDevice = current.currentDevice
            val activeProfile = current.activeProfile.copy(isAutoMode = enabled)
            val updatedProfiles = current.profiles.toMutableMap().apply {
                put(activeDevice, activeProfile)
            }
            current.copy(profiles = updatedProfiles)
        }
        // TODO: Automatically compute EQ curve from song genre and headphone acoustic curve
    }

    /**
     * Set one of the 8 standard presets. Updates the 5 bands to the preset's dB values.
     */
    fun selectPreset(preset: EqualizerPreset) {
        _equalizerState.update { current ->
            val activeDevice = current.currentDevice
            val activeProfile = current.activeProfile.copy(
                selectedPreset = preset,
                bandGains = preset.bandGains
            )
            val updatedProfiles = current.profiles.toMutableMap().apply {
                put(activeDevice, activeProfile)
            }
            current.copy(profiles = updatedProfiles)
        }
        // TODO: Call equalizer.setBandLevel(band, (gainDb * 100).toInt().toShort())
    }

    /**
     * Adjust an individual frequency band (-10dB to +10dB).
     * Frequency bands: 0: 60Hz, 1: 230Hz, 2: 910Hz, 3: 3.6kHz, 4: 14kHz.
     */
    fun setBandGain(bandIndex: Int, gainDb: Float) {
        _equalizerState.update { current ->
            val activeDevice = current.currentDevice
            val currentBands = current.activeProfile.bandGains.toMutableList()
            if (bandIndex in currentBands.indices) {
                currentBands[bandIndex] = gainDb.coerceIn(-10f, 10f)
            }
            val activeProfile = current.activeProfile.copy(
                bandGains = currentBands,
                selectedPreset = EqualizerPreset.FLAT // Custom adjustments detach from strict preset
            )
            val updatedProfiles = current.profiles.toMutableMap().apply {
                put(activeDevice, activeProfile)
            }
            current.copy(profiles = updatedProfiles)
        }
        // TODO: Apply band level to hardware Equalizer engine
    }

    /**
     * Adjust Bass Boost level (0.0 to 1.0).
     */
    fun setBassBoost(level: Float) {
        _equalizerState.update { current ->
            val activeDevice = current.currentDevice
            val activeProfile = current.activeProfile.copy(
                bassBoostPercent = level.coerceIn(0f, 1f)
            )
            val updatedProfiles = current.profiles.toMutableMap().apply {
                put(activeDevice, activeProfile)
            }
            current.copy(profiles = updatedProfiles)
        }
        // TODO: bassBoost.setStrength((level * 1000).toInt().toShort())
    }

    /**
     * Adjust 3D Virtualizer level (0.0 to 1.0).
     */
    fun setVirtualizer(level: Float) {
        _equalizerState.update { current ->
            val activeDevice = current.currentDevice
            val activeProfile = current.activeProfile.copy(
                virtualizerPercent = level.coerceIn(0f, 1f)
            )
            val updatedProfiles = current.profiles.toMutableMap().apply {
                put(activeDevice, activeProfile)
            }
            current.copy(profiles = updatedProfiles)
        }
        // TODO: virtualizer.setStrength((level * 1000).toInt().toShort())
    }
}
