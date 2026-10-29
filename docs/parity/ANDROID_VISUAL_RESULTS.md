# Android build and visual results

2026-10-28 · Linux x86_64 · Temurin JDK 27 · Gradle 9.8.2 · AGP 9.4.1 · compile SDK 37 / target 36 / min 26.

## Current shared-playback continuation

2026-10-28: The isolated playback-preparation worktree passed the full local deterministic gate in 10m18s:

`:apps:android:assembleDebug :apps:android:assembleRelease :apps:android:assembleDebugAndroidTest :apps:android:lintDebug :apps:android:testDebugUnitTest :providers:excor:jvmTest :providers:apple:jvmTest :providers:newpipe:compileAndroidMain :shared:playback:jvmTest :shared:design:jvmTest :shared:ui:jvmTest --offline --console=plain`

JUnit reports contain 81 cases: 78 passed, three opt-in live-provider checks skipped, and zero failures/errors. Counts: shared playback 37, design 15, shared UI 7, Excor 8 (two live skips), Apple provider 10 (one live skip), and Android local unit tests 4. Android debug, minified unsigned release, and instrumentation APKs were produced. Lint reported zero errors and one warning (`OldTargetApi`, target SDK 36).

Artifacts from this worktree:

- Debug APK: 21,658,776 bytes; SHA-256 `9e8ca215020418b2735e4c47a7f1652d113d5ea122babc3fae975d0bd9e0be8b`.
- Minified release APK: 6,931,445 bytes; SHA-256 `98fa23990ddda87a4f9fec184980beea795a6adbb3c07852a5eefd4cc3b54433`. It is unsigned and is not a release candidate.
- Instrumentation APK: 430,090 bytes; SHA-256 `01c1c06485eeb9c0dbbbf27d809383f476ea2d5b85340616ff87831f47b559c4`.

The new shared preparation and stream-resolution contracts are covered by deterministic common tests; instrumentation asserts generated local audio metadata and pause/resume and separately checks URL-scoped request headers. The instrumentation APK only compiled: a local API 36 software emulator never reached `sys.boot_completed`, so no current-branch device test ran. The opt-in Excor live request failed before receiving a provider response (`java.io.IOException`, `ConnectPlan.kt:472`, connection refused). Apple live checks were not attempted in this run. Neither release playback nor full product parity is established by this build.

## Latest local verification

The complete command passed in 8m15s against Excor `4e8c8c5f683d75d83dabe4a9ca0c6ab18d8f92e1`:

`:apps:android:assembleDebug :apps:android:assembleRelease :apps:android:assembleDebugAndroidTest :apps:android:lintDebug :apps:android:testDebugUnitTest :apps:desktop:jvmTest :providers:excor:jvmTest :providers:newpipe:compileAndroidMain :shared:playback:jvmTest :shared:design:jvmTest :shared:ui:jvmTest`

| Scope | Cases | Failures / errors / skips |
|---|---:|---|
| Compose/JVM app harness | 39 | 0 / 0 / 0 |
| Shared material/geometry/budget | 7 | 0 / 0 / 0 |
| Shared UI renderer/morph/motion | 7 | 0 / 0 / 0 |
| Shared playback/order/lyrics/favorites | 17 | 0 / 0 / 0 |
| Excor adapter | 5 | 0 / 0 / 0 |
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

## Navigation-motion checkpoint

2026-10-28: `:shared:design:jvmTest :shared:ui:jvmTest :apps:desktop:jvmTest :apps:android:assembleDebug :apps:android:assembleDebugAndroidTest :apps:android:lintDebug :apps:android:testDebugUnitTest` completed successfully. The resulting reports contain 78 applicable tests: design 15, shared UI 7, Compose/JVM app harness 49, and Android local unit 7, with zero failures, errors or skips. The design suite was explicitly rerun after Gradle initially marked it up-to-date. Android debug and instrumentation APK packaging succeeded; lint reported zero errors and two existing warnings (`OldTargetApi`, `ObsoleteSdkInt`).

`NavigationDeviceTest` covers native Search text entry, query retention and Android Back navigation. It compiled into the instrumentation APK, but this workspace reports no connected device and no `/dev/kvm`, so that test did not execute locally. The PR's hosted Android workflow must be checked before claiming native device verification. These tests establish behavior in the headless Compose harness, not Apple/Vyon visual similarity, glass rendering quality or device motion timing.

## Hosted phone run 20

