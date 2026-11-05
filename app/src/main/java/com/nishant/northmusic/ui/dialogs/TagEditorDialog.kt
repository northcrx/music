package com.nishant.northmusic.ui.dialogs

import android.app.Activity
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.nishant.northmusic.data.local.LocalStatsDatabase
import com.nishant.northmusic.data.repository.normalizeText
import com.nishant.northmusic.data.util.DownloadPathHelper
import com.nishant.northmusic.data.model.Song
import com.nishant.northmusic.data.network.AlbumArtworkDownloader
import com.nishant.northmusic.data.util.TagEditData
import com.nishant.northmusic.data.util.TagWriterHelper
import com.nishant.northmusic.ui.components.AsyncSongArtwork
import com.nishant.northmusic.ui.theme.PillShape
import com.nishant.northmusic.ui.viewmodels.PlayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagEditorDialog(
    song: Song,
    onDismiss: () -> Unit,
    playerViewModel: PlayerViewModel? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedModeIndex by remember { mutableIntStateOf(0) } // 0: App only, 1: Write to file
    var title by remember { mutableStateOf(song.title) }
    var artist by remember { mutableStateOf(song.artist) }
    var album by remember { mutableStateOf(song.album) }
    var albumArtist by remember { mutableStateOf("") }
    var composer by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf(song.genre) }
    var year by remember { mutableStateOf(if (song.year > 0) song.year.toString() else "") }
    var track by remember { mutableStateOf(if (song.trackNumber > 0) song.trackNumber.toString() else "") }
    var disc by remember { mutableStateOf("") }
    var lyrics by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }

    var artworkUri by remember { mutableStateOf<Uri?>(song.albumArtUri) }
    var artworkLocalPath by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    // Load any existing overrides from DB
    LaunchedEffect(song.id) {
        val statsDb = LocalStatsDatabase.getInstance(context)
        val override = statsDb.getSongTagOverride(song.id)
        if (override != null) {
            override.title?.let { title = it }
            override.artist?.let { artist = it }
            override.album?.let { album = it }
            override.albumArtist?.let { albumArtist = it }
            override.composer?.let { composer = it }
            override.genre?.let { genre = it }
            override.year?.let { year = it.toString() }
            override.track?.let { track = it.toString() }
            override.disc?.let { disc = it.toString() }
            override.lyrics?.let { lyrics = it }
            override.comment?.let { comment = it }
            override.artworkPath?.let {
                artworkLocalPath = it
                artworkUri = Uri.parse(it)
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val dir = File(context.filesDir, "custom_artwork").apply { if (!exists()) mkdirs() }
                    val hash = AlbumArtworkDownloader.getHash("art_${song.id}_${System.currentTimeMillis()}")
                    val destFile = File(dir, "$hash.jpg")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (destFile.exists() && destFile.length() > 0) {
                        artworkLocalPath = destFile.absolutePath
                        artworkUri = Uri.fromFile(destFile)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun executeSave(writeDirectlyToFile: Boolean) {
        isSaving = true
        val editData = TagEditData(
            title = title,
            artist = artist,
            album = album,
            albumArtist = albumArtist,
            composer = composer,
            genre = genre,
            year = year,
            track = track,
            disc = disc,
            lyrics = lyrics,
            comment = comment,
            artworkUri = artworkUri,
            artworkPath = artworkLocalPath
        )

        scope.launch(Dispatchers.IO) {
            if (genre.isNotBlank() && artist.isNotBlank()) {
                val statsDb = LocalStatsDatabase.getInstance(context)
                val normArtist = normalizeText(DownloadPathHelper.getFirstArtist(artist))
                statsDb.setArtistCategory(normArtist, genre.trim())
            }
            if (writeDirectlyToFile) {
                TagWriterHelper.writeToFile(context, song, editData)
            } else {
                TagWriterHelper.saveAppOnly(context, song.id, editData)
            }
            withContext(Dispatchers.Main) {
                playerViewModel?.updateCurrentSongMetadata(
                    songId = song.id,
                    title = title,
                    artist = artist,
                    album = album,
                    albumArtUri = artworkUri,
                    genre = genre
                )
                onDismiss()
            }
        }
    }

    // Android 11+ MediaStore scoped storage edit request launcher
    val writePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            executeSave(writeDirectlyToFile = true)
        } else {
            Toast.makeText(context, "Permission denied. Saved in app database only.", Toast.LENGTH_SHORT).show()
            executeSave(writeDirectlyToFile = false)
        }
    }

    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Edit tags",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss, enabled = !isSaving) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        Button(
                            onClick = {
                                if (selectedModeIndex == 1) {
                                    val file = File(song.filePath)
                                    val isWritable = file.exists() && file.canWrite()
                                    if (!isWritable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                                        !song.filePath.startsWith("http://") && !song.filePath.startsWith("https://")) {
                                        try {
                                            val writeRequest = MediaStore.createWriteRequest(
                                                context.contentResolver,
                                                listOf(song.contentUri)
                                            )
                                            writePermissionLauncher.launch(
                                                IntentSenderRequest.Builder(writeRequest.intentSender).build()
                                            )
                                        } catch (e: Exception) {
                                            executeSave(writeDirectlyToFile = true)
                                        }
                                    } else {
                                        executeSave(writeDirectlyToFile = true)
                                    }
                                } else {
                                    executeSave(writeDirectlyToFile = false)
                                }
                            },
                            enabled = !isSaving,
                            shape = PillShape,
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text("Save")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Artwork Picker at top
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { galleryLauncher.launch("image/*") }
                    ) {
                        if (artworkUri != null) {
                            AsyncImage(
                                model = artworkUri,
                                contentDescription = "Album Artwork",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            AsyncSongArtwork(
                                albumArtUri = null,
                                colorSeed = song.colorSeed,
                                size = 120.dp,
                                shape = RoundedCornerShape(20.dp)
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                                .size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "Change artwork",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(20.dp)
                            )
                        }
                    }
                }

                // Storage Mode Segmented Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SegmentedButton(
                            selected = selectedModeIndex == 0,
                            onClick = { selectedModeIndex = 0 },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("App only", maxLines = 1)
                        }
                        SegmentedButton(
                            selected = selectedModeIndex == 1,
                            onClick = { selectedModeIndex = 1 },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Write to file", maxLines = 1)
                        }
                    }

                    Text(
                        text = if (selectedModeIndex == 0) {
                            "Overrides metadata inside North Music database without modifying audio files."
                        } else {
                            "Writes ID3/audio tags directly into the file with safe temp-file copy and MediaStore refresh."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Fields with 16dp spacing
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Artist") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it },
                    label = { Text("Album") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = albumArtist,
                    onValueChange = { albumArtist = it },
                    label = { Text("Album artist") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = composer,
                    onValueChange = { composer = it },
                    label = { Text("Composer") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = genre,
                    onValueChange = { genre = it },
                    label = { Text("Genre") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = year,
                        onValueChange = { year = it },
                        label = { Text("Year") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = track,
                        onValueChange = { track = it },
                        label = { Text("Track") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = disc,
                        onValueChange = { disc = it },
                        label = { Text("Disc") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = lyrics,
                    onValueChange = { lyrics = it },
                    label = { Text("Lyrics") },
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Comment") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
