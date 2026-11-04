package com.nishant.northmusic.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nishant.northmusic.data.model.Artist
import com.nishant.northmusic.data.model.Song
import com.nishant.northmusic.data.repository.SettingsRepository
import com.nishant.northmusic.data.repository.splitArtistNames
import com.nishant.northmusic.data.util.TagEditData
import com.nishant.northmusic.data.util.TagWriterHelper
import com.nishant.northmusic.ui.theme.PillShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenameArtistDialog(
    artist: Artist,
    songs: List<Song>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var newArtistName by remember { mutableStateOf(artist.name) }
    var selectedModeIndex by remember { mutableIntStateOf(0) } // 0: App only, 1: Write to file
    var isRenaming by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isRenaming) onDismiss() },
        modifier = modifier.fillMaxWidth(),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rename Artist",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss, enabled = !isRenaming) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Update artist name across all ${artist.songCount} tracks:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedModeIndex == 0,
                        onClick = { selectedModeIndex = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("App only", maxLines = 1)
                    }
                    SegmentedButton(
                        selected = selectedModeIndex == 1,
                        onClick = { selectedModeIndex = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Write to file", maxLines = 1)
                    }
                }

                OutlinedTextField(
                    value = newArtistName,
                    onValueChange = { newArtistName = it },
                    label = { Text("Artist Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = newArtistName.trim()
                    if (finalName.isBlank()) return@Button
                    isRenaming = true
                    scope.launch(Dispatchers.IO) {
                        val settings = SettingsRepository.current
                        val matchingSongs = songs.filter { s ->
                            val split = splitArtistNames(s.artist, settings.splitArtistsOnAmpersand, settings.dontSplitArtistNames)
                            split.any { it.equals(artist.name, ignoreCase = true) }
                        }

                        for (song in matchingSongs) {
                            val editData = TagEditData(
                                title = song.title,
                                artist = finalName,
                                album = song.album,
                                albumArtist = "",
                                composer = "",
                                genre = song.genre,
                                year = if (song.year > 0) song.year.toString() else "",
                                track = if (song.trackNumber > 0) song.trackNumber.toString() else "",
                                disc = "",
                                lyrics = "",
                                comment = "",
                                artworkUri = song.albumArtUri,
                                artworkPath = null
                            )

                            if (selectedModeIndex == 1) {
                                TagWriterHelper.writeToFile(context, song, editData)
                            } else {
                                TagWriterHelper.saveAppOnly(context, song.id, editData)
                            }
                        }
                        onDismiss()
                    }
                },
                enabled = newArtistName.isNotBlank() && !isRenaming,
                shape = PillShape
            ) {
                if (isRenaming) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isRenaming) {
                Text("Cancel")
            }
        }
    )
}
