package com.nishant.northmusic.data.model

object SampleData {
    val songs: List<Song> = listOf(
        Song(
            id = "1",
            title = "Starboy",
            artist = "The Weeknd ft. Daft Punk",
            album = "Starboy",
            durationSeconds = 230,
            colorSeed = 0xFF0D9488,
            isFavorite = true,
            isHighQuality = true,
            genre = "Pop"
        ),
        Song(
            id = "2",
            title = "Blinding Lights",
            artist = "The Weeknd",
            album = "After Hours",
            durationSeconds = 200,
            colorSeed = 0xFFB91C1C,
            isFavorite = true,
            isHighQuality = true,
            genre = "Synthwave"
        ),
        Song(
            id = "3",
            title = "Midnight City",
            artist = "M83",
            album = "Hurry Up, We're Dreaming",
            durationSeconds = 243,
            colorSeed = 0xFF15803D,
            isFavorite = false,
            isHighQuality = true,
            genre = "Electronic"
        ),
        Song(
            id = "4",
            title = "Resonant Horizons",
            artist = "Solaris Echo",
            album = "Kinetic Void",
            durationSeconds = 312,
            colorSeed = 0xFF0369A1,
            isFavorite = false,
            isHighQuality = true,
            genre = "Ambient"
        ),
        Song(
            id = "5",
            title = "After Hours (Live)",
            artist = "The Weeknd",
            album = "After Hours (Deluxe)",
            durationSeconds = 361,
            colorSeed = 0xFF334155,
            isFavorite = true,
            isHighQuality = true,
            genre = "R&B"
        ),
        Song(
            id = "6",
            title = "Nightcall",
            artist = "Kavinsky",
            album = "OutRun",
            durationSeconds = 259,
            colorSeed = 0xFF0E7490,
            isFavorite = false,
            isHighQuality = true,
            genre = "Synthwave"
        ),
        Song(
            id = "7",
            title = "Save Your Tears",
            artist = "The Weeknd",
            album = "After Hours",
            durationSeconds = 215,
            colorSeed = 0xFFC2410C,
            isFavorite = false,
            isHighQuality = true,
            genre = "Pop"
        ),
        Song(
            id = "8",
            title = "Instant Crush",
            artist = "Daft Punk ft. Julian Casablancas",
            album = "Random Access Memories",
            durationSeconds = 337,
            colorSeed = 0xFF4338CA,
            isFavorite = true,
            isHighQuality = true,
            genre = "Disco / Pop"
        ),
        Song(
            id = "9",
            title = "Chamber of Reflection",
            artist = "Mac DeMarco",
            album = "Salad Days",
            durationSeconds = 231,
            colorSeed = 0xFF0F766E,
            isFavorite = false,
            isHighQuality = true,
            genre = "Indie Rock"
        ),
        Song(
            id = "10",
            title = "Tech Noir",
            artist = "Gunship",
            album = "Gunship",
            durationSeconds = 297,
            colorSeed = 0xFFBE185D,
            isFavorite = false,
            isHighQuality = true,
            genre = "Synthwave"
        )
    )

    // Generate 200 items for smooth scrolling download performance tests
    val downloadingItems: List<DownloadItem> = (1..30).map { i ->
        DownloadItem(
            id = "d_active_$i",
            title = if (i == 1) "Starboy (Deluxe Edition)" else if (i == 2) "After Hours Master" else "Track $i (Lossless Remaster)",
            artist = if (i % 2 == 0) "The Weeknd" else "Solaris Echo",
            progress = if (i == 1) 0.68f else (0.1f + (i * 0.025f) % 0.85f),
            totalSizeMb = 12.4f + (i * 3.5f),
            downloadedMb = (12.4f + (i * 3.5f)) * (if (i == 1) 0.68f else (0.1f + (i * 0.025f) % 0.85f)),
            speedText = if (i % 3 == 0) "Paused" else "4.2 MB/s",
            status = DownloadStatus.DOWNLOADING,
            isPaused = (i % 3 == 0),
            isSelected = false,
            colorSeed = if (i % 2 == 0) 0xFF0D9488 else 0xFF0284C7
        )
    }

