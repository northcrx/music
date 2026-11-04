package com.nishant.northmusic.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nishant.northmusic.data.local.LocalStatsDatabase
import com.nishant.northmusic.data.model.Album
import com.nishant.northmusic.data.model.Song
import com.nishant.northmusic.data.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val forYouSongs: List<Song> = emptyList(),
    val recentlyPlayed: List<Song> = emptyList(),
    val topTracks: List<Song> = emptyList(),
    val lastAdded: List<Song> = emptyList(),
    val favorites: List<Song> = emptyList(),
    val yourMusic: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val isScanning: Boolean = false
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val musicRepo = MusicRepository.getInstance(application)
    private val statsDb = LocalStatsDatabase.getInstance(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                musicRepo.songs,
                musicRepo.albums,
                musicRepo.isScanning,
                statsDb.statsVersion
            ) { songs, albums, isScanning, _ ->
                computeHomeSections(songs, albums, isScanning)
            }.collect {}
        }
    }

    private suspend fun computeHomeSections(
        songs: List<Song>,
        albums: List<Album>,
        isScanning: Boolean
    ) {
        if (songs.isEmpty()) {
            _uiState.update {
                it.copy(
                    forYouSongs = emptyList(),
                    recentlyPlayed = emptyList(),
                    topTracks = emptyList(),
                    lastAdded = emptyList(),
                    favorites = emptyList(),
                    yourMusic = emptyList(),
                    albums = emptyList(),
                    isScanning = isScanning
                )
            }
            return
        }

        val songMap = songs.associateBy { it.id }

        // Top tracks by play count
        val topIds = statsDb.getTopTrackIds(30)
        val topTracks = topIds.mapNotNull { songMap[it] }.ifEmpty {
            songs.take(15)
        }

        // History / recently played
        val historyIds = statsDb.getHistoryTrackIds(30)
        val historyTracks = historyIds.mapNotNull { songMap[it] }

        // Favorites
        val favorites = songs.filter { it.isFavorite }

        // Last added (date added descending)
        val lastAdded = songs.sortedByDescending { it.dateAdded }

        // For You: recently played first, then most played, then remaining songs
        val forYouOrder = LinkedHashSet<Song>()
        forYouOrder.addAll(historyTracks)
        forYouOrder.addAll(topTracks)
        forYouOrder.addAll(songs)
        val forYouList = forYouOrder.take(12)

        _uiState.update {
            it.copy(
                forYouSongs = forYouList,
                recentlyPlayed = historyTracks.take(10),
                topTracks = topTracks,
                lastAdded = lastAdded.take(30),
                favorites = favorites,
                yourMusic = songs,
                albums = albums,
                isScanning = isScanning
            )
        }
    }

    fun onShuffleMixClick() {
        // Callback hook for personalized shuffle mix triggers
    }

    fun rescan() {
        musicRepo.rescan()
    }
}
