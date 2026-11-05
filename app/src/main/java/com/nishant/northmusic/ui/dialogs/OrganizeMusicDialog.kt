package com.nishant.northmusic.ui.dialogs

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nishant.northmusic.data.model.Song
import com.nishant.northmusic.data.repository.MusicRepository
import com.nishant.northmusic.data.repository.SettingsRepository
import com.nishant.northmusic.data.util.DownloadPathHelper
import com.nishant.northmusic.ui.components.AppDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class OrganizeCandidate(
    val song: Song,
    val oldFile: File,
    val targetDir: File,
    val targetRelativePath: String,
    val targetDisplayName: String
)

enum class OrganizeStep {
    SCANNING,
    PREVIEW,
    IN_PROGRESS,
    DONE
}

@Composable
fun OrganizeMusicDialog(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val musicRepo = remember { MusicRepository.getInstance(context) }

    var step by remember { mutableStateOf(OrganizeStep.SCANNING) }
    var candidates by remember { mutableStateOf<List<OrganizeCandidate>>(emptyList()) }
    var totalProcessed by remember { mutableIntStateOf(0) }
    var isCancelled by remember { mutableStateOf(false) }

    // MediaStore Write Request Launcher for Android 11+
    val writePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Permission granted, execute batch move
            step = OrganizeStep.IN_PROGRESS
            scope.launch {
                executeOrganize(
                    context = context,
                    candidates = candidates,
                    onProgress = { totalProcessed = it },
                    isCancelled = { isCancelled },
                    onComplete = {
                        step = OrganizeStep.DONE
                        musicRepo.rescan()
                    }
                )
            }
        }
    }

    // Step 1: Scan library and identify songs needing folder/name organization
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val allSongs = musicRepo.songs.value
            val folderStructure = SettingsRepository.current.folderStructure
            val pubMusic = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            val list = mutableListOf<OrganizeCandidate>()

            for (song in allSongs) {
                if (song.filePath.isBlank()) continue
                val oldFile = File(song.filePath)
                if (!oldFile.exists() || !oldFile.isFile) continue

                val firstArtist = DownloadPathHelper.getFirstArtist(song.artist)
                val category = DownloadPathHelper.resolveCategory(
                    context = context,
                    firstArtist = firstArtist,
                    sourceTags = listOf(song.genre).filter { it.isNotBlank() }
                )
                val artistFolder = DownloadPathHelper.resolveArtistFolder(
                    context = context,
                    category = category,
                    firstArtistClean = firstArtist,
                    structure = folderStructure
                )
                val targetRelativePath = DownloadPathHelper.buildRelativePath(
                    category = category,
                    artistFolder = artistFolder,
                    structure = folderStructure
                )

                val ext = oldFile.extension.ifBlank { "mp3" }
                val targetDisplayName = "${DownloadPathHelper.cleanName(song.artist)} - ${DownloadPathHelper.cleanName(song.title)}.$ext"

                val targetSub = targetRelativePath.removePrefix("Music/").removePrefix("Music").trim('/', '\\')
                val targetDir = if (targetSub.isBlank()) pubMusic else File(pubMusic, targetSub)

                val isSameDir = try {
                    oldFile.parentFile?.canonicalPath == targetDir.canonicalPath
                } catch (_: Exception) {
                    false
                }
                val isSameName = oldFile.name.equals(targetDisplayName, ignoreCase = true)

                // Skip files that already match! (Requirement 8)
                if (!isSameDir || !isSameName) {
                    list.add(
                        OrganizeCandidate(
                            song = song,
                            oldFile = oldFile,
                            targetDir = targetDir,
                            targetRelativePath = targetRelativePath,
                            targetDisplayName = targetDisplayName
                        )
                    )
                }
            }

            candidates = list
            step = OrganizeStep.PREVIEW
        }
    }

    when (step) {
        OrganizeStep.SCANNING -> {
            AppDialog(
                onDismissRequest = onDismissRequest,
                title = "Scanning library...",
                icon = Icons.AutoMirrored.Rounded.DriveFileMove,
                dismissButtonText = "Cancel",
                onDismiss = onDismissRequest
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                    Text("Checking existing songs against folder structure...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        OrganizeStep.PREVIEW -> {
            val structure = SettingsRepository.current.folderStructure
            val count = candidates.size

            AppDialog(
                onDismissRequest = onDismissRequest,
                title = "Organize existing music",
                subtitle = if (count > 0) "Move $count songs into ${structure.displayName} folders" else "All songs match folder structure",
                icon = Icons.AutoMirrored.Rounded.DriveFileMove,
                confirmButtonText = if (count > 0) "Apply ($count)" else "OK",
                onConfirm = {
                    if (count == 0) {
                        onDismissRequest()
                        return@AppDialog
                    }

                    // Check if MediaStore batch write request is required on Android 11+
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        try {
                            val uris = candidates.map { it.song.contentUri }
                            val pendingIntent = MediaStore.createWriteRequest(context.contentResolver, uris)
                            writePermissionLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                        } catch (_: Exception) {
                            // Fallback to direct execution
                            step = OrganizeStep.IN_PROGRESS
                            scope.launch {
                                executeOrganize(
                                    context = context,
                                    candidates = candidates,
                                    onProgress = { totalProcessed = it },
                                    isCancelled = { isCancelled },
                                    onComplete = {
                                        step = OrganizeStep.DONE
                                        musicRepo.rescan()
                                    }
                                )
                            }
                        }
                    } else {
                        step = OrganizeStep.IN_PROGRESS
                        scope.launch {
                            executeOrganize(
                                context = context,
                                candidates = candidates,
                                onProgress = { totalProcessed = it },
                                isCancelled = { isCancelled },
                                onComplete = {
                                    step = OrganizeStep.DONE
                                    musicRepo.rescan()
                                }
                            )
                        }
                    }
                },
                dismissButtonText = if (count > 0) "Cancel" else null,
                onDismiss = onDismissRequest
            ) {
                if (count == 0) {
                    Text(
                        text = "All songs in your library are already organized according to ${structure.displayName}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    Text(
                        text = "The following files will be relocated into organized folders:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(candidates.take(50)) { item ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "${item.song.title} — ${item.song.artist}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = item.oldFile.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = item.targetRelativePath,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (count > 50) {
                        Text(
                            text = "... and ${count - 50} more tracks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        OrganizeStep.IN_PROGRESS -> {
            AppDialog(
                onDismissRequest = {},
                title = "Organizing music...",
                subtitle = "Moved $totalProcessed of ${candidates.size} songs",
                icon = Icons.AutoMirrored.Rounded.DriveFileMove,
                dismissButtonText = "Cancel",
                onDismiss = { isCancelled = true }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val progress = if (candidates.isNotEmpty()) totalProcessed.toFloat() / candidates.size else 0f
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Text(
                        text = if (isCancelled) "Stopping safely..." else "Relocating and updating media scanner...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        OrganizeStep.DONE -> {
            AppDialog(
                onDismissRequest = onDismissRequest,
                title = "Organization complete",
                subtitle = if (isCancelled) "Cancelled. Safely moved $totalProcessed songs." else "Successfully organized $totalProcessed songs.",
                icon = Icons.Rounded.CheckCircle,
                confirmButtonText = "Done",
                onConfirm = onDismissRequest
            ) {
                Text(
                    text = "Library rescanned. All relocated tracks are now accessible under your organized folders.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

private suspend fun executeOrganize(
    context: Context,
    candidates: List<OrganizeCandidate>,
    onProgress: (Int) -> Unit,
    isCancelled: () -> Boolean,
    onComplete: () -> Unit
) = withContext(Dispatchers.IO) {
    var count = 0
    val scannedPaths = mutableListOf<String>()

    for (item in candidates) {
        if (isCancelled()) break

        try {
            item.targetDir.mkdirs()
            val destFile = File(item.targetDir, item.targetDisplayName)

            // Direct move
            var moved = false
            if (item.oldFile.exists() && item.oldFile != destFile) {
                moved = item.oldFile.renameTo(destFile)
            }

            // Update MediaStore relative path on Android 10+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val values = ContentValues().apply {
                        put(MediaStore.Audio.Media.RELATIVE_PATH, item.targetRelativePath)
                        put(MediaStore.Audio.Media.DISPLAY_NAME, item.targetDisplayName)
                    }
                    context.contentResolver.update(item.song.contentUri, values, null, null)
                } catch (_: Exception) {}
            }

            if (moved) {
                scannedPaths.add(destFile.absolutePath)
                scannedPaths.add(item.oldFile.absolutePath)
            } else if (destFile.exists()) {
                scannedPaths.add(destFile.absolutePath)
            }

            count++
            withContext(Dispatchers.Main) {
                onProgress(count)
            }
        } catch (_: Exception) {}
    }

    if (scannedPaths.isNotEmpty()) {
        MediaScannerConnection.scanFile(
            context,
            scannedPaths.toTypedArray(),
            null,
            null
        )
    }

    withContext(Dispatchers.Main) {
        onComplete()
    }
}
