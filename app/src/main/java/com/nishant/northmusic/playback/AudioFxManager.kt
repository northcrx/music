package com.nishant.northmusic.playback

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.nishant.northmusic.data.model.AudioOutputDevice
import com.nishant.northmusic.data.model.DeviceProfile
import com.nishant.northmusic.data.model.EqualizerPreset
import com.nishant.northmusic.data.model.DeviceSoundProfile
import com.nishant.northmusic.data.model.DeviceSoundProfiles
import com.nishant.northmusic.data.model.EqualizerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject

object AudioFxManager {

    private const val PREFS_NAME = "northmusic_audiofx_prefs"
    private const val KEY_MASTER_ENABLED = "master_enabled"
    private const val KEY_NORMALIZATION_ENABLED = "loudness_normalization_enabled"
    private const val KEY_PROFILES_JSON = "device_profiles_json"
    private const val KEY_SELECTED_PROFILE_ID = "selected_device_profile_id"

    private var currentSessionId: Int = 0
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    private var audioManager: AudioManager? = null
    private var deviceCallback: AudioDeviceCallback? = null
    private var prefs: SharedPreferences? = null

    private val _equalizerState = MutableStateFlow(EqualizerState())
    val equalizerState: StateFlow<EqualizerState> = _equalizerState.asStateFlow()

