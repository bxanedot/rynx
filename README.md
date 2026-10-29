# rynx

An independently authored Kotlin Multiplatform music application by **bxane**. Android phones are the active target; the existing desktop target also provides the headless Compose test harness.

The Android app includes local audio import with embedded titles, artists and covers, Media3 playback and background controls, YouTube Music search through eXcor, an independently obtained NewPipe fallback, favorites/history, an editable shuffled queue, and user-imported timed lyrics. Its shared interface provides Liquid Glass, Solid and True AMOLED materials, live appearance settings, and a single fluid player with coordinated navigation.

This is a development build. Spotify, downloads/offline management, advanced DSP/DJ, recognition, social/backend features and complete reference screen/motion parity are not finished. No production readiness, audible audio quality, benchmark or 95% visual-parity result is claimed. Apple discovery includes real genres, global/regional/city charts, release/tour collections and original local recommendations; see [discovery sources](docs/parity/APPLE_DISCOVERY.md).

See [current blockers](docs/parity/BLOCKERS.md) and the [Android feature inventory](docs/parity/ANDROID_FEATURE_MATRIX.md).

## Build and test

Use JDK 27, Android SDK 37/build tools, and the approved dependency submodules. Set `ANDROID_HOME` or `sdk.dir` in untracked `local.properties`.

```sh
git submodule update --init
./gradlew :apps:android:assembleDebug :apps:android:assembleRelease :apps:android:assembleDebugAndroidTest :apps:android:lintDebug :apps:android:testDebugUnitTest :apps:desktop:jvmTest :providers:excor:jvmTest :providers:apple:jvmTest :shared:playback:jvmTest :shared:design:jvmTest :shared:ui:jvmTest
./gradlew :apps:android:connectedDebugAndroidTest
```

To install from a GitHub Actions build, download and extract the `android-development` artifact from the [latest verification run](https://github.com/bxanedot/rynx/actions/runs/40838202297). Choose `apps/android/build/outputs/apk/debug/android-debug.apk` from the extracted files; do not try to install the artifact ZIP or the `androidTest` APK. With Android platform tools installed and USB debugging enabled, run `adb install -r apps/android/build/outputs/apk/debug/android-debug.apk`. The debug APK uses the Android development key. If Android reports `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, uninstall `dev.bxane.rynx` first, then retry; uninstalling removes its app data. For file-manager installation, open the extracted debug APK and allow that file manager to install apps if Android prompts.

The minified release output, `android-release-unsigned.apk`, is **not installable** until signed with bxane's owner-controlled release key. No release signing key is stored in this repository. CI builds both variants, installs the debug APK, and runs Android instrumentation on a hardware-accelerated emulator.

Liquid Glass uses the Apache-2.0 Haze renderer through an original shared Rynx material/interaction layer. Solid and AMOLED bypass backdrop effects. Preferences update live and persist on Android; the quality fallback retains the selected material. [Material behavior and limits](docs/parity/INTERFACE_MATERIALS.md).

The independently versioned [eXcor engine](https://github.com/bxanedot/eXcor) is primary. The hourly workflow fetches stable main, checks the candidate, then adopts a passing pin. A failed candidate leaves the default pin intact and opens a compatibility proposal. Runtime provider availability is a separate check.

Rynx application code and original vectors use **GPL-3.0-or-later**, compatible with the requested NewPipeExtractor fallback. Retained dependency licenses and notices ship in the Android assets. GlyphStudio's implementation is Apache licensed per its owner; Apple artwork terms are separate, so Android uses original Rynx glyphs. Original SVG masters are in [assets/brand](assets/brand).

No Vyon, Convx, BitChord or Vivi Music implementation source is a reference. The user authorizes public Apple Music screenshots and guides for visual/interaction reconstruction. Those images and Apple's fonts/symbol artwork are not bundled.
