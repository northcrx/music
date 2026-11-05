package com.nishant.northmusic

import android.content.Intent
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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.nishant.northmusic.data.model.ThemeMode
import com.nishant.northmusic.data.network.CdnMusicService
import com.nishant.northmusic.data.repository.SettingsRepository
import com.nishant.northmusic.navigation.MainScaffold
import com.nishant.northmusic.ui.theme.NorthMusicTheme
import com.nishant.northmusic.ui.viewmodels.DownloadsViewModel
import com.nishant.northmusic.ui.viewmodels.SettingsViewModel

class MainActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        SettingsRepository.init(this)
        handleIntent(intent)

        setContent {
            // Read only granular theme properties to avoid whole-app recompositions
            val themeMode by SettingsRepository.themeMode.collectAsState()
            val dynamicColor by SettingsRepository.dynamicColor.collectAsState()
            val accentColor by SettingsRepository.accentColor.collectAsState()

            val appIsDark = when (themeMode) {
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
                dynamicColor = dynamicColor,
                accentColor = accentColor
            ) {
                MainScaffold(
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("text/") == true) {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!text.isNullOrBlank()) {
                val url = CdnMusicService.extractFirstUrl(text) ?: text.trim()
                if (url.isNotBlank()) {
                    DownloadsViewModel.onSharedUrlReceived(url)
                }
            }
        }
    }
}