    // Hardware capabilities
    var numberOfBands: Int = 5
        private set
    var bandLabels: List<String> = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")
        private set
    var minGainDb: Float = -15f
        private set
    var maxGainDb: Float = 15f
        private set
    var isBassBoostSupported: Boolean = true
        private set
    var isVirtualizerSupported: Boolean = true
        private set
    var isLoudnessSupported: Boolean = true
        private set

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadPersistedState()
            setupDeviceDetection(context)
        }
    }

    private fun setupDeviceDetection(context: Context) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        audioManager = am

        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                updateCurrentDevice(am)
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                updateCurrentDevice(am)
            }
        }
        deviceCallback = callback
        am.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
        updateCurrentDevice(am)
    }

    private fun updateCurrentDevice(am: AudioManager) {
        val devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        var detected = AudioOutputDevice.SPEAKER
        var detectedName: String? = null

        for (device in devices) {
            when (device.type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                AudioDeviceInfo.TYPE_BLE_HEADSET,
                AudioDeviceInfo.TYPE_BLE_SPEAKER -> {
                    detected = AudioOutputDevice.BLUETOOTH
                    val pName = device.productName?.toString()
                    if (!pName.isNullOrBlank()) {
                        detectedName = pName
                    }
                    break
                }
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_USB_HEADSET,
                AudioDeviceInfo.TYPE_USB_DEVICE -> {
                    if (detected != AudioOutputDevice.BLUETOOTH) {
                        detected = AudioOutputDevice.WIRED
                        val pName = device.productName?.toString()
                        if (!pName.isNullOrBlank()) {
                            detectedName = pName
                        }
                    }
                }
                else -> {}
            }
        }

        val previousDevice = _equalizerState.value.currentDevice
        val previousName = _equalizerState.value.connectedDeviceName

        if (previousDevice != detected || previousName != detectedName) {
            val matched = DeviceSoundProfiles.matchProfile(detectedName)
            _equalizerState.update { current ->
                var state = current.copy(
                    currentDevice = detected,
                    connectedDeviceName = detectedName,
                    matchedProfile = matched ?: current.matchedProfile
                )
                if (matched != null && state.activeProfile.isAutoMode) {
                    val activeProfile = state.activeProfile.copy(
                        bandGains = adaptGainsToBandCount(matched.bandGains, numberOfBands),
                        bassBoostPercent = matched.bassBoostPercent,
                        virtualizerPercent = matched.virtualizerPercent
                    )
                    val updatedProfiles = state.profiles.toMutableMap().apply {
                        put(detected, activeProfile)
                    }
                    state = state.copy(profiles = updatedProfiles)
                }
                state
            }
            applyCurrentProfileToHardware()
        }
    }

    fun onAudioSessionIdChanged(context: Context, newSessionId: Int) {
        if (newSessionId == 0 || newSessionId == currentSessionId) return
        init(context)
        currentSessionId = newSessionId

        releaseHardwareEffects()
        initializeHardwareEffects(newSessionId)
        applyCurrentProfileToHardware()
    }

    private fun initializeHardwareEffects(sessionId: Int) {
        try {
            // Priority 0, session ID
            val eq = Equalizer(0, sessionId)
            numberOfBands = eq.numberOfBands.toInt().coerceAtLeast(1)

            val range = eq.bandLevelRange // in millibels (e.g. -1500 to 1500)
            if (range.size >= 2) {
                minGainDb = range[0] / 100f
                maxGainDb = range[1] / 100f
            }

            // Derive band labels from hardware center frequencies
            bandLabels = (0 until numberOfBands).map { bandIndex ->
                val freqMilliHz = eq.getCenterFreq(bandIndex.toShort())
                val hz = freqMilliHz / 1000
                if (hz >= 1000) {
                    val kHz = hz / 1000f
                    if (kHz == kHz.toInt().toFloat()) "${kHz.toInt()}kHz" else "%.1fkHz".format(kHz)
                } else {
                    "${hz}Hz"
                }
            }

            equalizer = eq
        } catch (t: Throwable) {
            equalizer = null
        }

        try {
            val bb = BassBoost(0, sessionId)
            isBassBoostSupported = bb.strengthSupported
            bassBoost = bb
        } catch (t: Throwable) {
            bassBoost = null
            isBassBoostSupported = false
        }

        try {
            val virt = Virtualizer(0, sessionId)
            isVirtualizerSupported = virt.strengthSupported
            virtualizer = virt
        } catch (t: Throwable) {
            virtualizer = null
            isVirtualizerSupported = false
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                loudnessEnhancer = LoudnessEnhancer(sessionId)
                isLoudnessSupported = true
            }
        } catch (t: Throwable) {
            loudnessEnhancer = null
            isLoudnessSupported = false
        }
    }

    private fun releaseHardwareEffects() {
        try {
            equalizer?.enabled = false
            equalizer?.release()
        } catch (_: Throwable) {}
        equalizer = null

        try {
            bassBoost?.enabled = false
            bassBoost?.release()
        } catch (_: Throwable) {}
        bassBoost = null

        try {
            virtualizer?.enabled = false
            virtualizer?.release()
        } catch (_: Throwable) {}
        virtualizer = null

        try {
            loudnessEnhancer?.enabled = false
            loudnessEnhancer?.release()
        } catch (_: Throwable) {}
        loudnessEnhancer = null
    }

    fun release() {
        releaseHardwareEffects()
        deviceCallback?.let { cb ->
            audioManager?.unregisterAudioDeviceCallback(cb)
        }
        deviceCallback = null
        audioManager = null
    }

    // --- State mutation & hardware synchronization ---

    fun toggleMaster(enabled: Boolean) {
        _equalizerState.update { it.copy(isMasterEnabled = enabled) }
        persistState()
        applyCurrentProfileToHardware()
    }

    fun selectOutputDevice(device: AudioOutputDevice) {
        _equalizerState.update { it.copy(currentDevice = device) }
        applyCurrentProfileToHardware()
    }

    fun toggleAutoMode(enabled: Boolean) {
        _equalizerState.update { current ->
            val activeDevice = current.currentDevice
            val activeProfile = current.activeProfile.copy(isAutoMode = enabled)
            val updatedProfiles = current.profiles.toMutableMap().apply {
                put(activeDevice, activeProfile)
            }
            current.copy(profiles = updatedProfiles)
        }
        persistState()
        applyCurrentProfileToHardware()
    }

    fun selectSoundProfile(profile: DeviceSoundProfile) {
        _equalizerState.update { current ->
            val activeDevice = current.currentDevice
            val activeProfile = current.activeProfile.copy(
                isAutoMode = false,
                bandGains = adaptGainsToBandCount(profile.bandGains, numberOfBands),
                bassBoostPercent = profile.bassBoostPercent,
                virtualizerPercent = profile.virtualizerPercent
            )
            val updatedProfiles = current.profiles.toMutableMap().apply {
                put(activeDevice, activeProfile)
            }
            current.copy(
                matchedProfile = profile,
                profiles = updatedProfiles
            )
        }
        persistState()
        applyCurrentProfileToHardware()
    }

    fun setWooferLevel(gainDb: Float) {
        _equalizerState.update { current ->
            val activeDevice = current.currentDevice
            val activeProfile = current.activeProfile.copy(wooferLevelDb = gainDb.coerceIn(-6f, 6f))
            val updatedProfiles = current.profiles.toMutableMap().apply {
                put(activeDevice, activeProfile)
            }
            current.copy(profiles = updatedProfiles)
        }
        persistState()
        applyCurrentProfileToHardware()
    }

    fun toggleSurroundExpanded(enabled: Boolean) {
        _equalizerState.update { current ->
            val activeDevice = current.currentDevice
            val activeProfile = current.activeProfile.copy(isSurroundExpanded = enabled)
            val updatedProfiles = current.profiles.toMutableMap().apply {
                put(activeDevice, activeProfile)
            }
            current.copy(profiles = updatedProfiles)
        }
        persistState()
        applyCurrentProfileToHardware()
    }

    fun toggleVoiceEnhanced(enabled: Boolean) {
        _equalizerState.update { current ->
            val activeDevice = current.currentDevice
            val activeProfile = current.activeProfile.copy(isVoiceEnhanced = enabled)
            val updatedProfiles = current.profiles.toMutableMap().apply {
                put(activeDevice, activeProfile)
            }
            current.copy(profiles = updatedProfiles)
        }
        persistState()
        applyCurrentProfileToHardware()
    }

    fun toggleNightMode(enabled: Boolean) {
        _equalizerState.update { current ->
            val activeDevice = current.currentDevice
            val activeProfile = current.activeProfile.copy(isNightMode = enabled)
            val updatedProfiles = current.profiles.toMutableMap().apply {
                put(activeDevice, activeProfile)
            }
            current.copy(profiles = updatedProfiles)
        }
        persistState()
        applyCurrentProfileToHardware()
    }

    fun toggleBassBoostMode(enabled: Boolean) {
        _equalizerState.update { current ->
            val activeDevice = current.currentDevice
            val activeProfile = current.activeProfile.copy(isBassBoostMode = enabled)
            val updatedProfiles = current.profiles.toMutableMap().apply {
                put(activeDevice, activeProfile)
            }
            current.copy(profiles = updatedProfiles)
        }
        persistState()
        applyCurrentProfileToHardware()
    }

    fun resetToCalibratedProfile() {
        val current = _equalizerState.value
        val profile = current.matchedProfile ?: return
        _equalizerState.update { state ->
            val activeDevice = state.currentDevice
            val activeProfile = state.activeProfile.copy(
                isAutoMode = true,
                bandGains = adaptGainsToBandCount(profile.bandGains, numberOfBands),
                bassBoostPercent = profile.bassBoostPercent,
                virtualizerPercent = profile.virtualizerPercent,
                wooferLevelDb = 0f,
                isSurroundExpanded = false,
                isVoiceEnhanced = false,
                isNightMode = false,
                isBassBoostMode = false
            )
            val updatedProfiles = state.profiles.toMutableMap().apply {
                put(activeDevice, activeProfile)
            }
            state.copy(profiles = updatedProfiles)
        }
        persistState()
        applyCurrentProfileToHardware()
    }

    fun selectPreset(preset: EqualizerPreset?) {
        _equalizerState.update { current ->
            val activeDevice = current.currentDevice
            val gains = if (preset != null) {
                adaptGainsToBandCount(preset.bandGains, numberOfBands)
            } else {
                List(numberOfBands) { 0f }
            }
            val activeProfile = current.activeProfile.copy(
                selectedPreset = preset,
                bandGains = gains
            )
            val updatedProfiles = current.profiles.toMutableMap().apply {
                put(activeDevice, activeProfile)
            }
            current.copy(profiles = updatedProfiles)
        }
        persistState()
        applyCurrentProfileToHardware()
    }

    fun setBandGain(bandIndex: Int, gainDb: Float) {
        _equalizerState.update { current ->
            val activeDevice = current.currentDevice
            val currentBands = current.activeProfile.bandGains.toMutableList()
            while (currentBands.size < numberOfBands) {
                currentBands.add(0f)
            }
            if (bandIndex in currentBands.indices) {
                currentBands[bandIndex] = gainDb.coerceIn(minGainDb, maxGainDb)
            }
            val activeProfile = current.activeProfile.copy(
                bandGains = currentBands,
                selectedPreset = null // manual tweak detaches from preset
            )
            val updatedProfiles = current.profiles.toMutableMap().apply {
                put(activeDevice, activeProfile)
            }
            current.copy(profiles = updatedProfiles)
        }
        persistState()
        applyCurrentProfileToHardware()
    }

    fun setBassBoost(level: Float) {
        _equalizerState.update { current ->
            val activeDevice = current.currentDevice
            val activeProfile = current.activeProfile.copy(bassBoostPercent = level.coerceIn(0f, 1f))
            val updatedProfiles = current.profiles.toMutableMap().apply {
                put(activeDevice, activeProfile)
            }
            current.copy(profiles = updatedProfiles)
        }
        persistState()
        applyCurrentProfileToHardware()
    }

    fun setVirtualizer(level: Float) {
        _equalizerState.update { current ->
            val activeDevice = current.currentDevice
            val activeProfile = current.activeProfile.copy(virtualizerPercent = level.coerceIn(0f, 1f))
            val updatedProfiles = current.profiles.toMutableMap().apply {
                put(activeDevice, activeProfile)
            }
            current.copy(profiles = updatedProfiles)
        }
        persistState()
        applyCurrentProfileToHardware()
    }

    fun updateCurrentGenre(genre: String) {
        val detected = genre.ifBlank { "Pop" }
        val (suggestedTitle, suggestedBands) = computeAutoPresetForGenre(detected, _equalizerState.value.currentDevice)

        _equalizerState.update { current ->
            current.copy(
                detectedGenre = detected,
                suggestedPresetTitle = suggestedTitle
            )
        }

        if (_equalizerState.value.activeProfile.isAutoMode) {
            applyCurrentProfileToHardware()
        }
    }

    private fun computeAutoPresetForGenre(genre: String, device: AudioOutputDevice): Pair<String, List<Float>> {
        val g = genre.lowercase()
        return when {
            g.contains("hip") || g.contains("rap") || g.contains("electronic") || g.contains("edm") || g.contains("dance") || g.contains("house") -> {
                "Bass lift" to listOf(4.5f, 3.0f, 0.5f, 1.0f, 2.5f)
            }
            g.contains("classical") || g.contains("jazz") || g.contains("acoustic") || g.contains("instrumental") -> {
                "Flat with mild treble" to listOf(0.0f, 1.0f, 1.5f, 3.0f, 4.0f)
            }
            g.contains("rock") || g.contains("metal") || g.contains("indie") || g.contains("alternative") -> {
                "Balanced Rock" to listOf(3.5f, 2.0f, -0.5f, 2.0f, 3.5f)
            }
            g.contains("vocal") || g.contains("podcast") || g.contains("talk") -> {
                "Vocal boost" to listOf(-2.0f, 1.0f, 4.0f, 2.5f, 1.0f)
            }
            else -> {
                "Balanced" to listOf(2.0f, 3.0f, 1.5f, 1.0f, 2.5f)
            }
        }
    }

    private fun adaptGainsToBandCount(sourceGains: List<Float>, targetCount: Int): List<Float> {
        if (sourceGains.isEmpty()) return List(targetCount) { 0f }
        if (sourceGains.size == targetCount) return sourceGains

        val result = ArrayList<Float>(targetCount)
        for (i in 0 until targetCount) {
            val srcIndex = (i.toFloat() / (targetCount - 1).coerceAtLeast(1) * (sourceGains.size - 1)).toInt()
            result.add(sourceGains.getOrElse(srcIndex) { 0f })
        }
        return result
    }

    private fun applyCurrentProfileToHardware() {
        val state = _equalizerState.value
        val isMasterOn = state.isMasterEnabled
        val profile = state.activeProfile

        val baseGains: List<Float> = if (profile.isAutoMode) {
            if (state.matchedProfile != null) {
                adaptGainsToBandCount(state.matchedProfile.bandGains, numberOfBands)
            } else {
                val (_, autoGains) = computeAutoPresetForGenre(state.detectedGenre, state.currentDevice)
                adaptGainsToBandCount(autoGains, numberOfBands)
            }
        } else {
            adaptGainsToBandCount(profile.bandGains, numberOfBands)
        }

        // Apply Control Deck acoustic modifiers
        val effectiveGains = baseGains.mapIndexed { index, gain ->
            var modified = gain
            when (index) {
                0 -> { // Sub-bass / Woofer
                    modified += profile.wooferLevelDb
                    if (profile.isBassBoostMode) modified += 2.5f
                    if (profile.isNightMode) modified -= 2.0f
                }
                1 -> { // Mid-bass
                    if (profile.isNightMode) modified -= 1.0f
                }
                2 -> { // Midrange / Vocals
                    if (profile.isVoiceEnhanced) modified += 2.0f
                }
                3 -> { // Treble / Dialogue clarity
                    if (profile.isVoiceEnhanced) modified += 1.5f
                }
                4 -> { // Highs / Air
                    if (profile.isNightMode) modified -= 1.5f
                }
            }
            modified.coerceIn(minGainDb, maxGainDb)
        }

        val baseBassBoost: Float = if (profile.isAutoMode && state.matchedProfile != null) {
            state.matchedProfile.bassBoostPercent
        } else {
            profile.bassBoostPercent
        }
        val effectiveBassBoost = if (profile.isBassBoostMode) {
            (baseBassBoost + 0.25f).coerceIn(0f, 1f)
        } else baseBassBoost

        val baseVirtualizer: Float = if (profile.isAutoMode && state.matchedProfile != null) {
            state.matchedProfile.virtualizerPercent
        } else {
            profile.virtualizerPercent
        }
        val effectiveVirtualizer = if (profile.isSurroundExpanded) {
            (baseVirtualizer + 0.30f).coerceIn(0f, 1f)
        } else baseVirtualizer

        try {
            equalizer?.let { eq ->
                eq.enabled = isMasterOn
                if (isMasterOn) {
                    for (i in 0 until eq.numberOfBands) {
                        val gainDb = effectiveGains.getOrElse(i) { 0f }
                        val milliBels = (gainDb * 100).toInt().coerceIn(minGainDb.toInt() * 100, maxGainDb.toInt() * 100).toShort()
                        eq.setBandLevel(i.toShort(), milliBels)
                    }
                }
            }
        } catch (_: Throwable) {}

        try {
            bassBoost?.let { bb ->
                if (isBassBoostSupported) {
                    bb.enabled = isMasterOn && effectiveBassBoost > 0.05f
                    val strength = (effectiveBassBoost * 1000).toInt().toShort()
                    bb.setStrength(strength)
                }
            }
        } catch (_: Throwable) {}

        try {
            virtualizer?.let { virt ->
                if (isVirtualizerSupported) {
                    virt.enabled = isMasterOn && effectiveVirtualizer > 0.05f
                    val strength = (effectiveVirtualizer * 1000).toInt().toShort()
                    virt.setStrength(strength)
                }
            }
        } catch (_: Throwable) {}

        try {
            loudnessEnhancer?.let { le ->
                le.enabled = isMasterOn
                le.setTargetGain(if (profile.isNightMode) 80 else 150)
            }
        } catch (_: Throwable) {}
    }

    private fun loadPersistedState() {
        val p = prefs ?: return
        val master = p.getBoolean(KEY_MASTER_ENABLED, true)
        val jsonStr = p.getString(KEY_PROFILES_JSON, null)

        val profilesMap = mutableMapOf<AudioOutputDevice, DeviceProfile>()

        if (!jsonStr.isNullOrBlank()) {
            try {
                val root = JSONObject(jsonStr)
                for (device in AudioOutputDevice.entries) {
                    if (root.has(device.name)) {
                        val obj = root.getJSONObject(device.name)
                        val isAuto = obj.optBoolean("isAutoMode", true)
                        val presetName = obj.optString("selectedPreset", "POP")
                        val preset = if (presetName == "null" || presetName.isBlank()) null else {
                            try { EqualizerPreset.valueOf(presetName) } catch (_: Throwable) { null }
                        }
                        val bass = obj.optDouble("bassBoostPercent", 0.5).toFloat()
                        val virt = obj.optDouble("virtualizerPercent", 0.3).toFloat()
                        val woofer = obj.optDouble("wooferLevelDb", 0.0).toFloat()
                        val surround = obj.optBoolean("isSurroundExpanded", false)
                        val voice = obj.optBoolean("isVoiceEnhanced", false)
                        val night = obj.optBoolean("isNightMode", false)
                        val bassMode = obj.optBoolean("isBassBoostMode", false)

                        val gainsArray = obj.optJSONArray("bandGains")
                        val gains = mutableListOf<Float>()
                        if (gainsArray != null) {
                            for (i in 0 until gainsArray.length()) {
                                gains.add(gainsArray.getDouble(i).toFloat())
                            }
                        }
                        profilesMap[device] = DeviceProfile(
                            isEnabled = true,
                            isAutoMode = isAuto,
                            selectedPreset = preset,
                            bandGains = gains.ifEmpty { preset?.bandGains ?: List(numberOfBands) { 0f } },
                            bassBoostPercent = bass,
                            virtualizerPercent = virt,
                            wooferLevelDb = woofer,
                            isSurroundExpanded = surround,
                            isVoiceEnhanced = voice,
                            isNightMode = night,
                            isBassBoostMode = bassMode
                        )
                    }
                }
            } catch (_: Throwable) {}
        }

        val savedProfileId = p.getString(KEY_SELECTED_PROFILE_ID, null)
        val savedProfile = DeviceSoundProfiles.ALL_PROFILES.find { it.id == savedProfileId }

        _equalizerState.update { current ->
            current.copy(
                isMasterEnabled = master,
                matchedProfile = savedProfile ?: current.matchedProfile,
                profiles = if (profilesMap.isNotEmpty()) profilesMap else current.profiles
            )
        }
    }

    private fun persistState() {
        val p = prefs ?: return
        val state = _equalizerState.value

        try {
            val root = JSONObject()
            state.profiles.forEach { (device, profile) ->
                val obj = JSONObject().apply {
                    put("isAutoMode", profile.isAutoMode)
                    put("selectedPreset", profile.selectedPreset?.name ?: "null")
                    put("bassBoostPercent", profile.bassBoostPercent.toDouble())
                    put("virtualizerPercent", profile.virtualizerPercent.toDouble())
                    put("wooferLevelDb", profile.wooferLevelDb.toDouble())
                    put("isSurroundExpanded", profile.isSurroundExpanded)
                    put("isVoiceEnhanced", profile.isVoiceEnhanced)
                    put("isNightMode", profile.isNightMode)
                    put("isBassBoostMode", profile.isBassBoostMode)
                    val arr = JSONArray()
                    profile.bandGains.forEach { arr.put(it.toDouble()) }
                    put("bandGains", arr)
                }
                root.put(device.name, obj)
            }

            val editor = p.edit()
                .putBoolean(KEY_MASTER_ENABLED, state.isMasterEnabled)
                .putString(KEY_PROFILES_JSON, root.toString())
            if (state.matchedProfile != null) {
                editor.putString(KEY_SELECTED_PROFILE_ID, state.matchedProfile.id)
            } else {
                editor.remove(KEY_SELECTED_PROFILE_ID)
            }
            editor.apply()
        } catch (_: Throwable) {}
    }
}
