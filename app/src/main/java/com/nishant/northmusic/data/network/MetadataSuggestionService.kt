package com.nishant.northmusic.data.network

import android.content.Context
import com.nishant.northmusic.data.local.LocalStatsDatabase
import com.nishant.northmusic.data.model.Song
import com.nishant.northmusic.data.model.SongTagOverride
import com.nishant.northmusic.data.repository.normalizeText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

enum class MetadataConfidence(val label: String) {
    HIGH("High confidence"),
    MEDIUM("Medium confidence"),
    LOW("Low confidence")
}

enum class MetadataIssueType(val label: String) {
    TITLE_HAS_ARTIST("Title contains artist"),
    UNKNOWN_ARTIST("Unknown artist"),
    MISSING_ALBUM("Missing album"),
    MISSING_ARTWORK("Missing artwork"),
    DUPLICATE_ALBUM("Duplicate album")
}

data class MetadataSuggestion(
    val song: Song,
    val issueType: MetadataIssueType,
    val suggestedTitle: String,
    val suggestedArtist: String,
    val suggestedAlbum: String,
    val suggestedArtworkUrl: String?,
    val confidence: MetadataConfidence,
    val isApplied: Boolean = false,
    val isSkipped: Boolean = false
)

object MetadataSuggestionService {

    suspend fun analyzeAndFetchSuggestions(
        context: Context,
        songs: List<Song>
    ): List<MetadataSuggestion> = withContext(Dispatchers.IO) {
        val result = mutableListOf<MetadataSuggestion>()
        val albumCounts = songs.groupBy { normalizeText(it.album) }

        for (song in songs) {
            val normTitle = normalizeText(song.title)
            val normArtist = normalizeText(song.artist)
            val normAlbum = normalizeText(song.album)

            var issue: MetadataIssueType? = null
            var cleanSearchTitle = song.title
            var cleanSearchArtist = song.artist

            if (song.title.contains(" - ") || (normArtist.isNotBlank() && normTitle.contains(normArtist))) {
                issue = MetadataIssueType.TITLE_HAS_ARTIST
                if (song.title.contains(" - ")) {
                    val parts = song.title.split(" - ")
                    if (parts.size >= 2) {
                        cleanSearchArtist = parts[0].trim()
                        cleanSearchTitle = parts[1].trim()
                    }
                }
            } else if (normArtist.isBlank() || normArtist == "unknown artist" || normArtist == "<unknown>") {
                issue = MetadataIssueType.UNKNOWN_ARTIST
            } else if (normAlbum.isBlank() || normAlbum == "unknown album" || normAlbum == "<unknown>") {
                issue = MetadataIssueType.MISSING_ALBUM
            } else if (song.albumArtUri == null || song.albumId <= 0L) {
                issue = MetadataIssueType.MISSING_ARTWORK
            } else if ((albumCounts[normAlbum]?.size ?: 0) > 1 && songs.any { it.album == song.album && it.artist != song.artist }) {
                issue = MetadataIssueType.DUPLICATE_ALBUM
            }

            if (issue != null) {
                val suggestion = fetchItunesSuggestion(context, song, issue, cleanSearchTitle, cleanSearchArtist)
                if (suggestion != null) {
                    result.add(suggestion)
                }
            }
        }
        result
    }

    private suspend fun fetchItunesSuggestion(
        context: Context,
        song: Song,
        issue: MetadataIssueType,
        searchTitle: String,
        searchArtist: String
    ): MetadataSuggestion? = withContext(Dispatchers.IO) {
        val query = if (searchArtist.isNotBlank() && !searchArtist.equals("unknown artist", ignoreCase = true)) {
            "$searchTitle $searchArtist"
        } else {
            searchTitle
        }

        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = URL("https://itunes.apple.com/search?term=$encoded&entity=song&limit=3")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "NorthMusic/1.0 (Android)")
            }

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(text)
                val results = json.optJSONArray("results")
                if (results != null && results.length() > 0) {
                    val best = results.getJSONObject(0)
                    val sTitle = best.optString("trackName", song.title)
                    val sArtist = best.optString("artistName", song.artist)
                    val sAlbum = best.optString("collectionName", song.album)
                    val art100 = if (best.has("artworkUrl100")) best.optString("artworkUrl100") else null
                    val sArtworkUrl = art100?.replace("100x100", "600x600")

                    val normSTitle = normalizeText(sTitle)
                    val normSArtist = normalizeText(sArtist)
                    val normSongTitle = normalizeText(searchTitle)
                    val normSongArtist = normalizeText(searchArtist)

                    val confidence = when {
                        normSTitle == normSongTitle && (normSArtist == normSongArtist || normSongArtist == "unknown artist") -> MetadataConfidence.HIGH
                        normSTitle.contains(normSongTitle) || normSongTitle.contains(normSTitle) -> MetadataConfidence.MEDIUM
                        else -> MetadataConfidence.LOW
                    }

                    return@withContext MetadataSuggestion(
                        song = song,
                        issueType = issue,
                        suggestedTitle = sTitle,
                        suggestedArtist = sArtist,
                        suggestedAlbum = sAlbum,
                        suggestedArtworkUrl = sArtworkUrl,
                        confidence = confidence
                    )
                }
            }
        } catch (_: Exception) {}

        // Fallback local heuristic suggestion if offline or search failed
        if (issue == MetadataIssueType.TITLE_HAS_ARTIST && song.title.contains(" - ")) {
            val parts = song.title.split(" - ")
            return@withContext MetadataSuggestion(
                song = song,
                issueType = issue,
                suggestedTitle = parts[1].trim(),
                suggestedArtist = parts[0].trim(),
                suggestedAlbum = song.album,
                suggestedArtworkUrl = null,
                confidence = MetadataConfidence.HIGH
            )
        }
        null
    }

    suspend fun applySuggestion(context: Context, suggestion: MetadataSuggestion) = withContext(Dispatchers.IO) {
        val statsDb = LocalStatsDatabase.getInstance(context)
        var savedArtworkPath: String? = null

        if (!suggestion.suggestedArtworkUrl.isNullOrBlank()) {
            try {
                val dir = File(context.filesDir, "album_images").apply { if (!exists()) mkdirs() }
                val fileHash = AlbumArtworkDownloader.getHash("${suggestion.suggestedAlbum}|${suggestion.suggestedArtist}")
                val destFile = File(dir, "$fileHash.jpg")
                val conn = URL(suggestion.suggestedArtworkUrl).openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.inputStream.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                conn.disconnect()
                if (destFile.exists() && destFile.length() > 0) {
                    savedArtworkPath = destFile.absolutePath
                    statsDb.saveAlbumImage("${normalizeText(suggestion.suggestedAlbum)}|${normalizeText(suggestion.suggestedArtist)}", savedArtworkPath, "itunes")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val override = SongTagOverride(
            songId = suggestion.song.id,
            title = suggestion.suggestedTitle,
            artist = suggestion.suggestedArtist,
            album = suggestion.suggestedAlbum,
            artworkPath = savedArtworkPath
        )
        statsDb.saveSongTagOverride(override)
    }
}
