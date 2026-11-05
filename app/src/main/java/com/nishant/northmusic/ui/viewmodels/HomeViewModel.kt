package com.nishant.northmusic.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nishant.northmusic.data.local.LocalStatsDatabase
import com.nishant.northmusic.data.model.Album
import com.nishant.northmusic.data.model.Artist
import com.nishant.northmusic.data.model.Song
import com.nishant.northmusic.data.network.OnlineTrack
import com.nishant.northmusic.data.network.ResolvedTrackInfo
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
    val artists: List<Artist> = emptyList(),
    val isScanning: Boolean = false
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val musicRepo = MusicRepository.getInstance(application)
    private val statsDb = LocalStatsDatabase.getInstance(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Persistent Search State across navigation and player expansion
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchCategory = MutableStateFlow("Songs")
    val searchCategory: StateFlow<String> = _searchCategory.asStateFlow()

    private val _onlineTracks = MutableStateFlow<List<OnlineTrack>>(emptyList())
    val onlineTracks: StateFlow<List<OnlineTrack>> = _onlineTracks.asStateFlow()

    private val _isOnlineSearching = MutableStateFlow(false)
    val isOnlineSearching: StateFlow<Boolean> = _isOnlineSearching.asStateFlow()

    private val _resolvedUrlTrack = MutableStateFlow<ResolvedTrackInfo?>(null)
    val resolvedUrlTrack: StateFlow<ResolvedTrackInfo?> = _resolvedUrlTrack.asStateFlow()

    private val _isResolvingUrl = MutableStateFlow(false)
    val isResolvingUrl: StateFlow<Boolean> = _isResolvingUrl.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSearchCategory(category: String) {
        _searchCategory.value = category
    }

    fun updateOnlineTracks(tracks: List<OnlineTrack>) {
        _onlineTracks.value = tracks
    }

    fun updateIsOnlineSearching(searching: Boolean) {
        _isOnlineSearching.value = searching
    }

    fun updateResolvedUrlTrack(track: ResolvedTrackInfo?) {
        _resolvedUrlTrack.value = track
    }

    fun updateIsResolvingUrl(resolving: Boolean) {
        _isResolvingUrl.value = resolving
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _onlineTracks.value = emptyList()
        _resolvedUrlTrack.value = null
        _isOnlineSearching.value = false
        _isResolvingUrl.value = false
    }

    init {
        viewModelScope.launch {
            combine(
                musicRepo.songs,
                musicRepo.albums,
                musicRepo.artists,
                musicRepo.isScanning,
                statsDb.statsVersion
            ) { songs, albums, artists, isScanning, _ ->
                computeHomeSections(songs, albums, artists, isScanning)
            }.collect {}
        }
    }

    private suspend fun computeHomeSections(
        songs: List<Song>,
        albums: List<Album>,
        artists: List<Artist>,
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
                    artists = emptyList(),
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
                artists = artists,
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
