# rynx

An independently authored Kotlin Multiplatform music application by **bxane**. Android phones are the active target; the existing desktop target also provides the headless Compose test harness.

The Android app includes local audio import with embedded titles, artists and covers, Media3 playback and background controls, Apple metadata/search with real cover artwork, and YouTube Music search through the InnerTubeX public API. Android audio resolution runs InnerTubeX first and NewPipeExtractor as fallback. Apple cover URLs are normalized to high-resolution image variants when available. eXcor has been removed from the active build; see [the archive note](docs/archive/EXCOR.md). The app also includes favorites/history, an editable shuffled queue, and user-imported timed lyrics. Its shared interface provides Liquid Glass, Solid and True AMOLED materials, live appearance settings, and a single fluid player with coordinated navigation.

This is a development build. Dark appearance is the clean-install default. Settings uses compact grouped rows inspired by Apple Music's iOS Music settings page. Spotify, downloads/offline management, advanced DSP/DJ, recognition, social/backend features and complete reference screen/motion parity are not finished. No production readiness, audible audio quality, benchmark or 95% visual-parity result is claimed. Apple discovery includes real genres, global/regional/city charts, release/tour collections and original local recommendations; see [discovery sources](docs/parity/APPLE_DISCOVERY.md).

See [current blockers](docs/parity/BLOCKERS.md) and the [Android feature inventory](docs/parity/ANDROID_FEATURE_MATRIX.md).

## Build and test

Use JDK 27, Android SDK 37/build tools, and the approved dependency submodules. Set `ANDROID_HOME` or `sdk.dir` in untracked `local.properties`.

```sh
git submodule update --init
./gradlew :apps:android:assembleDebug :apps:android:assembleRelease :apps:android:assembleDebugAndroidTest :apps:android:lintDebug :apps:android:testDebugUnitTest :apps:desktop:jvmTest :providers:innertubex:jvmTest :providers:apple:jvmTest :providers:newpipe:compileAndroidMain :shared:playback:jvmTest :shared:design:jvmTest :shared:ui:jvmTest
./gradlew :apps:android:connectedDebugAndroidTest
```

To install the Android debug build, open the [Android phone workflow runs](https://github.com/bxanedot/rynx/actions/workflows/android-phone.yml), choose a completed run, and download its `rynx-debug-apk` artifact. The artifact is a ZIP; extract `android-debug.apk` and install that APK (not the ZIP or the `androidTest` APK). With Android platform tools and USB debugging enabled, run `adb install -r android-debug.apk`. The debug APK uses the Android development key. If Android reports `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, uninstall `dev.bxane.rynx` first, then retry; uninstalling removes its app data. For file-manager installation, open the extracted APK and allow that file manager to install apps if Android prompts.

The minified release output, `android-release-unsigned.apk`, is **not installable** until signed with bxane's owner-controlled release key. No release signing key is stored in this repository. CI builds both variants, installs the debug APK, and runs Android instrumentation on a hardware-accelerated emulator.

Liquid Glass uses the Apache-2.0 Haze renderer through an original shared Rynx material/interaction layer. Solid and AMOLED bypass backdrop effects. Preferences update live and persist on Android; the quality fallback retains the selected material. [Material behavior and limits](docs/parity/INTERFACE_MATERIALS.md).

Android currently uses InnerTubeX and NewPipeExtractor. YouTube.js (`youtubei.js`) is a JavaScript library and has no Android/Kotlin adapter in this project; it is not presented as an active Android fallback. Provider runtime availability and audible playback remain unverified on a device.

Rynx application code and original vectors use **GPL-3.0-or-later**, compatible with the requested NewPipeExtractor fallback. Retained dependency licenses and notices ship in the Android assets. The semantic icon registry uses bxanedot's [GlyphStudio `:sfsymbols` KMP library](https://github.com/bxanedot/GlyphStudio) from shared `commonMain`; original Rynx vector fallbacks remain for glyph names absent from the catalog. Apple's SF Symbols artwork terms still need release review. Shared iOS target declarations are present for the future port, but there is no iOS host app or Apple-platform build verification yet. Original SVG masters are in [assets/brand](assets/brand).

No Vyon, Convx, BitChord or Vivi Music implementation source is a reference. The user authorizes public Apple Music screenshots and guides for visual/interaction reconstruction. Those images and Apple's fonts/symbol artwork are not bundled.
