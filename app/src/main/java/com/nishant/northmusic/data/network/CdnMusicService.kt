package com.nishant.northmusic.data.network

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.regex.Pattern
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

data class OnlineTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationSeconds: Int,
    val artworkUrl: String,
    val downloadUrl: String?,
    val bitrate: String = "320kbps",
    val source: String = "JioSaavn 320k"
)

data class ResolvedTrackInfo(
    val title: String,
    val artist: String,
    val album: String,
    val artworkUrl: String,
    val downloadUrl: String?,
    val bitrate: String = "320kbps",
    val source: String,
    val originalUrl: String
)

object CdnMusicService {

    private const val DES_KEY = "38346591"
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    private val SPOTIFY_REGEX = Pattern.compile(
        """(?:open\.spotify\.com/(?:intl-[a-z]+/)?(track|playlist|album)/|spotify:(track|playlist|album):)([a-zA-Z0-9]+)"""
    )
    private val YOUTUBE_REGEX = Pattern.compile(
        """(?:youtube\.com/(?:watch\?.*v=|embed/|shorts/|v/)|youtu\.be/|music\.youtube\.com/watch\?.*v=)([a-zA-Z0-9_-]{11})"""
    )

    fun isUrl(input: String): Boolean {
        val trimmed = input.trim()
        return trimmed.startsWith("http://") || trimmed.startsWith("https://")
    }

    fun isSupportedUrl(url: String): Boolean {
        val lower = url.trim().lowercase()
        return lower.contains("spotify.com") ||
                lower.contains("youtube.com") ||
                lower.contains("youtu.be") ||
                lower.contains("jiosaavn.com") ||
                lower.contains("saavn.com")
    }

    /**
     * Decrypts JioSaavn encrypted_media_url using DES-ECB with key "38346591"
     * and substitutes 320kbps stream filenames for studio audio quality.
     */
    fun decryptMediaUrl(encryptedUrl: String?): String? {
        if (encryptedUrl.isNullOrBlank()) return null
        return try {
            val keyBytes = DES_KEY.toByteArray(Charsets.UTF_8)
            val keySpec = SecretKeySpec(keyBytes, "DES")
            val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, keySpec)

            val cipherBytes = Base64.decode(encryptedUrl.trim(), Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(cipherBytes)
            val rawUrl = String(decryptedBytes, Charsets.UTF_8).trim()

            // Upgrade to 320kbps high-fidelity audio stream
            rawUrl.replace("_96.mp4", "_320.mp4")
                .replace("_160.mp4", "_320.mp4")
                .replace("_96.m4a", "_320.m4a")
                .replace("_160.m4a", "_320.m4a")
                .replace("_96.mp3", "_320.mp3")
                .replace("_160.mp3", "_320.mp3")
        } catch (_: Exception) {
            null
        }
    }

