package com.nishant.northmusic.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Local lightweight SQLite database storing user stats:
 * - Favorites
 * - Play counts
 * - History / timestamps
 * - Last played position
 *
 * Backed by Dispatchers.IO and reactive StateFlows for seamless UI updates.
 */
class LocalStatsDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _favoritesFlow = MutableStateFlow<Set<String>>(emptySet())
    val favoritesFlow: StateFlow<Set<String>> = _favoritesFlow.asStateFlow()

    private val _statsVersion = MutableStateFlow(0L)
    val statsVersion: StateFlow<Long> = _statsVersion.asStateFlow()

    init {
        scope.launch {
            loadInitialFavorites()
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
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Safe migration if needed
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

        // Insert into history
        val historyCv = ContentValues().apply {
            put("song_id", songId)
            put("timestamp", now)
        }
        writableDatabase.insert("playback_history", null, historyCv)
        _statsVersion.value = now
    }

    suspend fun updateLastPosition(songId: String, positionSeconds: Int) = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply {
            put("song_id", songId)
            put("last_position_seconds", positionSeconds)
        }
        val rows = writableDatabase.update("song_stats", cv, "song_id = ?", arrayOf(songId))
        if (rows == 0) {
            writableDatabase.insert("song_stats", null, cv)
        }
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

    suspend fun getLastPlayedTrack(): Pair<String, Int>? = withContext(Dispatchers.IO) {
        var result: Pair<String, Int>? = null
        try {
            val cursor = readableDatabase.query(
                "song_stats",
                arrayOf("song_id", "last_position_seconds"),
                "last_played_time > 0",
                null,
                null,
                null,
                "last_played_time DESC",
                "1"
            )
            cursor.use {
                if (it.moveToFirst()) {
                    val songId = it.getString(0)
                    val pos = it.getInt(1)
                    result = Pair(songId, pos)
                }
            }
        } catch (_: Exception) {}
        result
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

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        writableDatabase.delete("playback_history", null, null)
        _statsVersion.value = System.currentTimeMillis()
    }

    companion object {
        private const val DATABASE_NAME = "north_music_stats.db"
        private const val DATABASE_VERSION = 1

        @Volatile
        private var instance: LocalStatsDatabase? = null

        fun getInstance(context: Context): LocalStatsDatabase {
            return instance ?: synchronized(this) {
                instance ?: LocalStatsDatabase(context.applicationContext).also { instance = it }
            }
        }
    }
}
