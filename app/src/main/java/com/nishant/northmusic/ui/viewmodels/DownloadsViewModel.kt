package com.nishant.northmusic.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nishant.northmusic.data.model.DownloadItem
import com.nishant.northmusic.data.model.DownloadStatus
import com.nishant.northmusic.data.model.Song
import com.nishant.northmusic.data.network.CategoryPromptData
import com.nishant.northmusic.data.network.CdnMusicService
import com.nishant.northmusic.data.network.MusicDownloadManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DownloadsTab(val label: String) {
    ACTIVE("Active"),
    COMPLETED("Completed"),
    FAILED("Failed")
}

data class DownloadsUiState(
    val selectedTab: DownloadsTab = DownloadsTab.ACTIVE,
    val downloadingItems: List<DownloadItem> = emptyList(),
    val completedItems: List<DownloadItem> = emptyList(),
    val failedItems: List<DownloadItem> = emptyList(),
    val isSelectionMode: Boolean = false,
    val selectedItemIds: Set<String> = emptySet(),
    val downloadingCount: Int = 0,
    val completedCount: Int = 0,
    val failedCount: Int = 0,
    val totalCount: Int = 0,
    val networkSpeedText: String = "Idle"
) {
    val selectedCount: Int
        get() = selectedItemIds.size

    val currentList: List<DownloadItem>
        get() = when (selectedTab) {
            DownloadsTab.ACTIVE -> downloadingItems
            DownloadsTab.COMPLETED -> completedItems
            DownloadsTab.FAILED -> failedItems
        }
}

data class PlaylistTrackSelection(
    val title: String,
    val artist: String,
    val durationSeconds: Int = 0,
    val artworkUrl: String? = null,
    val isSelected: Boolean = true
)

data class PlaylistPreviewUiState(
    val title: String,
    val author: String,
    val artworkUrl: String?,
    val source: String,
    val originalUrl: String,
    val tracks: List<PlaylistTrackSelection>,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val selectedCount: Int get() = tracks.count { it.isSelected }
    val totalCount: Int get() = tracks.size
    val allSelected: Boolean get() = tracks.isNotEmpty() && tracks.all { it.isSelected }
}

class DownloadsViewModel(application: Application) : AndroidViewModel(application) {

    private val downloadManager = MusicDownloadManager.getInstance(application)
    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    private val _playlistPreview = MutableStateFlow<PlaylistPreviewUiState?>(null)
    val playlistPreview: StateFlow<PlaylistPreviewUiState?> = _playlistPreview.asStateFlow()

    val categoryPromptFlow: SharedFlow<CategoryPromptData> = downloadManager.categoryPromptFlow

    companion object {
        private val _sharedUrlFlow = MutableSharedFlow<String>(extraBufferCapacity = 5)
        val sharedUrlFlow: SharedFlow<String> = _sharedUrlFlow.asSharedFlow()

        fun onSharedUrlReceived(url: String) {
            _sharedUrlFlow.tryEmit(url)
        }
    }

    init {
        viewModelScope.launch {
            downloadManager.downloadItems.collect { items ->
                val downloading = items.filter { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED }
                val completed = items.filter { it.status == DownloadStatus.COMPLETED }.reversed()
                val failed = items.filter { it.status == DownloadStatus.FAILED }

                val activeSpeed = downloading.firstOrNull { it.status == DownloadStatus.DOWNLOADING && it.speedText.contains("MB/s") }?.speedText ?: "Idle"

                _uiState.update { current ->
                    current.copy(
                        downloadingItems = downloading.map { it.copy(isSelected = it.id in current.selectedItemIds) },
                        completedItems = completed.map { it.copy(isSelected = it.id in current.selectedItemIds) },
                        failedItems = failed.map { it.copy(isSelected = it.id in current.selectedItemIds) },
                        downloadingCount = downloading.size,
                        completedCount = completed.size,
                        failedCount = failed.size,
                        totalCount = items.size,
                        networkSpeedText = activeSpeed
                    )
                }
            }
        }

        viewModelScope.launch {
            sharedUrlFlow.collect { sharedUrl ->
                handleIncomingUrl(sharedUrl)
            }
        }
    }

    fun onTabSelected(tab: DownloadsTab) {
        _uiState.update { it.copy(selectedTab = tab) }
        if (_uiState.value.isSelectionMode) {
            exitSelectionMode()
        }
    }

    fun startSelectionMode(initialItemId: String) {
        _uiState.update { current ->
            val newSelected = current.selectedItemIds + initialItemId
            current.copy(
                isSelectionMode = true,
                selectedItemIds = newSelected,
                downloadingItems = current.downloadingItems.map { it.copy(isSelected = it.id in newSelected) },
                completedItems = current.completedItems.map { it.copy(isSelected = it.id in newSelected) },
                failedItems = current.failedItems.map { it.copy(isSelected = it.id in newSelected) }
            )
        }
    }

