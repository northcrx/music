package com.nishant.northmusic.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nishant.northmusic.data.model.DownloadItem
import com.nishant.northmusic.data.model.DownloadStatus
import com.nishant.northmusic.data.network.CdnMusicService
import com.nishant.northmusic.data.network.MusicDownloadManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

class DownloadsViewModel(application: Application) : AndroidViewModel(application) {

    private val downloadManager = MusicDownloadManager.getInstance(application)
    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            downloadManager.downloadItems.collect { items ->
                val downloading = items.filter { it.status == DownloadStatus.DOWNLOADING }
                val completed = items.filter { it.status == DownloadStatus.COMPLETED }
                val failed = items.filter { it.status == DownloadStatus.FAILED }

                val activeSpeed = downloading.firstOrNull { it.speedText.contains("MB/s") }?.speedText ?: "Idle"

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
        val item = _uiState.value.downloadingItems.find { it.id == itemId } ?: return
        if (item.isPaused) {
            downloadManager.resume(itemId)
        } else {
            downloadManager.pause(itemId)
        }
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
        artworkUrl: String? = null
    ) {
        downloadManager.enqueueDownload(title, artist, album, streamUrl, artworkUrl)
        _uiState.update { it.copy(selectedTab = DownloadsTab.ACTIVE) }
    }

    fun addUrls(urls: List<String>) {
        _uiState.update { it.copy(selectedTab = DownloadsTab.ACTIVE) }
        viewModelScope.launch(Dispatchers.IO) {
            urls.forEach { rawUrl ->
                val url = rawUrl.trim()
                if (url.isNotBlank()) {
                    val resolved = CdnMusicService.resolveUrl(url)
                    if (resolved != null) {
                        downloadManager.enqueueDownload(
                            title = resolved.title,
                            artist = resolved.artist,
                            album = resolved.album,
                            streamUrl = resolved.downloadUrl,
                            artworkUrl = resolved.artworkUrl
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
