package com.nishant.northmusic.data.network

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.nishant.northmusic.data.local.LocalStatsDatabase
import com.nishant.northmusic.data.model.DownloadFolderStructure
import com.nishant.northmusic.data.model.DownloadItem
import com.nishant.northmusic.data.model.DownloadStatus
import com.nishant.northmusic.data.model.Song
import com.nishant.northmusic.data.repository.MusicRepository
import com.nishant.northmusic.data.repository.SettingsRepository
import com.nishant.northmusic.data.repository.normalizeText
import com.nishant.northmusic.data.repository.splitArtistNames
import com.nishant.northmusic.data.util.DownloadPathHelper
import com.nishant.northmusic.data.util.TagWriterHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

data class CategoryPromptData(
    val downloadId: String,
    val title: String,
    val artist: String,
    val contentUri: Uri?,
    val currentFilePath: String,
    val currentCategory: String
)

private data class DownloadTask(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val streamUrl: String?,
    val artworkUrl: String?,
    val durationSeconds: Int,
    val tags: List<String>
)

class MusicDownloadManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val pausedFlags = ConcurrentHashMap<String, Boolean>()
    private val tasks = ConcurrentHashMap<String, DownloadTask>()
    private val queueMutex = Mutex()

    private val _downloadItems = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloadItems: StateFlow<List<DownloadItem>> = _downloadItems.asStateFlow()

    private val _categoryPromptFlow = MutableSharedFlow<CategoryPromptData>(extraBufferCapacity = 5)
    val categoryPromptFlow: SharedFlow<CategoryPromptData> = _categoryPromptFlow.asSharedFlow()

    /**
     * Checks if a duplicate already exists in the local library:
     * - Same normalized title + artist
     * - Duration within 3 seconds (if duration > 0)
     */
    fun findDuplicateInLibrary(
        title: String,
        artist: String,
        durationSeconds: Int = 0
    ): Song? {
        val musicRepo = MusicRepository.getInstance(context)
        val allSongs = musicRepo.songs.value
        val normTitle = normalizeText(DownloadPathHelper.cleanName(title))
        val firstArtist = DownloadPathHelper.getFirstArtist(artist)
        val normArtist = normalizeText(firstArtist)

        return allSongs.firstOrNull { song ->
            val songNormTitle = normalizeText(DownloadPathHelper.cleanName(song.title))
            val songFirstArtist = normalizeText(DownloadPathHelper.getFirstArtist(song.artist))
            val isTitleMatch = songNormTitle == normTitle
            val isArtistMatch = songFirstArtist == normArtist ||
                    splitArtistNames(song.artist).any { normalizeText(it) == normArtist }

            if (isTitleMatch && isArtistMatch) {
                if (durationSeconds > 0 && song.durationSeconds > 0) {
                    kotlin.math.abs(song.durationSeconds - durationSeconds) <= 3
                } else {
                    true
                }
            } else {
                false
            }
        }
    }

    fun enqueueDownload(
        title: String,
        artist: String,
        album: String = "",
        streamUrl: String? = null,
        artworkUrl: String? = null,
        durationSeconds: Int = 0,
        tags: List<String> = emptyList(),
        onDuplicate: ((Song) -> Unit)? = null
    ): String? {
        // Requirement 6: Duplicate check before downloading
        val duplicate = findDuplicateInLibrary(title, artist, durationSeconds)
        if (duplicate != null) {
            scope.launch(Dispatchers.Main) {
                Toast.makeText(context, "Already in your library", Toast.LENGTH_SHORT).show()
            }
            onDuplicate?.invoke(duplicate)
            return null
        }

        // Avoid adding duplicate download task if already downloading or queued
        val existing = _downloadItems.value.find {
            it.title.equals(title, ignoreCase = true) &&
            it.artist.equals(artist, ignoreCase = true) &&
            (it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED)
        }
        if (existing != null) {
            return existing.id
        }

        val id = "dl_${System.currentTimeMillis()}_${(100..999).random()}"
        val task = DownloadTask(
            id = id,
            title = title,
            artist = artist,
            album = album,
            streamUrl = streamUrl,
            artworkUrl = artworkUrl,
            durationSeconds = durationSeconds,
            tags = tags
        )
        tasks[id] = task

        val newItem = DownloadItem(
            id = id,
            title = title,
            artist = artist,
            album = album,
            artworkUrl = artworkUrl,
            progress = 0f,
            totalSizeMb = 8.5f,
            downloadedMb = 0f,
            speedText = "Queued",
            status = DownloadStatus.QUEUED,
            isPaused = false,
            colorSeed = (0xFF000000..0xFFFFFFFF).random()
        )

        _downloadItems.update { it + newItem }

        processQueue()

        return id
    }

    private fun processQueue() {
        scope.launch {
            queueMutex.withLock {
                val maxParallel = SettingsRepository.current.maxParallelDownloads.coerceIn(1, 2)
                val currentRunning = activeJobs.size
                val availableSlots = maxParallel - currentRunning
                if (availableSlots <= 0) return@launch

                val itemsToStart = _downloadItems.value.filter { item ->
                    (item.status == DownloadStatus.QUEUED ||
                     (item.status == DownloadStatus.DOWNLOADING && !activeJobs.containsKey(item.id))) &&
                    !item.isPaused &&
                    pausedFlags[item.id] != true
                }.take(availableSlots)

                for (item in itemsToStart) {
                    if (activeJobs.containsKey(item.id)) continue

                    val task = tasks[item.id] ?: DownloadTask(
                        id = item.id,
                        title = item.title,
                        artist = item.artist,
                        album = item.album,
                        streamUrl = null,
                        artworkUrl = item.artworkUrl,
                        durationSeconds = 0,
                        tags = emptyList()
                    ).also { tasks[item.id] = it }

                    _downloadItems.update { list ->
                        list.map {
                            if (it.id == item.id) it.copy(
                                status = DownloadStatus.DOWNLOADING,
                                speedText = "Connecting..."
                            ) else it
                        }
                    }

                    startDownloadJob(task)
                }
            }
        }
    }

    private fun startDownloadJob(task: DownloadTask) {
        val id = task.id
        val job = scope.launch {
            try {
                var streamUrl = task.streamUrl
                var artworkUrl = task.artworkUrl

                if (streamUrl.isNullOrBlank()) {
                    _downloadItems.update { list ->
                        list.map { if (it.id == id) it.copy(speedText = "Resolving...") else it }
                    }
                    val resolved = CdnMusicService.searchTracks("${task.title} ${task.artist}", limit = 1).firstOrNull()
                    streamUrl = resolved?.downloadUrl
                    if (artworkUrl == null) artworkUrl = resolved?.artworkUrl
                    if (!streamUrl.isNullOrBlank()) {
                        tasks[id] = task.copy(streamUrl = streamUrl, artworkUrl = artworkUrl)
                    }
                }

                if (streamUrl.isNullOrBlank()) {
                    markFailed(id, "Could not resolve audio stream URL")
                    return@launch
                }

                executeDownloadWithRetry(id, task.title, task.artist, task.album, streamUrl, artworkUrl, task.tags)
            } catch (e: Exception) {
                if (e is CancellationException || pausedFlags[id] == true) {
                    _downloadItems.update { list ->
                        list.map { if (it.id == id) it.copy(isPaused = true, speedText = "Paused") else it }
                    }
                } else {
                    markFailed(id, e.message ?: "Download failed")
                }
            } finally {
                activeJobs.remove(id)
                processQueue()
            }
        }
        activeJobs[id] = job
    }

    private suspend fun executeDownloadWithRetry(
        id: String,
        title: String,
        artist: String,
        album: String,
        streamUrl: String,
        artworkUrl: String?,
        tags: List<String>
    ) {
        var attempt = 0
        val maxAttempts = 3
        var lastError: Exception? = null

        while (attempt < maxAttempts) {
            if (pausedFlags[id] == true) return
            attempt++
            try {
                executeDownload(id, title, artist, album, streamUrl, artworkUrl, tags)
                return
            } catch (e: Exception) {
                if (e is CancellationException || pausedFlags[id] == true) {
                    throw e
                }
                lastError = e
                if (attempt < maxAttempts) {
                    _downloadItems.update { list ->
                        list.map {
                            if (it.id == id) it.copy(speedText = "Retrying ($attempt/$maxAttempts)...") else it
                        }
                    }
                    delay(2000L * attempt)
                }
            }
        }
        val cleanMsg = when {
            lastError is java.net.UnknownHostException -> "Server connection/DNS error: ${lastError?.message ?: "Host unreachable"}"
            lastError is java.net.SocketTimeoutException -> "Connection timed out"
            else -> lastError?.message ?: "Download failed"
        }
        throw IOException(cleanMsg)
    }

    private suspend fun executeDownload(
        id: String,
        rawTitle: String,
        rawArtist: String,
        rawAlbum: String,
        streamUrl: String,
        artworkUrl: String?,
        tags: List<String>
    ) = withContext(Dispatchers.IO) {
        // 1. Clean Names & Fallbacks
        val safeTitle = DownloadPathHelper.cleanName(rawTitle).ifBlank { id }
        val safeArtist = DownloadPathHelper.cleanName(rawArtist).ifBlank { "Various Artists" }
        val safeAlbum = DownloadPathHelper.cleanName(rawAlbum).ifBlank { "Single" }

        val firstArtist = DownloadPathHelper.getFirstArtist(safeArtist)

        // 2. Resolve Category
        val resolvedCategory = DownloadPathHelper.resolveCategory(
            context = context,
            firstArtist = firstArtist,
            sourceTags = tags
        )

        // 3. Resolve Folder Structure & Relative Path
        val folderStructure = SettingsRepository.current.folderStructure
        val artistFolder = DownloadPathHelper.resolveArtistFolder(
            context = context,
            category = resolvedCategory,
            firstArtistClean = firstArtist,
            structure = folderStructure
        )
        val relativePath = DownloadPathHelper.buildRelativePath(
            category = resolvedCategory,
            artistFolder = artistFolder,
            structure = folderStructure
        )

        val extension = when {
            streamUrl.contains(".mp3", ignoreCase = true) -> "mp3"
            streamUrl.contains(".flac", ignoreCase = true) -> "flac"
            streamUrl.contains(".wav", ignoreCase = true) -> "wav"
            else -> "m4a"
        }

        // 4. Resolve Unique Display Name
        val uniqueDisplayName = DownloadPathHelper.resolveUniqueDisplayName(
            context = context,
            relativePath = relativePath,
            artistClean = safeArtist,
            titleClean = safeTitle,
            extension = extension
        )

        // 5. Download Stream to Cache File First
        val tempFile = File(context.cacheDir, "dl_temp_$id.$extension")
        val existingBytes = if (tempFile.exists()) tempFile.length() else 0L

        // Generate candidate URLs with automatic bitrate fallbacks if 320k is not hosted on CDN
        val candidateUrls = buildList {
            add(streamUrl)
            if (streamUrl.contains("_320.mp4")) {
                add(streamUrl.replace("_320.mp4", "_160.mp4"))
                add(streamUrl.replace("_320.mp4", "_96.mp4"))
            } else if (streamUrl.contains("_320.m4a")) {
                add(streamUrl.replace("_320.m4a", "_160.m4a"))
                add(streamUrl.replace("_320.m4a", "_96.m4a"))
            }
        }.distinct()

        var activeConn: HttpURLConnection? = null
        var isPartial = false
        var lastHttpCode = -1

        for (candidate in candidateUrls) {
            if (pausedFlags[id] == true) return@withContext
            var conn: HttpURLConnection? = null
            try {
                conn = (URL(candidate).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 30000
                    readTimeout = 45000
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
                    setRequestProperty("Accept", "*/*")
                    setRequestProperty("Connection", "keep-alive")
                    if (existingBytes > 0) {
                        setRequestProperty("Range", "bytes=$existingBytes-")
                    }
                }
                conn.connect()
                val code = conn.responseCode
                lastHttpCode = code
                if (code in 200..299) {
                    activeConn = conn
                    isPartial = (code == 206)
                    break
                } else if (code == 416) {
                    tempFile.delete()
                    conn.disconnect()
                    val retryConn = (URL(candidate).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 30000
                        readTimeout = 45000
                        setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
                        setRequestProperty("Accept", "*/*")
                        setRequestProperty("Connection", "keep-alive")
                    }
                    retryConn.connect()
                    val retryCode = retryConn.responseCode
                    lastHttpCode = retryCode
                    if (retryCode in 200..299) {
                        activeConn = retryConn
                        isPartial = false
                        break
                    } else {
                        retryConn.disconnect()
                    }
                } else {
                    conn.disconnect()
                }
            } catch (e: Exception) {
                conn?.disconnect()
                if (candidate == candidateUrls.last()) {
                    throw e
                }
            }
        }

        if (activeConn == null) {
            throw IOException(if (lastHttpCode > 0) "HTTP error $lastHttpCode" else "Unable to connect to audio CDN")
        }

        val conn = activeConn
        val remoteLength = conn.contentLengthLong
        val totalBytesExpected = if (isPartial) {
            existingBytes + remoteLength
        } else {
            remoteLength
        }
        val totalMb = if (totalBytesExpected > 0) totalBytesExpected / (1024f * 1024f) else 7.5f

        _downloadItems.update { list ->
            list.map {
                if (it.id == id) it.copy(totalSizeMb = totalMb, speedText = "Downloading...") else it
            }
        }

        var totalBytesRead = if (isPartial) existingBytes else 0L
        var lastUpdateTime = System.currentTimeMillis()
        var bytesSinceLastUpdate = 0L

        conn.inputStream.use { input ->
            FileOutputStream(tempFile, isPartial).use { output ->
                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    if (pausedFlags[id] == true) {
                        return@withContext
                    }
                    output.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    bytesSinceLastUpdate += bytesRead

                    val now = System.currentTimeMillis()
                    val timeDelta = now - lastUpdateTime
                    if (timeDelta >= 400) {
                        val speedBps = (bytesSinceLastUpdate * 1000f) / timeDelta
                        val speedMb = speedBps / (1024f * 1024f)
                        val speedStr = String.format(Locale.US, "%.1f MB/s", speedMb)

                        val downloadedMb = totalBytesRead / (1024f * 1024f)
                        val progress = if (totalBytesExpected > 0) {
                            (totalBytesRead.toFloat() / totalBytesExpected.toFloat()).coerceIn(0f, 1f)
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

        // 6. Download Artwork for ID3 Tagging
        var artworkFile: File? = null
        if (!artworkUrl.isNullOrBlank()) {
            try {
                val artTemp = File(context.cacheDir, "art_temp_$id.jpg")
                val artConn = URL(artworkUrl).openConnection() as HttpURLConnection
                artConn.connectTimeout = 15000
                artConn.readTimeout = 15000
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

        // 7. Write ID3 / Audio Tags directly into temp file before saving
        val commentText = "Source: Online (320kbps) | Category: $resolvedCategory | License: Personal Use"
        TagWriterHelper.writeDirectTagsToFile(
            file = tempFile,
            title = safeTitle,
            artist = safeArtist,
            album = safeAlbum,
            genre = resolvedCategory,
            comment = commentText,
            artworkFile = artworkFile
        )
        artworkFile?.delete()

        // 8. Save with MediaStore (Android 10+, IS_PENDING = 1 while writing, 0 when finished)
        var finalContentUri: Uri? = null
        var finalFilePath: String? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.TITLE, safeTitle)
                put(MediaStore.Audio.Media.ARTIST, safeArtist)
                put(MediaStore.Audio.Media.ALBUM, safeAlbum)
                put(MediaStore.Audio.Media.DISPLAY_NAME, uniqueDisplayName)
                put(MediaStore.Audio.Media.MIME_TYPE, if (extension == "mp3") "audio/mpeg" else "audio/mp4")
                put(MediaStore.Audio.Media.RELATIVE_PATH, relativePath)
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }

            val targetUri = context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
            if (targetUri != null) {
                finalContentUri = targetUri
                context.contentResolver.openOutputStream(targetUri)?.use { output ->
                    tempFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }

                // Finalize: IS_PENDING = 0
                val finishValues = ContentValues().apply {
                    put(MediaStore.Audio.Media.IS_PENDING, 0)
                }
                context.contentResolver.update(targetUri, finishValues, null, null)

                // Retrieve file path
                try {
                    val proj = arrayOf(MediaStore.Audio.Media.DATA)
                    context.contentResolver.query(targetUri, proj, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            finalFilePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
                        }
                    }
                } catch (_: Exception) {}
            }
        } else {
            // Android < 10 fallback
            val pubMusic = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            val sub = relativePath.removePrefix("Music/").removePrefix("Music")
            val targetDir = File(pubMusic, sub)
            targetDir.mkdirs()
            val destFile = File(targetDir, uniqueDisplayName)
            tempFile.copyTo(destFile, overwrite = true)
            finalFilePath = destFile.absolutePath
        }

        tempFile.delete()

        // 9. Trigger MediaScanner Connection & MusicRepository rescan so song appears in Library
        val resolvedPath = finalFilePath ?: File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            relativePath.removePrefix("Music/").removePrefix("Music") + uniqueDisplayName
        ).absolutePath

        MediaScannerConnection.scanFile(
            context,
            arrayOf(resolvedPath),
            null
        ) { _, _ ->
            MusicRepository.getInstance(context).rescan()
        }

        val finalMb = File(resolvedPath).let { if (it.exists()) it.length() / (1024f * 1024f) else totalMb }

        _downloadItems.update { list ->
            list.map {
                if (it.id == id) {
                    it.copy(
                        progress = 1.0f,
                        downloadedMb = finalMb,
                        totalSizeMb = finalMb,
                        speedText = "Completed",
                        status = DownloadStatus.COMPLETED,
                        filePath = resolvedPath
                    )
                } else it
            }
        }

        // 10. Check if category was "Other" and "Ask category when unknown" switch is enabled
        if (SettingsRepository.current.askCategoryWhenUnknown && resolvedCategory == "Other") {
            _categoryPromptFlow.emit(
                CategoryPromptData(
                    downloadId = id,
                    title = safeTitle,
                    artist = safeArtist,
                    contentUri = finalContentUri,
                    currentFilePath = resolvedPath,
                    currentCategory = resolvedCategory
                )
            )
        }
    }

    suspend fun applyCategoryChoice(
        promptData: CategoryPromptData,
        newCategory: String,
        rememberForArtist: Boolean
    ) = withContext(Dispatchers.IO) {
        if (rememberForArtist) {
            val statsDb = LocalStatsDatabase.getInstance(context)
            val normArtist = normalizeText(DownloadPathHelper.getFirstArtist(promptData.artist))
            statsDb.setArtistCategory(normArtist, newCategory)
        }

        val file = File(promptData.currentFilePath)
        if (file.exists()) {
            TagWriterHelper.writeDirectTagsToFile(
                file = file,
                title = promptData.title,
                artist = promptData.artist,
                album = "",
                genre = newCategory
            )

            val folderStructure = SettingsRepository.current.folderStructure
            if (folderStructure == DownloadFolderStructure.CATEGORY_ARTIST) {
                val pubMusic = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                val artistFolder = DownloadPathHelper.resolveArtistFolder(
                    context = context,
                    category = newCategory,
                    firstArtistClean = DownloadPathHelper.getFirstArtist(promptData.artist),
                    structure = folderStructure
                )
                val newDir = File(File(pubMusic, newCategory), artistFolder)
                newDir.mkdirs()
                val newFile = File(newDir, file.name)
                if (file.renameTo(newFile)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && promptData.contentUri != null) {
                        try {
                            val newRelPath = "Music/$newCategory/$artistFolder/"
                            val cv = ContentValues().apply {
                                put(MediaStore.Audio.Media.RELATIVE_PATH, newRelPath)
                                put(MediaStore.Audio.Media.GENRE, newCategory)
                            }
                            context.contentResolver.update(promptData.contentUri, cv, null, null)
                        } catch (_: Exception) {}
                    }
                    MediaScannerConnection.scanFile(context, arrayOf(newFile.absolutePath, file.absolutePath), null) { _, _ ->
                        MusicRepository.getInstance(context).rescan()
                    }
                    return@withContext
                }
            }

            MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null) { _, _ ->
                MusicRepository.getInstance(context).rescan()
            }
        }
    }

    fun pause(id: String) {
        pausedFlags[id] = true
        activeJobs.remove(id)?.cancel()
        _downloadItems.update { list ->
            list.map { if (it.id == id) it.copy(isPaused = true, speedText = "Paused") else it }
        }
        processQueue()
    }

    fun resume(id: String) {
        val item = _downloadItems.value.find { it.id == id } ?: return
        if (item.status == DownloadStatus.COMPLETED) return

        pausedFlags[id] = false
        val task = tasks[id]
        if (task != null && item.status == DownloadStatus.FAILED) {
            tasks[id] = task.copy(streamUrl = null)
        }

        _downloadItems.update { list ->
            list.map {
                if (it.id == id) it.copy(
                    isPaused = false,
                    status = DownloadStatus.QUEUED,
                    speedText = "Queued",
                    errorMessage = null,
                    progress = 0f,
                    downloadedMb = 0f
                ) else it
            }
        }
        processQueue()
    }

    fun pauseAll() {
        _downloadItems.value.filter {
            (it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED) && !it.isPaused
        }.forEach {
            pausedFlags[it.id] = true
            activeJobs.remove(it.id)?.cancel()
        }
        _downloadItems.update { list ->
            list.map {
                if ((it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED) && !it.isPaused) {
                    it.copy(isPaused = true, speedText = "Paused")
                } else it
            }
        }
        processQueue()
    }

    fun resumeAll() {
        _downloadItems.value.filter { it.isPaused || it.status == DownloadStatus.FAILED }.forEach {
            pausedFlags[it.id] = false
            val task = tasks[it.id]
            if (task != null && it.status == DownloadStatus.FAILED) {
                tasks[it.id] = task.copy(streamUrl = null)
            }
        }
        _downloadItems.update { list ->
            list.map {
                if (it.isPaused || it.status == DownloadStatus.FAILED) {
                    it.copy(
                        isPaused = false,
                        status = DownloadStatus.QUEUED,
                        speedText = "Queued",
                        errorMessage = null,
                        progress = 0f,
                        downloadedMb = 0f
                    )
                } else it
            }
        }
        processQueue()
    }

    fun cancel(id: String) {
        pausedFlags.remove(id)
        activeJobs.remove(id)?.cancel()
        tasks.remove(id)
        _downloadItems.update { list -> list.filterNot { it.id == id } }
        processQueue()
    }

    fun clearCompleted() {
        val completedIds = _downloadItems.value.filter { it.status == DownloadStatus.COMPLETED }.map { it.id }.toSet()
        completedIds.forEach { tasks.remove(it) }
        _downloadItems.update { list -> list.filterNot { it.id in completedIds } }
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
