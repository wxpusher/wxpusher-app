# AGENTS.md

## Cursor Cloud specific instructions

This is a **Kotlin Multiplatform (KMP)** mobile app (Android + iOS) for WxPusher. On Linux cloud agents, only the **Android** build is available; iOS requires macOS + Xcode.

### Prerequisites (installed by snapshot)

- **JDK 17**: `/usr/lib/jvm/java-17-openjdk-amd64`
- **Android SDK**: `/opt/android-sdk` (platform 35, build-tools 35.0.0)
- Environment variables `JAVA_HOME` and `ANDROID_HOME` are set in `~/.bashrc`

### Build commands

See `README.md` for full details. Key commands:

```bash
# Production debug build (same as CI)
./gradlew :androidApp:assembleProdDebug

# Offline/test debug build
./gradlew :androidApp:assembleOfflineDebug

# Lint check
./gradlew :androidApp:lintProdDebug

# Shared module tests (currently NO-SOURCE but the task runs clean)
./gradlew :shared:allTests
```

### Gotchas

- **iOS cinterop warnings on Linux are normal**: You will see warnings about disabled Kotlin/Native targets (`iosArm64`, `iosSimulatorArm64`, `iosX64`) and cinterop cross-compilation. These are expected and do not affect the Android build.
- **Kotlin metadata version warnings during lint**: Lint analysis shows "incompatible version of Kotlin" errors in its output, but these do not cause lint to fail. This is a known issue with Kotlin 2.2.x and the lint tool's embedded Kotlin version.
- **No `key.properties` needed for debug builds**: The debug signing config uses `androidApp/debug.jks` with hardcoded credentials. Release builds need `secrets/android/key.properties`, which is not in the repo.
- **NDK abiFilters**: The Android build only targets `arm64-v8a`. APKs cannot be installed on x86/x86_64 emulators.
- **No local backend needed**: The app connects to the remote WxPusher API at `https://wxpusher.zjiecode.com`. The `offline` flavor has a `TestPanelActivity` for switching API hosts at runtime.
