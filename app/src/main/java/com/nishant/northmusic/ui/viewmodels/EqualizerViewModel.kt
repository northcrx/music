package com.nishant.northmusic.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.nishant.northmusic.data.model.AudioOutputDevice
import com.nishant.northmusic.data.model.EqualizerPreset
import com.nishant.northmusic.data.model.EqualizerState
import com.nishant.northmusic.playback.AudioFxManager
import kotlinx.coroutines.flow.StateFlow

class EqualizerViewModel : ViewModel() {

    val equalizerState: StateFlow<EqualizerState> = AudioFxManager.equalizerState

    val numberOfBands: Int get() = AudioFxManager.numberOfBands
    val bandLabels: List<String> get() = AudioFxManager.bandLabels
    val minGainDb: Float get() = AudioFxManager.minGainDb
    val maxGainDb: Float get() = AudioFxManager.maxGainDb
    val isBassBoostSupported: Boolean get() = AudioFxManager.isBassBoostSupported
    val isVirtualizerSupported: Boolean get() = AudioFxManager.isVirtualizerSupported
    val isLoudnessSupported: Boolean get() = AudioFxManager.isLoudnessSupported

    fun toggleMaster(enabled: Boolean) {
        AudioFxManager.toggleMaster(enabled)
    }

    fun selectOutputDevice(device: AudioOutputDevice) {
        AudioFxManager.selectOutputDevice(device)
    }

    fun toggleAutoMode(enabled: Boolean) {
        AudioFxManager.toggleAutoMode(enabled)
    }

    fun selectPreset(preset: EqualizerPreset) {
        AudioFxManager.selectPreset(preset)
    }

    fun setBandGain(bandIndex: Int, gainDb: Float) {
        AudioFxManager.setBandGain(bandIndex, gainDb)
    }

    fun setBassBoost(level: Float) {
        AudioFxManager.setBassBoost(level)
    }

    fun setVirtualizer(level: Float) {
        AudioFxManager.setVirtualizer(level)
    }
}
