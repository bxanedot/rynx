# Rynx implementation status

Status date: 2026-10-29. Branch `feature/android-audio-excor`, based on merged `main` commit `11681c9`. This is an engineering status report, not a production-readiness claim.

## Implemented and deterministically tested

- Shared Kotlin playback queue/order, metadata matching, recommendations, lyrics parsing, appearance/motion models, and provider-neutral playback preparation contracts.
- Apple public-catalog data models, Home collections, Explore genre navigation, local recommendation ranking, and original Rynx brand/palette SVG assets.
- Compose navigation/player shells, Liquid Glass/Solid/AMOLED materials, animated player/navigation handoff, and shared UI regression coverage.
- An iOS Apple Music-style grouped Settings page for implemented playback, appearance and library actions; dark appearance is the new-install default while previously saved light preferences are preserved. Shared icon names resolve through GlyphStudio's dynamic renderer in `commonMain` across Android, iOS, and desktop; the Library selection bounce uses GlyphStudio's layered symbol effect.
- Android Media3 session/service, local audio import, favorites, recent metadata, queue controls, and imported LRC support at code or unit-test level.
- A shared `commonMain` PCM DSP core (10/31-band and parametric biquads, tone shelves, headroom, stereo width/balance, crossfeed, linked compression and an interpolated peak guard) is attached to the existing Android Media3 sink. Pure Mode bypasses Rynx processing, Android settings persist locally, and the existing player exposes the sound sheet. Hosted Android API 36 instrumentation passes, including generated PCM playback through the Media3 service, pause/resume and playlist database reopen/order. Physical-device output quality and route behavior remain unverified.
- Portable KMP download provenance now encodes `Downloaded by Rynx`, app ID and track/provider/timestamp fields; an Android writer atomically writes a `.rynx.json` sidecar without touching downloaded media bytes. Serializer and file-preservation tests pass. Rynx does not yet have an app download queue, so the writer is a ready integration point and no current download invokes it automatically.
- Current Android provider slice: Apple public song search/artwork plus an InnerTubeX KMP catalog/resolver and NewPipe Android fallback. The current branch builds, passes the deterministic test gate, and packages debug/release/instrumentation APKs; live YouTube playback and device behavior remain unverified.
- Persistent playlists use SQLDelight with common KMP models, validation/edit rules, transactions and Flow observation; Android supplies the SQLite driver. Library create/rename/add/remove/reorder/delete and playlist detail UI are wired. Shared SQLDelight/rules tests, headless workflows and hosted Android SQLite close/reopen/order instrumentation pass.
- Android playlist backup now has a bounded, versioned JSON codec in `commonMain`, atomic repository import, and Android Storage Access Framework import/export actions. Archive codec/database tests and the headless UI action test pass. The archive stores playlist metadata and references, not local audio bytes or resolved stream URLs; an Android content URI may still lack a read grant on another installation.

