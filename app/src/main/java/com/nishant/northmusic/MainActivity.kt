package com.nishant.northmusic

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.nishant.northmusic.data.model.ThemeMode
import com.nishant.northmusic.navigation.MainScaffold
import com.nishant.northmusic.ui.theme.NorthMusicTheme
import com.nishant.northmusic.ui.viewmodels.SettingsViewModel

class MainActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val settingsState by settingsViewModel.settingsState.collectAsState()

            val appIsDark = when (settingsState.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }

            DisposableEffect(appIsDark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        AndroidColor.TRANSPARENT,
                        AndroidColor.TRANSPARENT
                    ) { appIsDark },
                    navigationBarStyle = SystemBarStyle.auto(
                        AndroidColor.TRANSPARENT,
                        AndroidColor.TRANSPARENT
                    ) { appIsDark }
                )
                onDispose {}
            }

            NorthMusicTheme(
                darkTheme = appIsDark,
                dynamicColor = settingsState.dynamicColor,
                accentColor = settingsState.accentColor
            ) {
                MainScaffold(
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }
}