Run [40737479787](https://github.com/bxanedot/rynx/actions/runs/40737479787) for commit `ff2b8ae` built the phone/instrumentation APKs and enabled KVM successfully, then failed during connected tests. `LyricsImportRecreationTest` used blocking `ActivityScenario.recreate()` while the system document picker held the foreground; it timed out waiting for the stopped host to resume. The following `NavigationDeviceTest` then launched a host that only reached `CREATED` and timed out waiting for the Search input. Device logs confirm no resumed `MainActivity` during that second test. The lyrics test now requests recreation through the retained host instance and asserts the replacement receives the picker result; this correction has compiled locally but is awaiting another hosted device run. Native Search and lifecycle behavior are not yet verified.

## Hosted phone run 22 and current rerun preparation

Run [40739637239](https://github.com/bxanedot/rynx/actions/runs/40739637239) for `e5b49d0` passed APK build/package in Verify run [40739637162](https://github.com/bxanedot/rynx/actions/runs/40739637162), but connected tests failed again. Calling `Activity.recreate()` directly while DocumentsUI was foregrounded brought the Rynx host above the picker, so its file row could not be selected. The separate Search test timed out finding an enabled `Search your music` field; that run had no accessibility-tree diagnostic. The current local test revision recreates the host before opening the real picker and then verifies the imported lyric on the active player. This avoids relying on rotation of a background task; returning from a picker that remains open through recreation is still unverified. Search now waits for tab selection and attaches an accessibility snapshot to timeouts. These changes are not yet built or rerun on hosted Android.


## Apple discovery checkpoint after review fixes

A fresh default suite passed on 2026-10-28 in 1m42s: Android debug/test packaging, lint/unit tests, all shared tests, both provider test suites, and the complete Compose harness. Reports contain 106 cases: 104 passed, two opt-in live checks skipped, no failures/errors. Breakdown: Compose harness 43; Android unit 7; playback/ranking/listening 26; design 7; shared UI 7; Apple provider 9 (one live skip); Excor provider 7 (one live skip). The explicit live run separately passed Apple engine search/lookup/artwork and public data checks (51 genres, 100 regional, 100 global, 50 cities, 50 Pop songs).

The independent review's four behavioral regressions were observed failing before fixes and now pass. New shared listening tracker tests pass; its service/background instrumentation is compiled but has not yet run. Default test tasks track the live-contract opt-in as an input so cached live results cannot silently stand in for default execution.

The complete pre-review build also packaged minified release in 9m04s. That release predates the final review fixes; a fresh release package is still needed for the latest source. Earlier hashes/counts above are historical checkpoints. Current source publication and native CI results are recorded separately, not inferred from local headless tests.

## Hosted phone run 24

Run [40742491136](https://github.com/bxanedot/rynx/actions/runs/40742491136) for `1b3ea78` passed APK packaging but 8 of 10 device tests passed. The recreated-host lyrics workflow reached DocumentsUI; Android displayed the temporary `.lrc` document as `.lrc.txt`, so the test's exact-name lookup missed the existing row. The navigation test opened Library correctly, but `AccessibilityNodeInfo.isSelected` did not reflect the Compose selected semantics; the screen content was present in the failure snapshot. The test now identifies destinations by page content and accepts the document provider suffix. The first follow-up run is recorded below. Verify run [40742490946](https://github.com/bxanedot/rynx/actions/runs/40742490946) passed.

## Local verification of run 24 test fixes

The updated shared design, shared UI and Compose app harness suites passed. A separate Gradle invocation with `--rerun-tasks` freshly compiled `:apps:android:compileDebugAndroidTestKotlin` successfully. Debug and instrumentation APK assembly targets also passed in the preceding suite. The hosted run exposed one more test-selector assumption, recorded below.

## Hosted phone run 26

Run [40745857565](https://github.com/bxanedot/rynx/actions/runs/40745857565) for `feaf55a` built both APKs and passed 9/10 device tests. Lyrics import after host recreation passed. Search opened and exposed both its accessible “Search your music” description and an enabled editable `EditText` with `ACTION_SET_TEXT`, but Compose presented them as separate accessibility nodes; the test incorrectly required the description node itself to carry text-input actions. The test was corrected to find the actual editable node; run 28 below verifies the full Search/back flow.

## Hosted phone run 28

Run [40747483200](https://github.com/bxanedot/rynx/actions/runs/40747483200) for `f6b5523` built the debug and instrumentation APKs and passed all 10/10 API 36 KVM device tests in 2m57s. This includes lyrics import after host recreation and Search text entry, Back to the previous tab, and query restoration. The `Verify Android-first Rynx` workflow [40747483065](https://github.com/bxanedot/rynx/actions/runs/40747483065) also passed its source-boundary audit and Android/shared/provider checks. These hosted results close the device-test failures described above; they do not establish visual parity or physical-device performance.
