package com.nishant.northmusic.data.model

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationSeconds: Int,
    val colorSeed: Long = 0xFF2DD4BF,
    val isFavorite: Boolean = false,
    val isHighQuality: Boolean = true,
    val genre: String = "Pop"
) {
    val formattedDuration: String
        get() {
            val minutes = durationSeconds / 60
            val seconds = durationSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
}

data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val songCount: Int,
    val year: Int,
    val colorSeed: Long = 0xFF2DD4BF
)

data class Artist(
    val id: String,
    val name: String,
    val songCount: Int,
    val albumCount: Int,
    val colorSeed: Long = 0xFF2DD4BF
)

data class Playlist(
    val id: String,
    val title: String,
    val songCount: Int,
    val colorSeed: Long = 0xFF2DD4BF
)

data class FolderItem(
    val name: String,
    val path: String,
    val songCount: Int
)

enum class DownloadStatus {
    DOWNLOADING,
    COMPLETED,
    FAILED
}

data class DownloadItem(
    val id: String,
    val title: String,
    val artist: String,
    val progress: Float, // 0.0 to 1.0
    val totalSizeMb: Float,
    val downloadedMb: Float,
    val speedText: String,
    val status: DownloadStatus,
    val errorMessage: String? = null,
    val isPaused: Boolean = false,
    val isSelected: Boolean = false,
    val colorSeed: Long = 0xFF2DD4BF
)

enum class LibraryFilter(val label: String) {
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    PLAYLISTS("Playlists"),
    FOLDERS("Folders")
}

enum class RepeatMode {
    OFF,
    ONE,
    ALL
}

data class PlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPositionSeconds: Int = 42,
    val isShuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val isFavorite: Boolean = false
) {
    val formattedCurrentPosition: String
        get() {
            val minutes = currentPositionSeconds / 60
            val seconds = currentPositionSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
}

data class LyricLine(
    val timeSeconds: Int,
    val text: String
)

enum class AudioOutputDevice(val label: String) {
    BLUETOOTH("Bluetooth"),
    WIRED("Wired"),
    SPEAKER("Speaker")
}

enum class EqualizerPreset(val label: String, val bandGains: List<Float>) {
    FLAT("Flat", listOf(0f, 0f, 0f, 0f, 0f)),
    BASS_BOOST("Bass boost", listOf(6f, 4.5f, 1f, 0f, -1f)),
    VOCAL("Vocal", listOf(-2f, 1f, 4.5f, 3f, 1f)),
    TREBLE("Treble", listOf(-3f, -1f, 1f, 4f, 6.5f)),
    ROCK("Rock", listOf(5f, 3f, -1f, 2f, 4.5f)),
    POP("Pop", listOf(2f, 4f, 2f, 1f, 3f)),
    JAZZ("Jazz", listOf(3f, 2.5f, -1f, 2f, 3.5f)),
    CLASSICAL("Classical", listOf(4f, 3f, -1f, 2.5f, 3.5f))
}

data class DeviceProfile(
    val isEnabled: Boolean = true,
    val isAutoMode: Boolean = true,
    val selectedPreset: EqualizerPreset = EqualizerPreset.POP,
    val bandGains: List<Float> = listOf(2f, 4f, 2f, 1f, 3f), // in dB (-10.0f .. +10.0f)
    val bassBoostPercent: Float = 0.55f, // 0.0 .. 1.0
    val virtualizerPercent: Float = 0.35f // 0.0 .. 1.0
)

data class EqualizerState(
    val isMasterEnabled: Boolean = true,
    val currentDevice: AudioOutputDevice = AudioOutputDevice.BLUETOOTH,
    val detectedGenre: String = "Pop",
    val suggestedPresetTitle: String = "Bass lift",
    val profiles: Map<AudioOutputDevice, DeviceProfile> = mapOf(
        AudioOutputDevice.BLUETOOTH to DeviceProfile(
            isEnabled = true,
            isAutoMode = true,
            selectedPreset = EqualizerPreset.POP,
            bandGains = listOf(2f, 4f, 2f, 1f, 3f),
            bassBoostPercent = 0.60f,
            virtualizerPercent = 0.40f
        ),
        AudioOutputDevice.WIRED to DeviceProfile(
            isEnabled = true,
            isAutoMode = true,
            selectedPreset = EqualizerPreset.FLAT,
            bandGains = listOf(0f, 0f, 0f, 0f, 0f),
            bassBoostPercent = 0.0f,
            virtualizerPercent = 0.0f
        ),
        AudioOutputDevice.SPEAKER to DeviceProfile(
            isEnabled = true,
            isAutoMode = true,
            selectedPreset = EqualizerPreset.VOCAL,
            bandGains = listOf(-2f, 1f, 4.5f, 3f, 1f),
            bassBoostPercent = 0.10f,
            virtualizerPercent = 0.0f
        )
    )
) {
    val activeProfile: DeviceProfile
        get() = profiles[currentDevice] ?: DeviceProfile()
}

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    DARK("Dark"),
    LIGHT("Light")
}

