package com.nishant.northmusic.data.repository

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.nishant.northmusic.data.local.LocalStatsDatabase
import com.nishant.northmusic.data.model.Album
import com.nishant.northmusic.data.model.Artist
import com.nishant.northmusic.data.model.FolderItem
import com.nishant.northmusic.data.model.Song
import com.nishant.northmusic.util.PermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Repository responsible for indexing local media via MediaStore.
 * - Queries MediaStore.Audio.Media on Dispatchers.IO
 * - Groups into Songs, Albums, Artists, Folders
 * - Observes external content changes via ContentObserver
 * - Integrates with LocalStatsDatabase for persistent favorites and play counts
 */
class MusicRepository private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val statsDb = LocalStatsDatabase.getInstance(context)

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    private val _artists = MutableStateFlow<List<Artist>>(emptyList())
    val artists: StateFlow<List<Artist>> = _artists.asStateFlow()

    private val _folders = MutableStateFlow<List<FolderItem>>(emptyList())
    val folders: StateFlow<List<FolderItem>> = _folders.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private var scanJob: Job? = null

    init {
        // Register ContentObserver to automatically detect when music files are added/deleted
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                rescan()
            }
        }
        try {
            context.contentResolver.registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true,
                observer
            )
        } catch (_: Exception) {}

        // Listen for stats version changes (e.g. favorite toggled)
        scope.launch {
            statsDb.statsVersion.collect {
                updateFavoritesInCurrentSongs()
            }
        }

        // Initial scan
        rescan()
    }

    fun rescan() {
        if (!PermissionUtils.hasAudioPermission(context)) {
            _songs.value = emptyList()
            _albums.value = emptyList()
            _artists.value = emptyList()
            _folders.value = emptyList()
            return
        }

        scanJob?.cancel()
        scanJob = scope.launch {
            _isScanning.value = true
            queryMediaStore()
            _isScanning.value = false
        }
    }

    suspend fun toggleFavorite(songId: String): Boolean {
        val newFav = statsDb.toggleFavorite(songId)
        _songs.value = _songs.value.map {
            if (it.id == songId) it.copy(isFavorite = newFav) else it
        }
        return newFav
    }

    private fun updateFavoritesInCurrentSongs() {
        val favs = statsDb.favoritesFlow.value
        val currentList = _songs.value
        if (currentList.isEmpty()) return
        val updated = currentList.map { song ->
            val isFav = favs.contains(song.id)
            if (song.isFavorite != isFav) song.copy(isFavorite = isFav) else song
        }
        _songs.value = updated
    }

    private suspend fun queryMediaStore() = withContext(Dispatchers.IO) {
        val songList = mutableListOf<Song>()
        val favorites = statsDb.favoritesFlow.value
        val settings = SettingsRepository.current
        val minDurationMs = if (settings.ignoreShortTracks) settings.minSongDurationSeconds * 1000L else 0L
        val blacklist = settings.foldersBlacklist.map { it.trim().lowercase() }.filter { it.isNotBlank() }

        val projection = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATA
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Audio.Media.RELATIVE_PATH)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(MediaStore.Audio.Media.GENRE)
            }
        }.toTypedArray()

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(minDurationMs.toString())
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val artistIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
                val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dateAddedCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val trackCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val yearCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val sizeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val displayNameCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                val relativePathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    c.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
                } else -1

                val genreCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    c.getColumnIndex(MediaStore.Audio.Media.GENRE)
                } else -1

                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val rawTitle = c.getString(titleCol)
                    val rawArtist = c.getString(artistCol)
                    val rawAlbum = c.getString(albumCol)
                    val albumId = c.getLong(albumIdCol)
                    val artistId = c.getLong(artistIdCol)
                    val durationMs = c.getLong(durationCol)
                    val dateAdded = c.getLong(dateAddedCol)
                    val track = c.getInt(trackCol)
                    val year = c.getInt(yearCol)
                    val size = c.getLong(sizeCol)
                    val displayName = c.getString(displayNameCol) ?: "Unknown"
                    val filePath = c.getString(dataCol) ?: ""

                    val relativePath = if (relativePathCol >= 0) c.getString(relativePathCol) ?: "" else ""
                    val genre = if (genreCol >= 0) c.getString(genreCol) ?: "Pop" else "Pop"

                    // Check blacklist
                    val lowerPath = filePath.lowercase()
                    if (blacklist.any { lowerPath.contains(it) }) {
                        continue
                    }

                    val title = if (!rawTitle.isNullOrBlank() && rawTitle != "<unknown>") rawTitle else displayName.substringBeforeLast('.')
                    val artist = if (!rawArtist.isNullOrBlank() && rawArtist != "<unknown>") rawArtist else "Unknown Artist"
                    val album = if (!rawAlbum.isNullOrBlank() && rawAlbum != "<unknown>") rawAlbum else "Unknown Album"

                    val folderName = if (relativePath.isNotBlank()) {
                        relativePath.trim('/').substringAfterLast('/')
                    } else if (filePath.isNotBlank()) {
                        File(filePath).parentFile?.name ?: "Music"
                    } else "Music"

                    // Deterministic color seed for UI accent fallback
                    val colorSeed = (title.hashCode().toLong() and 0xFFFFFFL) or 0xFF000000L

                    songList.add(
                        Song(
                            id = id.toString(),
                            title = title,
                            artist = artist,
                            album = album,
                            albumId = albumId,
                            artistId = artistId,
                            durationSeconds = (durationMs / 1000).toInt(),
                            colorSeed = colorSeed,
                            isFavorite = favorites.contains(id.toString()),
                            isHighQuality = true,
                            genre = genre,
                            filePath = filePath,
                            relativeFolder = folderName,
                            dateAdded = dateAdded,
                            year = year,
                            trackNumber = track,
                            sizeBytes = size
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Group into Albums
        val albumList = songList.groupBy { it.albumId to it.album }.map { (key, songsInAlbum) ->
            val first = songsInAlbum.first()
            Album(
                id = key.first.toString(),
                title = key.second,
                artist = first.artist,
                songCount = songsInAlbum.size,
                year = first.year,
                colorSeed = first.colorSeed,
                albumId = key.first
            )
        }.sortedBy { it.title.lowercase() }

        // Group into Artists
        val artistList = songList.groupBy { it.artist }.map { (artistName, songsByArtist) ->
            val first = songsByArtist.first()
            val albumCount = songsByArtist.map { it.albumId }.distinct().size
            Artist(
                id = first.artistId.toString(),
                name = artistName,
                songCount = songsByArtist.size,
                albumCount = albumCount,
                colorSeed = first.colorSeed
            )
        }.sortedBy { it.name.lowercase() }

        // Group into Folders
        val folderList = songList.groupBy { it.relativeFolder }.map { (folderName, songsInFolder) ->
            FolderItem(
                name = folderName.ifBlank { "Music" },
                path = songsInFolder.firstOrNull()?.filePath?.substringBeforeLast(File.separator) ?: "/storage/emulated/0/Music",
                songCount = songsInFolder.size
            )
        }.sortedBy { it.name.lowercase() }

        _songs.value = songList
        _albums.value = albumList
        _artists.value = artistList
        _folders.value = folderList
    }

    companion object {
        @Volatile
        private var instance: MusicRepository? = null

        fun getInstance(context: Context): MusicRepository {
            return instance ?: synchronized(this) {
                instance ?: MusicRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
