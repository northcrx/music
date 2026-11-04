package com.nishant.northmusic.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.nishant.northmusic.data.model.LyricLine
import com.nishant.northmusic.data.model.PlayerState
import com.nishant.northmusic.data.model.RepeatMode
import com.nishant.northmusic.data.model.SampleData
import com.nishant.northmusic.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PlayerViewModel : ViewModel() {

    private val _playerState = MutableStateFlow(SampleData.initialPlayerState)
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    // Up Next Playback Queue
    private val _queue = MutableStateFlow<List<Song>>(SampleData.songs)
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    // Synced Lyrics (Real lyrics will later be parsed from embedded ID3 USLT/SYLT tags or local .lrc files)
    private val _lyrics = MutableStateFlow<List<LyricLine>>(SampleData.sampleLyrics)
    val lyrics: StateFlow<List<LyricLine>> = _lyrics.asStateFlow()

    fun playSong(song: Song) {
        _playerState.update { current ->
            current.copy(
                currentSong = song,
                isPlaying = true,
                currentPositionSeconds = 0,
                isFavorite = song.isFavorite
            )
        }
        // If song is not in queue or not at the top, we keep queue updated
        _queue.update { currentQueue ->
            if (currentQueue.any { it.id == song.id }) {
                currentQueue
            } else {
                listOf(song) + currentQueue
            }
        }
        // TODO: Connect to real media player engine (ExoPlayer/Media3)
        // TODO: Read embedded USLT/SYLT tags or check for filename.lrc in the track directory
    }

    fun togglePlayPause() {
        _playerState.update { current ->
            current.copy(isPlaying = !current.isPlaying)
        }
        // TODO: Send play/pause command to playback service
    }

    fun toggleFavorite() {
        _playerState.update { current ->
            val newFav = !current.isFavorite
            current.copy(
                isFavorite = newFav,
                currentSong = current.currentSong?.copy(isFavorite = newFav)
            )
        }
        // TODO: Persist favorite status to database
    }

    fun toggleShuffle() {
        _playerState.update { current ->
            current.copy(isShuffle = !current.isShuffle)
        }
        // TODO: Apply shuffle order in playback queue
    }

    fun cycleRepeatMode() {
        _playerState.update { current ->
            val nextMode = when (current.repeatMode) {
                RepeatMode.OFF -> RepeatMode.ALL
                RepeatMode.ALL -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.OFF
            }
            current.copy(repeatMode = nextMode)
        }
        // TODO: Apply repeat mode to media player
    }

    fun seekTo(seconds: Int) {
        _playerState.update { current ->
            current.copy(currentPositionSeconds = seconds)
        }
        // TODO: Seek playback position in media player
    }

    fun skipNext() {
        val currentQueue = _queue.value
        val currentSong = _playerState.value.currentSong
        val currentIndex = currentQueue.indexOfFirst { it.id == currentSong?.id }
        val nextSong = if (currentIndex != -1 && currentIndex < currentQueue.lastIndex) {
            currentQueue[currentIndex + 1]
        } else if (currentQueue.isNotEmpty()) {
            currentQueue.first()
        } else {
            SampleData.songs.first()
        }
        playSong(nextSong)
        // TODO: Skip to next track in queue
    }

    fun skipPrevious() {
        val currentQueue = _queue.value
        val currentSong = _playerState.value.currentSong
        val currentIndex = currentQueue.indexOfFirst { it.id == currentSong?.id }
        val prevSong = if (currentIndex > 0) {
            currentQueue[currentIndex - 1]
        } else if (currentQueue.isNotEmpty()) {
            currentQueue.last()
        } else {
            SampleData.songs.last()
        }
        playSong(prevSong)
        // TODO: Skip to previous track in queue
    }

    // Queue actions
    fun removeFromQueue(songId: String) {
        _queue.update { currentQueue ->
            currentQueue.filterNot { it.id == songId }
        }
        // TODO: Notify media session queue item removal
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
        // TODO: Reorder media session playback queue
    }

    fun clearQueue() {
        val current = _playerState.value.currentSong
        _queue.update {
            // Keep current song if playing, or empty
            if (current != null) listOf(current) else emptyList()
        }
        // TODO: Clear queue in playback service
    }

    // Lyrics actions
    fun searchLyricsOnline() {
        // TODO: Fetch synced lyrics from online provider (e.g. lrclib / Musixmatch / Genius)
    }

    fun setEmptyLyrics(isEmpty: Boolean) {
        if (isEmpty) {
            _lyrics.value = emptyList()
        } else {
            _lyrics.value = SampleData.sampleLyrics
        }
    }
}
