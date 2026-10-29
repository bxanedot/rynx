# Android Provider-to-Playback Acceptance Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Verify and harden the real Android path from Apple Music metadata selection through conservative YouTube Music matching, Excor/NewPipe stream resolution, and the Media3 playback service.

**Architecture:** Keep discovery ranking, catalog contracts, stream descriptions, metadata matching, and playback preparation in `shared/playback/commonMain` so iOS can reuse them later. Provider modules implement those shared contracts; the Android app only adapts a prepared stream to Media3, which remains owned by `PlaybackService`. Preserve Excor as the primary YouTube extractor and NewPipeExtractor as the user-approved fallback.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Coroutines, Excor, NewPipeExtractor fallback, Android Media3, JUnit, Android instrumentation, GitHub Actions API 36 Pixel 6 emulator.

**Spec:** `docs/parity/ANDROID_FEATURE_MATRIX.md`, `docs/parity/APPLE_DISCOVERY.md`, `docs/parity/EXCOR_INTEGRATION.md`, and `docs/parity/ANDROID_AUDIO_TESTS.md`.

## Global Constraints

- Android phones are P0; keep shared domain and UI code in KMP `commonMain` where platform APIs are not required, including playback preparation and its provider contracts.
- Use Apple Music's iOS experience as the authorized design reference; do not substitute generic Material screens.
- Do not read Vyon, Convx, BitChord, or Vivi Music implementation source, tests, or derived implementation notes.
- Excor remains the primary YouTube engine; the user explicitly permits NewPipeExtractor as a fallback. Do not add another extractor.
- Keep Spotify access within officially authorized interfaces; do not simulate streaming or bypass service restrictions.
- Cancellation must propagate; stale playback requests must not replace newer user selections.
- Treat stream URLs and request headers as sensitive, short-lived data; never place them in user-facing errors or logs.
- A feature is not verified by compilation alone; record test/device evidence in the parity docs.
- Do not expand substantial desktop, TV, Wear OS, or iOS product work before the Android completion gate.

## Review Focus

- Apple metadata must never resolve to a different recording or alternate version; cover matching and no-match cases in `AndroidMusicPlaybackCoordinatorTest`.
- A newer user selection must win over an older slow resolution; cover cancellation and stale completion in `LatestPlaybackRequestTest`.
- Cancellation must not trigger NewPipe fallback; retain and run the existing `AudioResolutionChainTest.cancellationDoesNotStartFallback` case.
- Request headers must reach the matching resolved stream without leaking into diagnostics; cover the real Media3 data-source path and `StreamRequestHeadersTest`.
- Playback must survive Activity stop and ordinary transport actions must reach the service; run `PlaybackSessionTest`, `PlayerDeviceTest`, and `BackgroundListeningTest` on the hosted emulator.

---

## Current checkpoint

The repository already contains Apple metadata/search and Apple-derived Explore genres, local recommendation ranking and Home shelves, conservative Apple-to-YouTube Music matching, the Excor/NewPipe resolution chain, Media3 service playback, queue controls, lyrics, and the shared Apple-inspired player/navigation work. The latest known hosted Verify run 37 and Android phone run 33 passed. Those results do not prove live provider extraction and audible physical-device playback. `docs/parity/BLOCKERS.md` and the feature matrix must be reconciled with these latest results before new claims are made.

This plan covers the next testable playback slice only. Library/offline, Spotify, advanced audio, and complete screen parity are separate subsystems and remain sequenced follow-up plans rather than being treated as finished here.

### Task 1: Move playback preparation behind shared KMP contracts

**Files:**
- Create: `shared/playback/src/commonMain/kotlin/dev/bxane/rynx/playback/catalog/ResolvedAudioStream.kt`
- Create: `shared/playback/src/commonMain/kotlin/dev/bxane/rynx/playback/catalog/AudioStreamResolver.kt`
- Create: `shared/playback/src/commonMain/kotlin/dev/bxane/rynx/playback/discovery/MusicPlaybackPreparation.kt`
- Create: `shared/playback/src/commonTest/kotlin/dev/bxane/rynx/playback/discovery/MusicPlaybackPreparationTest.kt`
- Modify: `shared/playback/src/commonMain/kotlin/dev/bxane/rynx/playback/catalog/CatalogItem.kt`
- Modify: `shared/playback/build.gradle.kts`
- Modify: `providers/excor/src/jvmCommonMain/kotlin/dev/bxane/rynx/excor/ExcorMusicCatalog.kt`
- Modify: `providers/excor/src/jvmCommonMain/kotlin/dev/bxane/rynx/excor/ResolvedAudioStream.kt`
- Modify: `providers/excor/build.gradle.kts`
- Modify: `providers/newpipe/src/androidMain/kotlin/dev/bxane/rynx/newpipe/NewPipeFallbackAudioResolver.kt`
- Modify: `providers/newpipe/build.gradle.kts`
- Modify: `apps/android/src/main/kotlin/dev/bxane/rynx/android/MainActivity.kt`
- Move: `apps/android/src/main/kotlin/dev/bxane/rynx/android/AudioResolutionChain.kt` to `shared/playback/src/commonMain/kotlin/dev/bxane/rynx/playback/catalog/AudioResolutionChain.kt`
- Move: `apps/android/src/test/kotlin/dev/bxane/rynx/android/AudioResolutionChainTest.kt` to `shared/playback/src/commonTest/kotlin/dev/bxane/rynx/playback/catalog/AudioResolutionChainTest.kt`
- Test: `shared/playback/src/commonTest/kotlin/dev/bxane/rynx/playback/discovery/DiscoveryRankingTest.kt`

