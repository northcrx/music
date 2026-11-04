package com.nishant.northmusic.ui.viewmodels

import android.app.Application
import android.content.ComponentName
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
import com.nishant.northmusic.playback.LyricsHelper
import com.nishant.northmusic.playback.PlaybackService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
                if (playbackState == Player.STATE_ENDED) {
                    _playerState.update { it.copy(isPlaying = false, currentPositionSeconds = 0) }
                }
            }
        })
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                val controller = mediaController
                if (controller != null && controller.isPlaying) {
                    val posSec = (controller.currentPosition / 1000L).toInt()
                    _playerState.update { it.copy(currentPositionSeconds = posSec) }
                    _playerState.value.currentSong?.id?.let { id ->
                        statsDb.updateLastPosition(id, posSec)
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun restoreLastPlayedSongIfAny() {
        viewModelScope.launch {
            val last = statsDb.getLastPlayedTrack() ?: return@launch
            val (songId, posSec) = last
            val song = musicRepo.songs.value.find { it.id == songId } ?: return@launch

            _playerState.update {
                it.copy(
                    currentSong = song,
                    isPlaying = false,
                    currentPositionSeconds = posSec,
                    isFavorite = statsDb.isFavorite(song.id)
                )
            }
            _queue.value = listOf(song)
            loadLyrics(song)

            val controller = mediaController ?: return@launch
            controller.setMediaItem(song.toMediaItem(), posSec * 1000L)
            controller.prepare()
        }
    }

    fun playSong(song: Song, customQueue: List<Song>? = null) {
        val newQueue = customQueue ?: if (_queue.value.any { it.id == song.id }) _queue.value else listOf(song)
        _queue.value = newQueue

        _playerState.update {
            it.copy(
                currentSong = song,
                isPlaying = true,
                currentPositionSeconds = 0,
                isFavorite = statsDb.isFavorite(song.id)
            )
        }

        loadLyrics(song)

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

    fun shuffleAll(songs: List<Song>) {
        if (songs.isEmpty()) return
        val shuffled = songs.shuffled()
        playSong(shuffled.first(), shuffled)
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
        val newShuffle = !(_playerState.value.isShuffle)
        controller?.shuffleModeEnabled = newShuffle
        _playerState.update { it.copy(isShuffle = newShuffle) }
    }

    fun cycleRepeatMode() {
        val controller = mediaController
        val nextMode = when (_playerState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        val exoMode = when (nextMode) {
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
        }
        controller?.repeatMode = exoMode
        _playerState.update { it.copy(repeatMode = nextMode) }
    }

    fun seekTo(seconds: Int) {
        val controller = mediaController
        controller?.seekTo(seconds * 1000L)
        _playerState.update { it.copy(currentPositionSeconds = seconds) }
    }

    fun skipNext() {
        val controller = mediaController
        if (controller != null && controller.hasNextMediaItem()) {
            controller.seekToNextMediaItem()
        } else {
            val currentQueue = _queue.value
            val currentSong = _playerState.value.currentSong
            val currentIndex = currentQueue.indexOfFirst { it.id == currentSong?.id }
            if (currentIndex != -1 && currentIndex < currentQueue.lastIndex) {
                playSong(currentQueue[currentIndex + 1], currentQueue)
            } else if (currentQueue.isNotEmpty()) {
                playSong(currentQueue.first(), currentQueue)
            }
        }
    }

    fun skipToNext() = skipNext()

    fun skipPrevious() {
        val controller = mediaController
        if (controller != null && controller.hasPreviousMediaItem()) {
            controller.seekToPreviousMediaItem()
        } else {
            val currentQueue = _queue.value
            val currentSong = _playerState.value.currentSong
            val currentIndex = currentQueue.indexOfFirst { it.id == currentSong?.id }
            if (currentIndex > 0) {
                playSong(currentQueue[currentIndex - 1], currentQueue)
            } else if (currentQueue.isNotEmpty()) {
                playSong(currentQueue.last(), currentQueue)
            }
        }
    }

    fun skipToPrevious() = skipPrevious()

    fun removeFromQueue(songId: String) {
        val currentQueue = _queue.value
        val index = currentQueue.indexOfFirst { it.id == songId }
        if (index != -1) {
            mediaController?.removeMediaItem(index)
            _queue.update { it.filterNot { s -> s.id == songId } }
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        _queue.update { currentQueue ->
            if (fromIndex in currentQueue.indices && toIndex in currentQueue.indices) {
                val mutable = currentQueue.toMutableList()
                val item = mutable.removeAt(fromIndex)
                mutable.add(toIndex, item)
                mutable
            } else {
                currentQueue
            }
        }
        mediaController?.moveMediaItem(fromIndex, toIndex)
    }

    fun clearQueue() {
        val current = _playerState.value.currentSong
        if (current != null) {
            _queue.value = listOf(current)
            mediaController?.clearMediaItems()
            mediaController?.setMediaItem(current.toMediaItem())
        } else {
            _queue.value = emptyList()
            mediaController?.clearMediaItems()
        }
    }

    private fun loadLyrics(song: Song) {
        viewModelScope.launch {
            _lyrics.value = LyricsHelper.getLyricsForSong(context, song)
        }
    }

    fun searchLyricsOnline() {
        // TODO: Online lyrics search provider
    }

    override fun onCleared() {
        stopProgressTracker()
        mediaController?.release()
        mediaController = null
        super.onCleared()
    }
}

private fun Song.toMediaItem(): MediaItem {
    return MediaItem.Builder()
        .setMediaId(id)
        .setUri(contentUri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(albumArtUri)
                .build()
        )
        .build()
}
