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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.Normalizer

fun normalizeText(input: String): String {
    val decomposed = Normalizer.normalize(input, Normalizer.Form.NFD)
    val withoutAccents = decomposed.replace(Regex("\\p{M}"), "")
    var cleaned = withoutAccents.lowercase().trim().replace(Regex("\\s+"), " ")
    if (cleaned.endsWith(" - single")) {
        cleaned = cleaned.removeSuffix(" - single").trim()
    }
    if (cleaned.endsWith(" - ep")) {
        cleaned = cleaned.removeSuffix(" - ep").trim()
    }
    return cleaned
}

fun splitArtistNames(
    rawArtist: String,
    splitOnAmpersand: Boolean = false,
    dontSplitList: List<String> = emptyList()
): List<String> {
    if (rawArtist.isBlank()) return listOf("Unknown Artist")

    if (dontSplitList.any { it.equals(rawArtist.trim(), ignoreCase = true) }) {
        return listOf(rawArtist.trim())
    }

    var protectedText = rawArtist
    val replacements = mutableMapOf<String, String>()
    dontSplitList.forEachIndexed { index, name ->
        if (name.isNotBlank() && protectedText.contains(name, ignoreCase = true)) {
            val placeholder = "___PROTECTED_ARTIST_${index}___"
            val regex = Regex(Regex.escape(name), RegexOption.IGNORE_CASE)
            protectedText = regex.replace(protectedText, placeholder)
            replacements[placeholder] = name
        }
    }

    val pattern = if (splitOnAmpersand) {
        Regex("\\s*(?:;\\s*|/\\s*|\\s*,\\s*|\\s+(?:feat\\.?|ft\\.?|featuring|&)\\s+)\\s*", RegexOption.IGNORE_CASE)
    } else {
        Regex("\\s*(?:;\\s*|/\\s*|\\s*,\\s*|\\s+(?:feat\\.?|ft\\.?|featuring)\\s+)\\s*", RegexOption.IGNORE_CASE)
    }

    val tokens = protectedText.split(pattern)
        .map { token ->
            var restored = token.trim()
            replacements.forEach { (placeholder, original) ->
                restored = restored.replace(placeholder, original)
            }
            restored.trim()
        }
        .filter { it.isNotBlank() }

    return if (tokens.isEmpty()) listOf(rawArtist.trim()) else tokens.distinct()
}

private data class TrackRecord(
    val song: Song,
    val albumArtist: String?,
    val primaryArtist: String
)

/**
 * MusicRepository queries MediaStore.Audio.Media on Dispatchers.IO.
 * Automatically groups songs into Albums, Artists, Folders, and observes system audio changes.
 */
