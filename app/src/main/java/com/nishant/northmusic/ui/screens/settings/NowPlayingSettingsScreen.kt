package com.nishant.northmusic.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nishant.northmusic.ui.components.SettingsSectionTitle
import com.nishant.northmusic.ui.components.SettingsSwitchRow
import com.nishant.northmusic.ui.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingSettingsScreen(
    settingsViewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by settingsViewModel.settingsState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        TopAppBar(
            windowInsets = WindowInsets(0.dp),
            title = {
                Text(
                    text = "Now playing",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSectionTitle(title = "Controls & Gestures")

            SettingsSwitchRow(
                title = "Animate controls",
                subtitle = "Fluid spring animations for play/pause and track skips",
                checked = state.animateControls,
                onCheckedChange = { settingsViewModel.setAnimateControls(it) },
                icon = Icons.Rounded.Animation
            )

            SettingsSwitchRow(
                title = "Extra controls on mini player",
                subtitle = "Show previous track button beside play/pause on persistent bar",
                checked = state.extraControlsMiniPlayer,
                onCheckedChange = { settingsViewModel.setExtraControlsMiniPlayer(it) },
                icon = Icons.Rounded.MoreHoriz
            )

            SettingsSwitchRow(
                title = "Adaptive controls",
                subtitle = "Dynamically tint seek bar and playback controls with song cover accent",
                checked = state.adaptiveControls,
                onCheckedChange = { settingsViewModel.setAdaptiveControls(it) },
                icon = Icons.Rounded.Palette
            )

            SettingsSectionTitle(title = "Seek bar & Timers")

            SettingsSwitchRow(
                title = "Squiggly seek bar",
                subtitle = "Playful expressive audio waveform seek bar (default off)",
                checked = state.squigglySeekBar,
                onCheckedChange = { settingsViewModel.setSquigglySeekBar(it) },
                icon = Icons.Rounded.GraphicEq
            )

            SettingsSwitchRow(
                title = "Prefer remaining time",
                subtitle = "Display -MM:SS countdown instead of elapsed duration",
                checked = state.preferRemainingTime,
                onCheckedChange = { settingsViewModel.setPreferRemainingTime(it) },
                icon = Icons.Rounded.Timer
            )
        }
    }
}
