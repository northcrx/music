# Contributing to NorthMusic

Thank you for your interest in contributing to **NorthMusic**! We welcome contributions from developers, designers, translators, and audiophiles worldwide.

---

## Code of Conduct
We are committed to providing a welcoming, inclusive, and harassment-free community for everyone. Be respectful, constructive, and open to feedback.

---

## How Can You Contribute?

### 1. Reporting Bugs
- Search existing issues first to avoid duplicates.
- Include device model, Android OS version, and app version (e.g., `v0.3-beta`).
- Provide clear steps to reproduce the issue and attach logcats or screenshots if applicable.

### 2. Suggesting Features & Audio Enhancements
- Open a **Feature Request** on GitHub Issues.
- Describe the UX, why it helps audiophiles, and mockups if possible.
- Output device profiles (EQ curves for specific headphones/speakers) are especially welcome!

### 3. Submitting Code (Pull Requests)
1. **Fork** the repository and clone your fork:
   ```bash
   git clone https://github.com/<your-username>/music.git
   ```
2. **Create a topic branch**:
   ```bash
   git checkout -b feat/your-feature-name
   ```
3. **Coding Standards**:
   - Written in 100% Kotlin with Jetpack Compose & Material 3 Expressive guidelines.
   - Clean architecture: separate UI, ViewModel, Repository, and Network layers.
   - Run tests before submitting:
     ```bash
     ./gradlew testDebugUnitTest
     ./gradlew compileDebugKotlin
     ```
4. **Commit Guidelines**:
   - Use conventional commit messages:
     - `feat:` New features
     - `fix:` Bug fixes
     - `perf:` Performance optimizations
     - `docs:` Documentation changes
5. **Open a Pull Request**:
   - Target the `main` branch.
   - Describe what changed and link any related issues.

---

## Building the Project Locally

### Prerequisites
- Android Studio Ladybug (2024.2+) or newer
- JDK 17+
- Android SDK 36 (targetSdk 36, minSdk 29)

### Build Commands
```bash
# Compile debug variant
./gradlew compileDebugKotlin

# Run unit tests
./gradlew testDebugUnitTest

# Assemble debug APK
./gradlew assembleDebug

# Assemble release APK
./gradlew assembleRelease
```

---

## Licensing
By contributing to NorthMusic, you agree that your contributions will be licensed under its [GNU General Public License v3.0](LICENSE).
