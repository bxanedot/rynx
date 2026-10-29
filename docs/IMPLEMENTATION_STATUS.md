# Rynx implementation status

Status date: 2026-10-29. Branch `feature/android-audio-excor`, based on merged `main` commit `11681c9`. This is an engineering status report, not a production-readiness claim.

## Implemented and deterministically tested

- Shared Kotlin playback queue/order, metadata matching, recommendations, lyrics parsing, appearance/motion models, and provider-neutral playback preparation contracts.
- Apple public-catalog data models, Home collections, Explore genre navigation, local recommendation ranking, and original Rynx brand/palette SVG assets.
- Compose navigation/player shells, Liquid Glass/Solid/AMOLED materials, animated player/navigation handoff, and shared UI regression coverage.
- Android Media3 session/service, local audio import, favorites, recent metadata, queue controls, and imported LRC support at code or unit-test level.
- A shared `commonMain` PCM DSP core (10/31-band and parametric biquads, tone shelves, headroom, stereo width/balance, crossfeed, linked compression and an interpolated peak guard) is attached to the existing Android Media3 sink. Pure Mode bypasses Rynx processing, Android settings persist locally, and the existing player exposes the sound sheet. Hosted Android API 36 instrumentation passes, including generated PCM playback through the Media3 service, pause/resume and playlist database reopen/order. Physical-device output quality and route behavior remain unverified.
- Portable KMP download provenance now encodes `Downloaded by Rynx`, app ID and track/provider/timestamp fields; an Android writer atomically writes a `.rynx.json` sidecar without touching downloaded media bytes. Serializer and file-preservation tests pass. Rynx does not yet have an app download queue, so the writer is a ready integration point and no current download invokes it automatically.
- eXcor catalog/stream adapter and NewPipe Android fallback adapter, with deterministic mapping, redaction, ordering, cancellation, and build checks.
- Persistent playlists use SQLDelight with common KMP models, validation/edit rules, transactions and Flow observation; Android supplies the SQLite driver. Library create/rename/add/remove/reorder/delete and playlist detail UI are wired. Shared SQLDelight/rules tests, headless workflows and hosted Android SQLite close/reopen/order instrumentation pass.

Local verification of the app-source commit `a407e1c` completed successfully: Android debug/instrumentation packaging, Android unit tests, lint, desktop/headless Compose, shared playback/library/design/UI suites, eXcor and Apple provider tests, and NewPipe Android compilation. Reports contain 161 cases: 158 passed, three opt-in live-provider checks skipped, and zero failures/errors. Lint has zero errors and two warnings. On the current test-only follow-up commit `72cbf9a`, hosted Verify run [40844482143](https://github.com/bxanedot/rynx/actions/runs/40844482143) passed the source-boundary audit, debug/instrumentation/minified-release packaging, lint and configured shared/provider checks. Hosted phone run [40844482288](https://github.com/bxanedot/rynx/actions/runs/40844482288) passed debug APK installation and API 36 connected instrumentation. The release APK is unsigned and not installable without bxane's signing key.

## Implemented but unverified at runtime

- Physical Android phone acceptance, including real output routing, audibility, underrun behavior and performance; hosted emulator tests do not replace these checks.
- Live YouTube search/stream resolution, audible provider playback, audio focus, notification/headset behavior, and network recovery.
- Real Android output routing, audibility, glitch/underrun behavior, DAC format, and physical-device DSP/performance acceptance.
- Glass rendering cost, optical/motion comparison against Apple Music, physical-device performance, and battery behavior.

## Partial

- Android Library has local songs, favorites, recent tracks and persistent user playlists with basic editing/detail screens. The desktop/headless UI suite covers playlist detail, create/add/rename interactions, reorder and removal. Android instrumentation execution, import/export, richer collection screens and robust offline workflows remain incomplete.
- Android player controls and queue are implemented; device-level end-to-end playback and exact Apple Music iOS visual/motion comparison remain incomplete.
- Apple discovery uses real catalog data and original local ranking, but the public feature inventory is not a verified copy of every Apple Music recommendation or chart algorithm.
- Linux/JVM desktop code is present; it does not establish Windows/macOS application readiness or native system media integration.

## Blocked

- The prior local API 36 software emulator never reached `sys.boot_completed`; hosted hardware-accelerated instrumentation passes, but a physical phone is needed for device-specific acceptance.
- The latest opt-in YouTube live request was refused in the network connection path before a provider response. Live provider behavior remains unverified.
- Spotify needs a registered client/callback, an authorized test account, and a current supported-playback/API review.
- iOS/iPadOS, Android TV, and Wear targets are not configured. iOS/Apple Simulator validation requires macOS; Android TV/Wear also need dedicated target and device acceptance.
- Release signing, complete transitive dependency/artwork review, and physical-device audio/performance validation remain open.

## Deferred

Remaining advanced audio work includes ReplayGain tag normalization, overlapping crossfade, AutoEQ, resampling/channel-map controls, spatial processing, true-peak compliance testing, and DAC/exclusive USB support. DJ, downloads beyond locally authorized media, AI, recognition, social rooms/backend, and external provider modules also remain deferred until core Android journeys pass their acceptance tests.

## Next acceptance gates

1. Confirm live provider behavior and actual audible playback on a physical Android device without logging secrets.
2. Integrate download completion with the attribution sidecar and complete Spotify authorization.
3. Finish remaining Android screens, accessibility, Apple Music reference comparisons and device-level audio/performance tests.
4. Re-run the hosted Android suite after future changes to the app or navigation test harness.
5. Only after the Android completion gate, expand substantially to tablet, desktop, TV, Wear and iOS/iPadOS targets.

For full inventories and evidence see `docs/IMPLEMENTATION_PLAN.md`, `docs/parity/ANDROID_FEATURE_MATRIX.md`, `docs/parity/ANDROID_SCREEN_MATRIX.md`, `docs/parity/PLATFORM_MATRIX.md`, and `docs/parity/BLOCKERS.md`.
