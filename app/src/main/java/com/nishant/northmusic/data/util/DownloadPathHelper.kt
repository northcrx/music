package com.nishant.northmusic.data.util

import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.nishant.northmusic.data.local.LocalStatsDatabase
import com.nishant.northmusic.data.model.DownloadFolderStructure
import com.nishant.northmusic.data.repository.CategoryRulesRepository
import com.nishant.northmusic.data.repository.SettingsRepository
import com.nishant.northmusic.data.repository.normalizeText
import com.nishant.northmusic.data.repository.splitArtistNames
import java.io.File

object DownloadPathHelper {

    /**
     * Cleans folder or file names:
     * - Removes / \ : * ? " < > | and control characters
     * - Collapses repeated spaces
     * - Trims dots and spaces at the ends
     * - Limits name to 80 characters
     */
    fun cleanName(input: String?, default: String = "", maxLen: Int = 80): String {
        if (input.isNullOrBlank()) return default
        val invalidCharsRegex = Regex("""[\\/:*?"<>|\u0000-\u001F\u007F-\u009F]""")
        val noInvalid = invalidCharsRegex.replace(input, "")
        val collapsed = Regex("""\s+""").replace(noInvalid, " ")
        val trimmed = collapsed.trim(' ', '.')
        if (trimmed.isBlank()) return default
        return trimmed.take(maxLen).trim(' ', '.').ifBlank { default }
    }

    /**
     * Extracts the primary (first) artist from raw artist string using Library splitting rules.
     */
    fun getFirstArtist(rawArtist: String?): String {
        if (rawArtist.isNullOrBlank()) return "Various Artists"
        val settings = SettingsRepository.current
        val splitList = splitArtistNames(
            rawArtist = rawArtist,
            splitOnAmpersand = settings.splitArtistsOnAmpersand,
            dontSplitList = settings.dontSplitArtistNames
        )
        val first = splitList.firstOrNull { it.isNotBlank() } ?: "Various Artists"
        return cleanName(first, default = "Various Artists", maxLen = 80)
    }

    /**
     * Resolves category in strict order:
     * 1) Category already saved for this artist in artist_category table
     * 2) Mapping table matching against provided tags/genre tokens
     * 3) If nothing matches, returns "Other" (never "Unknown")
     */
    suspend fun resolveCategory(
        context: Context,
        firstArtist: String,
        sourceTags: List<String>
    ): String {
        val normArtist = normalizeText(firstArtist)
        val statsDb = LocalStatsDatabase.getInstance(context)

        // 1. Check artist_category DB
        val savedCategory = statsDb.getArtistCategory(normArtist)
        if (!savedCategory.isNullOrBlank()) {
            return savedCategory
        }

        // 2. Check mapping table
        val mappedCategory = CategoryRulesRepository.resolveCategory(sourceTags)
        if (!mappedCategory.isNullOrBlank()) {
            return mappedCategory
        }

        // 3. Fallback
        return "Other"
    }

    /**
     * Normalizes to find an existing folder under Music/<Category>/ (or Music/):
     * Compares case-insensitively and without accents against existing folders in MediaStore & storage,
     * reusing that exact folder name instead of creating "lifafa" next to "Lifafa".
     */
    fun resolveArtistFolder(
        context: Context,
        category: String,
        firstArtistClean: String,
        structure: DownloadFolderStructure
    ): String {
        if (structure == DownloadFolderStructure.FLAT) {
            return ""
        }

        val normTarget = normalizeText(firstArtistClean)
        val candidateFolders = mutableSetOf<String>()

        // 1. Check file system directories under Music
        try {
            val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            val baseParent = when (structure) {
                DownloadFolderStructure.CATEGORY_ARTIST -> File(musicDir, category)
                DownloadFolderStructure.ARTIST_ONLY -> musicDir
                DownloadFolderStructure.FLAT -> musicDir
            }
            if (baseParent.exists() && baseParent.isDirectory) {
                baseParent.listFiles { file -> file.isDirectory }?.forEach { dir ->
                    candidateFolders.add(dir.name)
                }
            }
        } catch (_: Exception) {}

        // 2. Check MediaStore RELATIVE_PATH
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val prefix = when (structure) {
                    DownloadFolderStructure.CATEGORY_ARTIST -> "Music/$category/"
                    DownloadFolderStructure.ARTIST_ONLY -> "Music/"
                    DownloadFolderStructure.FLAT -> "Music/"
                }
                val projection = arrayOf(MediaStore.Audio.Media.RELATIVE_PATH)
                val cursor = context.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?",
                    arrayOf("$prefix%"),
                    null
                )
                cursor?.use {
                    val pathIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH)
                    while (it.moveToNext()) {
                        val relPath = it.getString(pathIndex) ?: continue
                        if (relPath.startsWith(prefix)) {
                            val sub = relPath.removePrefix(prefix).trimStart('/')
                            val folderName = sub.substringBefore('/').trim()
                            if (folderName.isNotBlank()) {
                                candidateFolders.add(folderName)
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // Compare case-insensitively and without accents
        for (existing in candidateFolders) {
            if (normalizeText(existing) == normTarget) {
                return existing // Reuse exact existing folder name!
            }
        }

        return firstArtistClean
    }

    /**
     * Builds relative path: e.g. "Music/Hip-Hop/Kendrick Lamar/"
     */
    fun buildRelativePath(
        category: String,
        artistFolder: String,
        structure: DownloadFolderStructure
    ): String {
        return when (structure) {
            DownloadFolderStructure.CATEGORY_ARTIST -> "Music/$category/$artistFolder/"
            DownloadFolderStructure.ARTIST_ONLY -> "Music/$artistFolder/"
            DownloadFolderStructure.FLAT -> "Music/"
        }
    }

    /**
     * Resolves unique file display name: "<Artist> - <Title>.<ext>"
     * If file already exists, appends " (2)", " (3)", etc.
     */
    fun resolveUniqueDisplayName(
        context: Context,
        relativePath: String,
        artistClean: String,
        titleClean: String,
        extension: String
    ): String {
        var baseName = "$artistClean - $titleClean"
        var candidate = "$baseName.$extension"
        var counter = 2

        while (isFileNameTaken(context, relativePath, candidate)) {
            candidate = "$baseName ($counter).$extension"
            counter++
        }

        return candidate
    }

    private fun isFileNameTaken(
        context: Context,
        relativePath: String,
        fileName: String
    ): Boolean {
        // Check filesystem
        try {
            val pubMusic = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            val targetDir = File(pubMusic, relativePath.removePrefix("Music/").removePrefix("Music"))
            val targetFile = File(targetDir, fileName)
            if (targetFile.exists()) return true
        } catch (_: Exception) {}

        // Check MediaStore
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val projection = arrayOf(MediaStore.Audio.Media._ID)
                val cursor = context.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    "${MediaStore.Audio.Media.RELATIVE_PATH} = ? AND ${MediaStore.Audio.Media.DISPLAY_NAME} = ?",
                    arrayOf(relativePath, fileName),
                    null
                )
                cursor?.use {
                    if (it.count > 0) return true
                }
            } catch (_: Exception) {}
        }

        return false
    }
}
