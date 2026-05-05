# Halo

Halo is a modern Android social + messaging app built on Matrix.  
It combines direct messaging, social feed posts, and stories in a single Kotlin + Jetpack Compose experience.

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-7F52FF.svg)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-API%2026%2B-3DDC84.svg)](https://developer.android.com/)
[![Matrix](https://img.shields.io/badge/Protocol-Matrix-000000.svg)](https://matrix.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## Overview

Halo is designed to make decentralized communication feel native and fast on Android:

- **Chat:** 1:1 direct messaging with Matrix room sync and local persistence.
- **Feed:** Timeline-style post feed backed by local Room cache.
- **Stories:** Expiring stories grouped by author with seen/unseen tracking.
- **Offline-friendly UX:** Local-first rendering via Room + Flow, with sync-driven updates.

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **DI:** Hilt
- **Local DB:** Room
- **Async:** Kotlin Coroutines + Flow
- **Serialization:** kotlinx.serialization
- **Image Loading:** Coil
- **Protocol layer:** Matrix Rust SDK (`org.matrix.rustcomponents:sdk-android`)

## Project Structure

`app/src/main/java/com/halo` is organized by layer:

- `data/`
  - `local/` Room entities, DAO interfaces, database
  - `matrix/` Matrix client, sync managers, event processors
  - `repository/` app-facing data orchestration
- `domain/` UI-facing models
- `ui/`
  - `screens/` feature screens and view models
  - `components/` reusable Compose widgets
  - `navigation/` nav graph and bottom navigation
- `di/` dependency modules

## Requirements

- Android Studio Iguana or newer
- JDK 17
- Android SDK 34
- Emulator/device API 26+

## Quick Start

```bash
git clone https://github.com/sahilsheikh21/Halo.git
cd Halo
```

Then:

1. Open the project in Android Studio.
2. Let Gradle sync finish.
3. Select an emulator/device.
4. Run the `app` configuration.

## Build and Validation Commands

From project root:

```bash
# Compile Kotlin sources
./gradlew :app:compileDebugKotlin

# Build debug APK
./gradlew :app:assembleDebug
```

## Matrix + Data Flow (High-Level)

1. `MatrixClientManager` provides authenticated Matrix client/session access.
2. `SlidingSyncManager` and `SyncEventProcessor` observe sync and timeline updates.
3. Events are transformed into local entities and persisted in Room.
4. Repositories expose reactive `Flow` streams to UI ViewModels.
5. Compose screens render state from ViewModels and dispatch user actions back to repositories.

### Matrix SDK dependency issues

This project uses Matrix artifacts from Sonatype snapshots. Verify `settings.gradle.kts` still contains:

- `mavenCentral()`
- `maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/") }`

### App compiles but sync seems stale

- Verify connectivity and homeserver availability.
- Re-launch the app to reinitialize sync and listeners.
- Check logcat tags around Matrix and sync components for runtime errors.

## Contributing

Contributions are welcome.

1. Fork the repository.
2. Create a branch:

```bash
git checkout -b feature/your-feature-name
```

3. Make changes with tests where possible.
4. Ensure project compiles:

```bash
./gradlew :app:compileDebugKotlin
```

5. Open a pull request with a clear problem statement and validation notes.

## License

This project is licensed under the MIT License. See `LICENSE`.