enum class AccentColor(val label: String, val hexColor: Long) {
    TEAL("Teal", 0xFF2DD4BF),
    BLUE("Blue", 0xFF388AF6),
    GREEN("Green", 0xFF4CAF50),
    ORANGE("Orange", 0xFFFF9800),
    ROSE("Rose", 0xFFF43F5E),
    RED("Red", 0xFFEF4444),
    PURPLE("Purple", 0xFF8B5CF6)
}

data class MetadataFixItem(
    val songTitle: String,
    val artist: String,
    val fixDescription: String,
    val isDone: Boolean
)

data class MetadataFixState(
    val isScanning: Boolean = false,
    val progress: Float = 0f,
    val statusText: String = "",
    val items: List<MetadataFixItem> = emptyList()
)

data class SettingsState(
    // 1. Navigation
    val holdTabToSearch: Boolean = false,
    val rememberLastPage: Boolean = true,
    val lastOpenedRoute: String = "home",

    // 2. Appearance
    val dynamicColor: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val accentColor: AccentColor = AccentColor.TEAL,
    val largerHeaderImage: Boolean = false,
    val tabTitlesMode: String = "Always", // "Always", "Selected", "Never"
    val scrollingTextForLongTitles: Boolean = true,

    // 3. Now Playing
    val animateControls: Boolean = true,
    val extraControlsMiniPlayer: Boolean = false,
    val adaptiveControls: Boolean = true,
    val squigglySeekBar: Boolean = false, // default off
    val preferRemainingTime: Boolean = false,

    // 4. Lyrics
    val showTranslation: Boolean = false,
    val showTransliteration: Boolean = false,
    val centerCurrentLine: Boolean = true,
    val centerLyricsHorizontally: Boolean = true,
    val lyricsFontSize: Float = 18f,
    val customFont: String = "Default",

    // 5. Playback
    val gaplessPlayback: Boolean = true,
    val audioNormalization: Boolean = true,
    val highQualityAudio: Boolean = true,
    val replayGainMode: String = "None", // "Album", "Track", "None"
    val headphonesResumeOnConnect: Boolean = false,
    val headphonesPauseOnDisconnect: Boolean = true,
    val bluetoothResumeOnConnect: Boolean = false,
    val bluetoothPauseOnDisconnect: Boolean = true,
    val audioFocus: Boolean = true,

    // 6. Library
    val libraryCategories: List<String> = listOf("Songs", "Albums", "Artists", "Playlists", "Folders"),
    val minSongDurationSeconds: Int = 15,
    val artistMinSongCount: Int = 1,
    val albumMinSongCount: Int = 1,
    val foldersBlacklist: List<String> = emptyList(),
    val ignoreArticlesWhenSorting: Boolean = true,
    val sendDeletedToTrash: Boolean = true,
    val recordListeningHistory: Boolean = true,
    val historyIntervalDays: Int = 30,
    val lastAddedIntervalDays: Int = 14,
    val ignoreShortTracks: Boolean = true,
    val musicFolders: List<String> = listOf("/storage/emulated/0/Music", "/storage/emulated/0/Download"),
    val isRescanning: Boolean = false,
    val metadataFixState: MetadataFixState = MetadataFixState(),

    // 7. Downloads
    val saveFolder: String = "/storage/emulated/0/Music/Downloads",
    val wifiOnlyDownloads: Boolean = true,
    val maxParallelDownloads: Int = 3,
    val fileNamingFormat: String = "{artist} - {title}",

    // 8. Network
    val onlineMetadataLookup: Boolean = true,
    val lyricsProviders: String = "LRC Lib, NetEase",

    // 9. Advanced & Setup
    val appLanguage: String = "System default",
    val backupAndRestoreInfo: String = "No backup found",
    val rotationLock: Boolean = false,
    val setupDone: Boolean = false
)
