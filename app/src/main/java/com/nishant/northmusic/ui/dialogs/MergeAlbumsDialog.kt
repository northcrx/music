package com.nishant.northmusic.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nishant.northmusic.data.model.Album
import com.nishant.northmusic.data.model.Song
import com.nishant.northmusic.data.repository.normalizeText
import com.nishant.northmusic.data.util.TagEditData
import com.nishant.northmusic.data.util.TagWriterHelper
import com.nishant.northmusic.ui.components.AsyncSongArtwork
import com.nishant.northmusic.ui.theme.PillShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergeAlbumsDialog(
    sourceAlbum: Album,
    availableAlbums: List<Album>,
    songs: List<Song>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTargetAlbum by remember { mutableStateOf<Album?>(null) }
    var selectedModeIndex by remember { mutableIntStateOf(0) } // 0: App only, 1: Write to file
    var isMerging by remember { mutableStateOf(false) }

    val otherAlbums = availableAlbums.filter { it.id != sourceAlbum.id }
    val filteredAlbums = if (searchQuery.isBlank()) {
        otherAlbums
    } else {
        val query = normalizeText(searchQuery)
        otherAlbums.filter {
            normalizeText(it.title).contains(query) || normalizeText(it.artist).contains(query)
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isMerging) onDismiss() },
        modifier = modifier.fillMaxWidth(),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Merge Albums",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss, enabled = !isMerging) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Merge tracks from \"${sourceAlbum.title}\" into another album:",
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
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search target album...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredAlbums, key = { it.id }) { targetAlbum ->
                        val isSelected = selectedTargetAlbum?.id == targetAlbum.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTargetAlbum = targetAlbum },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AsyncSongArtwork(
                                    albumArtUri = targetAlbum.albumArtUri,
                                    colorSeed = targetAlbum.colorSeed,
                                    size = 40.dp,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = targetAlbum.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${targetAlbum.artist} • ${targetAlbum.songCount} tracks",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedTargetAlbum = targetAlbum },
                                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
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
                    val target = selectedTargetAlbum ?: return@Button
                    isMerging = true
                    scope.launch(Dispatchers.IO) {
                        val sourceSongs = songs.filter {
                            normalizeText(it.album) == normalizeText(sourceAlbum.title)
                        }
                        for (song in sourceSongs) {
                            val editData = TagEditData(
                                title = song.title,
                                artist = song.artist,
                                album = target.title,
                                albumArtist = target.artist,
                                composer = "",
                                genre = song.genre,
                                year = if (target.year > 0) target.year.toString() else (if (song.year > 0) song.year.toString() else ""),
                                track = if (song.trackNumber > 0) song.trackNumber.toString() else "",
                                disc = "",
                                lyrics = "",
                                comment = "",
                                artworkUri = target.albumArtUri,
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
                enabled = selectedTargetAlbum != null && !isMerging,
                shape = PillShape
            ) {
                if (isMerging) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text("Merge")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isMerging) {
                Text("Cancel")
            }
        }
    )
}
