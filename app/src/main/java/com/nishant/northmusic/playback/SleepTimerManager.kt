package com.nishant.northmusic.playback

import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class SleepTimerState(
    val isActive: Boolean = false,
    val totalDurationMinutes: Int = 0,
    val remainingSeconds: Int = 0,
    val finishLastSong: Boolean = false,
    val fadeOut: Boolean = true,
    val isWaitingForSongToEnd: Boolean = false
) {
    val formattedRemaining: String
        get() {
            if (isWaitingForSongToEnd) return "Finishing track"
            val m = remainingSeconds / 60
            val s = remainingSeconds % 60
            return "%d:%02d".format(m, s)
        }
}

object SleepTimerManager {

    private val _state = MutableStateFlow(SleepTimerState())
    val state: StateFlow<SleepTimerState> = _state.asStateFlow()

    private var activePlayer: Player? = null
    private var serviceScope: CoroutineScope? = null
    private var timerJob: Job? = null
    private var originalVolume: Float = 1.0f

    private val songEndListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
            if (_state.value.isWaitingForSongToEnd) {
                // Current song just ended and transitioned
                executeFinalPause()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (_state.value.isWaitingForSongToEnd && playbackState == Player.STATE_ENDED) {
                executeFinalPause()
            }
        }
    }

    fun attachPlayer(player: Player, scope: CoroutineScope) {
        activePlayer = player
        serviceScope = scope
        originalVolume = player.volume.coerceAtLeast(0.1f)
        player.addListener(songEndListener)
    }

    fun detachPlayer() {
        cancelTimer()
        activePlayer?.removeListener(songEndListener)
        activePlayer = null
        serviceScope = null
    }

    fun startTimer(minutes: Int, finishLastSong: Boolean, fadeOut: Boolean) {
        val scope = serviceScope ?: return
        val player = activePlayer ?: return

        timerJob?.cancel()
        originalVolume = player.volume.coerceAtLeast(0.1f)

        val totalSecs = (minutes * 60).coerceAtLeast(5)
        _state.value = SleepTimerState(
            isActive = true,
            totalDurationMinutes = minutes,
            remainingSeconds = totalSecs,
            finishLastSong = finishLastSong,
            fadeOut = fadeOut,
            isWaitingForSongToEnd = false
        )

        timerJob = scope.launch {
            var currentSecs = totalSecs
            while (isActive && currentSecs > 0) {
                delay(1000L)
                currentSecs--

                // Handle smooth 10s fade-out
                if (fadeOut && currentSecs <= 10) {
                    val progress = (currentSecs.toFloat() / 10f).coerceIn(0f, 1f)
                    player.volume = (originalVolume * progress).coerceAtLeast(0f)
                }

                _state.update { it.copy(remainingSeconds = currentSecs) }
            }

            if (finishLastSong && player.isPlaying) {
                // Keep playing until current song completes
                _state.update { it.copy(isWaitingForSongToEnd = true) }
            } else {
                executeFinalPause()
            }
        }
    }

    private fun executeFinalPause() {
        val player = activePlayer
        player?.pause()
        // Restore player volume
        player?.volume = originalVolume

        timerJob?.cancel()
        timerJob = null
        _state.value = SleepTimerState()
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        activePlayer?.volume = originalVolume
        _state.value = SleepTimerState()
    }
}
