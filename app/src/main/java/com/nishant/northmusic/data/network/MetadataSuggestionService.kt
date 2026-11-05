package com.nishant.northmusic.data.network

import android.content.Context
import android.util.Log
import com.nishant.northmusic.data.local.LocalStatsDatabase
import com.nishant.northmusic.data.model.Song
import com.nishant.northmusic.data.model.SongTagOverride
import com.nishant.northmusic.data.repository.MusicRepository
import com.nishant.northmusic.data.repository.normalizeText
import com.nishant.northmusic.data.util.TagEditData
import com.nishant.northmusic.data.util.TagWriterHelper
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
    MISSING_ARTWORK("Missing artwork")
}

data class ScanProgress(
    val current: Int,
    val total: Int,
    val currentSongTitle: String,
    val foundCount: Int
)

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

    private const val TAG = "MetadataSuggestionService"

    /**
     * Checks if a song actually has usable album artwork available.
     */
    fun hasArtwork(context: Context, song: Song): Boolean {
        if (song.customArtUri != null) return true
        val uri = song.albumArtUri ?: return false
        return try {
            context.contentResolver.openInputStream(uri)?.use { true } ?: false
        } catch (_: Exception) {
            false
        }
    }

    fun isUnknownArtist(artist: String): Boolean {
        val norm = normalizeText(artist)
        return norm.isBlank() ||
                norm == "unknown artist" ||
                norm == "unknown" ||
                norm == "<unknown>" ||
                norm == "various artists"
    }

    fun findCandidateIssue(context: Context, song: Song): Pair<MetadataIssueType, Pair<String, String>>? {
        val normTitle = normalizeText(song.title)
        val normArtist = normalizeText(song.artist)
        val normAlbum = normalizeText(song.album)

        var issue: MetadataIssueType? = null
        var cleanSearchTitle = song.title
        var cleanSearchArtist = song.artist

        val titleHasArtistPattern = song.title.contains(" - ") || (normArtist.isNotBlank() && normTitle.contains(normArtist))
        val unknownArtist = isUnknownArtist(song.artist)
        val missingAlbum = normAlbum.isBlank() || normAlbum == "unknown album" || normAlbum == "<unknown>"
        val missingArtwork = !hasArtwork(context, song)

        if (titleHasArtistPattern) {
            issue = MetadataIssueType.TITLE_HAS_ARTIST
            if (song.title.contains(" - ")) {
                val parts = song.title.split(" - ")
                if (parts.size >= 2) {
                    cleanSearchArtist = parts[0].trim()
                    cleanSearchTitle = parts[1].trim()
                }
            }
        } else if (unknownArtist) {
            issue = MetadataIssueType.UNKNOWN_ARTIST
            val filename = File(song.filePath).nameWithoutExtension
            if (filename.contains(" - ")) {
                val parts = filename.split(" - ")
                if (parts.size >= 2) {
                    cleanSearchArtist = parts[0].trim()
                    cleanSearchTitle = parts[1].trim()
                }
            }
        } else if (missingArtwork) {
            issue = MetadataIssueType.MISSING_ARTWORK
        } else if (missingAlbum) {
            issue = MetadataIssueType.MISSING_ALBUM
        }

        return if (issue != null) {
            issue to (cleanSearchTitle to cleanSearchArtist)
        } else null
    }

    fun hasRealDifference(
        context: Context,
        song: Song,
        sTitle: String,
        sArtist: String,
        sAlbum: String,
        sArtworkUrl: String?
    ): Boolean {
        val titleDiff = !song.title.trim().equals(sTitle.trim(), ignoreCase = true)
        val artistDiff = !song.artist.trim().equals(sArtist.trim(), ignoreCase = true)
        val albumDiff = !song.album.trim().equals(sAlbum.trim(), ignoreCase = true) &&
                sAlbum.isNotBlank() &&
                !sAlbum.equals("Unknown Album", ignoreCase = true) &&
                !sAlbum.equals("<unknown>", ignoreCase = true)
        val artworkAdded = !hasArtwork(context, song) && !sArtworkUrl.isNullOrBlank()

        return titleDiff || artistDiff || albumDiff || artworkAdded
    }

    suspend fun analyzeAndFetchSuggestionsStreaming(
        context: Context,
        songs: List<Song>,
        onProgress: (ScanProgress) -> Unit = {},
        onSuggestionFound: (MetadataSuggestion) -> Unit = {},
        isCancelled: () -> Boolean = { false }
    ): List<MetadataSuggestion> = withContext(Dispatchers.IO) {
        val result = mutableListOf<MetadataSuggestion>()
        val candidates = mutableListOf<Triple<Song, MetadataIssueType, Pair<String, String>>>()

        for (song in songs) {
            if (isCancelled()) break
            val candidate = findCandidateIssue(context, song)
            if (candidate != null) {
                candidates.add(Triple(song, candidate.first, candidate.second))
            }
        }

        val total = candidates.size
        for ((index, item) in candidates.withIndex()) {
            if (isCancelled()) break
            val (song, issue, searchPair) = item
            onProgress(
                ScanProgress(
                    current = index + 1,
                    total = total,
                    currentSongTitle = song.title,
                    foundCount = result.size
                )
            )

            val suggestion = fetchCombinedSuggestion(context, song, issue, searchPair.first, searchPair.second)
            if (suggestion != null) {
                if (hasRealDifference(
                        context,
                        song,
                        suggestion.suggestedTitle,
                        suggestion.suggestedArtist,
                        suggestion.suggestedAlbum,
                        suggestion.suggestedArtworkUrl
                    )
                ) {
                    result.add(suggestion)
                    onSuggestionFound(suggestion)
                }
            }
        }
        result
    }

    suspend fun analyzeAndFetchSuggestions(
        context: Context,
        songs: List<Song>
    ): List<MetadataSuggestion> = analyzeAndFetchSuggestionsStreaming(context, songs)

    private suspend fun fetchCombinedSuggestion(
        context: Context,
        song: Song,
        issue: MetadataIssueType,
        searchTitle: String,
        searchArtist: String
    ): MetadataSuggestion? = withContext(Dispatchers.IO) {
        val effectiveArtist = if (isUnknownArtist(searchArtist)) "" else searchArtist
        val query = if (effectiveArtist.isNotBlank()) "$searchTitle $effectiveArtist".trim() else searchTitle.trim()
        if (query.isBlank()) return@withContext null

        // 1. Try JioSaavn first (excellent coverage for Bollywood, Indian, Asian, and popular international hits)
        try {
            val jioResults = CdnMusicService.searchTracks(query, limit = 3)
            val bestJio = jioResults.firstOrNull()
            if (bestJio != null) {
                val normSTitle = normalizeText(bestJio.title)
                val normSongTitle = normalizeText(searchTitle)
                val durationDiff = if (song.durationSeconds > 0 && bestJio.durationSeconds > 0) {
                    kotlin.math.abs(song.durationSeconds - bestJio.durationSeconds)
                } else 999

                val confidence = when {
                    (normSTitle == normSongTitle || normSTitle.contains(normSongTitle)) && durationDiff <= 5 -> MetadataConfidence.HIGH
                    normSTitle == normSongTitle || normSTitle.contains(normSongTitle) -> MetadataConfidence.HIGH
                    durationDiff <= 5 -> MetadataConfidence.MEDIUM
                    else -> MetadataConfidence.MEDIUM
                }

                return@withContext MetadataSuggestion(
                    song = song,
                    issueType = issue,
                    suggestedTitle = bestJio.title,
                    suggestedArtist = bestJio.artist,
                    suggestedAlbum = bestJio.album.ifBlank { song.album },
                    suggestedArtworkUrl = bestJio.artworkUrl.ifBlank { null },
                    confidence = confidence
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "JioSaavn suggestion lookup failed for query=$query", e)
        }

        // 2. Try iTunes Search API fallback (global Western, Pop, Rock, Classical library)
        try {
            val itunesSuggestion = fetchItunesSuggestion(song, issue, searchTitle, effectiveArtist)
            if (itunesSuggestion != null) {
                return@withContext itunesSuggestion
            }
        } catch (e: Exception) {
            Log.e(TAG, "iTunes suggestion lookup failed for query=$query", e)
        }

        // 3. Fallback heuristic suggestion for "Artist - Title" format
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

    private suspend fun fetchItunesSuggestion(
        song: Song,
        issue: MetadataIssueType,
        searchTitle: String,
        searchArtist: String
    ): MetadataSuggestion? = withContext(Dispatchers.IO) {
        val query = if (searchArtist.isNotBlank()) "$searchTitle $searchArtist" else searchTitle
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = URL("https://itunes.apple.com/search?term=$encoded&entity=song&limit=3")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "Mozilla/5.0")
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
                        normSTitle == normSongTitle && (normSArtist == normSongArtist || normSongArtist.isBlank()) -> MetadataConfidence.HIGH
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
        null
    }

    /**
     * Applies metadata & artwork suggestion by PHYSICALLY writing the ID3 / MP4 tags
     * and embedding the artwork image bytes directly into the audio file on disk.
     */
    suspend fun applySuggestion(context: Context, suggestion: MetadataSuggestion): Boolean = withContext(Dispatchers.IO) {
        val statsDb = LocalStatsDatabase.getInstance(context)
        var savedArtworkFile: File? = null

        // 1. Download suggested album art to permanent cache file
        if (!suggestion.suggestedArtworkUrl.isNullOrBlank()) {
            try {
                val dir = File(context.filesDir, "album_images").apply { if (!exists()) mkdirs() }
                val fileHash = AlbumArtworkDownloader.getHash("${suggestion.suggestedAlbum}|${suggestion.suggestedArtist}")
                val destFile = File(dir, "$fileHash.jpg")
                val conn = URL(suggestion.suggestedArtworkUrl).openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                conn.inputStream.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                conn.disconnect()
                if (destFile.exists() && destFile.length() > 0) {
                    savedArtworkFile = destFile
                    statsDb.saveAlbumImage(
                        "${normalizeText(suggestion.suggestedAlbum)}|${normalizeText(suggestion.suggestedArtist)}",
                        destFile.absolutePath,
                        "auto_fix"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed downloading artwork from ${suggestion.suggestedArtworkUrl}", e)
            }
        }

        // 2. CRUCIAL: Physically write the ID3 / MP4 tags & embed the artwork into the audio file!
        val tagData = TagEditData(
            title = suggestion.suggestedTitle,
            artist = suggestion.suggestedArtist,
            album = suggestion.suggestedAlbum,
            albumArtist = suggestion.suggestedArtist,
            composer = "",
            genre = suggestion.song.genre,
            year = suggestion.song.year.toString().ifBlank { "" },
            track = suggestion.song.trackNumber.toString().ifBlank { "" },
            disc = "",
            lyrics = "",
            comment = "Tagged by NorthMusic",
            artworkPath = savedArtworkFile?.absolutePath
        )

        // Write directly to the physical file on storage via Jaudiotagger
        val fileWriteResult = TagWriterHelper.writeToFile(context, suggestion.song, tagData)

        // 3. Update SQLite cache for instant UI responsiveness
        val override = SongTagOverride(
            songId = suggestion.song.id,
            title = suggestion.suggestedTitle,
            artist = suggestion.suggestedArtist,
            album = suggestion.suggestedAlbum,
            artworkPath = savedArtworkFile?.absolutePath
        )
        statsDb.saveSongTagOverride(override)

        // 4. Trigger MediaRepository rescan so all screens immediately display updated singer and artwork
        MusicRepository.getInstance(context).rescan()

        fileWriteResult.isSuccess
    }
}
