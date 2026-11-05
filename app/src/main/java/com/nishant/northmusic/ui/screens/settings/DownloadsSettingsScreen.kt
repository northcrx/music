package com.nishant.northmusic.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wifi
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.nishant.northmusic.data.model.DownloadFolderStructure
import com.nishant.northmusic.navigation.BottomListScrollPadding
import com.nishant.northmusic.ui.components.AppDialog
import com.nishant.northmusic.ui.components.SettingsSectionTitle
import com.nishant.northmusic.ui.components.SettingsSwitchRow
import com.nishant.northmusic.ui.components.SettingsValueRow
import com.nishant.northmusic.ui.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsSettingsScreen(
    settingsViewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCategoryRules: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by settingsViewModel.settingsState.collectAsState()
    var showNamingDialog by remember { mutableStateOf(false) }
    var showFolderStructureDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        TopAppBar(
            windowInsets = WindowInsets(0.dp),
            title = {
                Text(
                    text = "Downloads",
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
            // Organization & Categorization Section
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
                        SettingsSectionTitle("ORGANIZATION & FOLDERS")

                        // Folder Structure Setting (Requirement 2)
                        SettingsValueRow(
                            title = "Folder structure",
                            value = state.folderStructure.displayName,
                            subtitle = when (state.folderStructure) {
                                DownloadFolderStructure.CATEGORY_ARTIST -> "Music/<Category>/<Artist>/<Title>"
                                DownloadFolderStructure.ARTIST_ONLY -> "Music/<Artist>/<Title>"
                                DownloadFolderStructure.FLAT -> "Music/<Title>"
                            },
                            icon = Icons.Rounded.Folder,
                            onClick = { showFolderStructureDialog = true }
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp)
                        )

                        // Category Rules Screen Navigation (Requirement 3)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToCategoryRules() }
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Category,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "Category rules",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "View and customize tag-to-folder mapping rules",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp)
                        )

                        // Ask Category when unknown switch (Requirement 3)
                        SettingsSwitchRow(
                            title = "Ask category when unknown",
                            subtitle = "Prompt to choose a category if genre tags cannot be matched",
                            checked = state.askCategoryWhenUnknown,
                            onCheckedChange = { settingsViewModel.toggleAskCategoryWhenUnknown(it) },
                            icon = Icons.AutoMirrored.Rounded.HelpOutline
                        )
                    }
                }
            }

            // Network & Download Speed
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
                        SettingsSectionTitle("NETWORK & THREADS")

                        // Wi-Fi Only Switch
                        SettingsSwitchRow(
                            title = "Download over Wi-Fi only",
                            subtitle = "Avoid cellular mobile data usage",
                            checked = state.wifiOnlyDownloads,
                            onCheckedChange = { settingsViewModel.toggleWifiOnlyDownloads(it) },
                            icon = Icons.Rounded.Wifi
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp)
                        )

                        // Parallel Downloads Slot (1 to 10)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SwapVert,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "Parallel downloads (1–10)",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${state.maxParallelDownloads} concurrent active download threads",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                (1..10).forEach { slots ->
                                    val selected = state.maxParallelDownloads == slots
                                    Surface(
                                        shape = CircleShape,
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .clickable { settingsViewModel.setMaxParallelDownloads(slots) }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = slots.toString(),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // File Naming Format
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
                        SettingsSectionTitle("FILE NAMING")

                        SettingsValueRow(
                            title = "File naming format",
                            value = state.fileNamingFormat,
                            subtitle = "Default: <Artist> - <Title>.<ext>",
                            icon = Icons.Rounded.FormatQuote,
                            onClick = { showNamingDialog = true }
                        )
                    }
                }
            }
        }
    }

    // Folder Structure Single Choice Dialog (AppDialog)
    if (showFolderStructureDialog) {
        AppDialog(
            onDismissRequest = { showFolderStructureDialog = false },
            title = "Folder structure",
            subtitle = "Choose how new downloaded songs are organized in the Music folder",
            dismissButtonText = "Cancel",
            onDismiss = { showFolderStructureDialog = false }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DownloadFolderStructure.entries.forEach { structure ->
                    val isSelected = state.folderStructure == structure
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                settingsViewModel.setFolderStructure(structure)
                                showFolderStructureDialog = false
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                settingsViewModel.setFolderStructure(structure)
                                showFolderStructureDialog = false
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Column {
                            Text(
                                text = structure.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = when (structure) {
                                    DownloadFolderStructure.CATEGORY_ARTIST -> "Music/<Category>/<Artist>/ (e.g. Music/Hip-Hop/Kendrick Lamar/)"
                                    DownloadFolderStructure.ARTIST_ONLY -> "Music/<Artist>/ (e.g. Music/Kendrick Lamar/)"
                                    DownloadFolderStructure.FLAT -> "Music/ (Directly in main music directory)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // File Naming Format Dialog (AppDialog)
    if (showNamingDialog) {
        val formats = listOf(
            "{artist} - {title}",
            "{title} - {artist}",
            "{track} - {title}",
            "{artist} - {album} - {title}"
        )
        AppDialog(
            onDismissRequest = { showNamingDialog = false },
            title = "File naming format",
            dismissButtonText = "Cancel",
            onDismiss = { showNamingDialog = false }
        ) {
            Column {
                formats.forEach { format ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                settingsViewModel.setFileNamingFormat(format)
                                showNamingDialog = false
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = state.fileNamingFormat == format,
                            onClick = {
                                settingsViewModel.setFileNamingFormat(format)
                                showNamingDialog = false
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = format,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
