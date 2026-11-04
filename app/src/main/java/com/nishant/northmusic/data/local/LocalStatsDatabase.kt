package com.nishant.northmusic.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.nishant.northmusic.data.model.Playlist
import com.nishant.northmusic.data.model.SongTagOverride
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Local lightweight SQLite database storing user stats:
 * - Favorites
 * - Play counts
 * - History / timestamps
 * - Last played position
 * - Artist images (Deezer / custom)
 * - Album artwork (iTunes / custom)
 * - Song tag overrides
 * - User custom playlists
 *
 * Backed by Dispatchers.IO and reactive StateFlows for seamless UI updates.
 */
class LocalStatsDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _favoritesFlow = MutableStateFlow<Set<String>>(emptySet())
    val favoritesFlow: StateFlow<Set<String>> = _favoritesFlow.asStateFlow()

    private val _artistImagesFlow = MutableStateFlow<Map<String, String>>(emptyMap())
    val artistImagesFlow: StateFlow<Map<String, String>> = _artistImagesFlow.asStateFlow()

    private val _albumImagesFlow = MutableStateFlow<Map<String, String>>(emptyMap())
    val albumImagesFlow: StateFlow<Map<String, String>> = _albumImagesFlow.asStateFlow()

    private val _tagOverridesFlow = MutableStateFlow<Map<String, SongTagOverride>>(emptyMap())
    val tagOverridesFlow: StateFlow<Map<String, SongTagOverride>> = _tagOverridesFlow.asStateFlow()

    private val _userPlaylistsFlow = MutableStateFlow<List<Playlist>>(emptyList())
    val userPlaylistsFlow: StateFlow<List<Playlist>> = _userPlaylistsFlow.asStateFlow()

    private val _statsVersion = MutableStateFlow(0L)
    val statsVersion: StateFlow<Long> = _statsVersion.asStateFlow()

    init {
        scope.launch {
            loadInitialFavorites()
            loadAllCachedData()
            loadUserPlaylists()
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS song_stats (
                song_id TEXT PRIMARY KEY,
                is_favorite INTEGER NOT NULL DEFAULT 0,
                play_count INTEGER NOT NULL DEFAULT 0,
                last_played_time INTEGER NOT NULL DEFAULT 0,
                last_position_seconds INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS playback_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                song_id TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_history_time ON playback_history(timestamp DESC)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS artist_images (
                artist_name TEXT PRIMARY KEY,
                image_path TEXT,
                source TEXT,
                last_updated INTEGER
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS album_images (
                album_key TEXT PRIMARY KEY,
                image_path TEXT,
                source TEXT,
                last_updated INTEGER
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS song_tag_overrides (
                song_id TEXT PRIMARY KEY,
                title TEXT,
                artist TEXT,
                album TEXT,
                album_artist TEXT,
                composer TEXT,
                genre TEXT,
                year INTEGER,
                track INTEGER,
                disc INTEGER,
                lyrics TEXT,
                comment TEXT,
                artwork_path TEXT
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS user_playlists (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS playlist_songs (
                playlist_id TEXT NOT NULL,
                song_id TEXT NOT NULL,
                added_at INTEGER NOT NULL,
                PRIMARY KEY (playlist_id, song_id)
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS artist_images (artist_name TEXT PRIMARY KEY, image_path TEXT, source TEXT, last_updated INTEGER)")
            db.execSQL("CREATE TABLE IF NOT EXISTS album_images (album_key TEXT PRIMARY KEY, image_path TEXT, source TEXT, last_updated INTEGER)")
            db.execSQL("CREATE TABLE IF NOT EXISTS song_tag_overrides (song_id TEXT PRIMARY KEY, title TEXT, artist TEXT, album TEXT, album_artist TEXT, composer TEXT, genre TEXT, year INTEGER, track INTEGER, disc INTEGER, lyrics TEXT, comment TEXT, artwork_path TEXT)")
        }
        if (oldVersion < 3) {
            db.execSQL("CREATE TABLE IF NOT EXISTS user_playlists (id TEXT PRIMARY KEY, title TEXT NOT NULL, created_at INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS playlist_songs (playlist_id TEXT NOT NULL, song_id TEXT NOT NULL, added_at INTEGER NOT NULL, PRIMARY KEY (playlist_id, song_id))")
        }
    }

    private fun loadInitialFavorites() {
        val favs = mutableSetOf<String>()
        try {
            val cursor = readableDatabase.query(
                "song_stats",
                arrayOf("song_id"),
                "is_favorite = 1",
                null,
                null,
                null,
                null
            )
            cursor.use {
                while (it.moveToNext()) {
                    favs.add(it.getString(0))
                }
            }
        } catch (_: Exception) {}
        _favoritesFlow.value = favs
    }

    private fun loadAllCachedData() {
        try {
            val artistMap = mutableMapOf<String, String>()
            readableDatabase.query("artist_images", arrayOf("artist_name", "image_path"), null, null, null, null, null).use { c ->
                while (c.moveToNext()) {
                    val name = c.getString(0)
                    val path = c.getString(1)
                    if (!name.isNullOrBlank() && !path.isNullOrBlank()) {
                        artistMap[name] = path
                    }
                }
            }
            _artistImagesFlow.value = artistMap

            val albumMap = mutableMapOf<String, String>()
            readableDatabase.query("album_images", arrayOf("album_key", "image_path"), null, null, null, null, null).use { c ->
                while (c.moveToNext()) {
                    val key = c.getString(0)
                    val path = c.getString(1)
                    if (!key.isNullOrBlank() && !path.isNullOrBlank()) {
                        albumMap[key] = path
                    }
                }
            }
            _albumImagesFlow.value = albumMap

            _tagOverridesFlow.value = getAllSongTagOverridesInternal()
        } catch (_: Exception) {}
    }

    private fun loadUserPlaylists() {
        val playlists = mutableListOf<Playlist>()
        try {
            val cursor = readableDatabase.rawQuery(
                """
                SELECT p.id, p.title, COUNT(s.song_id) as count
                FROM user_playlists p
                LEFT JOIN playlist_songs s ON p.id = s.playlist_id
                GROUP BY p.id, p.title
                ORDER BY p.created_at ASC
                """.trimIndent(),
                null
            )
            cursor.use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(0)
                    val title = c.getString(1)
                    val count = c.getInt(2)
                    playlists.add(
                        Playlist(
                            id = id,
                            title = title,
                            songCount = count,
                            colorSeed = (title.hashCode().toLong() and 0xFFFFFFL) or 0xFF000000L
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        _userPlaylistsFlow.value = playlists
    }

    suspend fun toggleFavorite(songId: String): Boolean = withContext(Dispatchers.IO) {
        val isFav = !isFavorite(songId)
        val cv = ContentValues().apply {
            put("song_id", songId)
            put("is_favorite", if (isFav) 1 else 0)
        }
        writableDatabase.insertWithOnConflict("song_stats", null, cv, SQLiteDatabase.CONFLICT_IGNORE)
        writableDatabase.update("song_stats", cv, "song_id = ?", arrayOf(songId))

        val current = _favoritesFlow.value.toMutableSet()
        if (isFav) current.add(songId) else current.remove(songId)
        _favoritesFlow.value = current
        _statsVersion.value = System.currentTimeMillis()
        isFav
    }

    fun isFavorite(songId: String): Boolean {
        return _favoritesFlow.value.contains(songId)
    }

    suspend fun recordSongPlay(songId: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val currentCount = getPlayCount(songId)
        val cv = ContentValues().apply {
            put("song_id", songId)
            put("play_count", currentCount + 1)
            put("last_played_time", now)
        }
        val rows = writableDatabase.update("song_stats", cv, "song_id = ?", arrayOf(songId))
        if (rows == 0) {
            writableDatabase.insert("song_stats", null, cv)
        }

        val historyCv = ContentValues().apply {
            put("song_id", songId)
            put("timestamp", now)
        }
        writableDatabase.insert("playback_history", null, historyCv)
        _statsVersion.value = now
    }

    suspend fun saveLastPosition(songId: String, positionSeconds: Int) = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply {
            put("song_id", songId)
            put("last_position_seconds", positionSeconds)
        }
        val rows = writableDatabase.update("song_stats", cv, "song_id = ?", arrayOf(songId))
        if (rows == 0) {
            writableDatabase.insert("song_stats", null, cv)
        }
    }

    suspend fun updateLastPosition(songId: String, positionSeconds: Int) = saveLastPosition(songId, positionSeconds)

    fun getLastPosition(songId: String): Int {
        var pos = 0
        try {
            val cursor = readableDatabase.query(
                "song_stats",
                arrayOf("last_position_seconds"),
                "song_id = ?",
                arrayOf(songId),
                null,
                null,
                null
            )
            cursor.use {
                if (it.moveToFirst()) {
                    pos = it.getInt(0)
                }
            }
        } catch (_: Exception) {}
        return pos
    }

    fun getPlayCount(songId: String): Int {
        var count = 0
        try {
            val cursor = readableDatabase.query(
                "song_stats",
                arrayOf("play_count"),
                "song_id = ?",
                arrayOf(songId),
                null,
                null,
                null
            )
            cursor.use {
                if (it.moveToFirst()) {
                    count = it.getInt(0)
                }
            }
        } catch (_: Exception) {}
        return count
    }

    suspend fun getRecentlyPlayed(limit: Int = 50): List<String> = getHistoryTrackIds(limit)

    suspend fun getHistoryTrackIds(limit: Int = 50): List<String> = withContext(Dispatchers.IO) {
        val list = mutableListOf<String>()
        try {
            val cursor = readableDatabase.query(
                "playback_history",
                arrayOf("DISTINCT song_id"),
                null,
                null,
                null,
                null,
                "timestamp DESC",
                limit.toString()
            )
            cursor.use {
                while (it.moveToNext()) {
                    list.add(it.getString(0))
                }
            }
        } catch (_: Exception) {}
        list
    }

    suspend fun getTopTrackIds(limit: Int = 50): List<String> = withContext(Dispatchers.IO) {
        val list = mutableListOf<String>()
        try {
            val cursor = readableDatabase.query(
                "song_stats",
                arrayOf("song_id"),
                "play_count > 0",
                null,
                null,
                null,
                "play_count DESC",
                limit.toString()
            )
            cursor.use {
                while (it.moveToNext()) {
                    list.add(it.getString(0))
                }
            }
        } catch (_: Exception) {}
        list
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        writableDatabase.delete("playback_history", null, null)
        _statsVersion.value = System.currentTimeMillis()
    }

    // Artist Images
    suspend fun saveArtistImage(artistName: String, path: String, source: String = "deezer") = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply {
            put("artist_name", artistName)
            put("image_path", path)
            put("source", source)
            put("last_updated", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict("artist_images", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        val m = _artistImagesFlow.value.toMutableMap()
        m[artistName] = path
        _artistImagesFlow.value = m
    }

    fun getArtistImage(artistName: String): String? {
        return _artistImagesFlow.value[artistName]
    }

    fun getArtistImageRecord(artistName: String): Pair<String?, Long>? {
        try {
            val cursor = readableDatabase.query(
                "artist_images",
                arrayOf("image_path", "last_updated"),
                "artist_name = ?",
                arrayOf(artistName),
                null,
                null,
                null
            )
            cursor.use {
                if (it.moveToFirst()) {
                    val path = it.getString(0)
                    val updated = it.getLong(1)
                    return Pair(path, updated)
                }
            }
        } catch (_: Exception) {}
        return null
    }

    suspend fun removeArtistImage(artistName: String) = withContext(Dispatchers.IO) {
        writableDatabase.delete("artist_images", "artist_name = ?", arrayOf(artistName))
        val m = _artistImagesFlow.value.toMutableMap()
        m.remove(artistName)
        _artistImagesFlow.value = m
    }

    // Album Images
    suspend fun saveAlbumImage(albumKey: String, path: String, source: String = "itunes") = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply {
            put("album_key", albumKey)
            put("image_path", path)
            put("source", source)
            put("last_updated", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict("album_images", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        val m = _albumImagesFlow.value.toMutableMap()
        m[albumKey] = path
        _albumImagesFlow.value = m
    }

    fun getAlbumImage(albumKey: String): String? {
        return _albumImagesFlow.value[albumKey]
    }

    fun getAlbumImageRecord(albumKey: String): Pair<String?, Long>? {
        try {
            val cursor = readableDatabase.query(
                "album_images",
                arrayOf("image_path", "last_updated"),
                "album_key = ?",
                arrayOf(albumKey),
                null,
                null,
                null
            )
            cursor.use {
                if (it.moveToFirst()) {
                    val path = it.getString(0)
                    val updated = it.getLong(1)
                    return Pair(path, updated)
                }
            }
        } catch (_: Exception) {}
        return null
    }

    // Song Tag Overrides
    suspend fun saveSongTagOverride(override: SongTagOverride) = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply {
            put("song_id", override.songId)
            put("title", override.title)
            put("artist", override.artist)
            put("album", override.album)
            put("album_artist", override.albumArtist)
            put("composer", override.composer)
            put("genre", override.genre)
            put("year", override.year)
            put("track", override.track)
            put("disc", override.disc)
            put("lyrics", override.lyrics)
            put("comment", override.comment)
            put("artwork_path", override.artworkPath)
        }
        writableDatabase.insertWithOnConflict("song_tag_overrides", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        val m = _tagOverridesFlow.value.toMutableMap()
        m[override.songId] = override
        _tagOverridesFlow.value = m
        _statsVersion.value = System.currentTimeMillis()
    }

    private fun getAllSongTagOverridesInternal(): Map<String, SongTagOverride> {
        val map = mutableMapOf<String, SongTagOverride>()
        try {
            readableDatabase.query("song_tag_overrides", null, null, null, null, null, null).use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(c.getColumnIndexOrThrow("song_id"))
                    map[id] = SongTagOverride(
                        songId = id,
                        title = c.getString(c.getColumnIndexOrThrow("title")),
                        artist = c.getString(c.getColumnIndexOrThrow("artist")),
                        album = c.getString(c.getColumnIndexOrThrow("album")),
                        albumArtist = c.getString(c.getColumnIndexOrThrow("album_artist")),
                        composer = c.getString(c.getColumnIndexOrThrow("composer")),
                        genre = c.getString(c.getColumnIndexOrThrow("genre")),
                        year = if (!c.isNull(c.getColumnIndexOrThrow("year"))) c.getInt(c.getColumnIndexOrThrow("year")) else null,
                        track = if (!c.isNull(c.getColumnIndexOrThrow("track"))) c.getInt(c.getColumnIndexOrThrow("track")) else null,
                        disc = if (!c.isNull(c.getColumnIndexOrThrow("disc"))) c.getInt(c.getColumnIndexOrThrow("disc")) else null,
                        lyrics = c.getString(c.getColumnIndexOrThrow("lyrics")),
                        comment = c.getString(c.getColumnIndexOrThrow("comment")),
                        artworkPath = c.getString(c.getColumnIndexOrThrow("artwork_path"))
                    )
                }
            }
        } catch (_: Exception) {}
        return map
    }

    suspend fun getAllSongTagOverrides(): Map<String, SongTagOverride> = withContext(Dispatchers.IO) {
        getAllSongTagOverridesInternal()
    }

    suspend fun getSongTagOverride(songId: String): SongTagOverride? = withContext(Dispatchers.IO) {
        _tagOverridesFlow.value[songId] ?: getAllSongTagOverridesInternal()[songId]
    }

    // User Playlists
    suspend fun getUserPlaylists(): List<Playlist> = withContext(Dispatchers.IO) {
        _userPlaylistsFlow.value
    }

    suspend fun createPlaylist(title: String): Playlist = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val cv = ContentValues().apply {
            put("id", id)
            put("title", title.trim())
            put("created_at", now)
        }
        writableDatabase.insert("user_playlists", null, cv)
        val playlist = Playlist(
            id = id,
            title = title.trim(),
            songCount = 0,
            colorSeed = (title.hashCode().toLong() and 0xFFFFFFL) or 0xFF000000L
        )
        loadUserPlaylists()
        playlist
    }

    suspend fun addSongToPlaylist(playlistId: String, songId: String) = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply {
            put("playlist_id", playlistId)
            put("song_id", songId)
            put("added_at", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict("playlist_songs", null, cv, SQLiteDatabase.CONFLICT_IGNORE)
        loadUserPlaylists()
    }

    suspend fun getPlaylistSongIds(playlistId: String): List<String> = withContext(Dispatchers.IO) {
        val list = mutableListOf<String>()
        try {
            val cursor = readableDatabase.query(
                "playlist_songs",
                arrayOf("song_id"),
                "playlist_id = ?",
                arrayOf(playlistId),
                null,
                null,
                "added_at ASC"
            )
            cursor.use {
                while (it.moveToNext()) {
                    list.add(it.getString(0))
                }
            }
        } catch (_: Exception) {}
        list
    }

    suspend fun deletePlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        writableDatabase.delete("playlist_songs", "playlist_id = ?", arrayOf(playlistId))
        writableDatabase.delete("user_playlists", "id = ?", arrayOf(playlistId))
        loadUserPlaylists()
    }

    companion object {
        private const val DATABASE_NAME = "north_music_stats.db"
        private const val DATABASE_VERSION = 3

        @Volatile
        private var instance: LocalStatsDatabase? = null

        fun getInstance(context: Context): LocalStatsDatabase {
            return instance ?: synchronized(this) {
                instance ?: LocalStatsDatabase(context.applicationContext).also { instance = it }
            }
        }
    }
}
