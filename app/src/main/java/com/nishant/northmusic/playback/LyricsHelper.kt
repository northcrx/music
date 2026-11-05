package com.nishant.northmusic.playback

import android.content.Context
import android.util.Log
import com.nishant.northmusic.data.local.LocalStatsDatabase
import com.nishant.northmusic.data.model.LyricLine
import com.nishant.northmusic.data.model.Song
import com.nishant.northmusic.data.model.SongTagOverride
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.TagOptionSingleton
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern
import kotlin.math.abs

object LyricsHelper {

    private const val TAG = "LyricsHelper"
    // Matches one or more [mm:ss.xx] timestamps in an LRC line
    private val TIME_TAG_REGEX = Pattern.compile("""\[(\d{1,2}):(\d{1,2}(?:\.\d{1,3})?)\]""")

    // In-memory cache for fast lookups during playback session
    private val memoryCache = ConcurrentHashMap<String, List<LyricLine>>()

    suspend fun getLyricsForSong(context: Context, song: Song): List<LyricLine> = withContext(Dispatchers.IO) {
        // 0. Check memory cache first
        memoryCache[song.id]?.let { cached ->
            if (cached.isNotEmpty()) return@withContext cached
        }

        // 1. Check user override / cached lyrics in database
        try {
            val statsDb = LocalStatsDatabase.getInstance(context)
            val override = statsDb.getSongTagOverride(song.id)
            if (!override?.lyrics.isNullOrBlank()) {
                val parsed = parseLrc(override!!.lyrics!!)
                if (parsed.isNotEmpty()) {
                    memoryCache[song.id] = parsed
                    return@withContext parsed
                }
                val plain = parsePlainText(override.lyrics!!, song.durationSeconds)
                if (plain.isNotEmpty()) {
                    memoryCache[song.id] = plain
                    return@withContext plain
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error checking db lyrics override", e)
        }

        val isNetwork = song.filePath.startsWith("http://") || song.filePath.startsWith("https://")

        // 2. Check for companion .lrc file next to audio file (for local files)
        if (!isNetwork && song.filePath.isNotBlank()) {
            try {
                val lrcFile = File(song.filePath.substringBeforeLast('.') + ".lrc")
                if (lrcFile.exists() && lrcFile.canRead()) {
                    val content = lrcFile.readText()
                    val parsed = parseLrc(content)
                    if (parsed.isNotEmpty()) {
                        memoryCache[song.id] = parsed
                        return@withContext parsed
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error reading companion .lrc", e)
            }
        }

        // 3. Check embedded ID3/Vorbis/MP4 lyrics metadata tag using Jaudiotagger
        if (!isNetwork && song.filePath.isNotBlank()) {
            try {
                val file = File(song.filePath)
                if (file.exists() && file.canRead()) {
                    try {
                        TagOptionSingleton.getInstance().isAndroid = true
                    } catch (_: Throwable) {}
                    val audioFile = AudioFileIO.read(file)
                    val tag = audioFile.tag
                    val embeddedLyrics = tag?.getFirst(FieldKey.LYRICS)
                    if (!embeddedLyrics.isNullOrBlank()) {
                        val parsed = parseLrc(embeddedLyrics)
                        if (parsed.isNotEmpty()) {
                            memoryCache[song.id] = parsed
                            return@withContext parsed
                        } else {
                            val plain = parsePlainText(embeddedLyrics, song.durationSeconds)
                            if (plain.isNotEmpty()) {
                                memoryCache[song.id] = plain
                                return@withContext plain
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error reading embedded lyrics with Jaudiotagger", e)
            }
        }

        // 4. Online Fetch from LRCLIB with intelligent fallback queries
        try {
            val (onlineLyrics, rawLrc) = fetchFromLrclib(song.title, song.artist, song.durationSeconds)
            if (onlineLyrics.isNotEmpty()) {
                memoryCache[song.id] = onlineLyrics
                // Cache into db if we have raw text
                if (!rawLrc.isNullOrBlank()) {
                    try {
                        val statsDb = LocalStatsDatabase.getInstance(context)
                        val existing = statsDb.getSongTagOverride(song.id)
                        statsDb.saveSongTagOverride(
                            existing?.copy(lyrics = rawLrc) ?: SongTagOverride(songId = song.id, lyrics = rawLrc)
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Error caching online lyrics to database", e)
                    }
                }
                return@withContext onlineLyrics
            }
        } catch (e: Exception) {
            Log.w(TAG, "LRCLIB fetch failed", e)
        }

        emptyList()
    }

    /**
     * Searches LRCLIB with multiple fallback candidates to maximize match rate.
     * Always prioritizes true syncedLyrics ([mm:ss.xx]) over plain text lyrics.
     * Returns a Pair of (parsed LyricLine list, raw LRC or text content).
     */
    private fun fetchFromLrclib(rawTitle: String, rawArtist: String, durationSeconds: Int): Pair<List<LyricLine>, String?> {
        val cleanInfo = cleanSongInfo(rawTitle, rawArtist)
        if (cleanInfo.cleanTitle.isBlank() && cleanInfo.coreTitle.isBlank()) return Pair(emptyList(), null)

        val candidateQueries = generateCandidateQueries(cleanInfo)
        var fallbackPlain: Pair<List<LyricLine>, String?>? = null

        // Pass 1: Try exact GET /api/get for top candidates
        val getPairs = mutableListOf<Pair<String, String>>()
        if (cleanInfo.cleanTitle.isNotBlank() && cleanInfo.cleanArtist.isNotBlank()) {
            getPairs.add(Pair(cleanInfo.cleanTitle, cleanInfo.cleanArtist))
        }
        if (cleanInfo.coreTitle.isNotBlank() && cleanInfo.coreTitle != cleanInfo.cleanTitle && cleanInfo.cleanArtist.isNotBlank()) {
            getPairs.add(Pair(cleanInfo.coreTitle, cleanInfo.cleanArtist))
        }
        for (part in cleanInfo.splitParts) {
            if (part.isNotBlank() && cleanInfo.cleanArtist.isNotBlank()) {
                getPairs.add(Pair(part, cleanInfo.cleanArtist))
            }
        }

        for ((t, a) in getPairs.distinct()) {
            try {
                val getUrl = buildString {
                    append("https://lrclib.net/api/get?")
                    append("track_name=").append(URLEncoder.encode(t, "UTF-8"))
                    if (a.isNotBlank()) {
                        append("&artist_name=").append(URLEncoder.encode(a, "UTF-8"))
                    }
                    if (durationSeconds > 0) {
                        append("&duration=").append(durationSeconds)
                    }
                }
                val response = httpGet(getUrl)
                if (!response.isNullOrBlank()) {
                    val json = JSONObject(response)
                    val synced = json.optString("syncedLyrics", "").trim()
                    if (synced.isNotBlank()) {
                        val parsed = parseLrc(synced)
                        if (parsed.isNotEmpty()) return Pair(parsed, synced)
                    }
                    val plain = json.optString("plainLyrics", "").trim()
                    if (plain.isNotBlank() && fallbackPlain == null) {
                        fallbackPlain = Pair(parsePlainText(plain, durationSeconds), plain)
                    }
                }
            } catch (_: Exception) {}
        }

        // Pass 2: Search via GET /api/search?q=... through candidate queries
        for (query in candidateQueries) {
            if (query.isBlank()) continue
            try {
                val searchUrl = "https://lrclib.net/api/search?q=" + URLEncoder.encode(query, "UTF-8")
                val searchResponse = httpGet(searchUrl)
                if (!searchResponse.isNullOrBlank()) {
                    val array = JSONArray(searchResponse)
                    val count = minOf(array.length(), 10)

                    // First collect all synced lyrics options
                    val syncedCandidates = mutableListOf<Pair<String, Int>>() // Pair(syncedLyrics, durationSec)
                    for (i in 0 until count) {
                        val item = array.optJSONObject(i) ?: continue
                        val synced = item.optString("syncedLyrics", "").trim()
                        val dur = item.optInt("duration", 0)
                        if (synced.isNotBlank()) {
                            syncedCandidates.add(Pair(synced, dur))
                        } else if (fallbackPlain == null) {
                            val plain = item.optString("plainLyrics", "").trim()
                            if (plain.isNotBlank()) {
                                fallbackPlain = Pair(parsePlainText(plain, durationSeconds), plain)
                            }
                        }
                    }

                    if (syncedCandidates.isNotEmpty()) {
                        // Choose best match based on duration proximity if known
                        val best = if (durationSeconds > 0) {
                            syncedCandidates.minByOrNull { abs(it.second - durationSeconds) }?.first
                                ?: syncedCandidates.first().first
                        } else {
                            syncedCandidates.first().first
                        }
                        val parsed = parseLrc(best)
                        if (parsed.isNotEmpty()) return Pair(parsed, best)
                    }
                }
            } catch (_: Exception) {}
        }

        // Return plain lyrics fallback if no synced lyrics were found anywhere
        if (fallbackPlain != null && fallbackPlain.first.isNotEmpty()) {
            return fallbackPlain
        }

        return Pair(emptyList(), null)
    }

    private data class CleanInfo(
        val cleanTitle: String,
        val coreTitle: String,
        val cleanArtist: String,
        val splitParts: List<String>
    )

    private fun cleanSongInfo(title: String, artist: String): CleanInfo {
        // 1. Remove file extensions
        var t = title.replace(Regex("""(?i)\.(mp3|m4a|flac|wav|opus|aac|ogg)$"""), "").trim()
        // 2. Remove leading track numbering (e.g. "01. ", "01 - ", "01 ")
        t = t.replace(Regex("""^\s*\d{1,3}[\.\s\-_]+"""), "").trim()
        // 3. Remove everything after pipe "|" (often youtube video credits)
        t = t.replace(Regex("""\|.*$"""), "").trim()

        // 4. Strip common video/audio/tag markers
        val tagPattern = Regex("""(?i)\((?:from|feat|ft|official|lyric|lyrics|audio|video|visualizer|remix|slowed|reverb|prod|full song|cover|live|instrumental|hd|4k)[^\)]*\)""")
        val bracketTagPattern = Regex("""(?i)\[(?:from|feat|ft|official|lyric|lyrics|audio|video|visualizer|remix|slowed|reverb|prod|full song|cover|live|instrumental|hd|4k)[^\]]*\]""")
        val moviePattern = Regex("""(?i)\((?:[^\)]*(?:movie|soundtrack|version|single|ep|ost|album|remastered|deluxe)[^\)]*)\)""")
        val bracketMoviePattern = Regex("""(?i)\[(?:[^\]]*(?:movie|soundtrack|version|single|ep|ost|album|remastered|deluxe)[^\]]*)\]""")
        val suffixPattern = Regex("""(?i)\s*-\s*(?:official|lyric|video|audio|remix|remastered|slowed|single|ep|live).*$""")

        var clean = t.replace(tagPattern, "")
            .replace(bracketTagPattern, "")
            .replace(moviePattern, "")
            .replace(bracketMoviePattern, "")
            .replace(suffixPattern, "")
            .replace(Regex("""['"“”‘’]"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()

        // 5. Core title: strip any remaining parentheses/brackets completely
        var core = clean.replace(Regex("""\(.*?\)"""), "")
            .replace(Regex("""\[.*?\]"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()

        // 6. Split parts if formatted as "Artist - Title" or "Title - Movie"
        val splitParts = core.split(" - ").map { it.trim() }.filter { it.isNotBlank() }

        // 7. Clean artist
        var a = artist.replace(Regex("""(?i)\s*-\s*topic$"""), "").trim()
        a = a.replace(Regex("""(?i)\b(feat|ft|featuring)\b.*$"""), "").trim()
        val firstArtist = a.split(",", "&", "/", "•", ";").firstOrNull()?.trim() ?: a
        val cleanArtist = firstArtist.replace(Regex("""['"“”‘’]"""), "").trim()
        val finalArtist = if (cleanArtist.lowercase() in listOf("unknown", "unknown artist", "various artists", "<unknown>")) "" else cleanArtist

        return CleanInfo(
            cleanTitle = clean,
            coreTitle = core,
            cleanArtist = finalArtist,
            splitParts = splitParts
        )
    }

    private fun generateCandidateQueries(info: CleanInfo): List<String> {
        val list = mutableListOf<String>()

        // Split parts queries (handles "Ed Sheeran - Shape of You" or "Chaleya - Jawan")
        if (info.splitParts.size >= 2) {
            val p0 = info.splitParts[0]
            val p1 = info.splitParts[1]
            list.add("$p1 $p0")
            list.add("$p0 $p1")
            list.add(p0)
            list.add(p1)
        }

        // Core title + artist (most effective for Bollywood / pop)
        if (info.coreTitle.isNotBlank() && info.cleanArtist.isNotBlank()) {
            list.add("${info.coreTitle} ${info.cleanArtist}")
        }
        // Clean title + artist
        if (info.cleanTitle.isNotBlank() && info.cleanTitle != info.coreTitle && info.cleanArtist.isNotBlank()) {
            list.add("${info.cleanTitle} ${info.cleanArtist}")
        }
        // Core title alone
        if (info.coreTitle.isNotBlank()) {
            list.add(info.coreTitle)
        }
        // Clean title alone
        if (info.cleanTitle.isNotBlank() && info.cleanTitle != info.coreTitle) {
            list.add(info.cleanTitle)
        }

        return list.distinct()
    }

    fun parseLrc(content: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        content.lineSequence().forEach { rawLine ->
            val trimmed = rawLine.trim()
            if (trimmed.isBlank() || trimmed.startsWith("[ti:") || trimmed.startsWith("[ar:") ||
                trimmed.startsWith("[al:") || trimmed.startsWith("[by:") || trimmed.startsWith("[offset:")) {
                return@forEach
            }

            val matcher = TIME_TAG_REGEX.matcher(trimmed)
            val timestamps = mutableListOf<Float>()
            var lastEnd = 0

            while (matcher.find()) {
                val minStr = matcher.group(1) ?: "0"
                val secStr = matcher.group(2) ?: "0"
                val minutes = minStr.toIntOrNull() ?: 0
                val seconds = secStr.toFloatOrNull() ?: 0f
                val totalSec = minutes * 60f + seconds
                timestamps.add(totalSec)
                lastEnd = matcher.end()
            }

            if (timestamps.isNotEmpty()) {
                val text = trimmed.substring(lastEnd).trim()
                if (text.isNotBlank()) {
                    for (ts in timestamps) {
                        lines.add(LyricLine(timestampSeconds = ts, text = text, timeSeconds = ts.toInt()))
                    }
                }
            }
        }
        return lines.sortedBy { it.timestampSeconds }
    }

    fun parsePlainText(text: String, durationSeconds: Int): List<LyricLine> {
        val rawLines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (rawLines.isEmpty()) return emptyList()

        val step = if (durationSeconds > 0) (durationSeconds.toFloat() / rawLines.size).coerceAtLeast(3f) else 4f
        return rawLines.mapIndexed { index, line ->
            val ts = index * step
            LyricLine(timestampSeconds = ts, text = line, timeSeconds = ts.toInt())
        }
    }

    private fun httpGet(urlString: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 7000
            conn.readTimeout = 7000
            conn.setRequestProperty("User-Agent", "NorthMusic/0.2 (Android; Nishant)")
            conn.setRequestProperty("Accept", "application/json")
            conn.connect()
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else null
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }
}
