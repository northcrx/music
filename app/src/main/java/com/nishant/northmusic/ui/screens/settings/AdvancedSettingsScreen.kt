package com.nishant.northmusic.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nishant.northmusic.ui.components.SettingsSectionTitle
import com.nishant.northmusic.ui.components.SettingsSwitchRow
import com.nishant.northmusic.ui.components.SettingsValueRow
import com.nishant.northmusic.ui.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsScreen(
    settingsViewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onRunSetupAgain: () -> Unit,
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
                    text = "Advanced",
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
            SettingsSectionTitle(title = "Localization")

            SettingsValueRow(
                title = "App language",
                value = state.appLanguage,
                subtitle = "Choose interface localization",
                icon = Icons.Rounded.Language,
                onClick = { /* TODO: Open language picker dialog */ }
            )

            SettingsSectionTitle(title = "Backup & Data")

            SettingsValueRow(
                title = "Backup & Restore",
                value = "Export / Import",
                subtitle = state.backupAndRestoreInfo,
                icon = Icons.Rounded.Backup,
                onClick = { /* TODO: Open backup/restore options */ }
            )

            SettingsSectionTitle(title = "Display & Screen")

            SettingsSwitchRow(
                title = "Rotation lock",
                subtitle = "Keep screen locked in portrait orientation while playback is active",
                checked = state.rotationLock,
                onCheckedChange = { settingsViewModel.setRotationLock(it) },
                icon = Icons.Rounded.ScreenRotation
            )

            SettingsSectionTitle(title = "Setup & Initialization")

            // Run Setup Again Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onRunSetupAgain() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoFixHigh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Run setup again",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Relaunch the initial welcome configuration wizard",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
