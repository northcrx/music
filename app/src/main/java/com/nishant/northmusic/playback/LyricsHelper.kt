package com.nishant.northmusic.playback

import android.content.Context
import android.media.MediaMetadataRetriever
import com.nishant.northmusic.data.model.LyricLine
import com.nishant.northmusic.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.regex.Pattern

object LyricsHelper {

    private val LRC_REGEX = Pattern.compile("""\[(\d{1,2}):(\d{1,2}(?:\.\d{1,3})?)\](.*)""")

    suspend fun getLyricsForSong(context: Context, song: Song): List<LyricLine> = withContext(Dispatchers.IO) {
        if (song.filePath.startsWith("http://") || song.filePath.startsWith("https://")) {
            return@withContext emptyList()
        }

        // 1. Check for companion .lrc file next to audio file
        if (song.filePath.isNotBlank()) {
            try {
                val lrcFile = File(song.filePath.substringBeforeLast('.') + ".lrc")
                if (lrcFile.exists() && lrcFile.canRead()) {
                    val content = lrcFile.readText()
                    val parsed = parseLrc(content)
                    if (parsed.isNotEmpty()) {
                        return@withContext parsed
                    }
                }
            } catch (_: Exception) {}
        }

        // 2. Check embedded ID3/MP4 lyrics metadata tag if supported
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, song.contentUri)
            // 1000 is often used by custom media engines for lyrics tags
            val embedded = retriever.extractMetadata(1000)
            retriever.release()
            if (!embedded.isNullOrBlank()) {
                val parsed = parseLrc(embedded)
                if (parsed.isNotEmpty()) {
                    return@withContext parsed
                } else {
                    // Plain text lyrics without timestamp tags
                    return@withContext parsePlainText(embedded, song.durationSeconds)
                }
            }
        } catch (_: Exception) {}

        // 3. Fallback: No lyrics found (UI displays empty state)
        emptyList()
    }

    private fun parseLrc(content: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        content.lineSequence().forEach { rawLine ->
            val matcher = LRC_REGEX.matcher(rawLine.trim())
            if (matcher.matches()) {
                val minStr = matcher.group(1) ?: "0"
                val secStr = matcher.group(2) ?: "0"
                val text = matcher.group(3)?.trim() ?: ""

                val minutes = minStr.toIntOrNull() ?: 0
                val seconds = secStr.toDoubleOrNull() ?: 0.0
                val totalSeconds = (minutes * 60 + seconds).toInt()

                if (text.isNotBlank()) {
                    lines.add(LyricLine(timeSeconds = totalSeconds, text = text))
                }
            }
        }
        return lines.sortedBy { it.timeSeconds }
    }

    private fun parsePlainText(text: String, durationSeconds: Int): List<LyricLine> {
        val rawLines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (rawLines.isEmpty()) return emptyList()

        val step = if (durationSeconds > 0) (durationSeconds / rawLines.size).coerceAtLeast(3) else 4
        return rawLines.mapIndexed { index, line ->
            LyricLine(timeSeconds = index * step, text = line)
        }
    }
}
