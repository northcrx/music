package com.nishant.northmusic.data.network

import android.net.Uri
import android.util.Base64
import android.util.Log
import com.nishant.northmusic.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.regex.Pattern
import java.util.zip.GZIPInputStream
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
    val source: String = "JioSaavn 320k",
    val tags: List<String> = emptyList()
) {
    fun toSong(): Song {
        return Song(
            id = "online_$id",
            title = title,
            artist = artist,
            album = album,
            durationSeconds = durationSeconds,
            filePath = downloadUrl ?: "",
            customArtUri = if (artworkUrl.isNotBlank()) Uri.parse(artworkUrl) else null,
            isHighQuality = true,
            genre = tags.firstOrNull() ?: "Online"
        )
    }
}

data class ResolvedTrackInfo(
    val title: String,
    val artist: String,
    val album: String,
    val artworkUrl: String,
    val downloadUrl: String?,
    val bitrate: String = "320kbps",
    val source: String,
    val originalUrl: String,
    val durationSeconds: Int = 0,
    val tags: List<String> = emptyList()
) {
    fun toSong(): Song {
        return Song(
            id = "online_${System.currentTimeMillis()}",
            title = title,
            artist = artist,
            album = album,
            durationSeconds = durationSeconds,
            filePath = downloadUrl ?: "",
            customArtUri = if (artworkUrl.isNotBlank()) Uri.parse(artworkUrl) else null,
            isHighQuality = true,
            genre = tags.firstOrNull() ?: "Online"
        )
    }
}

data class PlaylistTrackItem(
    val title: String,
    val artist: String,
    val durationSeconds: Int = 0,
    val artworkUrl: String? = null,
    val source: String = "Online"
)

data class PlaylistInfo(
    val title: String,
    val author: String,
    val artworkUrl: String?,
    val tracks: List<PlaylistTrackItem>,
    val source: String,
    val originalUrl: String
)

data class OnlineAlbum(
    val id: String,
    val title: String,
    val artist: String,
    val year: String = "",
    val artworkUrl: String = "",
    val songCount: Int = 0
)

data class OnlineArtist(
    val id: String,
    val name: String,
    val role: String = "Artist",
    val artworkUrl: String = ""
)

object CdnMusicService {

    private const val TAG = "CdnMusicService"
    private const val DES_KEY = "38346591" // JioSaavn media decryption key (verified 8-byte DES key)

    // Spotify & YouTube Link Matchers
    private val SPOTIFY_REGEX = Pattern.compile("https?://open\\.spotify\\.com/(?:intl-[a-z]{2}/)?track/([a-zA-Z0-9]+)")
    private val SPOTIFY_PLAYLIST_REGEX = Pattern.compile("open\\.spotify\\.com/(?:intl-[a-z]{2}/)?playlist/([a-zA-Z0-9]+)")
    private val SPOTIFY_ALBUM_REGEX = Pattern.compile("open\\.spotify\\.com/(?:intl-[a-z]{2}/)?album/([a-zA-Z0-9]+)")
    private val YOUTUBE_REGEX = Pattern.compile("(?:https?://)?(?:www\\.)?(?:youtube\\.com/watch\\?v=|youtu\\.be/|music\\.youtube\\.com/watch\\?v=)([a-zA-Z0-9_-]{11})")
    private val YOUTUBE_PLAYLIST_REGEX = Pattern.compile("(?:youtube\\.com/playlist\\?list=|music\\.youtube\\.com/playlist\\?list=|[?&]list=)([a-zA-Z0-9_-]+)")
    private val URL_FINDER_REGEX = Pattern.compile("https?://[^\\s]+")

    fun isUrl(input: String): Boolean {
        val s = input.trim()
        return s.startsWith("http://") || s.startsWith("https://")
    }

    fun extractFirstUrl(text: String): String? {
        val matcher = URL_FINDER_REGEX.matcher(text)
        return if (matcher.find()) matcher.group() else null
    }

    fun isPlaylistUrl(input: String): Boolean {
        val url = extractFirstUrl(input) ?: input.trim()
        return SPOTIFY_PLAYLIST_REGEX.matcher(url).find() ||
                SPOTIFY_ALBUM_REGEX.matcher(url).find() ||
                YOUTUBE_PLAYLIST_REGEX.matcher(url).find()
    }