**Interfaces:**
- Consumes: `CatalogItem`, `CatalogPage`, `CatalogSearchRepository`, `MusicMetadata`, `MetadataMatching`, and the existing Excor/NewPipe public adapters.
- Produces: `MusicPlaybackCatalog : CatalogSearchRepository` with `suspend fun searchAppleMusic(query: String, country: String?, limit: Int): List<MusicMetadata>` and `suspend fun appleSong(id: Long, country: String?): MusicMetadata?`; `AudioStreamResolver` with `suspend fun resolveAudio(videoId: String): ResolvedAudioStream?`; common `ResolvedAudioStream` with redacted `toString()`; `PreparedMusic(val item: CatalogItem, val metadata: MusicMetadata?, val stream: ResolvedAudioStream?)`; and `MusicPlaybackPreparer.prepareDiscovery(track: MusicMetadata)` / `prepareCatalog(item: CatalogItem)`.
- `AudioResolutionChain` implements `AudioStreamResolver`, tries Excor first, falls back to NewPipe only on null or non-cancellation failure, and rethrows cancellation. `ExcorMusicCatalog` implements the catalog and resolver contracts; the Android Activity supplies the region and commits prepared output to Media3.
- Provider adapters own any platform dispatcher needed for blocking/network work. `commonMain` must not depend on Android APIs or `Dispatchers.IO`.

- [x] **Step 1: Write failing common tests** for Apple metadata enrichment, a correctly matched YouTube recording, a direct YouTube Music `playbackId`, a no-match result that never calls the stream resolver, Apple lookup failure, and cancellation propagation.
- [x] **Step 2: Run the common tests** and confirm the missing contracts/behavior fail before implementation; the focused red run failed on the absent shared contracts, then passed after implementation.
- [x] **Step 3: Add the common contracts and preparer** in `shared/playback/commonMain`; move only provider-neutral stream data and fallback ordering into common code, retaining sensitive-field redaction.
- [x] **Step 4: Adapt provider implementations** so Excor remains primary and the approved Android NewPipe adapter remains fallback; preserve provider-specific dependencies and runtime dispatching.
- [x] **Step 5: Replace the inline preparation code in `MainActivity`** with the shared `MusicPlaybackPreparer`, leaving Media3 item creation, header registration, errors, and playback commit in the Android host.
- [x] **Step 6: Run shared and platform compilation**; the focused and final verification runs passed all included shared/provider/Android unit tasks.

### Task 2: Verify the prepared stream reaches the real Media3 service

**Files:**
- Create: `apps/android/src/main/kotlin/dev/bxane/rynx/android/ProviderHeaderResolver.kt`
- Create: `apps/android/src/androidTest/kotlin/dev/bxane/rynx/android/ProviderHeaderResolverTest.kt`
- Modify: `apps/android/src/androidTest/kotlin/dev/bxane/rynx/android/PlaybackSessionTest.kt`
- Modify: `apps/android/src/androidTest/kotlin/dev/bxane/rynx/android/PlayerDeviceTest.kt`
- Modify only if a reproduced defect requires it: `apps/android/src/main/kotlin/dev/bxane/rynx/android/PlaybackService.kt`, `MainActivity.kt`, or `StreamRequestHeaders.kt`
- Test: `apps/android/src/test/kotlin/dev/bxane/rynx/android/StreamRequestHeadersTest.kt`

**Interfaces:**
- Consumes: `PreparedMusic` from Task 1; existing `MediaController`, `PlaybackService`, `StreamRequestHeaderStore`, and `playbackDiagnostics`.
- Produces: a unit regression proving Media3 `DataSpec` receives headers only for its registered resolved URL, plus an Android instrumentation regression showing prepared metadata, playback progress, and pause/resume through the actual service. Use generated test audio and an isolated request fixture for deterministic playback; never embed a provider-signed URL in the test.

