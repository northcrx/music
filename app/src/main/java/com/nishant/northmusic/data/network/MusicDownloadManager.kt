package com.nishant.northmusic.data.network

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import com.nishant.northmusic.data.model.DownloadItem
import com.nishant.northmusic.data.model.DownloadStatus
import com.nishant.northmusic.data.model.Song
import com.nishant.northmusic.data.repository.MusicRepository
import com.nishant.northmusic.data.util.TagEditData
import com.nishant.northmusic.data.util.TagWriterHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

class MusicDownloadManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<String, kotlinx.coroutines.Job>()
    private val pausedFlags = ConcurrentHashMap<String, Boolean>()

    private val _downloadItems = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloadItems: StateFlow<List<DownloadItem>> = _downloadItems.asStateFlow()

    fun getDownloadDir(): File {
        val publicMusic = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        val northDir = File(publicMusic, "NorthMusic")
        return if (northDir.exists() || northDir.mkdirs()) {
            northDir
        } else {
            val fallback = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: File(context.filesDir, "downloads")
            fallback.mkdirs()
            fallback
        }
    }

    fun enqueueDownload(
        title: String,
        artist: String,
        album: String,
        streamUrl: String?,
        artworkUrl: String?
    ): String {
        val id = "dl_${System.currentTimeMillis()}_${(100..999).random()}"

        val newItem = DownloadItem(
            id = id,
            title = title,
            artist = artist,
            album = album,
            artworkUrl = artworkUrl,
            progress = 0f,
            totalSizeMb = 8.5f,
            downloadedMb = 0f,
            speedText = "Connecting...",
            status = DownloadStatus.DOWNLOADING,
            isPaused = false,
            colorSeed = (0xFF000000..0xFFFFFFFF).random()
        )

        _downloadItems.update { listOf(newItem) + it }

        if (streamUrl.isNullOrBlank()) {
            // Attempt to resolve streamUrl if null
            scope.launch {
                val resolved = CdnMusicService.searchTracks("$title $artist", limit = 1).firstOrNull()
                val urlToUse = resolved?.downloadUrl
                if (!urlToUse.isNullOrBlank()) {
                    startDownloadJob(id, title, artist, album, urlToUse, artworkUrl ?: resolved.artworkUrl)
                } else {
                    markFailed(id, "Could not resolve audio stream URL")
                }
            }
        } else {
            startDownloadJob(id, title, artist, album, streamUrl, artworkUrl)
        }

        return id
    }

    private fun startDownloadJob(
        id: String,
        title: String,
        artist: String,
        album: String,
        streamUrl: String,
        artworkUrl: String?
    ) {
        pausedFlags[id] = false
        val job = scope.launch {
            try {
                executeDownload(id, title, artist, album, streamUrl, artworkUrl)
            } catch (e: Exception) {
                if (pausedFlags[id] == true) {
                    _downloadItems.update { list ->
                        list.map { if (it.id == id) it.copy(isPaused = true, speedText = "Paused") else it }
                    }
                } else {
                    markFailed(id, e.message ?: "Download failed")
                }
            } finally {
                activeJobs.remove(id)
            }
        }
        activeJobs[id] = job
    }

    private suspend fun executeDownload(
        id: String,
        title: String,
        artist: String,
        album: String,
        streamUrl: String,
        artworkUrl: String?
    ) = withContext(Dispatchers.IO) {
        val safeTitle = title.replace(Regex("""[\\/:*?"<>|]"""), "_").trim()
        val safeArtist = artist.replace(Regex("""[\\/:*?"<>|]"""), "_").trim()
        val extension = when {
            streamUrl.contains(".mp3") -> "mp3"
            streamUrl.contains(".m4a") -> "m4a"
            streamUrl.contains(".mp4") -> "m4a"
            else -> "m4a"
        }
        val targetFile = File(getDownloadDir(), "$safeArtist - $safeTitle.$extension")

        val url = URL(streamUrl)
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "Mozilla/5.0")
        conn.connectTimeout = 15000
        conn.readTimeout = 20000
        conn.connect()

        val contentLength = conn.contentLengthLong
        val totalMb = if (contentLength > 0) contentLength / (1024f * 1024f) else 9.0f

        _downloadItems.update { list ->
            list.map { if (it.id == id) it.copy(totalSizeMb = totalMb) else it }
        }

        val tempFile = File(context.cacheDir, "dl_temp_$id.$extension")
        var totalBytesRead = 0L
        var lastUpdateTime = System.currentTimeMillis()
        var bytesSinceLastUpdate = 0L

        conn.inputStream.use { input ->
            FileOutputStream(tempFile).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    if (pausedFlags[id] == true) {
                        throw IllegalStateException("Download paused")
                    }

                    output.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    bytesSinceLastUpdate += bytesRead

                    val now = System.currentTimeMillis()
                    val timeDelta = now - lastUpdateTime
                    if (timeDelta >= 400) {
                        val speedBps = (bytesSinceLastUpdate * 1000f) / timeDelta
                        val speedMb = speedBps / (1024f * 1024f)
                        val speedStr = String.format("%.1f MB/s", speedMb)

                        val downloadedMb = totalBytesRead / (1024f * 1024f)
                        val progress = if (contentLength > 0) {
                            (totalBytesRead.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                        } else 0.5f

                        _downloadItems.update { list ->
                            list.map {
                                if (it.id == id) {
                                    it.copy(
                                        downloadedMb = downloadedMb,
                                        progress = progress,
                                        speedText = speedStr
                                    )
                                } else it
                            }
                        }

                        lastUpdateTime = now
                        bytesSinceLastUpdate = 0L
                    }
                }
            }
        }

        // Copy completed temp file to public destination
        tempFile.copyTo(targetFile, overwrite = true)
        tempFile.delete()

        // Download artwork file for ID3 tagging
        var artworkFile: File? = null
        if (!artworkUrl.isNullOrBlank()) {
            try {
                val artTemp = File(context.cacheDir, "art_temp_$id.jpg")
                val artConn = URL(artworkUrl).openConnection() as HttpURLConnection
                artConn.connectTimeout = 8000
                artConn.readTimeout = 8000
                artConn.inputStream.use { input ->
                    FileOutputStream(artTemp).use { output ->
                        input.copyTo(output)
                    }
                }
                if (artTemp.exists() && artTemp.length() > 0) {
                    artworkFile = artTemp
                }
            } catch (_: Exception) {}
        }

        // Write ID3 Metadata & Artwork
        try {
            val fakeSong = Song(
                id = id,
                title = title,
                artist = artist,
                album = album,
                durationSeconds = 0,
                filePath = targetFile.absolutePath
            )
            TagWriterHelper.writeToFile(
                context = context,
                song = fakeSong,
                tags = TagEditData(
                    title = title,
                    artist = artist,
                    album = album,
                    albumArtist = artist,
                    composer = "",
                    genre = "Music",
                    year = "",
                    track = "",
                    disc = "",
                    lyrics = "",
                    comment = "Downloaded via North Music 320k",
                    artworkPath = artworkFile?.absolutePath
                )
            )
        } catch (_: Exception) {}

        artworkFile?.delete()

        // Scan into Android MediaStore so it immediately appears in Library and Home
        MediaScannerConnection.scanFile(
            context,
            arrayOf(targetFile.absolutePath),
            null
        ) { _, _ ->
            MusicRepository.getInstance(context).rescan()
        }

        val finalMb = targetFile.length() / (1024f * 1024f)
        _downloadItems.update { list ->
            list.map {
                if (it.id == id) {
                    it.copy(
                        progress = 1.0f,
                        downloadedMb = finalMb,
                        totalSizeMb = finalMb,
                        speedText = "Completed",
                        status = DownloadStatus.COMPLETED,
                        filePath = targetFile.absolutePath
                    )
                } else it
            }
        }
    }

    fun pause(id: String) {
        pausedFlags[id] = true
        activeJobs[id]?.cancel()
        _downloadItems.update { list ->
            list.map { if (it.id == id) it.copy(isPaused = true, speedText = "Paused") else it }
        }
    }

    fun resume(id: String) {
        val item = _downloadItems.value.find { it.id == id } ?: return
        if (item.status == DownloadStatus.COMPLETED) return

        pausedFlags[id] = false
        _downloadItems.update { list ->
            list.map { if (it.id == id) it.copy(isPaused = false, speedText = "Resuming...") else it }
        }
        enqueueDownload(item.title, item.artist, item.album, null, item.artworkUrl)
    }

    fun pauseAll() {
        _downloadItems.value.filter { it.status == DownloadStatus.DOWNLOADING }.forEach {
            pause(it.id)
        }
    }

    fun resumeAll() {
        _downloadItems.value.filter { it.isPaused || it.status == DownloadStatus.FAILED }.forEach {
            resume(it.id)
        }
    }

    fun cancel(id: String) {
        activeJobs[id]?.cancel()
        activeJobs.remove(id)
        pausedFlags.remove(id)
        _downloadItems.update { list -> list.filterNot { it.id == id } }
    }

    fun clearCompleted() {
        _downloadItems.update { list -> list.filterNot { it.status == DownloadStatus.COMPLETED } }
    }

    fun retryAllFailed() {
        val failed = _downloadItems.value.filter { it.status == DownloadStatus.FAILED }
        failed.forEach { resume(it.id) }
    }

    private fun markFailed(id: String, error: String) {
        _downloadItems.update { list ->
            list.map {
                if (it.id == id) {
                    it.copy(
                        status = DownloadStatus.FAILED,
                        errorMessage = error,
                        speedText = "Failed"
                    )
                } else it
            }
        }
    }

    companion object {
        @Volatile
        private var instance: MusicDownloadManager? = null

        fun getInstance(context: Context): MusicDownloadManager {
            return instance ?: synchronized(this) {
                instance ?: MusicDownloadManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
