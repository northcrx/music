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

object ArtistPhotoDownloader {

    private const val THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000L

    fun getHash(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val bytes = md.digest(input.lowercase().trim().toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun fetchAndSaveArtistPhoto(
        context: Context,
        artistName: String,
        forceRefresh: Boolean = false
    ): String? = withContext(Dispatchers.IO) {
        val trimmed = artistName.trim()
        if (trimmed.isBlank() || trimmed.equals("Unknown Artist", ignoreCase = true)) {
            return@withContext null
        }

        val statsDb = LocalStatsDatabase.getInstance(context)
        if (!forceRefresh) {
            val record = statsDb.getArtistImageRecord(trimmed)
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

        // Query Deezer API: https://api.deezer.com/search/artist?q=<name>
        val encodedQuery = URLEncoder.encode(trimmed, "UTF-8")
        val url = URL("https://api.deezer.com/search/artist?q=$encodedQuery")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 10000
            setRequestProperty("User-Agent", "NorthMusic/1.0 (Android)")
        }

        var pictureUrl: String? = null
        try {
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val data = json.optJSONArray("data")
                if (data != null && data.length() > 0) {
                    val normalizedTarget = normalizeText(trimmed)
                    for (i in 0 until data.length()) {
                        val item = data.getJSONObject(i)
                        val name = item.optString("name", "")
                        // Only accept a result when the normalized name matches exactly; skip when unsure
                        if (normalizeText(name) == normalizedTarget) {
                            val picXl = if (item.has("picture_xl")) item.optString("picture_xl") else null
                            val picBig = if (item.has("picture_big")) item.optString("picture_big") else null
                            val picMed = if (item.has("picture_medium")) item.optString("picture_medium") else null
                            pictureUrl = when {
                                !picXl.isNullOrBlank() -> picXl
                                !picBig.isNullOrBlank() -> picBig
                                !picMed.isNullOrBlank() -> picMed
                                else -> null
                            }
                            if (!pictureUrl.isNullOrBlank()) {
                                break
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        } finally {
            connection.disconnect()
        }

        if (pictureUrl.isNullOrBlank()) {
            statsDb.saveArtistImage(trimmed, "NOT_FOUND", "deezer")
            return@withContext null
        }

        // Download the image and save to filesDir/artist_images/<hash>.jpg
        try {
            val dir = File(context.filesDir, "artist_images").apply { if (!exists()) mkdirs() }
            val fileHash = getHash(trimmed)
            val outputFile = File(dir, "$fileHash.jpg")
            val tempFile = File(dir, "$fileHash.tmp")

            val imgConn = (URL(pictureUrl).openConnection() as HttpURLConnection).apply {
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
                statsDb.saveArtistImage(trimmed, absolutePath, "deezer")
                return@withContext absolutePath
            } else {
                tempFile.delete()
                statsDb.saveArtistImage(trimmed, "NOT_FOUND", "deezer")
                return@withContext null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
}
