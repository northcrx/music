package com.nishant.northmusic.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.nishant.northmusic.data.model.SampleData
import com.nishant.northmusic.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class HomeUiState(
    val greeting: String = "Good evening, Alex",
    val sessionMood: String = "Late Night Session",
    val searchQuery: String = "",
    val selectedMoodTag: String = "For Alex",
    val moodTags: List<String> = listOf("For Alex", "Deep Focus", "Nocturnal Drive", "Chillwave Synth", "Lo-Fi Beats"),
    val recentlyPlayed: List<Song> = SampleData.songs.take(5),
    val yourMusic: List<Song> = SampleData.songs,
    val isShuffleActive: Boolean = false
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        // TODO: Filter songs based on search query
    }

    fun onMoodTagSelected(tag: String) {
        _uiState.update { it.copy(selectedMoodTag = tag) }
        // TODO: Refresh recommendations based on mood
    }

    fun onShuffleMixClick() {
        _uiState.update { it.copy(isShuffleActive = !it.isShuffleActive) }
        // TODO: Shuffle night mix playback
    }

    fun onTuneMixClick() {
        // TODO: Open mix equalizer/tuning sheet
    }

    fun onNotificationClick() {
        // TODO: Open notifications panel
    }
}
