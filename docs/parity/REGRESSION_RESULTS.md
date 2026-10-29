# Regression results

2026-10-27, Linux x86_64, OpenJDK Temurin 27.0.1, Gradle 9.8.2, Kotlin 2.4.21, Compose 1.12.1.

## Shared-playback continuation

2026-10-28, isolated Android-first worktree: full local gate passed in 10m18s. It built Android debug and minified unsigned release APKs plus the instrumentation APK; Android lint had zero errors and one `OldTargetApi` warning. The included reports contain 81 cases: 78 passed, three opt-in live checks skipped, zero failures/errors (37 playback, 15 design, 7 shared UI, 8 Excor, 10 Apple provider, 4 Android unit). Excor live smoke was attempted separately but the network connection was refused before any provider response. Instrumentation compiled; the local software emulator failed to boot, so no current-branch connected test result exists. See `ANDROID_VISUAL_RESULTS.md` and `BLOCKERS.md` for artifact hashes and limits.

## Checks
Executed with the pinned Gradle 9.8.2 distribution and the environment's proxy/CA flags:
`:apps:desktop:jvmTest :providers:excor:jvmTest :shared:playback:jvmTest`
and `:apps:desktop:packageUberJarForCurrentOS`.

- Shared queue: 9 tests pass (end/skip/repeat, duplicate media identities, edit invariants, invalid input and snapshot independence).
- PCM decoder: 3 tests pass (real generated WAV decode/duration, memory budget, invalid input).
- Local library persistence: 2 tests pass (Unicode paths/deduplication/missing media, replacement, corrupt index retained).
- Compose render/callback tests: 4 tests pass (accessible import, transport/queue callbacks, desktop/narrow library/player/queue render, YouTube Music query submission/pagination and provider-row semantics).
- Desktop session: 3 integration tests pass (persisted-library restore/unreadable media; corrupt-index retention after import; automatic-advance failure/recovery exercised twice using a deterministic test output driver). The driver does not validate audible output.
- Desktop catalog: 2 tests pass (query trimming, pagination/deduplication, safe provider-error text).
- Excor adapter: 5 tests pass (search type mapping, continuation mapping, absent metadata handling, stream URL/header redaction and refusal error classification).
- Total: 28 tests, zero failures/errors/skips. Linux uber-JAR packaging passes at `apps/desktop/build/compose/jars/rynx-linux-x64-0.1.0.jar`.
- Workspace source boundary audit passes. Both approved dependency tracked checkouts unchanged.

Headless frames: `apps/desktop/build/reports/screenshots/`. Own rendered captures are copied under `captures/` for review. These are smoke/regression captures, not Apple comparison goldens.
An earlier Java 21 baseline launch under Xvfb rendered with explicit `-Dskiko.renderApi=SOFTWARE`; Skiko's default GL path failed in that headless environment. That launch predates Excor linking and the JVM 26 target. The current packaged JAR was build-verified but not relaunched, so the retained screenshot is not evidence for the current binary. Xvfb was downloaded/extracted outside the repository; no system package mutation was required.

## Explicitly not verified
No current Java 26+ desktop launch, live YouTube Music search, audio extraction, stream playback, download/offline, audible output device test, background media integration, long playback, gapless/crossfade, native platform runtime beyond Linux, Spotify, advanced DSP, backend, Apple screenshot similarity score, glass, gesture/motion trajectories, reduced-motion equivalence or performance benchmarks. No public CI run has occurred; the workflow is configured only.

Build emits a Gradle `Configuration.setVisible` deprecation warning, scheduled for Gradle 11 removal (from Compose/Gradle build integration). No public signed production release or Android TV/Wear artifacts have been produced.

## Independent review and packaging limit
Review reproduced two bugs: unhandled automatic-advance failures terminated monitoring, and import after failed restore overwrote the original index. Both regressions failed before the fixes and now pass. Follow-up review found both resolved. DIY editor status was corrected to unverified.

