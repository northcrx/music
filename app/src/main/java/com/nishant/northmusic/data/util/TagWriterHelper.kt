package com.nishant.northmusic.data.util

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import com.nishant.northmusic.data.local.LocalStatsDatabase
import com.nishant.northmusic.data.model.Song
import com.nishant.northmusic.data.model.SongTagOverride
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.TagOptionSingleton
import org.jaudiotagger.tag.images.AndroidArtwork
import java.io.File
import java.io.FileInputStream

data class TagEditData(
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String,
    val composer: String,
    val genre: String,
    val year: String,
    val track: String,
    val disc: String,
    val lyrics: String,
    val comment: String,
    val artworkUri: Uri? = null,
    val artworkPath: String? = null
)

object TagWriterHelper {

    suspend fun saveAppOnly(
        context: Context,
        songId: String,
        tags: TagEditData
    ) = withContext(Dispatchers.IO) {
        val statsDb = LocalStatsDatabase.getInstance(context)
        val override = SongTagOverride(
            songId = songId,
            title = tags.title.trim().ifEmpty { null },
            artist = tags.artist.trim().ifEmpty { null },
            album = tags.album.trim().ifEmpty { null },
            albumArtist = tags.albumArtist.trim().ifEmpty { null },
            composer = tags.composer.trim().ifEmpty { null },
            genre = tags.genre.trim().ifEmpty { null },
            year = tags.year.trim().toIntOrNull(),
            track = tags.track.trim().toIntOrNull(),
            disc = tags.disc.trim().toIntOrNull(),
            lyrics = tags.lyrics.trim().ifEmpty { null },
            comment = tags.comment.trim().ifEmpty { null },
            artworkPath = tags.artworkPath ?: tags.artworkUri?.toString()
        )
        statsDb.saveSongTagOverride(override)
    }

    suspend fun writeToFile(
        context: Context,
        song: Song,
        tags: TagEditData
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Ensure Jaudiotagger operates in pure Android mode (no java.awt/ImageIO dependencies)
            try {
                TagOptionSingleton.getInstance().isAndroid = true
            } catch (_: Throwable) {}

            val srcFile = File(song.filePath)
            if (!srcFile.exists()) {
                // If physical file doesn't exist at path, persist in SQLite app-only
                saveAppOnly(context, song.id, tags)
                return@withContext Result.success(Unit)
            }

            // Safe temp-file copy to prevent corruption if interrupted
            val ext = srcFile.extension.ifBlank { "mp3" }
            val tempFile = File(context.cacheDir, "tag_edit_temp_${System.currentTimeMillis()}.$ext")
            srcFile.copyTo(tempFile, overwrite = true)

            try {
                val audioFile = AudioFileIO.read(tempFile)
                val tag = audioFile.tagOrCreateAndSetDefault

                if (tags.title.isNotBlank()) tag.setField(FieldKey.TITLE, tags.title.trim())
                if (tags.artist.isNotBlank()) tag.setField(FieldKey.ARTIST, tags.artist.trim())
                if (tags.album.isNotBlank()) tag.setField(FieldKey.ALBUM, tags.album.trim())
                if (tags.albumArtist.isNotBlank()) tag.setField(FieldKey.ALBUM_ARTIST, tags.albumArtist.trim())
                if (tags.composer.isNotBlank()) tag.setField(FieldKey.COMPOSER, tags.composer.trim())
                if (tags.genre.isNotBlank()) tag.setField(FieldKey.GENRE, tags.genre.trim())
                if (tags.year.isNotBlank()) tag.setField(FieldKey.YEAR, tags.year.trim())
                if (tags.track.isNotBlank()) tag.setField(FieldKey.TRACK, tags.track.trim())
                if (tags.disc.isNotBlank()) tag.setField(FieldKey.DISC_NO, tags.disc.trim())
                if (tags.lyrics.isNotBlank()) tag.setField(FieldKey.LYRICS, tags.lyrics.trim())
                if (tags.comment.isNotBlank()) tag.setField(FieldKey.COMMENT, tags.comment.trim())

                val artPath = tags.artworkPath
                if (!artPath.isNullOrBlank() && File(artPath).exists()) {
                    try {
                        val artFile = File(artPath)
                        val artwork = AndroidArtwork().apply {
                            setFromFile(artFile)
                        }
                        try {
                            tag.deleteArtworkField()
                        } catch (_: Throwable) {}
                        tag.setField(artwork)
                    } catch (t: Throwable) {
                        t.printStackTrace()
                    }
                }

                audioFile.commit()

                // Safe overwrite of destination
                var wroteDirectly = false
                if (srcFile.canWrite()) {
                    tempFile.copyTo(srcFile, overwrite = true)
                    wroteDirectly = true
                } else {
                    context.contentResolver.openOutputStream(song.contentUri)?.use { out ->
                        FileInputStream(tempFile).use { input ->
                            input.copyTo(out)
                        }
                        wroteDirectly = true
                    }
                }

                tempFile.delete()

                if (wroteDirectly) {
                    MediaScannerConnection.scanFile(context, arrayOf(srcFile.absolutePath), null, null)
                }

                // Always save to database for instant UI consistency
                saveAppOnly(context, song.id, tags)
                Result.success(Unit)
            } catch (t: Throwable) {
                tempFile.delete()
                saveAppOnly(context, song.id, tags)
                Result.failure(Exception(t.message ?: "Failed writing audio tags", t))
            }
        } catch (t: Throwable) {
            saveAppOnly(context, song.id, tags)
            Result.failure(Exception(t.message ?: "Failed accessing audio file", t))
        }
    }

    suspend fun writeDirectTagsToFile(
        file: File,
        title: String,
        artist: String,
        album: String = "",
        genre: String = "",
        comment: String = "",
        artworkFile: File? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            try {
                TagOptionSingleton.getInstance().isAndroid = true
            } catch (_: Throwable) {}

            val audioFile = AudioFileIO.read(file)
            val tag = audioFile.tagOrCreateAndSetDefault

            if (title.isNotBlank()) tag.setField(FieldKey.TITLE, title.trim())
            if (artist.isNotBlank()) tag.setField(FieldKey.ARTIST, artist.trim())
            if (album.isNotBlank()) tag.setField(FieldKey.ALBUM, album.trim())
            if (genre.isNotBlank()) tag.setField(FieldKey.GENRE, genre.trim())
            if (comment.isNotBlank()) tag.setField(FieldKey.COMMENT, comment.trim())

            if (artworkFile != null && artworkFile.exists() && artworkFile.length() > 0) {
                try {
                    val artwork = AndroidArtwork().apply {
                        setFromFile(artworkFile)
                    }
                    try {
                        tag.deleteArtworkField()
                    } catch (_: Throwable) {}
                    tag.setField(artwork)
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            }

            audioFile.commit()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