    /**
     * High-speed online search querying JioSaavn CDN API for 320kbps tracks.
     */
    suspend fun searchTracks(query: String, limit: Int = 20): List<OnlineTrack> = withContext(Dispatchers.IO) {
        val cleanQuery = cleanSearchQuery(query)
        if (cleanQuery.isBlank()) return@withContext emptyList()

        try {
            val encodedQuery = URLEncoder.encode(cleanQuery, "UTF-8")
            val endpoint = "https://www.jiosaavn.com/api.php?__call=search.getResults&_format=json&_marker=0&cc=in&p=1&n=$limit&q=$encodedQuery"
            val jsonText = httpGet(endpoint) ?: return@withContext emptyList()

            val root = JSONObject(jsonText)
            val results = root.optJSONArray("results") ?: return@withContext emptyList()

            val tracks = mutableListOf<OnlineTrack>()
            for (i in 0 until results.length()) {
                val item = results.optJSONObject(i) ?: continue
                val encrypted = item.optString("encrypted_media_url", "")
                val streamUrl = decryptMediaUrl(encrypted)

                val rawTitle = item.optString("song", item.optString("title", "Unknown Title"))
                val rawArtist = item.optString("singers", item.optString("primary_artists", "Unknown Artist"))
                val rawAlbum = item.optString("album", "")
                val duration = item.optString("duration", "0").toIntOrNull() ?: 0

                val image = item.optString("image", "")
                    .replace("150x150", "500x500")
                    .replace("50x50", "500x500")

                tracks.add(
                    OnlineTrack(
                        id = item.optString("id", "jio_${System.currentTimeMillis()}_$i"),
                        title = decodeHtml(rawTitle),
                        artist = decodeHtml(rawArtist),
                        album = decodeHtml(rawAlbum),
                        durationSeconds = duration,
                        artworkUrl = image,
                        downloadUrl = streamUrl,
                        bitrate = if (item.optString("320kbps", "true") == "true") "320kbps" else "160kbps",
                        source = "JioSaavn 320k"
                    )
                )
            }
            tracks
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Resolves pasted URLs (Spotify track, YouTube / YT Music video, or JioSaavn track)
     * into track metadata and a high-speed 320kbps CDN download URL.
     */
    suspend fun resolveUrl(rawUrl: String): ResolvedTrackInfo? = withContext(Dispatchers.IO) {
        val url = rawUrl.trim()
        if (url.isBlank()) return@withContext null

        // 1. YouTube & YouTube Music links
        val ytMatcher = YOUTUBE_REGEX.matcher(url)
        if (ytMatcher.find()) {
            return@withContext resolveYouTubeUrl(url)
        }

        // 2. Spotify track links
        val spotMatcher = SPOTIFY_REGEX.matcher(url)
        if (spotMatcher.find()) {
            return@withContext resolveSpotifyUrl(url)
        }

        // 3. JioSaavn links
        if (url.contains("jiosaavn.com") || url.contains("saavn.com")) {
            return@withContext resolveJioSaavnUrl(url)
        }

        // 4. Fallback: Generic direct audio link (e.g. .mp3, .m4a, .flac)
        if (url.endsWith(".mp3") || url.endsWith(".m4a") || url.endsWith(".flac") || url.endsWith(".wav")) {
            val fileName = url.substringAfterLast('/').substringBefore('?')
            val title = fileName.substringBeforeLast('.').replace('_', ' ')
            return@withContext ResolvedTrackInfo(
                title = title,
                artist = "Direct Audio Download",
                album = "Downloads",
                artworkUrl = "",
                downloadUrl = url,
                bitrate = "Direct",
                source = "Direct Link",
                originalUrl = url
            )
        }

        null
    }

    private suspend fun resolveYouTubeUrl(url: String): ResolvedTrackInfo? {
        try {
            val encoded = URLEncoder.encode(url, "UTF-8")
            val oembedUrl = "https://www.youtube.com/oembed?url=$encoded&format=json"
            val jsonText = httpGet(oembedUrl) ?: return null
            val json = JSONObject(jsonText)

            val rawTitle = json.optString("title", "YouTube Track")
            val author = json.optString("author_name", "YouTube")
            val thumbnail = json.optString("thumbnail_url", "")

            // Parse clean artist & title (e.g. "Artist - Title (Official Video)")
            val cleanTitle = cleanSearchQuery(rawTitle)
            val parts = cleanTitle.split(" - ")
            val resolvedTitle = if (parts.size > 1) parts[1].trim() else cleanTitle
            val resolvedArtist = if (parts.size > 1) parts[0].trim() else author

            // Match with high-res 320kbps audio from JioSaavn CDN
            val searchResults = searchTracks("$resolvedTitle $resolvedArtist", limit = 3)
            val match = searchResults.firstOrNull()

            return ResolvedTrackInfo(
                title = decodeHtml(match?.title ?: resolvedTitle),
                artist = decodeHtml(match?.artist ?: resolvedArtist),
                album = decodeHtml(match?.album ?: "YouTube Music"),
                artworkUrl = match?.artworkUrl?.ifBlank { thumbnail } ?: thumbnail,
                downloadUrl = match?.downloadUrl,
                bitrate = match?.bitrate ?: "320kbps",
                source = if (match != null) "YouTube + JioSaavn 320k" else "YouTube Audio",
                originalUrl = url
            )
        } catch (_: Exception) {
            return null
        }
    }

    private suspend fun resolveSpotifyUrl(url: String): ResolvedTrackInfo? {
        try {
            // First try oEmbed for Spotify
            val encoded = URLEncoder.encode(url, "UTF-8")
            val oembedUrl = "https://open.spotify.com/oembed?url=$encoded"
            val jsonText = httpGet(oembedUrl)

            var trackTitle: String? = null
            var trackArtist: String? = null
            var artwork: String? = null

            if (!jsonText.isNullOrBlank()) {
                val json = JSONObject(jsonText)
                val fullTitle = json.optString("title", "")
                artwork = json.optString("thumbnail_url", "")
                val parts = fullTitle.split(" - ")
                if (parts.size > 1) {
                    trackArtist = parts[0].trim()
                    trackTitle = parts[1].trim()
                } else {
                    trackTitle = fullTitle
                }
            }

            // If oEmbed didn't provide track details, extract track ID and query title/artist via JioSaavn
            if (trackTitle.isNullOrBlank()) {
                val matcher = SPOTIFY_REGEX.matcher(url)
                if (matcher.find()) {
                    val trackId = matcher.group(3) ?: ""
                    trackTitle = "Spotify Track $trackId"
                }
            }

            if (trackTitle != null) {
                val query = cleanSearchQuery("$trackTitle ${trackArtist ?: ""}")
                val searchResults = searchTracks(query, limit = 3)
                val match = searchResults.firstOrNull()

                return ResolvedTrackInfo(
                    title = decodeHtml(match?.title ?: trackTitle),
                    artist = decodeHtml(match?.artist ?: trackArtist ?: "Spotify Artist"),
                    album = decodeHtml(match?.album ?: "Spotify"),
                    artworkUrl = match?.artworkUrl?.ifBlank { artwork } ?: artwork ?: "",
                    downloadUrl = match?.downloadUrl,
                    bitrate = match?.bitrate ?: "320kbps",
                    source = "Spotify + JioSaavn 320k",
                    originalUrl = url
                )
            }
        } catch (_: Exception) {}
        return null
    }

    private suspend fun resolveJioSaavnUrl(url: String): ResolvedTrackInfo? {
        try {
            // Search track by the link's song title slug
            val slug = url.substringAfterLast("/").substringBefore("?").replace("-", " ")
            val results = searchTracks(slug, limit = 3)
            val match = results.firstOrNull() ?: return null

            return ResolvedTrackInfo(
                title = match.title,
                artist = match.artist,
                album = match.album,
                artworkUrl = match.artworkUrl,
                downloadUrl = match.downloadUrl,
                bitrate = match.bitrate,
                source = "JioSaavn 320k",
                originalUrl = url
            )
        } catch (_: Exception) {
            return null
        }
    }

    private fun cleanSearchQuery(text: String): String {
        return text
            .replace(Regex("""\(.*?\)"""), "")
            .replace(Regex("""\[.*?\]"""), "")
            .replace(Regex("""(?i)official\s*(music\s*)?video"""), "")
            .replace(Regex("""(?i)audio\s*track"""), "")
            .replace(Regex("""(?i)lyric(s)?"""), "")
            .replace(Regex("""(?i)feat\..*"""), "")
            .replace(Regex("""(?i)ft\..*"""), "")
            .replace(Regex("""[\-_|]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun decodeHtml(input: String): String {
        return input
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&#039;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }

    private fun httpGet(urlStr: String): String? {
        return try {
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else null
        } catch (_: Exception) {
            null
        }
    }
}