`packageDeb` was attempted and failed because `fakeroot` is absent; jpackage explicitly reported this prerequisite. No Debian installer is available. The development JAR remains the runnable artifact.

## Android-first continuation
Android is P0; desktop UI development is paused. On 2026-10-28, `:apps:android:assembleDebug :apps:android:assembleDebugAndroidTest :apps:android:lintDebug :apps:android:testDebugUnitTest :apps:desktop:jvmTest :providers:excor:jvmTest :providers:newpipe:compileAndroidMain :shared:playback:jvmTest` succeeded. The generated test reports contain 19 Compose/JVM tests, 5 Android unit tests, 5 Excor adapter tests and 11 shared playback tests (40 total, zero failures). Lint has zero errors and two warnings (`OldTargetApi`, `ObsoleteSdkInt`). The connected instrumentation APK builds, but instrumentation is not run because this workspace has no attached Android device and no `/dev/kvm`. See ANDROID_VISUAL_RESULTS.md for artifact hash and visual limits. These checks verify compilation and deterministic tests; they do not establish Android reference parity, audible playback or release readiness.


## Universal-material continuation

2026-10-28: complete Android debug/release/test packaging, lint and 82 local cases passed. New shared design/UI tests cover real backdrop input, three material policies, player continuity, navigation ownership, lyrics, queue and cancellation. See ANDROID_VISUAL_RESULTS.md for the current scope, hashes and device limitations. Earlier counts above are historical. No production/parity claim follows from this pass.

## Navigation-motion continuation

2026-10-28: debug app and Android instrumentation APK builds, Android unit tests, lint, the Compose/JVM UI harness and shared UI/design suites succeeded. There were 78 applicable test cases with zero failures, errors or skips (15 design, 7 shared UI, 49 app harness, 7 Android unit). The design suite was explicitly rerun after the full command reported it up-to-date. Lint has zero errors and the two existing warnings noted above. Native instrumentation compiled but did not run locally because there is no connected device or `/dev/kvm`; see `ANDROID_VISUAL_RESULTS.md` for the hosted-check requirement and fidelity limits.

Hosted phone run 20 (`ff2b8ae`) built APKs but failed in connected instrumentation: the lyrics picker recreation test blocked in `ActivityScenario.recreate()` while its host was stopped, then the navigation Search test ran without a resumed host. The lifecycle test now requests recreation through the retained activity and checks the replacement instance after returning from the picker. Local app/test APK build passes; hosted rerun is pending. Details and limits are in `ANDROID_VISUAL_RESULTS.md`.

Run 22 (`e5b49d0`) passed APK packaging but connected device tests still failed: direct recreation covered the picker row and native Search input was not found. The current local revisions recreate before opening the picker, attach accessibility snapshots to test failures, and await another hosted run. See `ANDROID_VISUAL_RESULTS.md` for evidence and limits.


Hosted phone run 24 (`1b3ea78`) passed APK packaging and 8 of 10 device tests. The failures were strict lookups for DocumentsUI's `.lrc.txt` label and `AccessibilityNodeInfo.isSelected`. Run 26 (`feaf55a`) passed 9/10 and confirmed the lyrics fix; its remaining failure was a Search-test selector expecting the accessible description and editable action on one node. The test now targets the editable `EditText`. Run 28 (`f6b5523`) passed all 10/10 API 36 KVM device tests, including Search text entry/back restoration and lyrics import. Verify run 32 passed source-boundary, Android, shared and provider checks. See `ANDROID_VISUAL_RESULTS.md`.

## Persistent-playlist continuation

2026-10-28: SQLDelight-backed shared playlist persistence and Library workflows now include create, rename, add, remove, reorder and delete. The verification set passed 77 tests with zero failures, errors or skips: 11 shared library, 9 shared UI, 53 desktop/headless Compose, and 4 Android unit tests. Android debug and unsigned minified release APKs built; the Android instrumentation APK compiled and packaged. Android lint reported zero errors and one `OldTargetApi` warning for target SDK 36. The Android database close/reopen instrumentation test is not device-verified because this workspace has no connected device and no `/dev/kvm`. These builds do not establish release signing, audible playback or full Android/device parity.

