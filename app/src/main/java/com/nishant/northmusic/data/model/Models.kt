package com.nishant.northmusic.data.model

import android.content.ContentUris
import android.net.Uri

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationSeconds: Int,
    val isFavorite: Boolean = false,
    val colorSeed: Long = 0xFF2DD4BF,
    val lyrics: List<LyricLine> = emptyList(),
    val albumId: Long = 0L,
    val artistId: Long = 0L,
    val filePath: String = "",
    val relativeFolder: String = "",
    val dateAdded: Long = 0L,
    val year: Int = 0,
    val sizeBytes: Long = 0L,
    val isHighQuality: Boolean = false,
    val genre: String = "",
    val trackNumber: Int = 0,
    val customArtUri: Uri? = null
) {
    val contentUri: Uri
        get() = ContentUris.withAppendedId(
            android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            id.toLongOrNull() ?: 0L
        )

    val albumArtUri: Uri?
        get() = customArtUri ?: if (albumId > 0L) {
            ContentUris.withAppendedId(
                Uri.parse("content://media/external/audio/albumart"),
                albumId
            )
        } else null

    val formattedDuration: String
        get() {
            val minutes = durationSeconds / 60
            val seconds = durationSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }

    val formattedCurrentPosition: String
        get() = formattedDuration
}

data class LyricLine(
    val timestampSeconds: Float = 0f,
    val text: String = "",
    val translation: String? = null,
    val timeSeconds: Int = timestampSeconds.toInt()
) {
    constructor(timeSeconds: Int, text: String) : this(
        timestampSeconds = timeSeconds.toFloat(),
        text = text,
        translation = null,
        timeSeconds = timeSeconds
    )
}

data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val songCount: Int,
    val year: Int,
    val colorSeed: Long = 0xFF2DD4BF,
    val albumId: Long = id.toLongOrNull() ?: 0L,
    val customArtUri: Uri? = null
) {
    val albumArtUri: Uri?
        get() = customArtUri ?: if (albumId > 0L) {
            ContentUris.withAppendedId(
                Uri.parse("content://media/external/audio/albumart"),
                albumId
            )
        } else null
}

data class Artist(
    val id: String,
    val name: String,
    val songCount: Int,
    val albumCount: Int,
    val colorSeed: Long = 0xFF2DD4BF,
    val savedPhotoUri: String? = null,
    val albumArtUris: List<Uri> = emptyList()
) {
    val firstAlbumArtUri: Uri?
        get() = albumArtUris.firstOrNull()
}

data class SongTagOverride(
    val songId: String,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val albumArtist: String? = null,
    val composer: String? = null,
    val genre: String? = null,
    val year: Int? = null,
    val track: Int? = null,
    val disc: Int? = null,
    val lyrics: String? = null,
    val comment: String? = null,
    val artworkPath: String? = null
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
    val colorSeed: Long = 0xFF2DD4BF,
    val artworkUrl: String? = null,
    val album: String = "",
    val filePath: String? = null
)

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

data class PlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPositionSeconds: Int = 0,
    val isShuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val isFavorite: Boolean = false,
    val volume: Float = 0.8f,
    val playbackSource: String? = null
) {
    val formattedCurrentPosition: String
        get() {
            val minutes = currentPositionSeconds / 60
            val seconds = currentPositionSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
    val progress: Float
        get() = if (currentSong != null && currentSong.durationSeconds > 0) {
            currentPositionSeconds.toFloat() / currentSong.durationSeconds.toFloat()
        } else 0f
}

enum class LibraryFilter(val label: String) {
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    PLAYLISTS("Playlists"),
    FOLDERS("Folders")
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIconRes: Int,
    val unselectedIconRes: Int
)

enum class ThemeMode(val label: String) {
    SYSTEM("Default"),
    DARK("Dark"),
    LIGHT("Light")
}

enum class AccentColor(val label: String, val hexColor: Long) {
    BLUE("Blue", 0xFF2563EB),
    TEAL("Teal", 0xFF0D9488),
    GREEN("Green", 0xFF16A34A),
    ORANGE("Orange", 0xFFEA580C),
    ROSE("Rose", 0xFFE11D48),
    RED("Red", 0xFFDC2626),
    PURPLE("Purple", 0xFF7C3AED)
}

enum class AppIcon(
    val id: String,
    val title: String,
    val aliasName: String,
    val primaryColorHex: Long,
    val description: String
) {
    BLUE("blue", "Default Blue", "MainActivityDefault", 0xFF2563EB, "Matches app UI blue accent"),
    TEAL("teal", "Nordic Teal", "MainActivityTeal", 0xFF14B8A6, "Classic North teal"),
    MIDNIGHT("midnight", "Midnight", "MainActivityMidnight", 0xFF0F172A, "Dark slate & blue"),
    PURPLE("purple", "Purple", "MainActivityPurple", 0xFF7C3AED, "Electric purple"),
    ROSE("rose", "Rose", "MainActivityRose", 0xFFE11D48, "Sunset rose"),
    BLACK("black", "Onyx Black", "MainActivityBlack", 0xFF000000, "Pure AMOLED dark");

    companion object {
        fun fromId(id: String?): AppIcon = entries.find { it.id == id } ?: BLUE
    }
}

enum class AudioOutputDevice(val label: String) {
    BLUETOOTH("Bluetooth"),
    WIRED("Wired"),
    SPEAKER("Phone speaker")
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
    val accentColor: AccentColor = AccentColor.BLUE,
    val appIcon: AppIcon = AppIcon.BLUE,
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
    val fadeOnPlayPause: Boolean = true,
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
    val splitArtistsOnAmpersand: Boolean = false,
    val dontSplitArtistNames: List<String> = listOf("AC/DC", "Guns N' Roses", "Simon & Garfunkel", "Earth, Wind & Fire"),
    val isRescanning: Boolean = false,
    val metadataFixState: MetadataFixState = MetadataFixState(),

    // 7. Downloads
    val saveFolder: String = "/storage/emulated/0/Music/Downloads",
    val wifiOnly: Boolean = false,
    val wifiOnlyDownloads: Boolean = wifiOnly,
    val parallelSlots: Int = 3,
    val maxParallelDownloads: Int = parallelSlots,
    val namingFormat: String = "{artist} - {title}", // "{artist} - {title}", "{track} {title}", etc.
    val fileNamingFormat: String = namingFormat,
    val downloadSpeedLimitKbps: Int = 0, // 0 = unlimited

    // 8. Network & Metadata
    val autoDownloadMetadata: Boolean = true,
    val onlineMetadataLookup: Boolean = autoDownloadMetadata,
    val downloadArtistAndAlbumArt: Boolean = false,
    val downloadOnlyOnWifi: Boolean = true,
    val preferredLyricsProvider: String = "LRC-Lib",
    val lyricsProviders: String = preferredLyricsProvider,
    val streamingQuality: String = "Auto",

    // 9. Advanced
    val appLanguage: String = "System default",
    val lockScreenRotation: Boolean = false,
    val rotationLock: Boolean = lockScreenRotation,
    val backupAndRestoreInfo: String = "No backup found",
    val lastBackupDate: String? = null,
    val setupDone: Boolean = false
)
