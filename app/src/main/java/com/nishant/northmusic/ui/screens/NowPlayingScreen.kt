package com.nishant.northmusic.ui.screens

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nishant.northmusic.data.model.LyricLine
import com.nishant.northmusic.data.model.RepeatMode
import com.nishant.northmusic.data.model.Song
import com.nishant.northmusic.ui.components.AsyncSongArtwork
import com.nishant.northmusic.ui.components.SongItem
import com.nishant.northmusic.ui.dialogs.AddToPlaylistDialog
import com.nishant.northmusic.ui.dialogs.GoToDialog
import com.nishant.northmusic.ui.dialogs.SleepTimerBottomSheet
import com.nishant.northmusic.ui.dialogs.SongDetailsDialog
import com.nishant.northmusic.ui.dialogs.TagEditorDialog
import com.nishant.northmusic.ui.dialogs.WebSearchDialog
import com.nishant.northmusic.ui.theme.PillShape
import com.nishant.northmusic.ui.theme.SquircleLargeArtworkShape
import com.nishant.northmusic.ui.viewmodels.PlayerViewModel
import com.nishant.northmusic.util.FeatureFlags
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    playerViewModel: PlayerViewModel,
    onDismiss: () -> Unit,
    onNavigateToEqualizer: (() -> Unit)? = null,
    onNavigateToAlbum: ((String) -> Unit)? = null,
    onNavigateToArtist: ((String) -> Unit)? = null,
    onNavigateToFolder: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val playerState by playerViewModel.playerState.collectAsState()
    val queue by playerViewModel.queue.collectAsState()
    val lyrics by playerViewModel.lyrics.collectAsState()
    val sleepTimerState by playerViewModel.sleepTimerState.collectAsState()
    val song = playerState.currentSong ?: return

    var sliderPosition by remember(playerState.currentPositionSeconds) {
        mutableFloatStateOf(playerState.currentPositionSeconds.toFloat())
    }

    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedSheetTab by remember { mutableIntStateOf(0) } // 0 = Lyrics, 1 = Up Next
    var showMoreMenu by remember { mutableStateOf(false) }
    var isQueueItemDragging by remember { mutableStateOf(false) }

    // Dialog states
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    var showAddToPlaylist by remember { mutableStateOf(false) }
    var showGoToDialog by remember { mutableStateOf(false) }
    var showTagEditor by remember { mutableStateOf(false) }
    var showWebSearch by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(context, "Deleted from device", Toast.LENGTH_SHORT).show()
            playerViewModel.skipToNext()
        }
    }

    // Fixed page layout: No verticalScroll on outer column so the player is always stable
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top App Bar (0.dp insets to sit ~16dp below system bar)
        TopAppBar(
            windowInsets = WindowInsets(0.dp),
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val (sourceCategory, sourceDetail) = remember(playerState.playbackSource, song) {
                        val src = playerState.playbackSource?.trim().orEmpty()
                        when {
                            src.startsWith("Playing from album ", ignoreCase = true) -> {
                                "PLAYING FROM ALBUM" to src.substringAfter("Playing from album ").ifBlank { song.album }
                            }
                            src.startsWith("Album ", ignoreCase = true) -> {
                                "PLAYING FROM ALBUM" to src.substringAfter("Album ").ifBlank { song.album }
                            }
                            src.startsWith("Playing from artist ", ignoreCase = true) -> {
                                "PLAYING FROM ARTIST" to src.substringAfter("Playing from artist ").ifBlank { song.artist }
                            }
                            src.startsWith("Artist ", ignoreCase = true) -> {
                                "PLAYING FROM ARTIST" to src.substringAfter("Artist ").ifBlank { song.artist }
                            }
                            src.startsWith("Playlist ", ignoreCase = true) -> {
                                "PLAYING FROM PLAYLIST" to src.substringAfter("Playlist ")
                            }
                            src.startsWith("Folder ", ignoreCase = true) -> {
                                "PLAYING FROM FOLDER" to src.substringAfter("Folder ")
                            }
                            src.equals("Songs", ignoreCase = true) -> {
                                "PLAYING FROM" to "Songs"
                            }
                            src.equals("Search results", ignoreCase = true) -> {
                                "PLAYING FROM" to "Search results"
                            }
                            src.equals("Queue", ignoreCase = true) -> {
                                "PLAYING FROM" to "Queue"
                            }
                            src.isNotBlank() -> {
                                "PLAYING FROM" to src
                            }
                            else -> {
                                "PLAYING FROM ALBUM" to song.album
                            }
                        }
                    }
                    Text(
                        text = sourceCategory,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = sourceDetail,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(32.dp)
                    )
                }
            },
            actions = {
                Box {
                    IconButton(
                        onClick = { showMoreMenu = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        // 1. Sleep timer
                        DropdownMenuItem(
                            text = { Text("Sleep timer") },
                            onClick = {
                                showMoreMenu = false
                                showSleepTimerSheet = true
                            },
                            leadingIcon = {
                                Icon(Icons.Rounded.Bedtime, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        )

                        // 2. Equalizer
                        if (FeatureFlags.SHOW_EQUALIZER && onNavigateToEqualizer != null) {
                            DropdownMenuItem(
                                text = { Text("Equalizer") },
                                onClick = {
                                    showMoreMenu = false
                                    onNavigateToEqualizer()
                                },
                                leadingIcon = {
                                    Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            )
                        }

                        // 3. Add to playlist
                        DropdownMenuItem(
                            text = { Text("Add to playlist") },
                            onClick = {
                                showMoreMenu = false
                                showAddToPlaylist = true
                            },
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        )

                        // 4. Go to (Album, Artist, Folder)
                        DropdownMenuItem(
                            text = { Text("Go to") },
                            onClick = {
                                showMoreMenu = false
                                showGoToDialog = true
                            },
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        )

                        // 5. Tag editor
                        if (FeatureFlags.SHOW_TAG_EDITOR) {
                            DropdownMenuItem(
                                text = { Text("Tag editor") },
                                onClick = {
                                    showMoreMenu = false
                                    showTagEditor = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            )
                        }

                        // 6. Web search
                        DropdownMenuItem(
                            text = { Text("Web search") },
                            onClick = {
                                showMoreMenu = false
                                showWebSearch = true
                            },
                            leadingIcon = {
                                Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        )

                        // 7. Share
                        DropdownMenuItem(
                            text = { Text("Share") },
                            onClick = {
                                showMoreMenu = false
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "audio/*"
                                    putExtra(Intent.EXTRA_STREAM, song.contentUri)
                                    putExtra(Intent.EXTRA_SUBJECT, song.title)
                                    putExtra(Intent.EXTRA_TEXT, "Listening to ${song.title} by ${song.artist}")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share \"${song.title}\""))
                            },
                            leadingIcon = {
                                Icon(Icons.Rounded.Share, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        )

                        // 8. Details
                        DropdownMenuItem(
                            text = { Text("Details") },
                            onClick = {
                                showMoreMenu = false
                                showDetails = true
                            },
                            leadingIcon = {
                                Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        )

                        // 9. Delete from device
                        DropdownMenuItem(
                            text = { Text("Delete from device") },
                            onClick = {
                                showMoreMenu = false
                                showDeleteConfirm = true
                            },
                            leadingIcon = {
                                Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            }
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Artwork with Coil AsyncImage and placeholder fallback
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            AsyncSongArtwork(
                artworkUri = song.albumArtUri,
                colorSeed = song.colorSeed,
                size = 320.dp,
                shape = SquircleLargeArtworkShape,
                modifier = Modifier
                    .aspectRatio(1f, matchHeightConstraintsFirst = true)
                    .shadow(
                        elevation = 20.dp,
                        shape = SquircleLargeArtworkShape,
                        ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    )
            )
        }

        // Song Title, Artist, Badges, and Quick Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Badges row: High Quality Audio Badge + Sleep Timer countdown chip
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Surface(
                        shape = PillShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.HighQuality,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "High quality",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Sleep Timer countdown chip on player
                    if (sleepTimerState.isActive) {
                        Surface(
                            shape = PillShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .clip(PillShape)
                                .clickable { showSleepTimerSheet = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Bedtime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = sleepTimerState.formattedRemaining,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                Text(
                    text = song.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(
                    onClick = { playerViewModel.toggleFavorite() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (playerState.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (playerState.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
                IconButton(
                    onClick = { showAddToPlaylist = true },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                        contentDescription = "Add to playlist",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Progress Slider with thick 8dp track & round thumb
        Slider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            onValueChangeFinished = { playerViewModel.seekTo(sliderPosition.toInt()) },
            valueRange = 0f..song.durationSeconds.toFloat().coerceAtLeast(1f),
            thumb = {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(8.dp),
                    colors = SliderDefaults.colors(
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Time Indicator Labels (Position / Total Duration)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(sliderPosition.toInt()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = song.formattedDuration,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Playback Controls Row: Shuffle | Prev | Play/Pause | Next | Repeat
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { playerViewModel.toggleShuffle() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (playerState.isShuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(
                onClick = { playerViewModel.skipPrevious() },
                modifier = Modifier.size(54.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = "Previous",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(34.dp)
                )
            }

            // Central Play/Pause FAB
            FilledIconButton(
                onClick = { playerViewModel.togglePlayPause() },
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = if (playerState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(38.dp)
                )
            }

            IconButton(
                onClick = { playerViewModel.skipNext() },
                modifier = Modifier.size(54.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = "Next",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(34.dp)
                )
            }

            IconButton(
                onClick = { playerViewModel.cycleRepeatMode() },
                modifier = Modifier.size(48.dp)
            ) {
                val icon = when (playerState.repeatMode) {
                    RepeatMode.ONE -> Icons.Rounded.RepeatOne
                    else -> Icons.Rounded.Repeat
                }
                Icon(
                    imageVector = icon,
                    contentDescription = "Repeat",
                    tint = if (playerState.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Bottom Action Buttons: Lyrics and Up Next
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FilledTonalButton(
                onClick = {
                    selectedSheetTab = 0
                    showBottomSheet = true
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lyrics,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Lyrics",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            FilledTonalButton(
                onClick = {
                    selectedSheetTab = 1
                    showBottomSheet = true
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Up Next",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    // Modal Bottom Sheet for Lyrics & Up Next
    if (showBottomSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            sheetGesturesEnabled = !isQueueItemDragging,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                )
            },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(520.dp)
            ) {
                // Tab Header: Lyrics / Up Next
                TabRow(
                    selectedTabIndex = selectedSheetTab,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedSheetTab]),
                            color = MaterialTheme.colorScheme.primary,
                            height = 3.dp
                        )
                    },
                    divider = {}
                ) {
                    Tab(
                        selected = selectedSheetTab == 0,
                        onClick = { selectedSheetTab = 0 },
                        text = {
                            Text(
                                text = "Lyrics",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (selectedSheetTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedSheetTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    Tab(
                        selected = selectedSheetTab == 1,
                        onClick = { selectedSheetTab = 1 },
                        text = {
                            Text(
                                text = "Up Next",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (selectedSheetTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedSheetTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }

                // Tab Content Body
                Box(modifier = Modifier.fillMaxSize()) {
                    if (selectedSheetTab == 0) {
                        LyricsTabContent(
                            lyrics = lyrics,
                            currentPositionSeconds = playerState.currentPositionSeconds,
                            onSeekToLine = { seconds ->
                                playerViewModel.seekTo(seconds)
                            },
                            onSearchOnline = {
                                playerViewModel.searchLyricsOnline()
                            }
                        )
                    } else {
                        UpNextTabContent(
                            currentSong = song,
                            queue = queue,
                            onSongClick = { nextSong ->
                                playerViewModel.playSong(nextSong)
                            },
                            onRemoveFromQueue = { songId ->
                                playerViewModel.removeFromQueue(songId)
                            },
                            onMoveQueueItem = { from, to ->
                                playerViewModel.moveQueueItem(from, to)
                            },
                            onClearQueue = {
                                playerViewModel.clearQueue()
                            },
                            onDragStateChanged = { dragging ->
                                isQueueItemDragging = dragging
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showSleepTimerSheet) {
        SleepTimerBottomSheet(
            playerViewModel = playerViewModel,
            onDismiss = { showSleepTimerSheet = false }
        )
    }

    if (showAddToPlaylist) {
        AddToPlaylistDialog(
            song = song,
            onDismiss = { showAddToPlaylist = false }
        )
    }

    if (showGoToDialog) {
        GoToDialog(
            song = song,
            onGoToAlbum = { album ->
                onNavigateToAlbum?.invoke(album) ?: onDismiss()
            },
            onGoToArtist = { artist ->
                onNavigateToArtist?.invoke(artist) ?: onDismiss()
            },
            onGoToFolder = { folder ->
                onNavigateToFolder?.invoke(folder) ?: onDismiss()
            },
            onDismiss = { showGoToDialog = false }
        )
    }

    if (showTagEditor) {
        TagEditorDialog(
            song = song,
            onDismiss = { showTagEditor = false }
        )
    }

    if (showWebSearch) {
        WebSearchDialog(
            song = song,
            onDismiss = { showWebSearch = false }
        )
    }

    if (showDetails) {
        SongDetailsDialog(
            song = song,
            onDismiss = { showDetails = false }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete from device") },
            text = { Text("Are you sure you want to permanently delete \"${song.title}\" from your device?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            try {
                                val deleteRequest = MediaStore.createDeleteRequest(
                                    context.contentResolver,
                                    listOf(song.contentUri)
                                )
                                deleteLauncher.launch(
                                    IntentSenderRequest.Builder(deleteRequest.intentSender).build()
                                )
                            } catch (_: Exception) {
                                try {
                                    context.contentResolver.delete(song.contentUri, null, null)
                                    File(song.filePath).delete()
                                    Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                                    playerViewModel.skipToNext()
                                } catch (_: Exception) {}
                            }
                        } else {
                            try {
                                context.contentResolver.delete(song.contentUri, null, null)
                                File(song.filePath).delete()
                                Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                                playerViewModel.skipToNext()
                            } catch (_: Exception) {}
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}

/**
 * Lyrics Tab:
 * Displays lyrics lines with current line highlighted (bigger, bright) and others dimmed.
 * Auto-scrolls with song progress.
 * Tap a line seeks playback to that point.
 * Empty state: "No lyrics found" + "Search online" button.
 */
@Composable
fun LyricsTabContent(
    lyrics: List<LyricLine>,
    currentPositionSeconds: Int,
    onSeekToLine: (Int) -> Unit,
    onSearchOnline: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (lyrics.isEmpty()) {
        // Empty State
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Lyrics,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No lyrics found",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Add an .lrc file in the same folder or embed lyrics tags into the audio file",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            FilledTonalButton(
                onClick = onSearchOnline,
                shape = PillShape
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Search online")
            }
        }
    } else {
        val listState = rememberLazyListState()

        // Find current lyric index based on active playback time
        val activeIndex = remember(currentPositionSeconds, lyrics) {
            val idx = lyrics.indexOfLast { it.timeSeconds <= currentPositionSeconds }
            if (idx == -1) 0 else idx
        }

        // Auto-scroll so current lyric line stays centered
        LaunchedEffect(activeIndex) {
            if (lyrics.isNotEmpty() && activeIndex in lyrics.indices) {
                listState.animateScrollToItem(
                    index = activeIndex,
                    scrollOffset = -150
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 80.dp, bottom = 120.dp, start = 24.dp, end = 24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            itemsIndexed(lyrics) { index, line ->
                val isCurrent = index == activeIndex

                val textColor by animateColorAsState(
                    targetValue = if (isCurrent) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                    },
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    label = "lyricColor"
                )

                Text(
                    text = line.text,
                    style = if (isCurrent) {
                        MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            lineHeight = 28.sp
                        )
                    } else {
                        MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Normal,
                            fontSize = 17.sp,
                            lineHeight = 24.sp
                        )
                    },
                    color = textColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            onSeekToLine(line.timeSeconds)
                        }
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Up Next Tab:
 * Queue list using the shared SongItem with drag handles to reorder,
 * 3-dot menu with "Remove from queue", lifted-item shadow, and haptic feedback.
 */
@Composable
fun UpNextTabContent(
    currentSong: Song,
    queue: List<Song>,
    onSongClick: (Song) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onMoveQueueItem: (from: Int, to: Int) -> Unit,
    onClearQueue: () -> Unit,
    onDragStateChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    var currentQueueList by remember(queue) { mutableStateOf(queue) }

    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        currentQueueList = currentQueueList.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
        onMoveQueueItem(from.index, to.index)
    }

    LaunchedEffect(reorderableLazyListState.isAnyItemDragging) {
        onDragStateChanged(reorderableLazyListState.isAnyItemDragging)
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Queue Summary & Clear Queue Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val countText = if (currentQueueList.size == 1) "1 track in queue" else "${currentQueueList.size} tracks in queue"
            Text(
                text = countText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (currentQueueList.isNotEmpty()) {
                TextButton(
                    onClick = onClearQueue,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ClearAll,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear queue")
                }
            }
        }

        if (currentQueueList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 48.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = "Queue is empty",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(
                    items = currentQueueList,
                    key = { it.id }
                ) { song ->
                    val index = currentQueueList.indexOf(song)
                    ReorderableItem(
                        state = reorderableLazyListState,
                        key = song.id
                    ) { isDragging ->
                        val elevation by animateDpAsState(
                            targetValue = if (isDragging) 8.dp else 0.dp,
                            label = "elevation"
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(elevation, shape = RoundedCornerShape(12.dp))
                                .background(
                                    if (isDragging) MaterialTheme.colorScheme.surfaceContainerHigh
                                    else Color.Transparent
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                SongItem(
                                    song = song,
                                    isPlaying = song.id == currentSong.id,
                                    onClick = { onSongClick(song) },
                                    onOptionsClick = { onRemoveFromQueue(index) }
                                )
                            }

                            IconButton(
                                onClick = {},
                                modifier = Modifier
                                    .draggableHandle(
                                        onDragStarted = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    )
                                    .padding(end = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DragHandle,
                                    contentDescription = "Drag to reorder",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
