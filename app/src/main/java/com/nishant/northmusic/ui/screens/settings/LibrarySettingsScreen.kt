package com.nishant.northmusic.ui.screens.settings

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nishant.northmusic.navigation.BottomListScrollPadding
import com.nishant.northmusic.ui.components.SettingsSectionTitle
import com.nishant.northmusic.ui.components.SettingsSliderRow
import com.nishant.northmusic.ui.components.SettingsSwitchRow
import com.nishant.northmusic.ui.components.SettingsValueRow
import com.nishant.northmusic.ui.theme.PillShape
import com.nishant.northmusic.ui.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrarySettingsScreen(
    settingsViewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by settingsViewModel.settingsState.collectAsState()
    var showMetadataDialog by remember { mutableStateOf(false) }
    var showCategoriesDialog by remember { mutableStateOf(false) }
    var showBlacklistDialog by remember { mutableStateOf(false) }
    var showHistoryIntervalDialog by remember { mutableStateOf(false) }
    var showLastAddedIntervalDialog by remember { mutableStateOf(false) }
    var showArtistMinCountDialog by remember { mutableStateOf(false) }
    var showAlbumMinCountDialog by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "rescan")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        TopAppBar(
            windowInsets = WindowInsets(0.dp),
            title = {
                Text(
                    text = "Library",
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
            // Section 1: Categories
            item {
                SettingsSectionTitle("CATEGORIES")
            }
            item {
                SettingsValueRow(
                    title = "Library categories",
                    value = "${state.libraryCategories.size} enabled",
                    subtitle = state.libraryCategories.joinToString(", "),
                    icon = Icons.Rounded.Category,
                    onClick = { showCategoriesDialog = true }
                )
            }

            // Section 2: Song Filtering
            item {
                SettingsSectionTitle("FILTERING")
            }
            item {
                SettingsSliderRow(
                    title = "Minimum song duration",
                    value = state.minSongDurationSeconds.toFloat(),
                    valueRange = 0f..60f,
                    valueDisplay = { "${it.toInt()}s" },
                    onValueChange = { settingsViewModel.setMinSongDurationSeconds(it.toInt()) }
                )
            }
            item {
                SettingsValueRow(
                    title = "Artist minimum song count",
                    value = "${state.artistMinSongCount} song(s)",
                    subtitle = "Minimum tracks required to list an artist",
                    icon = Icons.Rounded.Person,
                    onClick = { showArtistMinCountDialog = true }
                )
            }
            item {
                SettingsValueRow(
                    title = "Album minimum song count",
                    value = "${state.albumMinSongCount} song(s)",
                    subtitle = "Minimum tracks required to list an album",
                    icon = Icons.Rounded.Album,
                    onClick = { showAlbumMinCountDialog = true }
                )
            }
            item {
                SettingsSwitchRow(
                    title = "Ignore short tracks",
                    subtitle = "Filter out voice notes and notifications under 30 seconds",
                    checked = state.ignoreShortTracks,
                    onCheckedChange = { settingsViewModel.toggleIgnoreShortTracks(it) },
                    icon = Icons.Rounded.FolderSpecial
                )
            }

            // Section 3: Folders & Exclusions
            item {
                SettingsSectionTitle("FOLDERS & EXCLUSIONS")
            }
            item {
                SettingsValueRow(
                    title = "Folders blacklist",
                    value = if (state.foldersBlacklist.isEmpty()) "0" else "${state.foldersBlacklist.size}",
                    subtitle = if (state.foldersBlacklist.isEmpty()) "None excluded" else "${state.foldersBlacklist.size} folder(s) excluded",
                    icon = Icons.Rounded.Block,
                    onClick = { showBlacklistDialog = true }
                )
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = "Scan folders",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Directories included in local media library indexing",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        state.musicFolders.forEach { folder ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = folder,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Section 4: Sorting & Storage
            item {
                SettingsSectionTitle("SORTING & STORAGE")
            }
            item {
                SettingsSwitchRow(
                    title = "Ignore articles when sorting",
                    subtitle = "Ignore 'The', 'A', and 'An' at the start of titles & artists",
                    checked = state.ignoreArticlesWhenSorting,
                    onCheckedChange = { settingsViewModel.setIgnoreArticlesWhenSorting(it) },
                    icon = Icons.Rounded.SortByAlpha
                )
            }
            item {
                SettingsSwitchRow(
                    title = "Send deleted files to trash",
                    subtitle = "Move deleted songs to system trash instead of deleting permanently",
                    checked = state.sendDeletedToTrash,
                    onCheckedChange = { settingsViewModel.setSendDeletedToTrash(it) },
                    icon = Icons.Rounded.Delete
                )
            }

            // Section 5: History Tracking
            item {
                SettingsSectionTitle("HISTORY & INTERVALS")
            }
            item {
                SettingsSwitchRow(
                    title = "Record listening history",
                    subtitle = "Track song playback to populate Top tracks and History tabs",
                    checked = state.recordListeningHistory,
                    onCheckedChange = { settingsViewModel.setRecordListeningHistory(it) },
                    icon = Icons.Rounded.History
                )
            }
            item {
                SettingsValueRow(
                    title = "History interval",
                    value = "${state.historyIntervalDays} days",
                    subtitle = "Duration to retain playback history",
                    icon = Icons.Rounded.Timer,
                    onClick = { showHistoryIntervalDialog = true }
                )
            }
            item {
                SettingsValueRow(
                    title = "Last added interval",
                    value = "${state.lastAddedIntervalDays} days",
                    subtitle = "Duration tracks remain in Last added",
                    icon = Icons.Rounded.HourglassBottom,
                    onClick = { showLastAddedIntervalDialog = true }
                )
            }

            // Section 6: Diagnostics & Repair
            item {
                SettingsSectionTitle("DIAGNOSTICS & ACTIONS")
            }
            // Fix Metadata and Artwork Row
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
                            .clickable {
                                showMetadataDialog = true
                                settingsViewModel.startFixingMetadata()
                            }
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoFixHigh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Fix metadata and artwork",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Match missing covers and release tags via iTunes Search API",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Rescan Local Library Button
            item {
                Button(
                    onClick = { settingsViewModel.triggerRescanLibrary() },
                    enabled = !state.isRescanning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = PillShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Sync,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(22.dp)
                            .then(if (state.isRescanning) Modifier.rotate(rotationAngle) else Modifier)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (state.isRescanning) "Rescanning local library..." else "Rescan local library now",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }

    // Categories Dialog
    if (showCategoriesDialog) {
        val allCategories = listOf("Songs", "Albums", "Artists", "Playlists", "Folders")
        var currentSelection by remember { mutableStateOf(state.libraryCategories.toSet()) }

        AlertDialog(
            onDismissRequest = { showCategoriesDialog = false },
            title = { Text("Library Categories") },
            text = {
                Column {
                    allCategories.forEach { category ->
                        val isChecked = currentSelection.contains(category)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentSelection = if (isChecked) {
                                        if (currentSelection.size > 1) currentSelection - category else currentSelection
                                    } else {
                                        currentSelection + category
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    currentSelection = if (checked) {
                                        currentSelection + category
                                    } else {
                                        if (currentSelection.size > 1) currentSelection - category else currentSelection
                                    }
                                },
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                            )
                            Text(text = category, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ordered = allCategories.filter { currentSelection.contains(it) }
                        settingsViewModel.setLibraryCategories(ordered)
                        showCategoriesDialog = false
                    },
                    shape = PillShape
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCategoriesDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Blacklist Dialog
    if (showBlacklistDialog) {
        AlertDialog(
            onDismissRequest = { showBlacklistDialog = false },
            title = { Text("Folders Blacklist") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Exclude specific folders such as Ringtones, Audiobooks, or Recordings from being indexed in the library.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (state.foldersBlacklist.isEmpty()) {
                        Text(
                            text = "No blacklisted folders configured.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        state.foldersBlacklist.forEach { folder ->
                            Text(text = "• $folder", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showBlacklistDialog = false },
                    shape = PillShape
                ) {
                    Text("OK")
                }
            }
        )
    }

    // Artist Min Count Dialog
    if (showArtistMinCountDialog) {
        AlertDialog(
            onDismissRequest = { showArtistMinCountDialog = false },
            title = { Text("Artist Minimum Song Count") },
            text = {
                Column {
                    listOf(1, 2, 3, 5).forEach { count ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    settingsViewModel.setArtistMinSongCount(count)
                                    showArtistMinCountDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = state.artistMinSongCount == count,
                                onClick = {
                                    settingsViewModel.setArtistMinSongCount(count)
                                    showArtistMinCountDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Text(text = "$count song${if (count > 1) "s" else ""}")
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Album Min Count Dialog
    if (showAlbumMinCountDialog) {
        AlertDialog(
            onDismissRequest = { showAlbumMinCountDialog = false },
            title = { Text("Album Minimum Song Count") },
            text = {
                Column {
                    listOf(1, 2, 3, 5).forEach { count ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    settingsViewModel.setAlbumMinSongCount(count)
                                    showAlbumMinCountDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = state.albumMinSongCount == count,
                                onClick = {
                                    settingsViewModel.setAlbumMinSongCount(count)
                                    showAlbumMinCountDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Text(text = "$count song${if (count > 1) "s" else ""}")
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // History Interval Dialog
    if (showHistoryIntervalDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryIntervalDialog = false },
            title = { Text("History Retention Interval") },
            text = {
                Column {
                    listOf(7, 14, 30, 90, 365).forEach { days ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    settingsViewModel.setHistoryIntervalDays(days)
                                    showHistoryIntervalDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = state.historyIntervalDays == days,
                                onClick = {
                                    settingsViewModel.setHistoryIntervalDays(days)
                                    showHistoryIntervalDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Text(text = "$days days")
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Last Added Interval Dialog
    if (showLastAddedIntervalDialog) {
        AlertDialog(
            onDismissRequest = { showLastAddedIntervalDialog = false },
            title = { Text("Last Added Interval") },
            text = {
                Column {
                    listOf(7, 14, 30, 60).forEach { days ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    settingsViewModel.setLastAddedIntervalDays(days)
                                    showLastAddedIntervalDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = state.lastAddedIntervalDays == days,
                                onClick = {
                                    settingsViewModel.setLastAddedIntervalDays(days)
                                    showLastAddedIntervalDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Text(text = "$days days")
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Metadata & Artwork Progress Dialog
    if (showMetadataDialog) {
        val fixState = state.metadataFixState

        AlertDialog(
            onDismissRequest = {
                if (!fixState.isScanning) {
                    showMetadataDialog = false
                    settingsViewModel.dismissMetadataFix()
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.AutoFixHigh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Fix Metadata & Artwork",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = fixState.statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LinearProgressIndicator(
                        progress = { fixState.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )

                    if (fixState.items.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Matched items:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        fixState.items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${item.songTitle} - ${item.artist}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = item.fixDescription,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showMetadataDialog = false
                        settingsViewModel.dismissMetadataFix()
                    },
                    shape = PillShape
                ) {
                    Text(if (fixState.isScanning) "Cancel" else "Done")
                }
            }
        )
    }
}
