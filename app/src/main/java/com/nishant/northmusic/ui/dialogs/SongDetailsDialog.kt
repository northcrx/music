package com.nishant.northmusic.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nishant.northmusic.data.model.Song
import com.nishant.northmusic.ui.components.AppDialog
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
    val context = LocalContext.current
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
    val pathText = song.filePath.ifBlank { song.contentUri.toString() }

    AppDialog(
        onDismissRequest = onDismiss,
        title = "Track details",
        icon = Icons.Rounded.Info,
        confirmButtonText = "Close",
        onConfirm = onDismiss,
        modifier = modifier
    ) {
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
            DetailRow("Genre", song.genre.ifBlank { "Unknown" })
            if (song.year > 0) DetailRow("Year", song.year.toString())
            if (song.trackNumber > 0) DetailRow("Track", song.trackNumber.toString())

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            DetailRow("Format", ext)
            DetailRow("File size", sizeMbFormatted)
            DetailRow("Date added", dateAddedFormatted)

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "File path",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("File Path", pathText))
                            Toast.makeText(context, "Path copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Copy path",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                SelectionContainer {
                    Text(
                        text = pathText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
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