class MusicRepository private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
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
        // Register ContentObserver to detect new or removed audio files automatically
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
        val records = mutableListOf<TrackRecord>()
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
                add("album_artist")
            }
        }.toTypedArray()

        val selection = "(${MediaStore.Audio.Media.IS_MUSIC} != 0 OR ${MediaStore.Audio.Media.MIME_TYPE} LIKE 'audio/%') AND (${MediaStore.Audio.Media.DURATION} >= ? OR ${MediaStore.Audio.Media.DURATION} IS NULL)"
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
                val idCol = c.getColumnIndex(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = c.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                val artistIdCol = c.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID)
                val durationCol = c.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val dateAddedCol = c.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
                val trackCol = c.getColumnIndex(MediaStore.Audio.Media.TRACK)
                val yearCol = c.getColumnIndex(MediaStore.Audio.Media.YEAR)
                val sizeCol = c.getColumnIndex(MediaStore.Audio.Media.SIZE)
                val displayNameCol = c.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                val dataCol = c.getColumnIndex(MediaStore.Audio.Media.DATA)

                val relativePathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    c.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
                } else -1

                val genreCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    c.getColumnIndex(MediaStore.Audio.Media.GENRE)
                } else -1

                val albumArtistCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    c.getColumnIndex("album_artist")
                } else -1

                while (c.moveToNext()) {
                    val id = if (idCol >= 0) c.getLong(idCol) else 0L
                    if (id == 0L) continue

                    val rawTitle = if (titleCol >= 0) c.getString(titleCol) else null
                    val rawArtist = if (artistCol >= 0) c.getString(artistCol) else null
                    val rawAlbum = if (albumCol >= 0) c.getString(albumCol) else null
                    val albumId = if (albumIdCol >= 0) c.getLong(albumIdCol) else 0L
                    val artistId = if (artistIdCol >= 0) c.getLong(artistIdCol) else 0L
                    val durationMs = if (durationCol >= 0) c.getLong(durationCol) else 0L
                    val dateAdded = if (dateAddedCol >= 0) c.getLong(dateAddedCol) else 0L
                    val track = if (trackCol >= 0) c.getInt(trackCol) else 0
                    val year = if (yearCol >= 0) c.getInt(yearCol) else 0
                    val size = if (sizeCol >= 0) c.getLong(sizeCol) else 0L
                    val displayName = if (displayNameCol >= 0) c.getString(displayNameCol) ?: "Unknown" else "Unknown"
                    val filePath = if (dataCol >= 0) c.getString(dataCol) ?: "" else ""

                    val relativePath = if (relativePathCol >= 0) c.getString(relativePathCol) ?: "" else ""
                    val genre = if (genreCol >= 0) c.getString(genreCol) ?: "Pop" else "Pop"

                    val rawAlbumArtist = if (albumArtistCol >= 0) c.getString(albumArtistCol) else null
                    val effectiveAlbumArtist = if (!rawAlbumArtist.isNullOrBlank() && rawAlbumArtist != "<unknown>") rawAlbumArtist.trim() else null

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

                    val song = Song(
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

                    songList.add(song)

                    val primaryArtist = splitArtistNames(artist, splitOnAmpersand = false, dontSplitList = emptyList()).firstOrNull() ?: artist
                    records.add(TrackRecord(song = song, albumArtist = effectiveAlbumArtist, primaryArtist = primaryArtist))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Step 3: Album Grouping:
        // Key = normalize(albumTitle) + "|" + normalize(albumArtist)
        // If same album title has >= 3 distinct primary artists, group as "Various Artists"
        val titleToDistinctPrimaryArtists = records.groupBy { normalizeText(it.song.album) }
            .mapValues { (_, recs) ->
                recs.map { normalizeText(it.primaryArtist) }.distinct().size
            }

        val albumList = records.groupBy { rec ->
            val normTitle = normalizeText(rec.song.album)
            val isVarious = (titleToDistinctPrimaryArtists[normTitle] ?: 0) >= 3
            val candidateArtist = rec.albumArtist ?: rec.primaryArtist
            val finalAlbumArtist = if (isVarious) "Various Artists" else candidateArtist
            normTitle + "|" + normalizeText(finalAlbumArtist)
        }.map { (groupKey, groupRecords) ->
            val songsInAlbum = groupRecords.map { it.song }
            val first = songsInAlbum.first()
            val firstRecord = groupRecords.first()
            val normTitle = normalizeText(first.album)
            val isVarious = (titleToDistinctPrimaryArtists[normTitle] ?: 0) >= 3
            val albumTitle = songsInAlbum.firstOrNull { it.album.isNotBlank() && it.album != "Unknown Album" }?.album ?: first.album
            val albumArtist = if (isVarious) {
                "Various Artists"
            } else {
                groupRecords.mapNotNull { it.albumArtist }.firstOrNull { it.isNotBlank() } ?: firstRecord.primaryArtist
            }
            // Merge artwork: use first available artwork among grouped tracks
            val firstArtUri = songsInAlbum.firstOrNull { it.albumId > 0L }?.albumArtUri ?: first.albumArtUri
            val maxYear = songsInAlbum.map { it.year }.firstOrNull { it > 0 } ?: first.year

            Album(
                id = groupKey,
                title = albumTitle,
                artist = albumArtist,
                songCount = songsInAlbum.size,
                year = maxYear,
                colorSeed = first.colorSeed,
                albumId = first.albumId,
                customArtUri = firstArtUri
            )
        }.sortedBy { it.title.lowercase() }

        // Step 4: Artist Splitting
        val splitArtistsOnAmpersand = settings.splitArtistsOnAmpersand
        val dontSplitList = settings.dontSplitArtistNames

        val artistMap = mutableMapOf<String, MutableList<Song>>()

        records.forEach { rec ->
            val song = rec.song
            val splitNames = splitArtistNames(song.artist, splitArtistsOnAmpersand, dontSplitList)
            splitNames.forEach { name ->
                artistMap.getOrPut(name) { mutableListOf() }.add(song)
            }
        }

        val artistList = artistMap.map { (artistName, songsByArtist) ->
            val first = songsByArtist.first()
            val distinctAlbumCount = songsByArtist.map { normalizeText(it.album) }.distinct().size
            Artist(
                id = artistName.hashCode().toString(),
                name = artistName,
                songCount = songsByArtist.size,
                albumCount = distinctAlbumCount,
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