Artifacts from this run:

- Debug APK: `apps/android/build/outputs/apk/debug/android-debug.apk` (SHA-256 `76980586157316492d77096df5e5899af87493e52f763b017b7a2191d9033891`).
- Unsigned release APK: `apps/android/build/outputs/apk/release/android-release-unsigned.apk` (SHA-256 `117f0771fe4c61465e82e25dd42236565d45cfc538d07fef5c7bd080b9b58765`).
- Instrumentation APK: `apps/android/build/outputs/apk/androidTest/debug/android-debug-androidTest.apk` (SHA-256 `f0fbb112b141dba21bb2ea07d7dfb613347144f13af6410037b1a48792268349`).

## Integrated DSP continuation

2026-10-28: shared playback tests passed 43/43, shared UI tests 9/9, desktop/headless Compose tests 54/54 after correcting a fixed-state UI test fixture, and Android JVM tests 8/8. Android debug Kotlin compilation, instrumentation Kotlin compilation, and `assembleDebug` passed; the generated debug APK is `apps/android/build/outputs/apk/debug/android-debug.apk` (22,331,218 bytes). The unit tests exercise PCM16 byte-exact Pure Mode, PCM16 EQ, active 24-bit integer processing, active 32-bit float processing, shared EQ response, limiter ceiling, and stereo-linked compression. No Android device is attached, the software AVD has not booted, and `/dev/kvm` is absent. This is code/build/signal-test evidence only: no audible output, DAC route, underrun, battery, or performance claim is made. The current signed/release artifact was not rebuilt in this continuation.

2026-10-29: KMP download-provenance serialization and Android sidecar file-preservation tests passed; shared playback now passes 44 tests and Android JVM tests pass 9 tests. `:apps:android:assembleDebug` and `:apps:android:compileDebugAndroidTestKotlin` passed. Debug APK: `apps/android/build/outputs/apk/debug/android-debug.apk` (22,331,218 bytes, SHA-256 `0548ea7e8ac1625eb02d8e1a35f9edf785062569c0a88597e063a95c09dfe825`). The sidecar records `Downloaded by Rynx` without altering media, but Rynx's download pipeline does not yet exist, so automatic per-download invocation remains pending.

## Combined eXcor/audio/library branch verification

2026-10-29, branch `feature/android-audio-excor`, based on merged `main` `11681c9`. Local command completed successfully in 9m15s (279 Gradle tasks): Android debug and instrumentation APK packaging, Android lint/unit tests, desktop/headless UI, shared playback/library/design/UI tests, eXcor/Apple provider tests, and NewPipe Android compilation. The XML reports contain 161 test cases: 158 passed, three opt-in live-provider cases skipped, zero failures/errors. Lint reported zero errors and two warnings. The live-provider skips were not converted into successful provider/network results.

- Debug APK: `apps/android/build/outputs/apk/debug/android-debug.apk`, 22,003,626 bytes, SHA-256 `c0f5c292f539361649f6cb9227ce848e149ecad994c549e84e08f5a0a91bff14`.
- Instrumentation APK: `apps/android/build/outputs/apk/androidTest/debug/android-debug-androidTest.apk`, 434,010 bytes, SHA-256 `f0fbb112b141dba21bb2ea07d7dfb613347144f13af6410037b1a48792268349`.

The instrumentation APK compiled but `adb devices -l` returned no attached devices. No connected run, audible output, route/DAC test, or current-branch emulator result exists yet. Release minification/signing was not run on this combined branch. GitHub's API 36 hardware-accelerated phone workflow must run on the pushed branch before marking device tests verified.

## Current combined eXcor/audio/library CI

