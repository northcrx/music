package com.nishant.northmusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsSuggest
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nishant.northmusic.navigation.BottomListScrollPadding

/**
 * Settings Hub:
 * Unified settings layout with neutral white icons (unaffected by accent color changes),
 * grouped into cohesive sections on one page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToAppearance: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    onNavigateToLyrics: () -> Unit,
    onNavigateToPlayback: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToNetwork: () -> Unit,
    onNavigateToAdvanced: () -> Unit,
    onNavigateToAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        TopAppBar(
            windowInsets = WindowInsets(0.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Group 1: Preferences & Playback
            item {
                Text(
                    text = "PLAYBACK & INTERFACE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsGroupRow(
                            title = "Appearance",
                            summary = "Accent colors, dynamic wallpaper palette & theme mode",
                            icon = Icons.Rounded.Palette,
                            onClick = onNavigateToAppearance
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            modifier = Modifier.padding(start = 68.dp)
                        )
                        SettingsGroupRow(
                            title = "Now playing",
                            summary = "Seek bar style, player controls animation & mini player",
                            icon = Icons.Rounded.GraphicEq,
                            onClick = onNavigateToNowPlaying
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            modifier = Modifier.padding(start = 68.dp)
                        )
                        SettingsGroupRow(
                            title = "Lyrics",
                            summary = "Translation, transliteration, font size & alignment",
                            icon = Icons.Rounded.Lyrics,
                            onClick = onNavigateToLyrics
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            modifier = Modifier.padding(start = 68.dp)
                        )
                        SettingsGroupRow(
                            title = "Playback",
                            summary = "Equalizer, gapless transition, normalization & devices",
                            icon = Icons.Rounded.Tune,
                            onClick = onNavigateToPlayback
                        )
                    }
                }
            }

            // Group 2: Library & Media
            item {
                Text(
                    text = "MEDIA & STORAGE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsGroupRow(
                            title = "Library",
                            summary = "Categories, duration filter, scan folders & metadata repair",
                            icon = Icons.Rounded.FolderSpecial,
                            onClick = onNavigateToLibrary
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            modifier = Modifier.padding(start = 68.dp)
                        )
                        SettingsGroupRow(
                            title = "Downloads",
                            summary = "Save folder, Wi-Fi only, parallel slots & naming pattern",
                            icon = Icons.Rounded.DownloadDone,
                            onClick = onNavigateToDownloads
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            modifier = Modifier.padding(start = 68.dp)
                        )
                        SettingsGroupRow(
                            title = "Network",
                            summary = "Online metadata lookup & lyrics search providers",
                            icon = Icons.Rounded.CloudDownload,
                            onClick = onNavigateToNetwork
                        )
                    }
                }
            }

            // Group 3: System & Info
            item {
                Text(
                    text = "SYSTEM & ABOUT",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsGroupRow(
                            title = "Advanced",
                            summary = "Language, backup & restore, rotation lock & setup wizard",
                            icon = Icons.Rounded.SettingsSuggest,
                            onClick = onNavigateToAdvanced
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            modifier = Modifier.padding(start = 68.dp)
                        )
                        SettingsGroupRow(
                            title = "About",
                            summary = "Version 1.0.0, open source licenses & legal info",
                            icon = Icons.Rounded.Info,
                            onClick = onNavigateToAbout
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsGroupRow(
    title: String,
    summary: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
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
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
            contentDescription = "Open $title",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(14.dp)
        )
    }
}
