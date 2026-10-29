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
