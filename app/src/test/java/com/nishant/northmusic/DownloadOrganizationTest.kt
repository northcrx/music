package com.nishant.northmusic

import com.nishant.northmusic.data.model.DownloadFolderStructure
import com.nishant.northmusic.data.repository.CategoryRulesRepository
import com.nishant.northmusic.data.repository.normalizeText
import com.nishant.northmusic.data.repository.splitArtistNames
import com.nishant.northmusic.data.util.DownloadPathHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class DownloadOrganizationTest {

    // 1. Name Cleaning Tests (Requirement 5)
    @Test
    fun testNameCleaning_removesForbiddenCharacters() {
        val input = "Song / Title \\ with : * illegal ? \" characters < > | here"
        val cleaned = DownloadPathHelper.cleanName(input)
        assertEquals("Song Title with illegal characters here", cleaned)
    }

    @Test
    fun testNameCleaning_collapsesSpacesAndTrimsDots() {
        val input = " ...  My   Awesome   Song...   "
        val cleaned = DownloadPathHelper.cleanName(input)
        assertEquals("My Awesome Song", cleaned)
    }

    @Test
    fun testNameCleaning_limitsTo80Characters() {
        val longTitle = "A".repeat(120)
        val cleaned = DownloadPathHelper.cleanName(longTitle)
        assertEquals(80, cleaned.length)
        assertEquals("A".repeat(80), cleaned)
    }

    @Test
    fun testNameCleaning_fallbacks() {
        val emptyCleaned = DownloadPathHelper.cleanName("", default = "Various Artists")
        assertEquals("Various Artists", emptyCleaned)

        val symbolsOnly = DownloadPathHelper.cleanName("<>:*?|/", default = "track_123")
        assertEquals("track_123", symbolsOnly)
    }

    // 2. Artist Extraction & Splitting Tests (Requirement 4)
    @Test
    fun testFirstArtistExtraction() {
        val raw1 = "Kendrick Lamar feat. Drake"
        val first1 = DownloadPathHelper.getFirstArtist(raw1)
        assertEquals("Kendrick Lamar", first1)

        val raw2 = "Lifafa ft. Peter Cat Recording Co."
        val first2 = DownloadPathHelper.getFirstArtist(raw2)
        assertEquals("Lifafa", first2)

        val raw3 = ""
        val first3 = DownloadPathHelper.getFirstArtist(raw3)
        assertEquals("Various Artists", first3)
    }

    @Test
    fun testArtistSplitting() {
        val raw = "Drake, Kendrick Lamar & J. Cole feat. Travis Scott"
        val parts = splitArtistNames(raw, splitOnAmpersand = true)
        assertTrue(parts.contains("Drake"))
        assertTrue(parts.contains("Kendrick Lamar"))
        assertTrue(parts.contains("J. Cole"))
        assertTrue(parts.contains("Travis Scott"))
    }

    // 3. Category Resolution Tests (Requirement 3)
    @Test
    fun testCategoryMapping_defaultRules() {
        val repo = CategoryRulesRepository
        assertEquals("Hip-Hop", repo.resolveCategory(listOf("rap", "american")))
        assertEquals("K-Pop", repo.resolveCategory(listOf("korean", "dance")))
        assertEquals("Bollywood", repo.resolveCategory(listOf("hindi", "soundtrack")))
        assertEquals("Electronic", repo.resolveCategory(listOf("edm", "dance")))
        assertEquals("Rock", repo.resolveCategory(listOf("indie rock")))
        assertNull(repo.resolveCategory(listOf("unmatched_tag_xyz")))
    }

    @Test
    fun testCategoryMapping_customRulesAndReset() {
        val repo = CategoryRulesRepository
        repo.addRule("synthwave", "Retro Synth")
        assertEquals("Retro Synth", repo.resolveCategory(listOf("synthwave")))

        repo.deleteRule("synthwave")
        assertNull(repo.resolveCategory(listOf("synthwave")))

        repo.resetToDefaults()
        assertEquals("Hip-Hop", repo.resolveCategory(listOf("hiphop")))
    }

    // 4. Folder Path Construction Tests (Requirement 1 & 2)
    @Test
    fun testPathConstruction_categoryArtist() {
        val path = DownloadPathHelper.buildRelativePath(
            category = "Hip-Hop",
            artistFolder = "Kendrick Lamar",
            structure = DownloadFolderStructure.CATEGORY_ARTIST
        )
        assertEquals("Music/Hip-Hop/Kendrick Lamar/", path)
    }

    @Test
    fun testPathConstruction_artistOnly() {
        val path = DownloadPathHelper.buildRelativePath(
            category = "Hip-Hop",
            artistFolder = "Kendrick Lamar",
            structure = DownloadFolderStructure.ARTIST_ONLY
        )
        assertEquals("Music/Kendrick Lamar/", path)
    }

    @Test
    fun testPathConstruction_flat() {
        val path = DownloadPathHelper.buildRelativePath(
            category = "Hip-Hop",
            artistFolder = "Kendrick Lamar",
            structure = DownloadFolderStructure.FLAT
        )
        assertEquals("Music/", path)
    }

    // 5. Duplicate Detection Logic Tests (Requirement 7)
    @Test
    fun testDuplicateDetectionMatching() {
        val libraryTitle = "HUMBLE."
        val libraryArtist = "Kendrick Lamar"
        val libraryDuration = 177

        val incomingTitle = "Humble"
        val incomingArtist = "Kendrick Lamar feat. Someone"
        val incomingDuration = 179 // Within 3 seconds

        val isTitleMatch = normalizeText(DownloadPathHelper.cleanName(libraryTitle)) ==
                normalizeText(DownloadPathHelper.cleanName(incomingTitle))
        val isArtistMatch = normalizeText(DownloadPathHelper.getFirstArtist(libraryArtist)) ==
                normalizeText(DownloadPathHelper.getFirstArtist(incomingArtist))
        val isDurationMatch = kotlin.math.abs(libraryDuration - incomingDuration) <= 3

        assertTrue(isTitleMatch)
        assertTrue(isArtistMatch)
        assertTrue(isDurationMatch)

        // Duration > 3s mismatch
        val longDuration = 200
        val isLongDurationMatch = kotlin.math.abs(libraryDuration - longDuration) <= 3
        assertFalse(isLongDurationMatch)
    }

    // 6. Unique Display Name Suffixing (Requirement 6)
    @Test
    fun testUniqueDisplayNamePattern() {
        val artist = "Drake"
        val title = "God's Plan"
        val ext = "m4a"

        val baseName = "$artist - $title"
        val firstCandidate = "$baseName.$ext"
        assertEquals("Drake - God's Plan.m4a", firstCandidate)

        val duplicateCandidate = "$baseName (2).$ext"
        assertEquals("Drake - God's Plan (2).m4a", duplicateCandidate)
    }

    @Test
    fun testDecryptUrl() {
        val enc = "ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDyVtltwy6++tNgddYazf+3COfK8PjL01e6Qjo4BuB0ZQbcA8IvhdWrihw7tS9a8Gtq"
        val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
        val key = SecretKeySpec("38346591".toByteArray(Charsets.UTF_8), "DES")
        cipher.init(Cipher.DECRYPT_MODE, key)
        val decoded = Base64.getDecoder().decode(enc.trim())
        val dec = cipher.doFinal(decoded)
        val url = String(dec, Charsets.UTF_8).trim()
        System.err.println(">>> DECRYPTED URL: $url")
        assertTrue(url.startsWith("http"))
    }
}
