package com.nishant.northmusic

import com.nishant.northmusic.data.network.CdnMusicService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistDetectionTest {

    @Test
    fun testExtractFirstUrlFromShareSheetText() {
        val ytShareText = "Check out this playlist on YouTube Music: https://music.youtube.com/playlist?list=PL4fGSIndQPTjH28Vf6D_8j_1eS7j89X1k"
        val spotifyShareText = "Listen to Top 50 India on Spotify: https://open.spotify.com/playlist/37i9dQZF1DX0XUfTFmNBRM?si=123abc456"

        val ytUrl = CdnMusicService.extractFirstUrl(ytShareText)
        val spotifyUrl = CdnMusicService.extractFirstUrl(spotifyShareText)

        assertNotNull(ytUrl)
        assertTrue(ytUrl!!.startsWith("https://music.youtube.com/playlist?list="))

        assertNotNull(spotifyUrl)
        assertTrue(spotifyUrl!!.startsWith("https://open.spotify.com/playlist/"))
    }

    @Test
    fun testIsPlaylistUrlForSupportedPlatforms() {
        // Spotify Playlist & Album
        assertTrue(CdnMusicService.isPlaylistUrl("https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M"))
        assertTrue(CdnMusicService.isPlaylistUrl("https://open.spotify.com/intl-ja/playlist/37i9dQZF1DXcBWIGoYBM5M?si=xyz"))
        assertTrue(CdnMusicService.isPlaylistUrl("https://open.spotify.com/album/4LH4d3cOWNNXdsqFdFamSV"))

        // YouTube & YouTube Music Playlists
        assertTrue(CdnMusicService.isPlaylistUrl("https://www.youtube.com/playlist?list=PLrEnWoR732-BHrPp_CXUjiutQD69TLFea"))
        assertTrue(CdnMusicService.isPlaylistUrl("https://music.youtube.com/playlist?list=PLrEnWoR732-BHrPp_CXUjiutQD69TLFea"))
        assertTrue(CdnMusicService.isPlaylistUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=PLrEnWoR732-BHrPp_CXUjiutQD69TLFea"))

        // Single tracks should NOT be flagged as playlists
        assertFalse(CdnMusicService.isPlaylistUrl("https://open.spotify.com/track/4cOdK2wGLETKBW3PvgPWqT"))
        assertFalse(CdnMusicService.isPlaylistUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertFalse(CdnMusicService.isPlaylistUrl("https://youtu.be/dQw4w9WgXcQ"))
    }
}
