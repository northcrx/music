# Changelog & Catalog of Changes

All notable changes to the **NorthMusic** project are documented in this file.
This project adheres to [Semantic Versioning](https://semver.org/).

---

## [0.3-beta] - 2026-11-05

### Added
- **[+] Category & Filtered Online Search**:
  - Filter tabs in search screen: `All`, `Tracks`, `Albums`, and `Artists`.
  - Direct JioSaavn album and artist search support with dedicated result rows and card layouts.
  - Infinite pagination with smooth incremental loading on scroll.
  - Expressive Shimmer Loading skeletons (`ShimmerTrackRow`, `ShimmerAlbumCard`) while fetching online results.
- **[+] Controlled Concurrency Download Manager**:
  - Resilient FIFO download queue governed by user concurrency settings (defaults to 1 sequential download at a time, maximum 2).
  - Explicit `QUEUED` state in `DownloadStatus` with pause and cancel controls.
  - Automatic queue progression: as soon as an active track completes or fails, the next waiting track begins immediately.
  - Multi-stream retry logic with progressive backoff (2s, 4s) to survive CDN hiccups without failing downloads.
  - Automatic bitrate fallback: transparently falls back from 320kbps to 160kbps or 96kbps if high-bitrate files are not hosted on CDN for that specific track.
  - HTTP Range (`206 Partial Content`) support for automatic resumption of partial downloads.
- **[+] 1-Tap Album & Playlist Batch Downloads**:
  - One-tap "Download Whole Album" and "Download Selected Tracks" buttons with progress tracking.
  - Seamless track ordering preservation (Track 1 -> Track 2 -> Track 3...).
- **[+] Direct Retry and Delete Controls**:
  - Dedicated one-tap retry button for failed downloads that refreshes CDN tokens upon restarting.
  - Individual and batch cancellation/removal of queued items.
- **[+] Equalizer & Output Device Profiles**:
  - Equalizer toggle and deselect support.
  - Compact UI for selecting Bluetooth, Wired, and Speaker audio profiles.
  - Automated device model detection with sound profile presets.
- **[+] Zone-Based Edge Gesture Navigation**:
  - Top 20% swipe zone for opening drawer and settings menus.
  - Middle 60% swipe zone for fluid tab switching between Home, Library, and Downloads.
  - Drawer gesture conflict resolution: drawer only intercepts horizontal drags when already open or when initiated in the top gesture zone.
- **[+] Community & DMCA Compliance**:
  - Full DMCA notice & content disclaimer documentation (`DISCLAIMER.md`).
  - Open-source contribution guidelines (`CONTRIBUTING.md`).
  - Issue templates for bug reports and feature requests.
  - Automated GitHub Actions release pipeline.

### Changed
- **[+] MiniPlayer Persistence & Visibility**:
  - MiniPlayer remains active and interactive across all sub-screens, search results, and album detail bottom sheets.
  - Search track tap starts playback in MiniPlayer first with an upward expansion affordance to Full Player.
  - Download action button inside MiniPlayer tinted to high-contrast surface color.
- **[+] Artist Attribution & Tagging Engine**:
  - Prioritized `primary_artists` over background `singers` in JioSaavn CDN parser, resolving incorrect folder creation and misattributed lead singers.
  - Category-based and artist-based directory organization (`Music/<Category>/<Artist>/<Song>.ext`).
  - Direct ID3 tagging with embedded artwork, lyrics, comments, and album metadata.
- **[+] Cleaned Notifications & Search UX**:
  - Removed intrusive "Downloading..." toast spam.
  - Cleaned up search bar hint text to concise "Search music".
  - Light mode / dark mode color consistency for frosted glass bottom bar and MiniPlayer.

### Fixed
- **[-]** Fixed download freezes where multiple concurrent connections caused bandwidth drops to 0 MB/s and `SocketTimeoutException`.
- **[-]** Fixed `Unable to resolve host "aac.saavncdn.com"` DNS exhaustion caused by too many parallel download sockets.
- **[-]** Fixed unresponsive "Retry" button on failed download cards.
- **[-]** Fixed `resume(id)` creating duplicate items and IDs in active download state.
- **[-]** Fixed bottom navigation bar shifting to the top of the screen in certain drawer states.
- **[-]** Fixed library scanning showing hardcoded artwork missing counts.

---

## [0.2] - 2026-11-04

### Added
- **[+]** Online 320kbps music indexing directly from studio CDN in Home search bar.
- **[+]** Instant tap-to-stream engine via Jetpack Media3 ExoPlayer for online search results.
- **[+]** Smart link resolver detecting Spotify, YouTube Music, and JioSaavn URLs.
- **[+]** Automated track downloader with embedded ID3 metadata tags (Title, Artist, Album, Year).
- **[+]** High-resolution album cover artwork embedding for downloaded files.
- **[+]** Dedicated "Downloaded Music" carousel section on Home tab.
- **[+]** Compact 54dp floating pill dock bottom navigation with snug spacing.
- **[+]** Network permissions (`INTERNET`, `ACCESS_NETWORK_STATE`) and cleartext audio traffic support.

### Changed
- **[+]** Redesigned navigation bar from tall 80dp container to 54dp rounded floating pill dock.
- **[+]** Optimized list bottom padding to 110dp to ensure fluid scrolling above floating dock.
- **[+]** Upgraded audio playback pipeline to support remote HTTP/HTTPS streaming alongside local MediaStore URIs.
- **[+]** Updated search bar with debounced flow to prevent excessive network queries during typing.

### Fixed
- **[-]** Fixed empty search results caused by missing network permissions.
- **[-]** Eliminated excessive dead space and vertical voids across bottom tab bar.
- **[-]** Removed offline-only limitations, enabling hybrid local and cloud playback.

---

## [0.1] - 2026-11-04
- Initial release of NorthMusic local media player with Jetpack Compose & Material 3 Expressive UI.
