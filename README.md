# NorthMusic 🎵

[![Release](https://img.shields.io/badge/Release-v0.3--Beta-2563EB?style=for-the-badge&logo=android)](https://github.com/northcrx/music/releases/tag/v0.3-beta)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-0D9488?style=for-the-badge)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-100%25-7F52FF?style=for-the-badge&logo=kotlin)](https://kotlinlang.org)
[![UI: Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Build & Release](https://img.shields.io/badge/CI%2FCD-GitHub%20Actions-2088FF?style=for-the-badge&logo=githubactions)](.github/workflows/release.yml)

> **A Nocturnal, Modern Android Music Experience for Audiophiles.**  
> Designed with 100% Jetpack Compose, Material 3 Expressive, and Media3 ExoPlayer.

---

## 📖 Table of Contents
1. [Overview](#-overview)
2. [Downloads](#-downloads)
3. [Catalog of Changes (v0.3 Beta)](#-catalog-of-changes-v03-beta)
4. [Key Features](#-key-features)
5. [Gesture Navigation](#-gesture-navigation)
6. [Resilient Download Engine](#-resilient-download-engine)
7. [Device Profiles & Equalizer](#-device-profiles--equalizer)
8. [Automating Signing Keys & Releases](#-automating-signing-keys--releases)
9. [Local Development & Building](#-local-development--building)
10. [Contributing](#-contributing)
11. [Legal & DMCA Policy](#-legal--dmca-policy)
12. [License](#-license)

---

## 🌟 Overview

**NorthMusic** is an open-source, hybrid Android music client. It effortlessly merges your offline local audio library (`FLAC`, `MP3`, `M4A`, `WAV`) with online 320kbps studio CDN streaming and downloading, automated ID3 tagging, and audiophile-grade device sound profiles.

- **Offline-First & Hybrid**: Stream online or listen to local music from your device storage.
- **Pure Material 3 Expressive**: Dark glass aesthetic, floating pill navigation dock, and smooth shimmer skeletons.
- **Zero Ads & Zero Tracking**: Built for pure listening pleasure with 100% respectful open-source code.

---

## 📲 Downloads

Download the latest universal release APK directly:

| Version | Status | Package | Download Link |
|---|---|---|---|
| **v0.3-beta** | **Latest (Active)** | `NorthMusic-v0.3-beta.apk` (55.1 MB) | [📥 Download v0.3 Beta APK](https://github.com/northcrx/music/releases/download/v0.3-beta/NorthMusic-v0.3-beta.apk) |
| **v0.2** | Stable | `NorthMusic-v0.2.apk` (54.5 MB) | [📥 Download v0.2 APK](https://github.com/northcrx/music/releases/download/v0.2/NorthMusic-v0.2.apk) |
| **GitHub Release** | Notes & Assets | All Releases | [📦 View GitHub Releases](https://github.com/northcrx/music/releases) |

---

## 📋 Catalog of Changes (v0.3 Beta)

### Added
- `[+]` **Categorized Search Filters**: Search tabs for `All`, `Tracks`, `Albums`, and `Artists` with JioSaavn online indexing.
- `[+]` **Infinite Pagination & Shimmers**: Fluid continuous scrolling with `ShimmerTrackRow` and `ShimmerAlbumCard` loading skeletons.
- `[+]` **Sequential Download Queue**: Concurrency-controlled FIFO queue (defaults to 1-by-1 sequential download) preventing network saturation and socket timeouts.
- `[+]` **Automatic Bitrate Fallback**: If 320kbps audio is not hosted for a specific song on the CDN, it smoothly falls back to 160kbps or 96kbps instead of failing with 403/404 errors.
- `[+]` **Resumable HTTP Range Streams**: Downloads support HTTP 206 Partial Content, resuming interrupted downloads without loss.
- `[+]` **1-Tap Batch Downloads**: Download entire albums or playlists while strictly maintaining track sequence order (Track 1 → 2 → 3...).
- `[+]` **Direct Retry Controls**: Dedicated one-tap retry for failed downloads with automatic token refresh.
- `[+]` **Output Device Profiles**: Sound presets for Bluetooth headphones, wired earphones, and phone speakers with automated device model detection.
- `[+]` **Zone-Based Edge Gestures**: Top 20% swipe zone for drawer/settings; middle 60% swipe zone for fluid tab switching.
- `[+]` **Community & DMCA Governance**: Full `DISCLAIMER.md`, `CONTRIBUTING.md`, `LICENSE` (GPLv3), and GitHub Actions automated release pipeline.

### Changed
- `[+]` **MiniPlayer Persistence**: MiniPlayer remains active and interactive across search results, album sheets, and sub-screens.
- `[+]` **Artist Attribution Engine**: Strict prioritization of `primary_artists` over background `singers`, preventing misattributed folder categorization.
- `[+]` **Folder Organization**: Clean category-based directory structure (`Music/<Category>/<Artist>/<Song>.ext`).
- `[+]` **Clean Search UX**: Simplified search bar hint text to concise "Search music" and removed intrusive "Downloading..." toast notifications.

### Fixed
- `[-]` Fixed download freezes where multiple concurrent connections caused bandwidth drops to 0 MB/s and socket timeouts.
- `[-]` Fixed `Unable to resolve host "aac.saavncdn.com"` DNS exhaustion caused by parallel socket contention.
- `[-]` Fixed unresponsive "Retry" button on failed download cards.
- `[-]` Fixed duplicate items created when resuming paused or failed downloads.
- `[-]` Fixed bottom navigation dock shifting to the top of the screen in certain drawer states.
- `[-]` Fixed hardcoded artwork missing counts in library scanning.

---

## 🎯 Key Features

### 🎧 Audio & Playback
- **Media3 ExoPlayer Pipeline**: Low-latency, gapless playback with audio normalization and replay gain.
- **Device-Specific Sound Profiles**: Parametric equalizer, bass boost, virtualizer, and preset curves.
- **Synchronized LRC Lyrics**: Real-time line-by-line scrolling lyrics with translation support.

### 🔍 Link Resolver & Online Indexing
- **Smart Link Detection**: Paste any Spotify, YouTube Music, or JioSaavn link to automatically index track metadata.
- **Embedded Metadata & Artwork**: Downloads automatically receive ID3 tags (Title, Artist, Album, Category/Genre, Year, synchronized lyrics) and embedded 500x500 artwork.

---

## 🧭 Gesture Navigation

To ensure conflict-free swipe navigation between the side drawer, main tabs, and player controls, the screen is split into intuitive touch zones:

| Screen Area | Gesture | Action |
|---|---|---|
| **Top 20% Zone** | Swipe Right from Left Edge | Opens Navigation Drawer / Menu |
| **Top 20% Zone** | Swipe Left from Right Edge | Opens Settings Screen |
| **Middle 60% Zone** | Swipe Left / Right | Seamlessly switches between **Home**, **Library**, and **Downloads** |
| **Bottom 20% Zone** | Tap / Slide Up | Expands MiniPlayer into full-screen **Now Playing** |

---

## 📥 Resilient Download Engine

Downloading entire albums (15–30 tracks) or playlists on mobile networks can easily cause CDN throttling and DNS failures if all tracks are fetched at once. 

NorthMusic solves this with a **controlled FIFO queue**:
1. **Sequential By Default (1 Active Stream)**: 1 track receives 100% of your bandwidth (10–25 MB/s), completing in 1–2 seconds per song without triggering CDN rate limits.
2. **Order Preservation**: Tracks queue and finish in album order (Track 1, Track 2, Track 3...).
3. **Automatic Fallback**: If `_320.mp4` returns 403 or 404, it automatically tries `_160.mp4` and `_96.mp4`.
4. **Resumable HTTP Range**: Partial downloads resume from the exact byte where connection dropped.

---

## 🎛️ Device Profiles & Equalizer

NorthMusic automatically detects your phone model (e.g. `Pixel 8`, `Galaxy S24`, `OnePlus 12`) and provides sound curves optimized for:
- 🎧 **Bluetooth**: Enhanced frequency separation and surround expansion.
- 🔌 **Wired IEMs**: Neutral reference frequency curves and vocal clarity.
- 🔊 **Phone Speaker**: Vocal enhancement and woofer level compensation.

*Have a sound profile for your specific headphones or device? Submit an issue or PR to have it added to NorthMusic!*

---

## 🔑 Automating Signing Keys & Releases

Currently, the release APK uses Android's universal release configuration so users can easily sideload without signature conflicts. To automate signed production releases on GitHub:

### 1. Generate Your Private Release Keystore
Run in your local terminal:
```bash
keytool -genkey -v -keystore release.jks -alias northmusic -keyalg RSA -keysize 2048 -validity 10000
```

### 2. Encode Keystore for GitHub
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.jks")) | Set-Clipboard
```

### 3. Add to GitHub Secrets
In your GitHub repo (`Settings` → `Secrets and variables` → `Actions`):
- `KEYSTORE_BASE64`: *(paste base64 keystore)*
- `KEYSTORE_PASSWORD`: *(your keystore password)*
- `KEY_ALIAS`: `northmusic`
- `KEY_PASSWORD`: *(your key password)*

### 4. Automated CI/CD
The repository includes [`.github/workflows/release.yml`](.github/workflows/release.yml). Whenever you push a Git tag:
```bash
git tag v0.4
git push origin v0.4
```
GitHub Actions will automatically test the code, compile the release APK, and publish the release with changelog attached!

---

## 🛠️ Local Development & Building

### Prerequisites
- Android Studio Ladybug (2024.2+) or newer
- JDK 17+
- Android SDK 36 (targetSdk 36, minSdk 29)

### Build Commands
```bash
# Clone the repository
git clone https://github.com/northcrx/music.git
cd music

# Compile debug Kotlin
./gradlew compileDebugKotlin

# Run unit tests
./gradlew testDebugUnitTest

# Assemble debug APK
./gradlew assembleDebug

# Assemble release APK
./gradlew assembleRelease
```

---

## 🤝 Contributing

We are actively open for contributions! Whether you want to improve the UI, add new equalizer sound presets, report bugs, or translate strings:
- Please review our [**Contributing Guidelines**](CONTRIBUTING.md).
- Join discussions or open an issue on [**GitHub Issues**](https://github.com/northcrx/music/issues).

---

## ⚖️ Legal & DMCA Policy

NorthMusic does not host, store, or distribute any media files. It functions as a client querying publicly accessible third-party endpoints.
- Read our full [**Legal Disclaimer & DMCA Policy**](DISCLAIMER.md).
- To submit a DMCA takedown notice, please contact the maintainers via [GitHub Issues](https://github.com/northcrx/music/issues).

---

## 📜 License

NorthMusic is licensed under the [**GNU General Public License v3.0 (GPLv3)**](LICENSE).
All contributions and derivative works must remain free and open source.