Historical verification for earlier commits: local checks on `a407e1c` passed Android debug/instrumentation packaging, Android unit tests, lint, desktop/headless Compose, shared suites, eXcor/Apple provider tests, and NewPipe Android compilation. On follow-up `72cbf9a`, hosted Verify run [40844482143](https://github.com/bxanedot/rynx/actions/runs/40844482143) and phone run [40844482288](https://github.com/bxanedot/rynx/actions/runs/40844482288) passed. Those runs predate the current eXcor removal and real-artwork changes; they do not validate this worktree. The release APK is unsigned and not installable without bxane's signing key.

## Implemented but unverified at runtime

- Physical Android phone acceptance, including real output routing, audibility, underrun behavior and performance; hosted emulator tests do not replace these checks.
- Live YouTube search/stream resolution, audible provider playback, audio focus, notification/headset behavior, and network recovery.
- Real Android output routing, audibility, glitch/underrun behavior, DAC format, and physical-device DSP/performance acceptance.
- Glass rendering cost, optical/motion comparison against Apple Music, physical-device performance, and battery behavior.

## Partial

- Android Library has local songs, favorites, recent tracks, persistent user playlists, playlist JSON backup actions, and basic editing/detail screens. The desktop/headless UI suite covers playlist detail, create/add/rename interactions, reorder, removal, and backup callbacks. Android SAF file-picker round trips, richer collection screens and robust offline workflows remain incomplete.
- Android player controls and queue are implemented; device-level end-to-end playback and exact Apple Music iOS visual/motion comparison remain incomplete.
- Apple discovery uses real catalog data and original local ranking, but the public feature inventory is not a verified copy of every Apple Music recommendation or chart algorithm.
- Linux/JVM desktop code is present; it does not establish Windows/macOS application readiness or native system media integration.

## Blocked

- The prior local API 36 software emulator never reached `sys.boot_completed`; hosted hardware-accelerated instrumentation passes, but a physical phone is needed for device-specific acceptance.
- The latest opt-in YouTube live request was refused in the network connection path before a provider response. Live provider behavior remains unverified.
- Spotify needs a registered client/callback, an authorized test account, and a current supported-playback/API review.
- The iOS host app is not configured; shared iOS target declarations exist but require an Apple toolchain and artwork review to validate. Android TV and Wear targets are not configured and need dedicated target/device acceptance.
- Release signing, complete transitive dependency/artwork review, and physical-device audio/performance validation remain open.

## Deferred

Remaining advanced audio work includes ReplayGain tag normalization, overlapping crossfade, AutoEQ, resampling/channel-map controls, spatial processing, true-peak compliance testing, and DAC/exclusive USB support. DJ, downloads beyond locally authorized media, AI, recognition, social rooms/backend, and external provider modules also remain deferred until core Android journeys pass their acceptance tests.

## Historical: 2026-10-29 eXcor/InnerTubeX provider chain

That earlier branch used Excor → InnerTubeX v0.11.2 → NewPipeExtractor v0.26.5. On 2026-10-29 the user requested that Excor be archived and removed from the active build; the old test results below are historical only.

Verification for this change: shared playback JVM tests passed (46), InnerTubeX adapter JVM tests passed (4), Android unit tests passed (9), Android debug APK packaging passed, Android lint passed, and the workspace boundary audit passed. This verifies the adapter contract and compilation only; live InnertubeX extraction and audible device playback remain unverified. The generated APK is debug-signed and not a production release.

## Current provider/archive and artwork change — local verification passed

The active Android chain is InnerTubeX → NewPipeExtractor. Apple public iTunes/RSS song search and lookup return real cover-art URLs, with matching artwork mapped to catalog results; InnerTubeX search rows map their largest published thumbnail. A live Apple search returned a real “Get Lucky” cover URL, and a separate image request returned HTTP 200 with `image/jpeg`. This proves metadata/artwork retrieval only, not redistribution rights or Android rendering.

The eXcor submodule, adapter, dependency linkage, hourly refresh workflow, and eXcor-only license assets have been removed; the upstream repository is untouched. YouTube.js (`youtubei.js`) remains a documented, unintegrated JavaScript provider because no Android/Kotlin host exists in this project. The Android provider switch is not described as including YouTube.js.

The local verification gate passed: Android debug, unsigned release, and instrumentation APK packaging; Android lint and unit tests; desktop/headless tests; Apple and InnerTubeX provider tests; NewPipe Android compilation; shared playback/design/UI tests; and the source-boundary audit. The latest debug APK is `apps/android/build/outputs/apk/debug/android-debug.apk` (SHA-256 `33a0500e3e23971a15230a229c7084be2d56cca67182f91ab2fb4479ee90bd0b`). The release APK is unsigned and cannot be installed without bxane's signing key. No current-worktree Android device/emulator test ran: adb is unavailable and the local software emulator did not boot. Live InnerTubeX/NewPipe extraction and audible playback remain unverified.

## 2026-10-29 Playlist archive slice

Added a bounded, versioned KMP JSON format and transactional SQLDelight import for user playlists, plus Android Storage Access Framework import/export controls. Playlist order and stable media references are retained; archives do not contain downloaded audio, resolved stream URLs, or Android document-provider grants. A local content URI can remain as a reference without a transferable read permission.

Local verification passed: `:shared:library:jvmTest`, `:apps:desktop:jvmTest`, `:apps:android:testDebugUnitTest`, `:apps:android:compileDebugAndroidTestKotlin`, `:apps:android:lintDebug`, and `:apps:android:assembleDebug`. Real Android system-picker round trips have not yet been run. At the starting commit `f390fde`, both PR #4 workflows passed: [Android verification](https://github.com/bxanedot/rynx/actions/runs/40859593200) and [hosted Android phone tests](https://github.com/bxanedot/rynx/actions/runs/40859593231). PR #4 currently has no submitted review comments; this new change still requires fresh CI.

## Next acceptance gates

1. Confirm live provider behavior and actual audible playback on a physical Android device without logging secrets.
2. Integrate download completion with the attribution sidecar and complete Spotify authorization.
3. Finish remaining Android screens, accessibility, Apple Music reference comparisons and device-level audio/performance tests.
4. Re-run the hosted Android suite after future changes to the app or navigation test harness.
5. Only after the Android completion gate, expand substantially to tablet, desktop, TV, Wear and iOS/iPadOS targets.

For full inventories and evidence see `docs/IMPLEMENTATION_PLAN.md`, `docs/parity/ANDROID_FEATURE_MATRIX.md`, `docs/parity/ANDROID_SCREEN_MATRIX.md`, `docs/parity/PLATFORM_MATRIX.md`, and `docs/parity/BLOCKERS.md`.
