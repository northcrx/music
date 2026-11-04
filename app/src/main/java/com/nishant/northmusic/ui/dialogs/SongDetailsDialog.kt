package com.nishant.northmusic.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nishant.northmusic.data.model.Song
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SongDetailsDialog(
    song: Song,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val durationFormatted = "%d:%02d".format(song.durationSeconds / 60, song.durationSeconds % 60)
    val sizeMbFormatted = if (song.sizeBytes > 0) {
        "%.2f MB".format(song.sizeBytes / (1024.0 * 1024.0))
    } else {
        val file = File(song.filePath)
        if (file.exists()) "%.2f MB".format(file.length() / (1024.0 * 1024.0)) else "Unknown"
    }
    val ext = File(song.filePath).extension.uppercase().ifBlank { "AUDIO" }
    val dateAddedFormatted = if (song.dateAdded > 0) {
        val millis = if (song.dateAdded < 10000000000L) song.dateAdded * 1000L else song.dateAdded
        SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(millis))
    } else {
        "Unknown"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Track details")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DetailRow("Title", song.title)
                DetailRow("Artist", song.artist)
                DetailRow("Album", song.album)
                DetailRow("Duration", durationFormatted)
                DetailRow("Genre", song.genre.ifBlank { "Pop" })
                if (song.year > 0) DetailRow("Year", song.year.toString())
                if (song.trackNumber > 0) DetailRow("Track", song.trackNumber.toString())

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                DetailRow("Format", ext)
                DetailRow("File size", sizeMbFormatted)
                DetailRow("Date added", dateAddedFormatted)
                DetailRow("File path", song.filePath.ifBlank { song.contentUri.toString() })
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        modifier = modifier
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}