    fun toggleItemSelection(itemId: String) {
        if (!_uiState.value.isSelectionMode) {
            startSelectionMode(itemId)
            return
        }
        _uiState.update { current ->
            val newSelected = if (itemId in current.selectedItemIds) {
                current.selectedItemIds - itemId
            } else {
                current.selectedItemIds + itemId
            }

            if (newSelected.isEmpty()) {
                current.copy(
                    isSelectionMode = false,
                    selectedItemIds = emptySet(),
                    downloadingItems = current.downloadingItems.map { it.copy(isSelected = false) },
                    completedItems = current.completedItems.map { it.copy(isSelected = false) },
                    failedItems = current.failedItems.map { it.copy(isSelected = false) }
                )
            } else {
                current.copy(
                    selectedItemIds = newSelected,
                    downloadingItems = current.downloadingItems.map { it.copy(isSelected = it.id in newSelected) },
                    completedItems = current.completedItems.map { it.copy(isSelected = it.id in newSelected) },
                    failedItems = current.failedItems.map { it.copy(isSelected = it.id in newSelected) }
                )
            }
        }
    }

    fun selectAll() {
        _uiState.update { current ->
            val currentItems = current.currentList
            val allIds = currentItems.map { it.id }.toSet()
            val areAllSelected = current.selectedItemIds.containsAll(allIds)

            if (areAllSelected) {
                current.copy(
                    isSelectionMode = false,
                    selectedItemIds = emptySet(),
                    downloadingItems = current.downloadingItems.map { it.copy(isSelected = false) },
                    completedItems = current.completedItems.map { it.copy(isSelected = false) },
                    failedItems = current.failedItems.map { it.copy(isSelected = false) }
                )
            } else {
                val newSelected = current.selectedItemIds + allIds
                current.copy(
                    selectedItemIds = newSelected,
                    downloadingItems = current.downloadingItems.map { it.copy(isSelected = it.id in newSelected) },
                    completedItems = current.completedItems.map { it.copy(isSelected = it.id in newSelected) },
                    failedItems = current.failedItems.map { it.copy(isSelected = it.id in newSelected) }
                )
            }
        }
    }

    fun exitSelectionMode() {
        _uiState.update { current ->
            current.copy(
                isSelectionMode = false,
                selectedItemIds = emptySet(),
                downloadingItems = current.downloadingItems.map { it.copy(isSelected = false) },
                completedItems = current.completedItems.map { it.copy(isSelected = false) },
                failedItems = current.failedItems.map { it.copy(isSelected = false) }
            )
        }
    }

    fun togglePauseResume(itemId: String) {
        val item = _uiState.value.downloadingItems.find { it.id == itemId }
            ?: _uiState.value.failedItems.find { it.id == itemId }
            ?: return
        if (item.status == DownloadStatus.FAILED || item.isPaused) {
            downloadManager.resume(itemId)
        } else {
            downloadManager.pause(itemId)
        }
    }

    fun retry(itemId: String) {
        downloadManager.resume(itemId)
    }

    fun cancel(itemId: String) {
        downloadManager.cancel(itemId)
    }

    fun pauseAll() {
        downloadManager.pauseAll()
    }

    fun resumeAll() {
        downloadManager.resumeAll()
    }

    fun pauseSelected() {
        val selected = _uiState.value.selectedItemIds
        selected.forEach { downloadManager.pause(it) }
    }

    fun resumeSelected() {
        val selected = _uiState.value.selectedItemIds
        selected.forEach { downloadManager.resume(it) }
    }

    fun deleteSelected() {
        val selected = _uiState.value.selectedItemIds
        selected.forEach { downloadManager.cancel(it) }
        exitSelectionMode()
    }

    fun clearCompleted() {
        downloadManager.clearCompleted()
    }

    fun retryAllFailed() {
        downloadManager.retryAllFailed()
    }

    fun enqueue(
        title: String,
        artist: String,
        album: String = "",
        streamUrl: String? = null,
        artworkUrl: String? = null,
        durationSeconds: Int = 0,
        tags: List<String> = emptyList(),
        onDuplicate: ((Song) -> Unit)? = null
    ): String? {
        val res = downloadManager.enqueueDownload(
            title = title,
            artist = artist,
            album = album,
            streamUrl = streamUrl,
            artworkUrl = artworkUrl,
            durationSeconds = durationSeconds,
            tags = tags,
            onDuplicate = onDuplicate
        )
        if (res != null) {
            _uiState.update { it.copy(selectedTab = DownloadsTab.ACTIVE) }
        }
        return res
    }

    fun applyCategoryChoice(
        promptData: CategoryPromptData,
        newCategory: String,
        rememberForArtist: Boolean
    ) {
        viewModelScope.launch {
            downloadManager.applyCategoryChoice(promptData, newCategory, rememberForArtist)
        }
    }