- [x] **Step 1: Write a failing instrumentation `ProviderHeaderResolverTest`** asserting that a registered URL receives the exact headers, another URL receives none, and the input `DataSpec` stays unchanged. The test was moved to `androidTest` because Android `Uri` is unavailable to the plain JVM test runner.
- [x] **Step 2: Extract the production `ResolvingDataSource.Resolver`** into `ProviderHeaderResolver.kt` and wire `PlaybackService` to use `StreamRequestHeaderStore` through it.
- [x] **Step 3: Extend the existing generated-audio Media3 session case** to assert source identity, metadata, advancing position, and pause/resume behavior. This verifies the service path with deterministic local audio; it does not exercise remote provider extraction.
- [x] **Step 4: Build the focused instrumentation target** with `:apps:android:assembleDebugAndroidTest`; APK compilation passed.
- [ ] **Step 5: Run the focused target** on an available API 36 emulator and verify service/device cases. The local API 36 software emulator did not boot and no connected device was available.
- [x] **Step 6: Review diagnostics and test artifacts** to confirm they contain neither signed URLs nor request-header values.

### Task 3: Run live provider smoke checks without making CI flaky

**Files:**
- Modify only if behavior or evidence changes: `providers/excor/src/jvmTest/kotlin/dev/bxane/rynx/excor/LiveYouTubeEngineTest.kt`
- Modify: `docs/parity/EXCOR_INTEGRATION.md`
- Modify: `docs/parity/BLOCKERS.md`
- Modify: `docs/parity/ANDROID_FEATURE_MATRIX.md`
- Modify: `docs/parity/ANDROID_AUDIO_TESTS.md`

**Interfaces:**
- Consumes: Excor's approved public search and resolution adapter; the NewPipe resolver only for its existing, explicitly permitted fallback behavior.
- Produces: current, dated evidence distinguishing deterministic tests, opt-in live provider checks, emulator playback, and physical-device audible playback.

- [ ] **Step 1: Run the opt-in primary-engine smoke test** with `RYNX_YOUTUBE_LIVE=1 ./gradlew :providers:excor:jvmTest --tests '*LiveYouTubeEngineTest'`; the attempt failed before provider response with `java.io.IOException` in the network connection path (`ConnectPlan.kt:472`, connection refused). No stream URL or headers were emitted.
- [ ] **Step 2: Run the Apple contract smoke test only when network access is available** with `RYNX_APPLE_LIVE=1 ./gradlew :providers:excor:jvmTest --tests '*LiveAppleEngineTest'`; keep transient endpoint failures separate from deterministic test failures.
- [x] **Step 3: Update parity status precisely**: successful metadata or URL resolution is not audible playback, and emulator decode is not a physical-device or audio-quality result.

### Task 4: Re-run the Android release-readiness checkpoint

**Files:**
- Update as needed: `docs/parity/ANDROID_VISUAL_RESULTS.md`
- Update as needed: `docs/parity/REGRESSION_RESULTS.md`
- Update as needed: `docs/parity/ANDROID_FEATURE_MATRIX.md`
- Update as needed: `docs/parity/BLOCKERS.md`

**Interfaces:**
- Consumes: Tasks 1–3 and the existing CI workflows `.github/workflows/verify.yml` and `.github/workflows/android-phone.yml`.
- Produces: a reproducible verification record and a current list of remaining release blockers. No desktop, iOS, or production-release claim is produced by this checkpoint.

- [x] **Step 1: Run deterministic verification** with `:apps:android:assembleDebug :apps:android:assembleRelease :apps:android:assembleDebugAndroidTest :apps:android:lintDebug :apps:android:testDebugUnitTest :providers:excor:jvmTest :providers:apple:jvmTest :providers:newpipe:compileAndroidMain :shared:playback:jvmTest :shared:design:jvmTest :shared:ui:jvmTest --offline --console=plain`; the command passed in 10m18s. It produced 78 passing tests and three skipped opt-in live checks.
- [ ] **Step 2: Run the hosted phone workflow for this worktree** and inspect the connected-test report and screenshots; historical results on earlier commits do not validate the current branch.
- [x] **Step 3: Record artifact hashes, test counts, warnings, and limitations**; physical Android, live extraction, audible output quality, performance, Spotify, downloads, and signing gates remain open.

## Follow-up plan order after this slice

1. Persistent Android library, playlists, local audio, downloads, and offline playback.
2. Spotify Android integration after a registered client/callback, permitted account, and current official capability audit are available.
3. Remaining Apple Music iOS-inspired Android screens, settings, responsive layouts, and reference screenshot/motion checks.
4. Lyrics sources and customization, real audio processing, then other requested Android features with independent acceptance tests.
5. Android accessibility, physical-device performance/audio review, dependency and artwork/license audit, signed release configuration, and the Android completion gate.
6. Only after that gate, substantial Windows/macOS/Linux, Android TV, Wear OS, and optional iOS work.

Each follow-up is its own testable implementation plan because these are separate subsystems. The Android feature matrix remains the source of truth for what is complete, partial, blocked, or unverified.
