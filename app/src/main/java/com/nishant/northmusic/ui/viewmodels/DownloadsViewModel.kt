package com.nishant.northmusic.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.nishant.northmusic.data.model.DownloadItem
import com.nishant.northmusic.data.model.DownloadStatus
import com.nishant.northmusic.data.model.SampleData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class DownloadsTab(val label: String) {
    DOWNLOADING("Downloading"),
    COMPLETED("Completed"),
    FAILED("Failed")
}

data class DownloadsUiState(
    val selectedTab: DownloadsTab = DownloadsTab.DOWNLOADING,
    val downloadingItems: List<DownloadItem> = SampleData.downloadingItems,
    val completedItems: List<DownloadItem> = SampleData.completedItems,
    val failedItems: List<DownloadItem> = SampleData.failedItems,
    val isSelectionMode: Boolean = false,
    val selectedItemIds: Set<String> = emptySet(),
    val downloadingCount: Int = 58,
    val completedCount: Int = 142,
    val failedCount: Int = 28,
    val totalCount: Int = 200,
    val networkSpeedText: String = "38 MB/s"
) {
    val selectedCount: Int
        get() = selectedItemIds.size

    val currentList: List<DownloadItem>
        get() = when (selectedTab) {
            DownloadsTab.DOWNLOADING -> downloadingItems
            DownloadsTab.COMPLETED -> completedItems
            DownloadsTab.FAILED -> failedItems
        }
}

class DownloadsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

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
        _uiState.update { current ->
            val updatedDownloading = current.downloadingItems.map { item ->
                if (item.id == itemId) {
                    item.copy(isPaused = !item.isPaused)
                } else item
            }
            current.copy(downloadingItems = updatedDownloading)
        }
        // TODO: Signal download worker to pause/resume
    }

    fun pauseAll() {
        _uiState.update { current ->
            current.copy(
                downloadingItems = current.downloadingItems.map { it.copy(isPaused = true) }
            )
        }
        // TODO: Pause all active download tasks
    }

    fun resumeAll() {
        _uiState.update { current ->
            current.copy(
                downloadingItems = current.downloadingItems.map { it.copy(isPaused = false) }
            )
        }
        // TODO: Resume all active download tasks
    }

    fun pauseSelected() {
        val selected = _uiState.value.selectedItemIds
        _uiState.update { current ->
            current.copy(
                downloadingItems = current.downloadingItems.map {
                    if (it.id in selected) it.copy(isPaused = true) else it
                }
            )
        }
        // TODO: Batch pause download tasks
    }

    fun resumeSelected() {
        val selected = _uiState.value.selectedItemIds
        _uiState.update { current ->
            current.copy(
                downloadingItems = current.downloadingItems.map {
                    if (it.id in selected) it.copy(isPaused = false) else it
                }
            )
        }
        // TODO: Batch resume download tasks
    }

    fun deleteSelected() {
        val selected = _uiState.value.selectedItemIds
        _uiState.update { current ->
            current.copy(
                isSelectionMode = false,
                selectedItemIds = emptySet(),
                downloadingItems = current.downloadingItems.filterNot { it.id in selected },
                completedItems = current.completedItems.filterNot { it.id in selected },
                failedItems = current.failedItems.filterNot { it.id in selected }
            )
        }
        // TODO: Delete downloaded files from disk and database
    }

    fun clearCompleted() {
        _uiState.update { current ->
            current.copy(
                completedItems = emptyList(),
                completedCount = 0
            )
        }
        // TODO: Clear completed records from history
    }

    fun retryAllFailed() {
        _uiState.update { current ->
            val recovered = current.failedItems.map {
                it.copy(
                    status = DownloadStatus.DOWNLOADING,
                    errorMessage = null,
                    isPaused = false,
                    speedText = "Retrying..."
                )
            }
            current.copy(
                failedItems = emptyList(),
                downloadingItems = current.downloadingItems + recovered,
                failedCount = 0
            )
        }
        // TODO: Re-enqueue all failed download jobs
    }

    fun addUrls(urls: List<String>) {
        val newItems = urls.mapIndexed { idx, url ->
            val cleanUrl = url.trim()
            val filename = cleanUrl.substringAfterLast("/").ifEmpty { "Audio_Stream_$idx.mp3" }
            DownloadItem(
                id = "d_new_${System.currentTimeMillis()}_$idx",
                title = filename.substringBeforeLast("."),
                artist = "Direct URL Source",
                progress = 0.05f,
                totalSizeMb = 14.5f,
                downloadedMb = 0.7f,
                speedText = "Connecting...",
                status = DownloadStatus.DOWNLOADING,
                isPaused = false,
                isSelected = false,
                colorSeed = 0xFF0D9488
            )
        }
        _uiState.update { current ->
            current.copy(
                downloadingItems = newItems + current.downloadingItems,
                downloadingCount = current.downloadingCount + newItems.size,
                selectedTab = DownloadsTab.DOWNLOADING
            )
        }
        // TODO: onAddUrls(list) - dispatch download tasks to WorkManager / DownloadService
    }
}
