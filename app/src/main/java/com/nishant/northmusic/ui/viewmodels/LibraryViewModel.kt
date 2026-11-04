package com.nishant.northmusic.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nishant.northmusic.data.local.LocalStatsDatabase
import com.nishant.northmusic.data.model.Album
import com.nishant.northmusic.data.model.Artist
import com.nishant.northmusic.data.model.FolderItem
import com.nishant.northmusic.data.model.LibraryFilter
import com.nishant.northmusic.data.model.Playlist
import com.nishant.northmusic.data.model.Song
import com.nishant.northmusic.data.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.Normalizer

data class LibraryUiState(
    val selectedFilter: LibraryFilter = LibraryFilter.SONGS,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val isSortAscending: Boolean = true,
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val folders: List<FolderItem> = emptyList(),
    val isScanning: Boolean = false,
    val isLibraryEmpty: Boolean = false,
    val activeScrollLetter: String = "A"
)

fun String.normalizeForSearch(): String {
    val normalized = Normalizer.normalize(this, Normalizer.Form.NFD)
    return normalized.replace("""\p{M}""".toRegex(), "").lowercase()
}

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val musicRepo = MusicRepository.getInstance(application)
    private val localStatsDb = LocalStatsDatabase.getInstance(application)

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                musicRepo.songs,
                musicRepo.albums,
                musicRepo.artists,
                musicRepo.folders,
                musicRepo.isScanning,
                localStatsDb.userPlaylistsFlow
            ) { array ->
                @Suppress("UNCHECKED_CAST")
                val songs = array[0] as List<Song>
                @Suppress("UNCHECKED_CAST")
                val albums = array[1] as List<Album>
                @Suppress("UNCHECKED_CAST")
                val artists = array[2] as List<Artist>
                @Suppress("UNCHECKED_CAST")
                val folders = array[3] as List<FolderItem>
                val isScanning = array[4] as Boolean
                @Suppress("UNCHECKED_CAST")
                val userPlaylists = array[5] as List<Playlist>

                val favCount = songs.count { it.isFavorite }
                val dynamicPlaylists = mutableListOf<Playlist>()
                if (favCount > 0) {
                    dynamicPlaylists.add(
                        Playlist(
                            id = "fav",
                            title = "Favorites",
                            songCount = favCount,
                            colorSeed = 0xFFF43F5E
                        )
                    )
                }
                dynamicPlaylists.addAll(userPlaylists)

                val query = _uiState.value.searchQuery.trim().normalizeForSearch()
                val isAsc = _uiState.value.isSortAscending

                val filteredSongs = if (query.isBlank()) {
                    songs
                } else {
                    songs.filter {
                        it.title.normalizeForSearch().contains(query) ||
                                it.artist.normalizeForSearch().contains(query) ||
                                it.album.normalizeForSearch().contains(query)
                    }
                }.let { list ->
                    if (isAsc) list.sortedBy { it.title.lowercase() } else list.sortedByDescending { it.title.lowercase() }
                }

                val filteredAlbums = if (query.isBlank()) {
                    albums
                } else {
                    albums.filter {
                        it.title.normalizeForSearch().contains(query) ||
                                it.artist.normalizeForSearch().contains(query)
                    }
                }

                val filteredArtists = if (query.isBlank()) {
                    artists
                } else {
                    artists.filter {
                        it.name.normalizeForSearch().contains(query)
                    }
                }

                val filteredFolders = if (query.isBlank()) {
                    folders
                } else {
                    folders.filter {
                        it.name.normalizeForSearch().contains(query)
                    }
                }

                _uiState.update { current ->
                    current.copy(
                        songs = filteredSongs,
                        albums = filteredAlbums,
                        artists = filteredArtists,
                        playlists = dynamicPlaylists,
                        folders = filteredFolders,
                        isScanning = isScanning,
                        isLibraryEmpty = !isScanning && songs.isEmpty()
                    )
                }
            }.collect {}
        }
    }

    fun onFilterSelected(filter: LibraryFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun selectFilter(filter: LibraryFilter) = onFilterSelected(filter)

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        reapplyFilterAndSort()
    }

    fun updateSearchQuery(query: String) = onSearchQueryChange(query)

    fun toggleSearchActive() {
        _uiState.update { it.copy(isSearchActive = !it.isSearchActive) }
        if (!_uiState.value.isSearchActive) {
            onSearchQueryChange("")
        }
    }

    fun toggleSearch() {
        toggleSearchActive()
    }

    fun toggleSortOrder() {
        _uiState.update { it.copy(isSortAscending = !it.isSortAscending) }
        reapplyFilterAndSort()
    }

    fun onLetterSelected(letter: String) {
        _uiState.update { it.copy(activeScrollLetter = letter) }
    }

    fun scanMusic() {
        musicRepo.rescan()
    }

    private fun reapplyFilterAndSort() {
        val allSongs = musicRepo.songs.value
        val allAlbums = musicRepo.albums.value
        val allArtists = musicRepo.artists.value
        val allFolders = musicRepo.folders.value

        val query = _uiState.value.searchQuery.trim().normalizeForSearch()
        val isAsc = _uiState.value.isSortAscending

        val filteredSongs = if (query.isBlank()) {
            allSongs
        } else {
            allSongs.filter {
                it.title.normalizeForSearch().contains(query) ||
                        it.artist.normalizeForSearch().contains(query) ||
                        it.album.normalizeForSearch().contains(query)
            }
        }.let { list ->
            if (isAsc) list.sortedBy { it.title.lowercase() } else list.sortedByDescending { it.title.lowercase() }
        }

        val filteredAlbums = if (query.isBlank()) {
            allAlbums
        } else {
            allAlbums.filter {
                it.title.normalizeForSearch().contains(query) ||
                        it.artist.normalizeForSearch().contains(query)
            }
        }

        val filteredArtists = if (query.isBlank()) {
            allArtists
        } else {
            allArtists.filter {
                it.name.normalizeForSearch().contains(query)
            }
        }

        val filteredFolders = if (query.isBlank()) {
            allFolders
        } else {
            allFolders.filter {
                it.name.normalizeForSearch().contains(query)
            }
        }

        _uiState.update {
            it.copy(
                songs = filteredSongs,
                albums = filteredAlbums,
                artists = filteredArtists,
                folders = filteredFolders
            )
        }
    }
}
