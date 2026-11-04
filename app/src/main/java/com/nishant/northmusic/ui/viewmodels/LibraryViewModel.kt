package com.nishant.northmusic.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nishant.northmusic.data.model.LibraryFilter
import com.nishant.northmusic.data.model.SampleData
import com.nishant.northmusic.data.model.Song
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibraryUiState(
    val selectedFilter: LibraryFilter = LibraryFilter.SONGS,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val isSortAscending: Boolean = true,
    val songs: List<Song> = SampleData.songs,
    val isScanning: Boolean = false,
    val isLibraryEmpty: Boolean = false,
    val activeScrollLetter: String = "A"
)

class LibraryViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    fun onFilterSelected(filter: LibraryFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
        // TODO: Filter content by filter category
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { current ->
            val filtered = if (query.isBlank()) {
                SampleData.songs
            } else {
                SampleData.songs.filter {
                    it.title.contains(query, ignoreCase = true) ||
                            it.artist.contains(query, ignoreCase = true)
                }
            }
            current.copy(searchQuery = query, songs = filtered)
        }
        // TODO: Perform database query search
    }

    fun toggleSearchActive() {
        _uiState.update { it.copy(isSearchActive = !it.isSearchActive) }
    }

    fun toggleSearch() {
        toggleSearchActive()
    }

    fun toggleSortOrder() {
        _uiState.update { current ->
            val newAsc = !current.isSortAscending
            val sorted = if (newAsc) {
                current.songs.sortedBy { it.title }
            } else {
                current.songs.sortedByDescending { it.title }
            }
            current.copy(isSortAscending = newAsc, songs = sorted)
        }
        // TODO: Apply sorting in persistent preferences
    }

    fun onLetterSelected(letter: String) {
        _uiState.update { it.copy(activeScrollLetter = letter) }
    }

    fun scanMusic() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            delay(1500)
            _uiState.update {
                it.copy(
                    isScanning = false,
                    isLibraryEmpty = false,
                    songs = SampleData.songs
                )
            }
            // TODO: Invoke MediaStore local audio indexing
        }
    }

    fun toggleEmptyStateForPreview() {
        _uiState.update {
            it.copy(
                isLibraryEmpty = !it.isLibraryEmpty,
                songs = if (!it.isLibraryEmpty) emptyList() else SampleData.songs
            )
        }
    }
}
