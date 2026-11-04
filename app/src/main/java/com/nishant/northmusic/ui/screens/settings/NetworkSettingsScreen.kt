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
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Wifi
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nishant.northmusic.data.work.ArtistPhotoWorker
import com.nishant.northmusic.ui.components.SettingsSectionTitle
import com.nishant.northmusic.ui.components.SettingsSwitchRow
import com.nishant.northmusic.ui.components.SettingsValueRow
import com.nishant.northmusic.ui.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkSettingsScreen(
    settingsViewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by settingsViewModel.settingsState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        TopAppBar(
            windowInsets = WindowInsets(0.dp),
            title = {
                Text(
                    text = "Network",
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
            SettingsSectionTitle(title = "Artwork & Photos")

            SettingsSwitchRow(
                title = "Download artist photos and missing album art",
                subtitle = "Download high-res artist photos from Deezer and missing covers from iTunes",
                checked = state.downloadArtistAndAlbumArt,
                onCheckedChange = { checked ->
                    settingsViewModel.setDownloadArtistAndAlbumArt(checked)
                    if (checked) {
                        ArtistPhotoWorker.enqueue(context)
                    } else {
                        ArtistPhotoWorker.cancel(context)
                    }
                },
                icon = Icons.Rounded.Image
            )

            SettingsSwitchRow(
                title = "Only on Wi-Fi",
                subtitle = "Only download artist photos and artwork when connected to Wi-Fi",
                checked = state.downloadOnlyOnWifi,
                onCheckedChange = { checked ->
                    settingsViewModel.setDownloadOnlyOnWifi(checked)
                    if (state.downloadArtistAndAlbumArt) {
                        ArtistPhotoWorker.enqueue(context)
                    }
                },
                icon = Icons.Rounded.Wifi
            )

            SettingsSectionTitle(title = "Online Metadata")

            SettingsSwitchRow(
                title = "Online metadata and artwork lookup",
                subtitle = "Fetch missing album covers and tags from iTunes automatically",
                checked = state.onlineMetadataLookup,
                onCheckedChange = { settingsViewModel.setOnlineMetadataLookup(it) },
                icon = Icons.Rounded.CloudDownload
            )

            SettingsSectionTitle(title = "Lyrics Services")

            SettingsValueRow(
                title = "Lyrics providers",
                subtitle = "Prioritize search sources for synchronized .lrc lyrics",
                value = state.lyricsProviders,
                onClick = { /* TODO: Lyrics provider order dialog */ },
                icon = Icons.Rounded.Lyrics
            )
        }
    }
}
