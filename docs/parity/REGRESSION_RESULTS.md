# Regression results

2026-10-27, Linux x86_64, OpenJDK Temurin 27.0.1, Gradle 9.8.2, Kotlin 2.4.21, Compose 1.12.1.

## Checks
Executed with the pinned Gradle 9.8.2 distribution and the environment's proxy/CA flags:
`:apps:desktop:jvmTest :providers:vyonex:jvmTest :shared:playback:jvmTest`
and `:apps:desktop:packageUberJarForCurrentOS`.

- Shared queue: 9 tests pass (end/skip/repeat, duplicate media identities, edit invariants, invalid input and snapshot independence).
- PCM decoder: 3 tests pass (real generated WAV decode/duration, memory budget, invalid input).
- Local library persistence: 2 tests pass (Unicode paths/deduplication/missing media, replacement, corrupt index retained).
- Compose render/callback tests: 4 tests pass (accessible import, transport/queue callbacks, desktop/narrow library/player/queue render, YouTube Music query submission/pagination and provider-row semantics).
- Desktop session: 3 integration tests pass (persisted-library restore/unreadable media; corrupt-index retention after import; automatic-advance failure/recovery exercised twice using a deterministic test output driver). The driver does not validate audible output.
- Desktop catalog: 2 tests pass (query trimming, pagination/deduplication, safe provider-error text).
- VyonEx adapter: 5 tests pass (search type mapping, continuation mapping, absent metadata handling, stream URL/header redaction and refusal error classification).
- Total: 28 tests, zero failures/errors/skips. Linux uber-JAR packaging passes at `apps/desktop/build/compose/jars/rynx-linux-x64-0.1.0.jar`.
- Workspace source boundary audit passes. Both approved dependency tracked checkouts unchanged.

Headless frames: `apps/desktop/build/reports/screenshots/`. Own rendered captures are copied under `captures/` for review. These are smoke/regression captures, not Apple comparison goldens.
An earlier Java 21 baseline launch under Xvfb rendered with explicit `-Dskiko.renderApi=SOFTWARE`; Skiko's default GL path failed in that headless environment. That launch predates VyonEx linking and the JVM 26 target. The current packaged JAR was build-verified but not relaunched, so the retained screenshot is not evidence for the current binary. Xvfb was downloaded/extracted outside the repository; no system package mutation was required.

## Explicitly not verified
No current Java 26+ desktop launch, live YouTube Music search, audio extraction, stream playback, download/offline, audible output device test, background media integration, long playback, gapless/crossfade, native platform runtime beyond Linux, Spotify, advanced DSP, backend, Apple screenshot similarity score, glass, gesture/motion trajectories, reduced-motion equivalence or performance benchmarks. No public CI run has occurred; the workflow is configured only.

Build emits a Gradle `Configuration.setVisible` deprecation warning, scheduled for Gradle 11 removal (from Compose/Gradle build integration). No public signed production release or Android TV/Wear artifacts have been produced.

## Independent review and packaging limit
Review reproduced two bugs: unhandled automatic-advance failures terminated monitoring, and import after failed restore overwrote the original index. Both regressions failed before the fixes and now pass. Follow-up review found both resolved. DIY editor status was corrected to unverified.

`packageDeb` was attempted and failed because `fakeroot` is absent; jpackage explicitly reported this prerequisite. No Debian installer is available. The development JAR remains the runnable artifact.

## Android-first continuation
Android is P0; desktop UI development is paused. On 2026-10-28, `:apps:android:assembleDebug :apps:android:assembleDebugAndroidTest :apps:android:lintDebug :apps:android:testDebugUnitTest :apps:desktop:jvmTest :providers:vyonex:jvmTest :providers:newpipe:compileAndroidMain :shared:playback:jvmTest` succeeded. The generated test reports contain 19 Compose/JVM tests, 5 Android unit tests, 5 VyonEx adapter tests and 11 shared playback tests (40 total, zero failures). Lint has zero errors and two warnings (`OldTargetApi`, `ObsoleteSdkInt`). The connected instrumentation APK builds, but instrumentation is not run because this workspace has no attached Android device and no `/dev/kvm`. See ANDROID_VISUAL_RESULTS.md for artifact hash and visual limits. These checks verify compilation and deterministic tests; they do not establish Android reference parity, audible playback or release readiness.


## Universal-material continuation

2026-10-28: complete Android debug/release/test packaging, lint and 82 local cases passed. New shared design/UI tests cover real backdrop input, three material policies, player continuity, navigation ownership, lyrics, queue and cancellation. See ANDROID_VISUAL_RESULTS.md for the current scope, hashes and device limitations. Earlier counts above are historical. No production/parity claim follows from this pass.

## Navigation-motion continuation

2026-10-28: debug app and Android instrumentation APK builds, Android unit tests, lint, the Compose/JVM UI harness and shared UI/design suites succeeded. There were 78 applicable test cases with zero failures, errors or skips (15 design, 7 shared UI, 49 app harness, 7 Android unit). The design suite was explicitly rerun after the full command reported it up-to-date. Lint has zero errors and the two existing warnings noted above. Native instrumentation compiled but did not run locally because there is no connected device or `/dev/kvm`; see `ANDROID_VISUAL_RESULTS.md` for the hosted-check requirement and fidelity limits.

Hosted phone run 20 (`ff2b8ae`) built APKs but failed in connected instrumentation: the lyrics picker recreation test blocked in `ActivityScenario.recreate()` while its host was stopped, then the navigation Search test ran without a resumed host. The lifecycle test now requests recreation from the retained activity and checks the replacement instance after returning from the picker. Local app/test APK build passes; hosted rerun is pending. Details and limits are in `ANDROID_VISUAL_RESULTS.md`.