    /**
     * Searches JioSaavn API directly for high quality 320kbps music CDN links.
     */
    suspend fun searchTracks(query: String, page: Int = 1, limit: Int = 20): List<OnlineTrack> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<OnlineTrack>()
        val cleanQuery = cleanSearchQuery(query)
        if (cleanQuery.isBlank()) return@withContext emptyList()

        // Method 1: search.getResults endpoint
        try {
            val encodedQuery = URLEncoder.encode(cleanQuery, "UTF-8")
            val endpoint = "https://www.jiosaavn.com/api.php?__call=search.getResults&_format=json&_marker=0&cc=in&p=$page&n=$limit&q=$encodedQuery"
            val jsonText = httpGet(endpoint)
            if (!jsonText.isNullOrBlank()) {
                val root = JSONObject(jsonText)
                val results = root.optJSONArray("results")
                if (results != null) {
                    for (i in 0 until results.length()) {
                        val item = results.optJSONObject(i) ?: continue
                        val encrypted = item.optString("encrypted_media_url", "")
                        val streamUrl = decryptMediaUrl(encrypted)

                        val rawTitle = item.optString("song", item.optString("title", "Unknown Title"))
                        val primary = item.optString("primary_artists").trim()
                        val singers = item.optString("singers").trim()
                        val rawArtist = when {
                            primary.isNotBlank() -> primary
                            singers.isNotBlank() -> singers
                            else -> item.optString("music", "Unknown Artist")
                        }
                        val rawAlbum = item.optString("album", "")
                        val duration = item.optString("duration", "0").toIntOrNull() ?: 0
                        val language = item.optString("language", "")
                        val moreInfo = item.optJSONObject("more_info")
                        val genre = moreInfo?.optString("genre", "") ?: ""
                        val tagsList = buildList {
                            if (language.isNotBlank()) add(language)
                            if (genre.isNotBlank()) add(genre)
                            if (rawAlbum.isNotBlank()) add(rawAlbum)
                        }

                        // Get 500x500 high-res album art
                        val rawImage = item.optString("image", "")
                        val highResArt = rawImage
                            .replace("150x150", "500x500")
                            .replace("50x50", "500x500")

                        tracks.add(
                            OnlineTrack(
                                id = item.optString("id", "${System.currentTimeMillis()}_$i"),
                                title = decodeHtml(rawTitle),
                                artist = decodeHtml(rawArtist),
                                album = decodeHtml(rawAlbum),
                                durationSeconds = duration,
                                artworkUrl = highResArt,
                                downloadUrl = streamUrl,
                                bitrate = if (item.optString("320kbps", "true") == "true") "320kbps" else "160kbps",
                                source = "JioSaavn 320k",
                                tags = tagsList
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "searchTracks search.getResults failed for query=$cleanQuery", e)
        }

        // Method 2: Autocomplete fallback if results are sparse
        if (tracks.size < 3) {
            try {
                val encodedQuery = URLEncoder.encode(cleanQuery, "UTF-8")
                val acEndpoint = "https://www.jiosaavn.com/api.php?__call=autocomplete.get&_format=json&_marker=0&cc=in&query=$encodedQuery"
                val acText = httpGet(acEndpoint)
                if (!acText.isNullOrBlank()) {
                    val root = JSONObject(acText)
                    val songsObj = root.optJSONObject("songs")
                    val songData = songsObj?.optJSONArray("data")
                    if (songData != null) {
                        for (i in 0 until songData.length()) {
                            val obj = songData.optJSONObject(i) ?: continue
                            val id = obj.optString("id")
                            if (id.isNotBlank() && tracks.none { it.id == id }) {
                                val songDetailsUrl = "https://www.jiosaavn.com/api.php?__call=song.getDetails&pids=$id&_format=json"
                                val songText = httpGet(songDetailsUrl)
                                if (!songText.isNullOrBlank()) {
                                    val detailRoot = JSONObject(songText)
                                    val songDetail = detailRoot.optJSONObject(id) ?: continue
                                    val encrypted = songDetail.optString("encrypted_media_url", "")
                                    val streamUrl = decryptMediaUrl(encrypted)
                                    val image = songDetail.optString("image", "")
                                        .replace("150x150", "500x500")
                                        .replace("50x50", "500x500")

                                    val tagsList = buildList {
                                        val lang = songDetail.optString("language", "")
                                        if (lang.isNotBlank()) add(lang)
                                        val more = songDetail.optJSONObject("more_info")
                                        val genre = more?.optString("genre", "") ?: ""
                                        if (genre.isNotBlank()) add(genre)
                                    }

                                    tracks.add(
                                        OnlineTrack(
                                            id = id,
                                            title = decodeHtml(songDetail.optString("song", obj.optString("title"))),
                                            artist = decodeHtml(
                                                run {
                                                    val primary = songDetail.optString("primary_artists").trim()
                                                    val singers = songDetail.optString("singers").trim()
                                                    when {
                                                        primary.isNotBlank() -> primary
                                                        singers.isNotBlank() -> singers
                                                        else -> obj.optString("description")
                                                    }
                                                }
                                            ),
                                            album = decodeHtml(songDetail.optString("album", "")),
                                            durationSeconds = songDetail.optString("duration", "0").toIntOrNull() ?: 0,
                                            artworkUrl = image,
                                            downloadUrl = streamUrl,
                                            bitrate = if (obj.optString("320kbps", "true") == "true") "320kbps" else "160kbps",
                                            source = "JioSaavn 320k",
                                            tags = tagsList
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "autocomplete fallback failed for query=$cleanQuery", e)
            }
        }

        deduplicateOnlineTracks(tracks)
    }

    private fun deduplicateOnlineTracks(tracks: List<OnlineTrack>): List<OnlineTrack> {
        val seen = HashSet<String>()
        val result = ArrayList<OnlineTrack>()
        for (track in tracks) {
            val normTitle = track.title.lowercase()
                .replace(Regex("""(?i)\(official.*?\)|\\[official.*?\\]|\(audio\)|\(video\)|\(lyric.*?\)"""), "")
                .replace(Regex("[^a-z0-9]"), "")
                .trim()
            val firstArtist = track.artist.split(",", "&", "•", "/", ";").firstOrNull()?.trim()?.lowercase()?.replace(Regex("[^a-z0-9]"), "") ?: ""
            val key = if (normTitle.isNotBlank()) "$normTitle|$firstArtist" else track.id
            if (seen.add(key)) {
                result.add(track)
            }
        }
        return result
    }

    /**
     * Searches JioSaavn API for albums.
     */
    suspend fun searchAlbums(query: String, page: Int = 1, limit: Int = 15): List<OnlineAlbum> = withContext(Dispatchers.IO) {
        val albums = mutableListOf<OnlineAlbum>()
        val cleanQuery = cleanSearchQuery(query)
        if (cleanQuery.isBlank()) return@withContext emptyList()

        try {
            val encodedQuery = URLEncoder.encode(cleanQuery, "UTF-8")
            val endpoint = "https://www.jiosaavn.com/api.php?__call=search.getAlbumResults&_format=json&_marker=0&cc=in&p=$page&n=$limit&q=$encodedQuery"
            val jsonText = httpGet(endpoint)
            if (!jsonText.isNullOrBlank()) {
                val root = JSONObject(jsonText)
                val results = root.optJSONArray("results")
                if (results != null) {
                    for (i in 0 until results.length()) {
                        val item = results.optJSONObject(i) ?: continue
                        val albumId = item.optString("albumid", item.optString("id", ""))
                        if (albumId.isBlank()) continue
                        val rawTitle = item.optString("title", item.optString("album", "Unknown Album"))
                        val rawArtist = item.optString("primary_artists", item.optString("artist", item.optString("music", "Various Artists")))
                        val year = item.optString("year", "")
                        val rawImage = item.optString("image", "")
                        val highResArt = rawImage.replace("150x150", "500x500").replace("50x50", "500x500")
                        val songCount = item.optInt("song_count", 0)

                        albums.add(
                            OnlineAlbum(
                                id = albumId,
                                title = decodeHtml(rawTitle),
                                artist = decodeHtml(rawArtist),
                                year = year,
                                artworkUrl = highResArt,
                                songCount = songCount
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "searchAlbums failed for query=$cleanQuery", e)
        }
        albums
    }

    /**
     * Fetches detailed track list and metadata for an album by ID.
     */
    suspend fun getAlbumTracks(albumId: String): Pair<OnlineAlbum?, List<OnlineTrack>> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<OnlineTrack>()
        var albumMeta: OnlineAlbum? = null
        try {
            val endpoint = "https://www.jiosaavn.com/api.php?__call=content.getAlbumDetails&albumid=$albumId&_format=json"
            val jsonText = httpGet(endpoint)
            if (!jsonText.isNullOrBlank()) {
                val root = JSONObject(jsonText)
                val rawTitle = root.optString("title", root.optString("name", "Unknown Album"))
                val rawArtist = root.optString("primary_artists", root.optString("artist", ""))
                val year = root.optString("year", "")
                val rawImage = root.optString("image", "")
                val highResArt = rawImage.replace("150x150", "500x500").replace("50x50", "500x500")

                val trackArray = root.optJSONArray("list") ?: root.optJSONArray("songs")
                if (trackArray != null) {
                    for (i in 0 until trackArray.length()) {
                        val item = trackArray.optJSONObject(i) ?: continue
                        val encrypted = item.optString("encrypted_media_url", "")
                        val streamUrl = decryptMediaUrl(encrypted)
                        val songTitle = item.optString("song", item.optString("title", "Unknown Track"))
                        val primary = item.optString("primary_artists").trim()
                        val singers = item.optString("singers").trim()
                        val songArtist = when {
                            primary.isNotBlank() -> primary
                            singers.isNotBlank() -> singers
                            else -> rawArtist
                        }
                        val duration = item.optString("duration", "0").toIntOrNull() ?: 0
                        val songImage = item.optString("image", rawImage).replace("150x150", "500x500").replace("50x50", "500x500")

                        songs.add(
                            OnlineTrack(
                                id = item.optString("id", "${albumId}_$i"),
                                title = decodeHtml(songTitle),
                                artist = decodeHtml(songArtist),
                                album = decodeHtml(rawTitle),
                                durationSeconds = duration,
                                artworkUrl = songImage,
                                downloadUrl = streamUrl,
                                bitrate = "320kbps",
                                source = "JioSaavn 320k"
                            )
                        )
                    }
                }

                albumMeta = OnlineAlbum(
                    id = albumId,
                    title = decodeHtml(rawTitle),
                    artist = decodeHtml(rawArtist),
                    year = year,
                    artworkUrl = highResArt,
                    songCount = songs.size
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "getAlbumTracks failed for albumId=$albumId", e)
        }
        Pair(albumMeta, songs)
    }

    /**
     * Searches JioSaavn API for artists.
     */
    suspend fun searchArtists(query: String, page: Int = 1, limit: Int = 15): List<OnlineArtist> = withContext(Dispatchers.IO) {
        val artists = mutableListOf<OnlineArtist>()
        val cleanQuery = cleanSearchQuery(query)
        if (cleanQuery.isBlank()) return@withContext emptyList()

        try {
            val encodedQuery = URLEncoder.encode(cleanQuery, "UTF-8")
            val endpoint = "https://www.jiosaavn.com/api.php?__call=search.getArtistResults&_format=json&_marker=0&cc=in&p=$page&n=$limit&q=$encodedQuery"
            val jsonText = httpGet(endpoint)
            if (!jsonText.isNullOrBlank()) {
                val root = JSONObject(jsonText)
                val results = root.optJSONArray("results")
                if (results != null) {
                    for (i in 0 until results.length()) {
                        val item = results.optJSONObject(i) ?: continue
                        val artistId = item.optString("artistid", item.optString("id", ""))
                        if (artistId.isBlank()) continue
                        val name = decodeHtml(item.optString("name", item.optString("title", "Unknown Artist")))
                        val role = item.optString("role", "Artist")
                        val rawImage = item.optString("image", "")
                        val highResArt = rawImage.replace("150x150", "500x500").replace("50x50", "500x500")

                        artists.add(
                            OnlineArtist(
                                id = artistId,
                                name = name,
                                role = role,
                                artworkUrl = highResArt
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "searchArtists failed for query=$cleanQuery", e)
        }
        artists
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
                originalUrl = url,
                durationSeconds = 0,
                tags = emptyList()
            )
        }

        null
    }

    /**
     * Resolves Spotify and YouTube playlists/albums into full metadata and track listings.
     */
    suspend fun resolvePlaylist(rawUrl: String): PlaylistInfo? = withContext(Dispatchers.IO) {
        val url = extractFirstUrl(rawUrl) ?: rawUrl.trim()
        if (url.isBlank()) return@withContext null

        val spotPlaylistMatcher = SPOTIFY_PLAYLIST_REGEX.matcher(url)
        if (spotPlaylistMatcher.find()) {
            val playlistId = spotPlaylistMatcher.group(1) ?: return@withContext null
            return@withContext resolveSpotifyEmbed("playlist", playlistId, url)
        }

        val spotAlbumMatcher = SPOTIFY_ALBUM_REGEX.matcher(url)
        if (spotAlbumMatcher.find()) {
            val albumId = spotAlbumMatcher.group(1) ?: return@withContext null
            return@withContext resolveSpotifyEmbed("album", albumId, url)
        }

        val ytListMatcher = YOUTUBE_PLAYLIST_REGEX.matcher(url)
        if (ytListMatcher.find()) {
            val listId = ytListMatcher.group(1) ?: return@withContext null
            return@withContext resolveYouTubePlaylist(listId, url)
        }

        null
    }

    private suspend fun resolveSpotifyEmbed(type: String, id: String, originalUrl: String): PlaylistInfo? {
        try {
            val embedUrl = "https://open.spotify.com/embed/$type/$id"
            val html = httpGet(embedUrl) ?: return null

            val scriptPattern = Pattern.compile("<script id=\"__NEXT_DATA__\" type=\"application/json\">(.*?)</script>")
            val matcher = scriptPattern.matcher(html)
            if (!matcher.find()) return null

            val jsonStr = matcher.group(1) ?: return null
            val root = JSONObject(jsonStr)
            val entity = root.optJSONObject("props")
                ?.optJSONObject("pageProps")
                ?.optJSONObject("state")
                ?.optJSONObject("data")
                ?.optJSONObject("entity") ?: return null

            val title = decodeHtml(entity.optString("title", entity.optString("name", "Spotify Playlist")))
            val subtitle = decodeHtml(entity.optString("subtitle", entity.optString("author", "Spotify")))

            var artworkUrl: String? = null
            val visualImages = entity.optJSONObject("visualIdentity")?.optJSONArray("image")
            if (visualImages != null && visualImages.length() > 0) {
                artworkUrl = visualImages.optJSONObject(0)?.optString("url")
            }
            if (artworkUrl.isNullOrBlank()) {
                artworkUrl = entity.optString("coverArt", "")
            }

            val trackList = entity.optJSONArray("trackList") ?: return null
            val tracks = mutableListOf<PlaylistTrackItem>()

            for (i in 0 until trackList.length()) {
                val item = trackList.optJSONObject(i) ?: continue
                val rawTrackTitle = item.optString("title", "")
                val rawTrackArtist = item.optString("subtitle", subtitle)
                val durationMs = item.optInt("duration", 0)

                if (rawTrackTitle.isNotBlank()) {
                    tracks.add(
                        PlaylistTrackItem(
                            title = decodeHtml(rawTrackTitle),
                            artist = decodeHtml(rawTrackArtist),
                            durationSeconds = durationMs / 1000,
                            artworkUrl = artworkUrl,
                            source = "Spotify"
                        )
                    )
                }
            }

            if (tracks.isEmpty()) return null

            return PlaylistInfo(
                title = title,
                author = subtitle,
                artworkUrl = artworkUrl,
                tracks = tracks,
                source = if (type == "album") "Spotify Album" else "Spotify Playlist",
                originalUrl = originalUrl
            )
        } catch (e: Exception) {
            Log.e(TAG, "resolveSpotifyEmbed failed", e)
            return null
        }
    }

    private suspend fun resolveYouTubePlaylist(listId: String, originalUrl: String): PlaylistInfo? {
        try {
            val pageUrl = "https://www.youtube.com/playlist?list=$listId&hl=en"
            val html = httpGet(pageUrl) ?: return null

            val dataPattern = Pattern.compile("var ytInitialData\\s*=\\s*(\\{.+?\\});<")
            val dataMatcher = dataPattern.matcher(html)
            var jsonStr: String? = null
            if (dataMatcher.find()) {
                jsonStr = dataMatcher.group(1)
            } else {
                val altPattern = Pattern.compile("ytInitialData\\s*=\\s*(\\{.+?\\});</script>")
                val altMatcher = altPattern.matcher(html)
                if (altMatcher.find()) {
                    jsonStr = altMatcher.group(1)
                }
            }

            if (jsonStr.isNullOrBlank()) return null

            val root = JSONObject(jsonStr)

            val microformat = root.optJSONObject("microformat")?.optJSONObject("microformatDataRenderer")
            val metaRenderer = root.optJSONObject("metadata")?.optJSONObject("playlistMetadataRenderer")
            val playlistTitle = decodeHtml(
                metaRenderer?.optString("title")
                    ?: microformat?.optString("title")
                    ?: "YouTube Playlist"
            )
            val author = decodeHtml(
                metaRenderer?.optString("author")
                    ?: microformat?.optString("ownerChannelName")
                    ?: "YouTube"
            )
            val artworkUrl = microformat?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")?.let {
                if (it.length() > 0) it.optJSONObject(it.length() - 1)?.optString("url") else null
            }

            val tracks = mutableListOf<PlaylistTrackItem>()
            findYouTubePlaylistTracks(root, tracks)

            if (tracks.isEmpty()) return null

            return PlaylistInfo(
                title = playlistTitle,
                author = author,
                artworkUrl = artworkUrl,
                tracks = tracks,
                source = "YouTube Playlist",
                originalUrl = originalUrl
            )
        } catch (e: Exception) {
            Log.e(TAG, "resolveYouTubePlaylist failed", e)
            return null
        }
    }

    private fun findYouTubePlaylistTracks(json: Any?, tracks: MutableList<PlaylistTrackItem>) {
        when (json) {
            is JSONObject -> {
                if (json.has("playlistVideoRenderer")) {
                    val pvr = json.optJSONObject("playlistVideoRenderer")
                    if (pvr != null) {
                        val titleRuns = pvr.optJSONObject("title")?.optJSONArray("runs")
                        val rawTitle = titleRuns?.optJSONObject(0)?.optString("text") ?: ""
                        val bylineRuns = pvr.optJSONObject("shortBylineText")?.optJSONArray("runs")
                        val rawAuthor = bylineRuns?.optJSONObject(0)?.optString("text") ?: ""
                        val lengthSec = pvr.optString("lengthSeconds", "0").toIntOrNull() ?: 0
                        val thumbUrl = pvr.optJSONObject("thumbnail")?.optJSONArray("thumbnails")?.let {
                            if (it.length() > 0) it.optJSONObject(it.length() - 1)?.optString("url") else null
                        }

                        if (rawTitle.isNotBlank() && rawTitle != "[Deleted video]" && rawTitle != "[Private video]") {
                            val clean = cleanSearchQuery(rawTitle)
                            val parts = clean.split(" - ")
                            val songTitle = if (parts.size > 1) parts[1].trim() else clean
                            val songArtist = if (parts.size > 1) parts[0].trim() else rawAuthor
                            tracks.add(
                                PlaylistTrackItem(
                                    title = decodeHtml(songTitle),
                                    artist = decodeHtml(songArtist.ifBlank { rawAuthor }),
                                    durationSeconds = lengthSec,
                                    artworkUrl = thumbUrl,
                                    source = "YouTube"
                                )
                            )
                        }
                    }
                } else if (json.has("lockupMetadataViewModel")) {
                    val lmvm = json.optJSONObject("lockupMetadataViewModel")
                    if (lmvm != null) {
                        val title = lmvm.optJSONObject("title")?.optString("content") ?: ""
                        val rows = lmvm.optJSONObject("metadata")?.optJSONObject("contentMetadataViewModel")?.optJSONArray("metadataRows")
                        val author = rows?.optJSONObject(0)?.optJSONArray("parts")?.optJSONObject(0)?.optJSONObject("text")?.optString("content") ?: ""
                        if (title.isNotBlank() && !tracks.any { it.title == title }) {
                            val clean = cleanSearchQuery(title)
                            val parts = clean.split(" - ")
                            val songTitle = if (parts.size > 1) parts[1].trim() else clean
                            val songArtist = if (parts.size > 1) parts[0].trim() else author
                            tracks.add(
                                PlaylistTrackItem(
                                    title = decodeHtml(songTitle),
                                    artist = decodeHtml(songArtist.ifBlank { author }),
                                    durationSeconds = 0,
                                    artworkUrl = null,
                                    source = "YouTube"
                                )
                            )
                        }
                    }
                }

                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    findYouTubePlaylistTracks(json.opt(key), tracks)
                }
            }
            is org.json.JSONArray -> {
                for (i in 0 until json.length()) {
                    findYouTubePlaylistTracks(json.opt(i), tracks)
                }
            }
        }
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
                originalUrl = url,
                durationSeconds = match?.durationSeconds ?: 0,
                tags = match?.tags ?: emptyList()
            )
        } catch (e: Exception) {
            Log.e(TAG, "resolveYouTubeUrl error", e)
            return null
        }
    }

    private suspend fun resolveSpotifyUrl(url: String): ResolvedTrackInfo? {
        try {
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

            if (trackTitle.isNullOrBlank()) {
                val matcher = SPOTIFY_REGEX.matcher(url)
                if (matcher.find()) {
                    val trackId = matcher.group(1)
                    val webUrl = "https://open.spotify.com/track/$trackId"
                    val html = httpGet(webUrl)
                    if (!html.isNullOrBlank()) {
                        val titlePattern = Pattern.compile("<title>(.*?) - song and lyrics by (.*?) \\| Spotify</title>")
                        val m = titlePattern.matcher(html)
                        if (m.find()) {
                            trackTitle = m.group(1)
                            trackArtist = m.group(2)
                        }
                    }
                }
            }

            if (!trackTitle.isNullOrBlank()) {
                val query = if (trackArtist != null) "$trackTitle $trackArtist" else trackTitle
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
                    originalUrl = url,
                    durationSeconds = match?.durationSeconds ?: 0,
                    tags = match?.tags ?: emptyList()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "resolveSpotifyUrl error", e)
        }
        return null
    }

    private suspend fun resolveJioSaavnUrl(url: String): ResolvedTrackInfo? {
        try {
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
                originalUrl = url,
                durationSeconds = match.durationSeconds,
                tags = match.tags
            )
        } catch (e: Exception) {
            Log.e(TAG, "resolveJioSaavnUrl error", e)
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

    /**
     * Decrypts encrypted JioSaavn media URLs using DES in ECB mode.
     */
    fun decryptMediaUrl(encryptedUrl: String): String? {
        if (encryptedUrl.isBlank()) return null
        return try {
            val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            val key = SecretKeySpec(DES_KEY.toByteArray(Charsets.UTF_8), "DES")
            cipher.init(Cipher.DECRYPT_MODE, key)
            val decodedBytes = Base64.decode(encryptedUrl.trim(), Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            var decryptedUrl = String(decryptedBytes, Charsets.UTF_8).trim()
            if (decryptedUrl.startsWith("http://")) {
                decryptedUrl = "https://" + decryptedUrl.removePrefix("http://")
            }
            // Upgrade to 320kbps CDN if available
            decryptedUrl = decryptedUrl.replace("_96.mp4", "_320.mp4")
                .replace("_160.mp4", "_320.mp4")
                .replace(".mp4", ".mp4")
            decryptedUrl
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt media url: $encryptedUrl", e)
            null
        }
    }

    private fun httpGet(urlString: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            conn.setRequestProperty("Accept-Encoding", "gzip, deflate")
            conn.setRequestProperty("Accept", "*/*")
            conn.connect()

            val isGzipped = "gzip".equals(conn.contentEncoding, ignoreCase = true)
            val rawStream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream ?: return null
            val inputStream = if (isGzipped) GZIPInputStream(rawStream) else rawStream

            inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (e: Exception) {
            Log.e(TAG, "httpGet failed for $urlString", e)
            null
        } finally {
            conn?.disconnect()
        }
    }
}
