package com.nishant.northmusic.data.repository

import com.nishant.northmusic.data.model.AccentColor
import com.nishant.northmusic.data.model.SettingsState
import com.nishant.northmusic.data.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Singleton repository caching application settings.
 * Initialized once at app start to eliminate rendering flashes and heavy compose passes.
 * Exposes granular StateFlows for Theme properties to avoid whole-app recompositions.
 */
object SettingsRepository {

    private val _settingsState = MutableStateFlow(SettingsState())
    val settingsState: StateFlow<SettingsState> = _settingsState.asStateFlow()

    // Granular StateFlows for App Theme to prevent full-app recomposition
    private val _themeMode = MutableStateFlow(_settingsState.value.themeMode)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _dynamicColor = MutableStateFlow(_settingsState.value.dynamicColor)
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    private val _accentColor = MutableStateFlow(_settingsState.value.accentColor)
    val accentColor: StateFlow<AccentColor> = _accentColor.asStateFlow()

    val current: SettingsState
        get() = _settingsState.value

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
            updated
        }
    }
}
