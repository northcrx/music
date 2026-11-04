package com.nishant.northmusic.ui.viewmodels

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.nishant.northmusic.data.local.LocalStatsDatabase
import com.nishant.northmusic.data.model.LyricLine
import com.nishant.northmusic.data.model.PlayerState
import com.nishant.northmusic.data.model.RepeatMode
import com.nishant.northmusic.data.model.Song
import com.nishant.northmusic.data.repository.MusicRepository
import com.nishant.northmusic.playback.AudioFxManager
import com.nishant.northmusic.playback.LyricsHelper
import com.nishant.northmusic.playback.PlaybackService
import com.nishant.northmusic.playback.SleepTimerManager
import com.nishant.northmusic.playback.SleepTimerState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val musicRepo = MusicRepository.getInstance(context)
    private val statsDb = LocalStatsDatabase.getInstance(context)

    private var mediaController: MediaController? = null
    private var progressJob: Job? = null

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    // Up Next Playback Queue
    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    // Synced Lyrics parsed from companion .lrc or embedded tag
    private val _lyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val lyrics: StateFlow<List<LyricLine>> = _lyrics.asStateFlow()

    // Sleep Timer State exposed from playback service
    val sleepTimerState: StateFlow<SleepTimerState> = SleepTimerManager.state

    init {
        initMediaController()
    }

    private fun initMediaController() {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture.addListener({
            try {
                val controller = controllerFuture.get()
                mediaController = controller
                setupPlayerListener(controller)
                restoreLastPlayedSongIfAny()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun setupPlayerListener(controller: MediaController) {
        controller.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                val mediaId = mediaItem?.mediaId
                if (mediaId != null) {
                    val matchingSong = _queue.value.find { it.id == mediaId }
                        ?: musicRepo.songs.value.find { it.id == mediaId }

                    if (matchingSong != null) {
                        _playerState.update {
                            it.copy(
                                currentSong = matchingSong,
                                isFavorite = statsDb.isFavorite(matchingSong.id)
                            )
                        }
                        loadLyrics(matchingSong)
                        AudioFxManager.updateCurrentGenre(matchingSong.genre)
                        viewModelScope.launch {
                            statsDb.recordSongPlay(matchingSong.id)
                        }
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                _playerState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) {
                    startProgressTracker()
                } else {
                    stopProgressTracker()
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                super.onShuffleModeEnabledChanged(shuffleModeEnabled)
                _playerState.update { it.copy(isShuffle = shuffleModeEnabled) }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                super.onRepeatModeChanged(repeatMode)
                val mode = when (repeatMode) {
                    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                    Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                    else -> RepeatMode.OFF
                }
                _playerState.update { it.copy(repeatMode = mode) }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                if (playbackState == Player.STATE_READY) {
                    val durationMs = controller.duration
                    if (durationMs > 0) {
                        val durationSecs = (durationMs / 1000).toInt()
                        _playerState.update { current ->
                            val updatedSong = current.currentSong?.copy(durationSeconds = durationSecs)
                            current.copy(currentSong = updatedSong)
                        }
                    }
                }
            }
        })
    }

    private fun restoreLastPlayedSongIfAny() {
        viewModelScope.launch {
            val history = statsDb.getRecentlyPlayed(1)
            val lastId = history.firstOrNull() ?: return@launch
            val song = musicRepo.songs.value.find { it.id == lastId } ?: return@launch
            val posSec = statsDb.getLastPosition(song.id)

            _playerState.update {
                it.copy(
                    currentSong = song,
                    isPlaying = false,
                    currentPositionSeconds = posSec,
                    isFavorite = statsDb.isFavorite(song.id),
                    playbackSource = "Queue"
                )
            }
            _queue.value = listOf(song)
            loadLyrics(song)
            AudioFxManager.updateCurrentGenre(song.genre)

            val controller = mediaController ?: return@launch
            controller.setMediaItem(song.toMediaItem(), posSec * 1000L)
            controller.prepare()
        }
    }

    fun playSong(song: Song, customQueue: List<Song>? = null, source: String? = null) {
        val newQueue = customQueue ?: if (_queue.value.any { it.id == song.id }) _queue.value else listOf(song)
        _queue.value = newQueue

        _playerState.update {
            it.copy(
                currentSong = song,
                isPlaying = true,
                currentPositionSeconds = 0,
                isFavorite = statsDb.isFavorite(song.id),
                playbackSource = source ?: it.playbackSource ?: "Songs"
            )
        }

        loadLyrics(song)
        AudioFxManager.updateCurrentGenre(song.genre)

        viewModelScope.launch {
            statsDb.recordSongPlay(song.id)
        }

        val controller = mediaController ?: return
        val mediaItems = newQueue.map { it.toMediaItem() }
        val startIndex = newQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)

        controller.setMediaItems(mediaItems, startIndex, 0L)
        controller.prepare()
        controller.play()
    }

    fun shuffleAll(songs: List<Song>, source: String? = null) {
        if (songs.isEmpty()) return
        val shuffled = songs.shuffled()
        playSong(shuffled.first(), shuffled, source)
        mediaController?.shuffleModeEnabled = true
        _playerState.update { it.copy(isShuffle = true) }
    }

    fun togglePlayPause() {
        val controller = mediaController
        if (controller != null) {
            if (controller.isPlaying) {
                controller.pause()
            } else {
                if (controller.playbackState == Player.STATE_IDLE || controller.mediaItemCount == 0) {
                    val current = _playerState.value.currentSong
                    if (current != null) {
                        playSong(current, _queue.value.ifEmpty { listOf(current) })
                        return
                    }
                }
                controller.play()
            }
        } else {
            _playerState.update { it.copy(isPlaying = !it.isPlaying) }
        }
    }

    fun toggleFavorite() {
        val currentSong = _playerState.value.currentSong ?: return
        viewModelScope.launch {
            val newFav = musicRepo.toggleFavorite(currentSong.id)
            _playerState.update {
                it.copy(
                    isFavorite = newFav,
                    currentSong = it.currentSong?.copy(isFavorite = newFav)
                )
            }
        }
    }

    fun toggleShuffle() {
        val controller = mediaController
        val newShuffle = !_playerState.value.isShuffle
        controller?.shuffleModeEnabled = newShuffle
        _playerState.update { it.copy(isShuffle = newShuffle) }
    }

    fun cycleRepeatMode() {
        val controller = mediaController ?: return
        val nextMode = when (_playerState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        val exoMode = when (nextMode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
        controller.repeatMode = exoMode
        _playerState.update { it.copy(repeatMode = nextMode) }
    }

    fun skipToNext() {
        val controller = mediaController
        if (controller != null && controller.hasNextMediaItem()) {
            controller.seekToNextMediaItem()
        } else {
            // Manual queue fallback
            val q = _queue.value
            val currentId = _playerState.value.currentSong?.id
            val currentIndex = q.indexOfFirst { it.id == currentId }
            if (currentIndex != -1 && currentIndex < q.size - 1) {
                playSong(q[currentIndex + 1], q)
            }
        }
    }

    fun skipNext() = skipToNext()

    fun skipToPrevious() {
        val controller = mediaController
        if (controller != null) {
            if (controller.currentPosition > 3000L || !controller.hasPreviousMediaItem()) {
                controller.seekTo(0L)
            } else {
                controller.seekToPreviousMediaItem()
            }
        } else {
            // Manual fallback
            val q = _queue.value
            val currentId = _playerState.value.currentSong?.id
            val currentIndex = q.indexOfFirst { it.id == currentId }
            if (currentIndex > 0) {
                playSong(q[currentIndex - 1], q)
            } else {
                seekTo(0)
            }
        }
    }

    fun skipPrevious() = skipToPrevious()

    fun seekTo(seconds: Int) {
        val controller = mediaController
        val targetMs = seconds * 1000L
        controller?.seekTo(targetMs)
        _playerState.update { it.copy(currentPositionSeconds = seconds) }
    }

    fun playQueueIndex(index: Int) {
        val q = _queue.value
        if (index in q.indices) {
            val song = q[index]
            playSong(song, q)
        }
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        val currentList = _queue.value.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)
            _queue.value = currentList
            mediaController?.moveMediaItem(fromIndex, toIndex)
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) = reorderQueue(fromIndex, toIndex)

    fun removeFromQueue(index: Int) {
        val currentList = _queue.value.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            _queue.value = currentList
            mediaController?.removeMediaItem(index)
        }
    }

    fun clearQueue() {
        val currentSong = _playerState.value.currentSong
        if (currentSong != null) {
            _queue.value = listOf(currentSong)
            mediaController?.clearMediaItems()
            mediaController?.setMediaItem(currentSong.toMediaItem())
        } else {
            _queue.value = emptyList()
            mediaController?.clearMediaItems()
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                val controller = mediaController
                if (controller != null && controller.isPlaying) {
                    val posSec = (controller.currentPosition / 1000).toInt()
                    val durMs = controller.duration
                    val durSec = if (durMs > 0) (durMs / 1000).toInt() else 0
                    _playerState.update { state ->
                        val current = state.currentSong
                        val updatedSong = if (current != null && current.durationSeconds <= 0 && durSec > 0) {
                            current.copy(durationSeconds = durSec)
                        } else {
                            current
                        }
                        state.copy(
                            currentPositionSeconds = posSec,
                            currentSong = updatedSong
                        )
                    }
                    _playerState.value.currentSong?.let { song ->
                        statsDb.saveLastPosition(song.id, posSec)
                    }
                }
                delay(500L)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun loadLyrics(song: Song) {
        viewModelScope.launch {
            _lyrics.value = LyricsHelper.getLyricsForSong(context, song)
        }
    }

    fun searchLyricsOnline() {
        val song = _playerState.value.currentSong ?: return
        viewModelScope.launch {
            _lyrics.value = LyricsHelper.getLyricsForSong(context, song)
        }
    }

    fun startSleepTimer(minutes: Int, finishLastSong: Boolean, fadeOut: Boolean) {
        SleepTimerManager.startTimer(minutes, finishLastSong, fadeOut)
    }

    fun cancelSleepTimer() {
        SleepTimerManager.cancelTimer()
    }

    override fun onCleared() {
        stopProgressTracker()
        mediaController?.release()
        mediaController = null
        super.onCleared()
    }
}

private fun Song.toMediaItem(): MediaItem {
    val targetUri = if (filePath.startsWith("http://") || filePath.startsWith("https://")) {
        Uri.parse(filePath)
    } else {
        val file = if (filePath.isNotBlank()) File(filePath) else null
        if (file != null && file.exists()) {
            Uri.fromFile(file)
        } else {
            contentUri
        }
    }
    val metadataBuilder = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setAlbumTitle(album)
    albumArtUri?.let { metadataBuilder.setArtworkUri(it) }

    return MediaItem.Builder()
        .setMediaId(id)
        .setUri(targetUri)
        .setMediaMetadata(metadataBuilder.build())
        .build()
}
