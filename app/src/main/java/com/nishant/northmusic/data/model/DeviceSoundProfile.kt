package com.nishant.northmusic.data.model

enum class DeviceCategory(val label: String) {
    HOME_THEATER("Home Theater / Soundbar"),
    NECKBAND("Neckband"),
    HEADPHONES("Headphones"),
    EARBUDS("TWS Earbuds"),
    GENERIC("Generic Audio")
}

data class DeviceSoundProfile(
    val id: String,
    val name: String,
    val brand: String,
    val category: DeviceCategory,
    val matchKeywords: List<String>,
    val description: String,
    val bandGains: List<Float>, // 5-band gains in dB: 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz
    val bassBoostPercent: Float = 0.25f,
    val virtualizerPercent: Float = 0.35f
)

object DeviceSoundProfiles {

    val SAMSUNG_B_SERIES = DeviceSoundProfile(
        id = "samsung_b_series",
        name = "Samsung B-Series Soundbar (B67EF / B670 / B550)",
        brand = "Samsung",
        category = DeviceCategory.HOME_THEATER,
        matchKeywords = listOf("b67", "b670", "b67e", "b67ef", "b550", "b650", "hw-b", "b450"),
        description = "Calibrated for Samsung 5.1/2.1 soundbars: +3dB deep sub-bass shelf, reduced 230Hz mid-bass resonance, +2dB center-channel dialogue clarity, and expanded surround virtualizer.",
        bandGains = listOf(3.0f, -1.5f, 1.5f, 2.0f, 1.5f),
        bassBoostPercent = 0.30f,
        virtualizerPercent = 0.45f
    )

    val SAMSUNG_Q_SERIES = DeviceSoundProfile(
        id = "samsung_q_series",
        name = "Samsung Q-Series Soundbar (Q600 / Q700)",
        brand = "Samsung",
        category = DeviceCategory.HOME_THEATER,
        matchKeywords = listOf("hw-q", "q600", "q700", "q800", "q900"),
        description = "Acoustic Beam & Dolby Atmos tuning: neutral transparent sub-bass with dynamic speech and wide soundstage.",
        bandGains = listOf(1.5f, -0.5f, 1.0f, 1.5f, 2.0f),
        bassBoostPercent = 0.20f,
        virtualizerPercent = 0.50f
    )

    val SONY_HT_SERIES = DeviceSoundProfile(
        id = "sony_ht_series",
        name = "Sony Home Theater (HT-S20R / S40R / S500RF)",
        brand = "Sony",
        category = DeviceCategory.HOME_THEATER,
        matchKeywords = listOf("ht-s20r", "ht-s40r", "ht-s500rf", "ht-s700rf", "ht-g700", "ht-s20", "ht-s40"),
        description = "Calibrated for India's best-selling Sony 5.1 soundbars: tames aggressive 3.6kHz harshness (-2dB), lifts center vocals, and widens rear satellite separation.",
        bandGains = listOf(2.0f, 0.0f, 1.5f, -2.0f, 1.0f),
        bassBoostPercent = 0.25f,
        virtualizerPercent = 0.50f
    )

    val JBL_BOAT_SOUNDBAR = DeviceSoundProfile(
        id = "jbl_boat_soundbar",
        name = "JBL Cinema & boAt Aavante Soundbars",
        brand = "JBL / boAt",
        category = DeviceCategory.HOME_THEATER,
        matchKeywords = listOf("sb241", "sb271", "cinema sb", "bar 2.1", "aavante", "boat bar"),
        description = "Eliminates boomy 230Hz cabinet bloom, providing tight subwoofer punch and intelligible vocal clarity.",
        bandGains = listOf(2.5f, -2.5f, 2.0f, 1.5f, 2.0f),
        bassBoostPercent = 0.25f,
        virtualizerPercent = 0.40f
    )

    val GENERIC_HOME_THEATER = DeviceSoundProfile(
        id = "generic_home_theater",
        name = "Generic Home Theater / Soundbar (Cinema Curve)",
        brand = "Generic",
        category = DeviceCategory.HOME_THEATER,
        matchKeywords = listOf("soundbar", "home theater", "surround", "cinema"),
        description = "Balanced cinema response with natural sub-bass extension and clear vocal center presence.",
        bandGains = listOf(2.0f, -1.0f, 1.0f, 1.5f, 1.5f),
        bassBoostPercent = 0.25f,
        virtualizerPercent = 0.40f
    )

    val ONEPLUS_BULLETS_Z2 = DeviceSoundProfile(
        id = "oneplus_bullets_z2",
        name = "OnePlus Bullets Wireless Z2 / ANC",
        brand = "OnePlus",
        category = DeviceCategory.NECKBAND,
        matchKeywords = listOf("bullets wireless", "bullets z2", "bullets z", "oneplus bullets", "op bullets"),
        description = "Tames the boomy 230Hz mid-bass hump (-3dB), brings forward recessed vocals (+2.5dB), and opens up airy highs (+2dB).",
        bandGains = listOf(1.0f, -3.0f, 2.5f, 2.5f, 2.0f),
        bassBoostPercent = 0.15f,
        virtualizerPercent = 0.20f
    )

