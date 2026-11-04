package com.nishant.northmusic.data.network

import android.content.Context
import com.nishant.northmusic.data.local.LocalStatsDatabase
import com.nishant.northmusic.data.repository.normalizeText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest

object AlbumArtworkDownloader {

    private const val THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000L

    fun getHash(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val bytes = md.digest(input.lowercase().trim().toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun fetchAndSaveAlbumArtwork(
        context: Context,
        albumTitle: String,
        albumArtist: String,
        albumKey: String? = null,
        forceRefresh: Boolean = false
    ): String? = withContext(Dispatchers.IO) {
        val normTitle = normalizeText(albumTitle)
        val normArtist = normalizeText(albumArtist)
        val key = albumKey ?: (normTitle + "|" + normArtist)

        if (normTitle.isBlank() || normTitle == "unknown album") {
            return@withContext null
        }

        val statsDb = LocalStatsDatabase.getInstance(context)
        if (!forceRefresh) {
            val record = statsDb.getAlbumImageRecord(key)
            if (record != null) {
                val (path, lastUpdated) = record
                if (path != null && path != "NOT_FOUND" && File(path).exists()) {
                    return@withContext path
                }
                if (path == "NOT_FOUND" && (System.currentTimeMillis() - lastUpdated) < THIRTY_DAYS_MS) {
                    return@withContext null
                }
            }
        }

        // Query iTunes Search API: https://itunes.apple.com/search?term=<album+artist>&entity=album&limit=5
        val searchQuery = if (normArtist.isNotBlank() && normArtist != "unknown artist" && normArtist != "various artists") {
            "$albumTitle $albumArtist"
        } else {
            albumTitle
        }
        val encodedTerm = URLEncoder.encode(searchQuery, "UTF-8")
        val url = URL("https://itunes.apple.com/search?term=$encodedTerm&entity=album&limit=5")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 10000
            setRequestProperty("User-Agent", "NorthMusic/1.0 (Android)")
        }

        var artworkUrl: String? = null
        try {
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val results = json.optJSONArray("results")
                if (results != null && results.length() > 0) {
                    var bestScore = -1
                    var bestUrl: String? = null

                    for (i in 0 until results.length()) {
                        val item = results.getJSONObject(i)
                        val collectionName = item.optString("collectionName", "")
                        val artistName = item.optString("artistName", "")
                        val art100 = item.optString("artworkUrl100", "")

                        if (art100.isBlank()) continue

                        val normItemTitle = normalizeText(collectionName)
                        val normItemArtist = normalizeText(artistName)

                        var score = 0
                        if (normItemTitle == normTitle) score += 3
                        else if (normItemTitle.contains(normTitle) || normTitle.contains(normItemTitle)) score += 1

                        if (normArtist.isNotBlank() && (normItemArtist == normArtist || normItemArtist.contains(normArtist) || normArtist.contains(normItemArtist))) {
                            score += 2
                        }

                        if (score > bestScore && score >= 2) {
                            bestScore = score
                            bestUrl = art100
                        }
                    }

                    if (bestUrl != null) {
                        artworkUrl = bestUrl.replace("100x100", "600x600")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        } finally {
            connection.disconnect()
        }

        if (artworkUrl.isNullOrBlank()) {
            statsDb.saveAlbumImage(key, "NOT_FOUND", "itunes")
            return@withContext null
        }

        // Download artwork file to filesDir/album_images/<hash>.jpg
        try {
            val dir = File(context.filesDir, "album_images").apply { if (!exists()) mkdirs() }
            val fileHash = getHash(key)
            val outputFile = File(dir, "$fileHash.jpg")
            val tempFile = File(dir, "$fileHash.tmp")

            val imgConn = (URL(artworkUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 15000
            }

            imgConn.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            imgConn.disconnect()

            if (tempFile.exists() && tempFile.length() > 0) {
                if (outputFile.exists()) outputFile.delete()
                tempFile.renameTo(outputFile)
                val absolutePath = outputFile.absolutePath
                statsDb.saveAlbumImage(key, absolutePath, "itunes")
                return@withContext absolutePath
            } else {
                tempFile.delete()
                statsDb.saveAlbumImage(key, "NOT_FOUND", "itunes")
                return@withContext null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
}