    fun handleIncomingUrl(rawText: String) {
        val url = CdnMusicService.extractFirstUrl(rawText) ?: rawText.trim()
        if (url.isBlank()) return

        if (CdnMusicService.isPlaylistUrl(url)) {
            loadPlaylist(url)
        } else {
            addUrls(listOf(url))
        }
    }

    fun loadPlaylist(url: String) {
        _uiState.update { it.copy(selectedTab = DownloadsTab.ACTIVE) }
        _playlistPreview.value = PlaylistPreviewUiState(
            title = "Analyzing Playlist...",
            author = "Fetching tracks & high quality audio info",
            artworkUrl = null,
            source = "",
            originalUrl = url,
            tracks = emptyList(),
            isLoading = true,
            errorMessage = null
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val info = CdnMusicService.resolvePlaylist(url)
                if (info != null && info.tracks.isNotEmpty()) {
                    _playlistPreview.value = PlaylistPreviewUiState(
                        title = info.title,
                        author = info.author,
                        artworkUrl = info.artworkUrl,
                        source = info.source,
                        originalUrl = info.originalUrl,
                        tracks = info.tracks.map {
                            PlaylistTrackSelection(
                                title = it.title,
                                artist = it.artist,
                                durationSeconds = it.durationSeconds,
                                artworkUrl = it.artworkUrl ?: info.artworkUrl,
                                isSelected = true
                            )
                        },
                        isLoading = false
                    )
                } else {
                    _playlistPreview.value = PlaylistPreviewUiState(
                        title = "Could not load playlist",
                        author = "",
                        artworkUrl = null,
                        source = "",
                        originalUrl = url,
                        tracks = emptyList(),
                        isLoading = false,
                        errorMessage = "No tracks found in playlist or link is private."
                    )
                }
            } catch (e: Exception) {
                _playlistPreview.value = PlaylistPreviewUiState(
                    title = "Error loading playlist",
                    author = "",
                    artworkUrl = null,
                    source = "",
                    originalUrl = url,
                    tracks = emptyList(),
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to parse playlist"
                )
            }
        }
    }

    fun togglePlaylistTrack(index: Int) {
        _playlistPreview.update { current ->
            if (current == null || index !in current.tracks.indices) return@update current
            val updated = current.tracks.toMutableList()
            val item = updated[index]
            updated[index] = item.copy(isSelected = !item.isSelected)
            current.copy(tracks = updated)
        }
    }

    fun selectAllPlaylistTracks(selected: Boolean) {
        _playlistPreview.update { current ->
            if (current == null) return@update null
            current.copy(tracks = current.tracks.map { it.copy(isSelected = selected) })
        }
    }

    fun dismissPlaylistPreview() {
        _playlistPreview.value = null
    }

    fun downloadSelectedPlaylistTracks() {
        val state = _playlistPreview.value ?: return
        val selectedTracks = state.tracks.filter { it.isSelected }
        if (selectedTracks.isEmpty()) return

        _uiState.update { it.copy(selectedTab = DownloadsTab.ACTIVE) }
        _playlistPreview.value = null

        viewModelScope.launch(Dispatchers.IO) {
            selectedTracks.forEach { track ->
                downloadManager.enqueueDownload(
                    title = track.title,
                    artist = track.artist,
                    album = state.title,
                    streamUrl = null, // Will automatically resolve JioSaavn 320kbps CDN stream
                    artworkUrl = track.artworkUrl ?: state.artworkUrl,
                    durationSeconds = track.durationSeconds,
                    tags = listOf(state.title, state.author)
                )
            }
        }
    }

    fun addUrls(urls: List<String>) {
        _uiState.update { it.copy(selectedTab = DownloadsTab.ACTIVE) }
        viewModelScope.launch(Dispatchers.IO) {
            urls.forEach { rawText ->
                val url = CdnMusicService.extractFirstUrl(rawText) ?: rawText.trim()
                if (url.isNotBlank()) {
                    if (CdnMusicService.isPlaylistUrl(url)) {
                        loadPlaylist(url)
                        return@forEach
                    }

                    val resolved = CdnMusicService.resolveUrl(url)
                    if (resolved != null) {
                        downloadManager.enqueueDownload(
                            title = resolved.title,
                            artist = resolved.artist,
                            album = resolved.album,
                            streamUrl = resolved.downloadUrl,
                            artworkUrl = resolved.artworkUrl,
                            durationSeconds = resolved.durationSeconds,
                            tags = resolved.tags
                        )
                    } else {
                        // If direct link fallback
                        val filename = url.substringAfterLast("/").substringBefore("?").ifEmpty { "Audio_Track.m4a" }
                        downloadManager.enqueueDownload(
                            title = filename.substringBeforeLast("."),
                            artist = "Downloaded Link",
                            album = "Downloads",
                            streamUrl = url,
                            artworkUrl = null
                        )
                    }
                }
            }
        }
    }
}
