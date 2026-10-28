# Android build and visual results

2026-10-28 · Linux x86_64 · Temurin JDK 27 · Gradle 9.8.2 · AGP 9.4.1 · compile SDK 37 / target 36 / min 26.

## Latest local verification

The complete command passed in 8m15s against VyonEx `4e8c8c5f683d75d83dabe4a9ca0c6ab18d8f92e1`:

`:apps:android:assembleDebug :apps:android:assembleRelease :apps:android:assembleDebugAndroidTest :apps:android:lintDebug :apps:android:testDebugUnitTest :apps:desktop:jvmTest :providers:vyonex:jvmTest :providers:newpipe:compileAndroidMain :shared:playback:jvmTest :shared:design:jvmTest :shared:ui:jvmTest`

| Scope | Cases | Failures / errors / skips |
|---|---:|---|
| Compose/JVM app harness | 39 | 0 / 0 / 0 |
| Shared material/geometry/budget | 7 | 0 / 0 / 0 |
| Shared UI renderer/morph/motion | 7 | 0 / 0 / 0 |
| Shared playback/order/lyrics/favorites | 17 | 0 / 0 / 0 |
| VyonEx adapter | 5 | 0 / 0 / 0 |
| Android local unit | 7 | 0 / 0 / 0 |
| Total | 82 | 0 / 0 / 0 |

The JVM Compose target is an Android UI regression harness; desktop product development remains paused. Checks cover real backdrop source changes, Solid/AMOLED policies, player geometry continuity, reversible gestures, navigation handoff/hit testing, panel input ownership, seek cancellation, queue actions, lyrics, mode controls, landscape and enlarged fonts. They do not establish native playback or Android GPU behavior.

Lint has zero errors and two warnings: `OldTargetApi` (target 36) and `ObsoleteSdkInt` (adaptive-icon v26 resource folder with min 26). No baseline hides failures.

## Development artifacts

- `apps/android/build/outputs/apk/debug/android-debug.apk`: 28,862,808 bytes, SHA-256 `768ea0b691b31c8cfd196573a27a0e5f7e3c59b74763fa336015c77575e565d9`. Local debug signature verifies. Package `dev.bxane.rynx`, display name `rynx`, version `0.1.0-dev`.
- `apps/android/build/outputs/apk/release/android-release-unsigned.apk`: 6,837,545 bytes, SHA-256 `70cfa9eebdfd82b6fcca9dcbb3752565663c9bd1b75c6f052d080041fbd7dba4`. Minified/resource-shrunk, unsigned; no release key is supplied.
- Instrumentation APK compiles. Full license/notice files are bundled as generated Android assets.

These are development artifacts, not a release candidate. Hashes describe this local build, rather than all future CI builds.

## Visual evidence and limits

Own deterministic frames are generated under `apps/desktop/build/reports/screenshots/`, including `phone-player-liquidglass.png`, `phone-player-solid.png`, `phone-player-amoled.png`, queue and Appearance renders. The real-renderer test changes red/blue backdrop content and observes Glass respond; Solid ignores the input and AMOLED samples pure black. This is not an Apple similarity metric.

The user authorizes Apple Music as direct reference; public player/queue stills and guides supply observed proportions. No Vyon capture, 95% similarity result or measured reference motion trace exists. Original vectors avoid embedded Apple font/artwork payloads.

The local software API 35 ATD emulator eventually booted, then its host QEMU process crashed before app installation. API 36 software-emulator attempts were also unstable without `/dev/kvm`. No connected app test passed locally. The existing Rynx repository CI is configured to run API 36 Pixel 6 instrumentation with hardware acceleration and collect device frames/logs; results must be recorded after execution.

Physical-device optical fidelity, audible quality, accessibility service behavior, low-end/high-refresh performance, battery/latency, live extraction, Spotify, downloads and advanced DSP remain incomplete or unverified. See `BLOCKERS.md` and `ANDROID_FEATURE_MATRIX.md`.


## Apple discovery checkpoint after review fixes

A fresh default suite passed on 2026-10-28 in 1m42s: Android debug/test packaging, lint/unit tests, all shared tests, both provider test suites, and the complete Compose harness. Reports contain 106 cases: 104 passed, two opt-in live checks skipped, no failures/errors. Breakdown: Compose harness 43; Android unit 7; playback/ranking/listening 26; design 7; shared UI 7; Apple provider 9 (one live skip); VyonEx provider 7 (one live skip). The explicit live run separately passed Apple engine search/lookup/artwork and public data checks (51 genres, 100 regional, 100 global, 50 cities, 50 Pop songs).

The independent review's four behavioral regressions were observed failing before fixes and now pass. New shared listening tracker tests pass; its service/background instrumentation is compiled but has not yet run. Default test tasks track the live-contract opt-in as an input so cached live results cannot silently stand in for default execution.

The complete pre-review build also packaged minified release in 9m04s. That release predates the final review fixes; a fresh release package is still needed for the latest source. Earlier hashes/counts above are historical checkpoints. Current source publication and native CI results are recorded separately, not inferred from local headless tests.
