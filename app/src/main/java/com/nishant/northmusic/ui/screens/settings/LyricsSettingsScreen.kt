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
import androidx.compose.material.icons.rounded.FormatAlignCenter
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.GTranslate
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.VerticalAlignCenter
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
import com.nishant.northmusic.ui.components.SettingsSliderRow
import com.nishant.northmusic.ui.components.SettingsSwitchRow
import com.nishant.northmusic.ui.components.SettingsValueRow
import com.nishant.northmusic.ui.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsSettingsScreen(
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
                    text = "Lyrics",
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
            SettingsSectionTitle(title = "Language & Multi-line")

            SettingsSwitchRow(
                title = "Show translation",
                subtitle = "Display secondary translated line below original lyric",
                checked = state.showTranslation,
                onCheckedChange = { settingsViewModel.setShowTranslation(it) },
                icon = Icons.Rounded.GTranslate
            )

            SettingsSwitchRow(
                title = "Show transliteration",
                subtitle = "Display romanized reading for foreign language lyrics",
                checked = state.showTransliteration,
                onCheckedChange = { settingsViewModel.setShowTransliteration(it) },
                icon = Icons.Rounded.Language
            )

            SettingsSectionTitle(title = "Alignment & Layout")

            SettingsSwitchRow(
                title = "Center current line",
                subtitle = "Keep active synchronized line anchored in the vertical center",
                checked = state.centerCurrentLine,
                onCheckedChange = { settingsViewModel.setCenterCurrentLine(it) },
                icon = Icons.Rounded.VerticalAlignCenter
            )

            SettingsSwitchRow(
                title = "Center lyrics horizontally",
                subtitle = "Center text alignment instead of start-aligned flow",
                checked = state.centerLyricsHorizontally,
                onCheckedChange = { settingsViewModel.setCenterLyricsHorizontally(it) },
                icon = Icons.Rounded.FormatAlignCenter
            )

            SettingsSectionTitle(title = "Typography")

            SettingsSliderRow(
                title = "Font size",
                subtitle = "Adjust lyric line text scale",
                value = state.lyricsFontSize,
                valueRange = 14f..28f,
                steps = 7,
                valueDisplay = { "%.0f sp".format(it) },
                onValueChange = { settingsViewModel.setLyricsFontSize(it) }
            )

            SettingsValueRow(
                title = "Custom font",
                subtitle = "Select TTF / OTF font file from device",
                value = state.customFont,
                onClick = { /* TODO: Font picker */ },
                icon = Icons.Rounded.TextFields
            )
        }
    }
}