    val completedItems: List<DownloadItem> = (1..142).map { i ->
        DownloadItem(
            id = "d_completed_$i",
            title = when (i % 6) {
                0 -> "Hurry Up, We're Dreaming"
                1 -> "Random Access Memories"
                2 -> "Kinetic Void (EP)"
                3 -> "OutRun (Master Edition)"
                4 -> "Discovery (FLAC)"
                else -> "Cyber City Soundtrack vol. $i"
            },
            artist = when (i % 4) {
                0 -> "M83"
                1 -> "Daft Punk"
                2 -> "Kavinsky"
                else -> "Solaris Echo"
            },
            progress = 1.0f,
            totalSizeMb = 8.2f + (i * 1.5f) % 40f,
            downloadedMb = 8.2f + (i * 1.5f) % 40f,
            speedText = "Completed",
            status = DownloadStatus.COMPLETED,
            isPaused = false,
            isSelected = false,
            colorSeed = when (i % 4) {
                0 -> 0xFF15803D
                1 -> 0xFF0E7490
                2 -> 0xFF0369A1
                else -> 0xFF334155
            }
        )
    }

    val failedItems: List<DownloadItem> = (1..28).map { i ->
        DownloadItem(
            id = "d_failed_$i",
            title = if (i == 1) "OutRun (Master Edition)" else "Corrupted Packet Archive #$i",
            artist = if (i == 1) "Kavinsky" else "Unknown Artist",
            progress = 0.15f,
            totalSizeMb = 110.0f,
            downloadedMb = 16.5f,
            speedText = "Failed",
            status = DownloadStatus.FAILED,
            errorMessage = if (i % 2 == 0) "Network timeout - peer disconnected" else "HTTP 503 Service Unavailable",
            isPaused = true,
            isSelected = false,
            colorSeed = 0xFFB91C1C
        )
    }

    val initialPlayerState = PlayerState(
        currentSong = songs.first(),
        isPlaying = true,
        currentPositionSeconds = 64,
        isShuffle = false,
        repeatMode = RepeatMode.OFF,
        isFavorite = true
    )

    val sampleLyrics: List<LyricLine> = listOf(
        LyricLine(0, "♪ (Intro - Night Synthesizer) ♪"),
        LyricLine(12, "I'm tryna put you in the worst mood, ah"),
        LyricLine(16, "P1 cleaner than your church shoes, ah"),
        LyricLine(20, "Milli point two just to hurt you, ah"),
        LyricLine(25, "All red Lamb' just to tease you, ah"),
        LyricLine(29, "None of these toys on lease too, ah"),
        LyricLine(33, "Made your whole year in a week too, yah"),
        LyricLine(38, "Main girl out of your league too, ah"),
        LyricLine(42, "Side girl out of your league too, ah"),
        LyricLine(46, "House so empty, need a centerpiece"),
        LyricLine(50, "Twenty racks a table, cut from ebony"),
        LyricLine(54, "Cut that sound into pristine pieces"),
        LyricLine(58, "Then we ride through the city, man, I love this feeling"),
        LyricLine(62, "You talkin' money, need a hearing aid"),
        LyricLine(66, "You talkin' 'bout me, I don't see a shade"),
        LyricLine(70, "Switch up my style, I take any lane"),
        LyricLine(74, "I switch up my sound, I kill any pain"),
        LyricLine(79, "Look what you've done"),
        LyricLine(83, "I'm a nocturnal starboy"),
        LyricLine(87, "Look what you've done"),
        LyricLine(91, "I'm a nocturnal starboy"),
        LyricLine(96, "Every day they try to test me, ah"),
        LyricLine(100, "Every day they try to chase me, ah"),
        LyricLine(104, "Pull off in that Roadster SV, ah"),
        LyricLine(109, "Pockets overweight, gettin' hefty, ah"),
        LyricLine(114, "Coming for the crown, that's a far cry, ah"),
        LyricLine(118, "I come alive in the fall time, I"),
        LyricLine(122, "No competition, don't hesitate"),
        LyricLine(126, "Let's cruise into midnight")
    )
}
