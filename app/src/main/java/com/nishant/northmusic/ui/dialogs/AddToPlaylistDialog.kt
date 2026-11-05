package com.nishant.northmusic.ui.dialogs

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nishant.northmusic.data.local.LocalStatsDatabase
import com.nishant.northmusic.data.model.Song
import com.nishant.northmusic.ui.components.AppDialog
import kotlinx.coroutines.launch

@Composable
fun AddToPlaylistDialog(
    song: Song,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val statsDb = remember { LocalStatsDatabase.getInstance(context) }
    val playlists by statsDb.userPlaylistsFlow.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    if (showCreateDialog) {
        AppDialog(
            onDismissRequest = { showCreateDialog = false },
            title = "New Playlist",
            subtitle = "Create a custom playlist to collect your favorite tracks",
            icon = Icons.Rounded.Add,
            confirmButtonText = "Create",
            dismissButtonText = "Cancel",
            isConfirmEnabled = newPlaylistName.trim().isNotBlank(),
            onConfirm = {
                val name = newPlaylistName.trim()
                if (name.isNotBlank()) {
                    scope.launch {
                        val created = statsDb.createPlaylist(name)
                        statsDb.addSongToPlaylist(created.id, song.id)
                        Toast.makeText(context, "Added to $name", Toast.LENGTH_SHORT).show()
                        showCreateDialog = false
                        onDismiss()
                    }
                }
            },
            onDismiss = { showCreateDialog = false }
        ) {
            OutlinedTextField(
                value = newPlaylistName,
                onValueChange = { newPlaylistName = it },
                label = { Text("Playlist name") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    AppDialog(
        onDismissRequest = onDismiss,
        title = "Add to playlist",
        subtitle = "Select an existing playlist or create a new one",
        icon = Icons.Rounded.PlaylistAdd,
        dismissButtonText = "Cancel",
        onDismiss = onDismiss,
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // New playlist quick trigger card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCreateDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Create new playlist",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (playlists.isEmpty()) {
                Text(
                    text = "No custom playlists yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                ) {
                    items(playlists, key = { it.id }) { playlist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    scope.launch {
                                        statsDb.addSongToPlaylist(playlist.id, song.id)
                                        Toast.makeText(context, "Added to ${playlist.title}", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    }
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = playlist.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${playlist.songCount} songs",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