2026-10-29, commit `a407e1c` on PR #4. Verify run [40838202297](https://github.com/bxanedot/rynx/actions/runs/40838202297) passed the source-boundary audit, Android debug/instrumentation/minified-release packaging, lint, Android unit tests, desktop/headless Compose, shared modules, provider tests and NewPipe Android compilation. The release APK built successfully but remains unsigned.

Hosted phone run [40838202274](https://github.com/bxanedot/rynx/actions/runs/40838202274) passed the `adb install -r` step and the configured API 36 Pixel 6 `connectedDebugAndroidTest` suite. This verifies APK installation and current Android instrumentation in the hosted emulator. It does not prove physical-device audibility, DAC routing, audio quality or performance.

The first device run on `4d0400c` exposed that the new playlist test returned a `Boolean`, which JUnit rejects; the test now has a `void` method signature and verifies database reopen/order. A later run on `ab4c09b` passed Verify but the navigation test found the launcher instead of Rynx; the latest run passed. The cause of that intermittent failure is not established. The Android check script now prints sanitized fatal-app logcat excerpts if a connected test fails again.

Install the debug APK with `adb install -r`. A debug APK signed by a different development key cannot update an existing install; uninstalling `dev.bxane.rynx` first removes app data. The release artifact is unsigned, so Android correctly refuses to install it until an owner-controlled signing key is configured.

## Android APK installation and navigation-test follow-up

2026-10-29, current PR commit `72cbf9a`. Verify run [40844482143](https://github.com/bxanedot/rynx/actions/runs/40844482143) and API 36 Pixel 6 phone run [40844482288](https://github.com/bxanedot/rynx/actions/runs/40844482288) both passed. The phone run explicitly installed the debug APK and passed connected instrumentation, including Search/back behavior. The app implementation did not change in this follow-up; the instrumentation launcher was made deterministic.

Intermediate device runs documented the test harness issue: `ActivityScenario` intermittently left the launcher as the active accessibility window. A `CLEAR_TASK` retry destroyed the scenario-owned activity, while a `force-stop` shell variant could terminate the instrumentation process and left an empty diagnostic report. The final test uses `am start -W` without force-stopping Rynx; the API 36 connected suite passes. This does not change the separate APK signing status: debug installs; minified release remains unsigned pending an owner-controlled key.
2026-10-29, current `feature/android-audio-excor` worktree after adding InnerTubeX. Local command passed `:shared:playback:jvmTest`, `:providers:innertubex:jvmTest`, `:apps:android:testDebugUnitTest`, `:apps:android:lintDebug`, and `:apps:android:assembleDebug`. Reports: 46 shared playback tests, 4 InnerTubeX adapter tests, 9 Android unit tests; zero failures. `tools/audit_workspace.py` passed. The APK package/signature/retained GPL assets were inspected. Live InnerTubeX extraction and audible device playback were not tested; local KVM/physical device is unavailable. APK SHA-256: `2e539dc6a036f451e1e92af610b2422f5ef88b0c3d3f240373a8bdccee591218`.

## Persistent local playback restoration

2026-10-29, `feature/android-audio-excor`: common playback tests passed 50/50 and Android JVM tests passed 9/9. `:apps:android:lintDebug`, `:apps:android:assembleDebug`, `:apps:android:assembleRelease`, and `:apps:android:assembleDebugAndroidTest` passed. The new service-level restoration test is packaged in the Android test APK but has not been executed locally because the API 35 software emulator remained offline without `/dev/kvm`; run it on the hosted API 36 phone workflow.

- Debug APK: `apps/android/build/outputs/apk/debug/android-debug.apk` (23,030,925 bytes), SHA-256 `1ea5fdb4d631bec0f4f36cc679875770010fef85969d653f961aa327fe529ade`; `apksigner verify` passed. Package `dev.bxane.rynx`, version `0.1.0-dev`, min SDK 26, target SDK 36.
- Unsigned minified release APK: `apps/android/build/outputs/apk/release/android-release-unsigned.apk` (7,235,932 bytes), SHA-256 `9e3b4f89443a84bcbbaa3dd833f98a122fa66bece2ecdbe09c982726b27de77a`.
- Android instrumentation APK: `apps/android/build/outputs/apk/androidTest/debug/android-debug-androidTest.apk` (481,955 bytes), SHA-256 `f19d20cee5bade9432d8df5d1fcbefdc30b80f414ae94fd7aecd66c21be8f84b`.

The resume format stores only bounded local `content://` media IDs plus display text, queue index/position, repeat, and shuffle order. It always restores paused. Provider IDs, signed stream URLs, and request headers are excluded; unsupported queues clear the snapshot. The feature remains incomplete until current-branch hosted instrumentation and permission-revocation behavior are verified.

2026-10-29 follow-up: Verify run [40882118834](https://github.com/bxanedot/rynx/actions/runs/40882118834) passed on `49abec7`, including Android variant assembly, lint and the shared/provider suites. The API 36 run [40882129696](https://github.com/bxanedot/rynx/actions/runs/40882129696) reached the later `PlaybackSessionTest` but failed its pause/resume assertion; it emitted no `PlaybackResumeTest` failure. The restore instrumentation now stops its service before deleting test MediaStore items so later tests receive a clean Media3 session. The diagnostic script now uses stock `grep` because hosted runners do not include ripgrep. The hosted API 36 run [40883902747](https://github.com/bxanedot/rynx/actions/runs/40883902747) on `07b3017` failed the resume assertion: the test could read its seeded snapshot before service startup, but the service observed no queue. The test now flushes its seed synchronously before service startup, and the rerun is pending.


## Apple artwork and provider/archive update

2026-10-29, local worktree on `feature/android-audio-excor`, starting at `b3ae8af`. The full verification command passed in 16m13s: Android debug, unsigned release, and instrumentation APK packaging; Android lint/unit tests; desktop JVM tests; Apple/InnerTubeX provider tests; NewPipe Android compilation; shared playback/design/UI tests. Reports across these suites contain 163 cases: 161 passed, two opt-in Apple live tests skipped, zero failures/errors. A second targeted JVM pass over provider/shared/UI/desktop/Android unit suites passed in 1m12s. `tools/audit_workspace.py`, JSON parsing for `features.json`, and `git diff --check` pass. The active app graph no longer includes eXcor, and a scan of the debug APK found no eXcor/EJS entries.

The opt-in Apple test `actualAppleSongSearchReturnsCoverArtwork` passed using the system CA truststore: Apple returned “Get Lucky” and the 1200×1200 `mzstatic.com` artwork URL recorded in `APPLE_DISCOVERY.md`. A separate image GET returned HTTP 200 (`image/jpeg`, 120,263 bytes). This is metadata/image retrieval evidence only. The Android live InnerTubeX/NewPipe path, audible playback, and artwork rendering on a device remain unverified. No local `adb` executable or booted emulator was available, so instrumentation was packaged but not run.

- Debug APK: `apps/android/build/outputs/apk/debug/android-debug.apk`, 38,856,071 bytes, SHA-256 `33a0500e3e23971a15230a229c7084be2d56cca67182f91ab2fb4479ee90bd0b`.
- Unsigned release APK: `apps/android/build/outputs/apk/release/android-release-unsigned.apk`, 13,974,642 bytes, SHA-256 `34a515797444766a835142da1f024d38f74876fa083d07a8e4b4bfb1f599e521`.
- Instrumentation APK: `apps/android/build/outputs/apk/androidTest/debug/android-debug-androidTest.apk`, 482,241 bytes, SHA-256 `32abd50f604d6514edfe2498f88b45053efeefba94cd5fe7ac0ef379418cd8ab`.

The debug APK is the local debug build intended for installation. The release APK is unsigned and requires the owner's signing key. See `ANDROID_VISUAL_RESULTS.md` for historical hosted-device results; they are not a current-worktree device result. YouTube.js (`youtubei.js`) remains unintegrated because this Android/KMP project has no tested JS host; it is tracked as a blocker rather than represented as a working fallback.
