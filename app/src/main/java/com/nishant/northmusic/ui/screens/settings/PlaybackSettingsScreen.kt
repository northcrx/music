package com.nishant.northmusic.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nishant.northmusic.navigation.BottomListScrollPadding
import com.nishant.northmusic.ui.components.SettingsSectionTitle
import com.nishant.northmusic.ui.components.SettingsSwitchRow
import com.nishant.northmusic.ui.components.SettingsValueRow
import com.nishant.northmusic.ui.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSettingsScreen(
    settingsViewModel: SettingsViewModel,
    onNavigateToEqualizer: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by settingsViewModel.settingsState.collectAsState()
    var showReplayGainDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        TopAppBar(
            windowInsets = WindowInsets(0.dp),
            title = {
                Text(
                    text = "Playback",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = BottomListScrollPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Equalizer Row
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .clickable { onNavigateToEqualizer() }
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.GraphicEq,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Equalizer",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "5-band EQ, Auto mode, Bass boost & Virtualizer",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                            contentDescription = "Open Equalizer",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Audio Quality & Normalization
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        SettingsSectionTitle(title = "AUDIO PROCESSING")

                        SettingsSwitchRow(
                            title = "Gapless playback",
                            subtitle = "Seamlessly transition between consecutive tracks",
                            checked = state.gaplessPlayback,
                            onCheckedChange = { settingsViewModel.toggleGaplessPlayback(it) },
                            icon = Icons.Rounded.Tune
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp)
                        )

                        SettingsSwitchRow(
                            title = "Audio normalization",
                            subtitle = "Balance volume differences across varied albums and artists",
                            checked = state.audioNormalization,
                            onCheckedChange = { settingsViewModel.toggleAudioNormalization(it) },
                            icon = Icons.Rounded.Hearing
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp)
                        )

                        SettingsValueRow(
                            title = "ReplayGain mode",
                            subtitle = "Apply embedded loudness metadata tag",
                            value = state.replayGainMode,
                            onClick = { showReplayGainDialog = true },
                            icon = Icons.AutoMirrored.Rounded.VolumeUp
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp)
                        )

                        SettingsSwitchRow(
                            title = "High quality audio",
                            subtitle = "Prioritize lossless 24-bit 96kHz stream decoding",
                            checked = state.highQualityAudio,
                            onCheckedChange = { settingsViewModel.toggleHighQualityAudio(it) },
                            icon = Icons.Rounded.HighQuality
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp)
                        )

                        SettingsSwitchRow(
                            title = "Audio focus",
                            subtitle = "Pause playback when another app requests audio focus",
                            checked = state.audioFocus,
                            onCheckedChange = { settingsViewModel.setAudioFocus(it) }
                        )
                    }
                }
            }

            // Per-Device Rules
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        SettingsSectionTitle(title = "HEADPHONES (WIRED)")

                        SettingsSwitchRow(
                            title = "Resume on connect",
                            subtitle = "Automatically start playing when headphones are plugged in",
                            checked = state.headphonesResumeOnConnect,
                            onCheckedChange = { settingsViewModel.setHeadphonesResume(it) },
                            icon = Icons.Rounded.Headphones
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp)
                        )

                        SettingsSwitchRow(
                            title = "Pause on disconnect",
                            subtitle = "Pause playback when unplugging headphones",
                            checked = state.headphonesPauseOnDisconnect,
                            onCheckedChange = { settingsViewModel.setHeadphonesPause(it) }
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        SettingsSectionTitle(title = "BLUETOOTH AUDIO")

                        SettingsSwitchRow(
                            title = "Resume on connect",
                            subtitle = "Automatically play when Bluetooth audio device connects",
                            checked = state.bluetoothResumeOnConnect,
                            onCheckedChange = { settingsViewModel.setBluetoothResume(it) },
                            icon = Icons.Rounded.Bluetooth
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp)
                        )

                        SettingsSwitchRow(
                            title = "Pause on disconnect",
                            subtitle = "Pause playback when Bluetooth disconnects",
                            checked = state.bluetoothPauseOnDisconnect,
                            onCheckedChange = { settingsViewModel.setBluetoothPause(it) }
                        )
                    }
                }
            }
        }
    }

    if (showReplayGainDialog) {
        AlertDialog(
            onDismissRequest = { showReplayGainDialog = false },
            title = {
                Text(
                    text = "ReplayGain Mode",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    listOf("Album", "Track", "None").forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    settingsViewModel.setReplayGainMode(mode)
                                    showReplayGainDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = state.replayGainMode == mode,
                                onClick = {
                                    settingsViewModel.setReplayGainMode(mode)
                                    showReplayGainDialog = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Text(
                                text = mode,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}