    val REALME_BUDS_WIRELESS = DeviceSoundProfile(
        id = "realme_buds_wireless",
        name = "Realme Buds Wireless 2 / 3",
        brand = "Realme",
        category = DeviceCategory.NECKBAND,
        matchKeywords = listOf("buds wireless 3", "buds wireless 2", "realme buds wireless"),
        description = "Harman-target tuning: smooths out sharp 3.6kHz sibilance while keeping punchy energetic sub-bass.",
        bandGains = listOf(1.5f, -1.5f, 1.5f, 0.5f, 1.5f),
        bassBoostPercent = 0.20f,
        virtualizerPercent = 0.15f
    )

    val BOAT_ROCKERZ = DeviceSoundProfile(
        id = "boat_rockerz",
        name = "boAt Rockerz Series (255 / 330)",
        brand = "boAt",
        category = DeviceCategory.NECKBAND,
        matchKeywords = listOf("rockerz", "boat 255", "boat 330", "rockerz 255", "rockerz 330"),
        description = "Corrects boAt's muddy mid-bass boom (-3.5dB), brings vocals into the foreground (+3dB), and sharpens instrument details.",
        bandGains = listOf(0.5f, -3.5f, 3.0f, 2.5f, 2.0f),
        bassBoostPercent = 0.10f,
        virtualizerPercent = 0.20f
    )

    val SONY_NECKBAND = DeviceSoundProfile(
        id = "sony_neckband",
        name = "Sony Neckbands (WI-C100 / WI-XB400)",
        brand = "Sony",
        category = DeviceCategory.NECKBAND,
        matchKeywords = listOf("wi-c100", "wi-xb400", "wi-c200", "wi-c310"),
        description = "Neutral studio curve with tight bass punch and natural vocal timbre.",
        bandGains = listOf(1.0f, -1.0f, 1.5f, 1.0f, 1.0f),
        bassBoostPercent = 0.20f,
        virtualizerPercent = 0.10f
    )

    val OPPO_ENCO = DeviceSoundProfile(
        id = "oppo_enco",
        name = "Oppo Enco M32 / Neckbands",
        brand = "Oppo",
        category = DeviceCategory.NECKBAND,
        matchKeywords = listOf("enco m32", "oppo enco"),
        description = "Balanced acoustic profile with natural midrange presence and clear highs.",
        bandGains = listOf(1.0f, -1.5f, 1.5f, 1.5f, 1.5f),
        bassBoostPercent = 0.15f,
        virtualizerPercent = 0.15f
    )

    val ONEPLUS_NORD_SERIES = DeviceSoundProfile(
        id = "oneplus_nord_series",
        name = "OnePlus Nord Series (Nord 4 / Nord Buds 2 & 3 Pro)",
        brand = "OnePlus",
        category = DeviceCategory.EARBUDS,
        matchKeywords = listOf("nord", "nord buds", "nord 4", "nord ce", "nord 3", "nord 2", "op nord"),
        description = "Master EQ calibration tuned for OnePlus Nord audio: deep 12.4mm driver sub-bass control, crystal-clear vocal boost (+2dB), and smooth anti-sibilance high end.",
        bandGains = listOf(2.0f, -2.0f, 2.0f, 1.5f, 2.0f),
        bassBoostPercent = 0.20f,
        virtualizerPercent = 0.20f
    )

    val GENERIC_NECKBAND = DeviceSoundProfile(
        id = "generic_neckband",
        name = "Generic In-Ear / Neckband",
        brand = "Generic",
        category = DeviceCategory.NECKBAND,
        matchKeywords = listOf("neckband", "earphone", "headset", "earbuds", "tws"),
        description = "Balanced profile with clean vocal presence and controlled bass resonance.",
        bandGains = listOf(1.0f, -2.0f, 2.0f, 1.5f, 1.5f),
        bassBoostPercent = 0.15f,
        virtualizerPercent = 0.15f
    )

    val ALL_PROFILES: List<DeviceSoundProfile> = listOf(
        SAMSUNG_B_SERIES,
        SAMSUNG_Q_SERIES,
        SONY_HT_SERIES,
        JBL_BOAT_SOUNDBAR,
        GENERIC_HOME_THEATER,
        ONEPLUS_BULLETS_Z2,
        REALME_BUDS_WIRELESS,
        BOAT_ROCKERZ,
        SONY_NECKBAND,
        OPPO_ENCO,
        ONEPLUS_NORD_SERIES,
        GENERIC_NECKBAND
    )

    fun matchProfile(productName: String?): DeviceSoundProfile? {
        if (productName.isNullOrBlank()) return null
        val lower = productName.lowercase()

        // First pass: exact specific model matches
        for (profile in ALL_PROFILES) {
            if (profile == GENERIC_HOME_THEATER || profile == GENERIC_NECKBAND) continue
            for (keyword in profile.matchKeywords) {
                if (lower.contains(keyword)) {
                    return profile
                }
            }
        }

        // Second pass: fallback generic category keywords
        for (keyword in GENERIC_HOME_THEATER.matchKeywords) {
            if (lower.contains(keyword)) return GENERIC_HOME_THEATER
        }
        for (keyword in GENERIC_NECKBAND.matchKeywords) {
            if (lower.contains(keyword)) return GENERIC_NECKBAND
        }

        return null
    }
}
