<p align="center">
  <h1 align="center">NorthMusic</h1>
  <p align="center">
    <strong>A Nocturnal, Modern Android Music Experience for Audiophiles.</strong>
  </p>
  <p align="center">
    <a href="https://github.com/northcrx/music/releases/latest"><img src="https://img.shields.io/badge/Release-v0.3--Beta-2563EB?style=for-the-badge&logo=android" alt="Latest Release"></a>
    <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPLv3-0D9488?style=for-the-badge" alt="License"></a>
    <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-100%25-7F52FF?style=for-the-badge&logo=kotlin" alt="Kotlin"></a>
    <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose" alt="Compose"></a>
  </p>
</p>

---

## Overview

**NorthMusic** is an open-source, hybrid Android music player and library manager built with modern Android standards (**Jetpack Compose**, **Material 3 Expressive**, and **Media3 ExoPlayer**). 

It bridges your local offline music library with high-resolution 320kbps online studio streaming, automated category organization, and audiophile-grade equalizer presets.

---

## ✨ Features in v0.3 Beta

### 🎧 Playback & Audio
- **Jetpack Media3 ExoPlayer Engine**: Seamless gapless playback, audio normalization, and replay gain.
- **Parametric Equalizer & Output Presets**: Device-specific profiles for Bluetooth earphones, wired IEMs, and phone speakers.
- **Synchronized LRC Lyrics**: Real-time line-by-line lyric rendering with Romanization/translation support.

### 🔍 Search & Online Indexing
- **Category Filters**: Search across `All`, `Tracks`, `Albums`, and `Artists`.
- **Infinite Pagination & Shimmer Skeletons**: Fluid loading with zero layout jumps.
- **Smart Link Resolver**: Direct link recognition for Spotify, YouTube Music, and JioSaavn URLs.

### 📥 Resilient Download Management
- **Controlled Concurrency Queue**: Strictly prevents bandwidth drops and CDN timeouts by managing sequential (1-by-1) background downloads.
- **Bitrate Fallback**: Transparently falls back from 320kbps to 160kbps/96kbps if high-bitrate files are unavailable on CDN.
- **1-Tap Batch Downloads**: Download entire albums or playlists with a single tap while maintaining track index order.
- **Embedded ID3 Tags & Album Covers**: Embedded covers, category/genre tags, and synchronized lyrics directly into downloaded files.
- **Resumable HTTP Range Streams**: Automatically resumes downloads after transient network disconnections.

### 🎨 Nocturnal Design & Gestures
- **Edge-to-Edge Frosted Glass**: Frosted dark glass MiniPlayer and floating pill navigation dock.
- **Zone-Based Gestures**:
  - **Top 20% Zone**: Quick-swipe for navigation drawer and settings.
  - **Middle 60% Zone**: Swipe to switch between Home, Library, and Downloads tabs.

---

## 📲 Download

Grab the latest release APK from GitHub:

| Version | Variant | Download Link |
|---|---|---|
| **v0.3-beta** | Universal Release APK | [Download NorthMusic-v0.3-beta.apk](https://github.com/northcrx/music/releases/download/v0.3-beta/NorthMusic-v0.3-beta.apk) |
| **v0.2** | Universal Release APK | [Download NorthMusic-v0.2.apk](https://github.com/northcrx/music/releases/download/v0.2/NorthMusic-v0.2.apk) |

---

## 🛠️ Build & Development

### Prerequisites
- Android Studio Ladybug (2024.2+) or newer
- JDK 17+
- Android SDK 36 (targetSdk 36, minSdk 29)

### Clone & Build
```bash
git clone https://github.com/northcrx/music.git
cd music

# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest
```

---

## 🤝 Contributing

We welcome contributions! Please read our [Contributing Guidelines](CONTRIBUTING.md) before submitting Pull Requests.

---

## ⚖️ Legal & DMCA

NorthMusic does not host any audio or copyrighted media files on its servers. For full details regarding copyright compliance and takedown policies, please review [DISCLAIMER.md](DISCLAIMER.md).

---

## 📜 License

NorthMusic is open-source software licensed under the **GNU General Public License v3.0 (GPLv3)**. See [LICENSE](LICENSE) for details.
