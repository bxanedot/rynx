# Regression results

2026-10-27, Linux x86_64, downloaded full Temurin JDK 21, Gradle 9.8.2, Kotlin 2.4.21, Compose 1.12.1.

## Checks
`./gradlew check :apps:desktop:packageUberJarForCurrentOS --console=plain` (environment used an equivalent local Gradle distribution with proxy flags and the system CA trust store).

- Shared queue: 9 tests pass (end/skip/repeat, duplicate media identities, edit invariants, invalid input and snapshot independence).
- PCM decoder: 3 tests pass (real generated WAV decode/duration, memory budget, invalid input).
- Local library persistence: 2 tests pass (Unicode paths/deduplication/missing media, replacement, corrupt index retained).
- Compose render/callback tests: 2 tests pass (accessible import, transport/queue callbacks, desktop/narrow library/player/queue render).
- Desktop session: 3 integration tests pass (persisted-library restore/unreadable media; corrupt-index retention after import; automatic-advance failure/recovery exercised twice using a deterministic test output driver). The driver does not validate audible output.
- Total: 19 tests, zero failures/errors/skips. Full `check` and Linux JAR packaging pass.
- Workspace source boundary audit passes. Both approved dependency tracked checkouts unchanged.

Headless frames: `apps/desktop/build/reports/screenshots/`. Own rendered captures are copied under `captures/` for review. These are smoke/regression captures, not Apple comparison goldens.
Linux graphical runtime launched under Xvfb and captured in `captures/linux-desktop.png`. Skiko reported GL-context failure. Initial automatic fallback rendered, but a repeat default launch produced a black window. Explicit `-Dskiko.renderApi=SOFTWARE` was then launch-tested successfully; the retained capture shows that final packaged build. This does not verify hardware acceleration or performance. Xvfb was downloaded/extracted outside the repository; no system package mutation was required.

## Explicitly not verified
No audible output device test, background media integration, long playback, gapless/crossfade, native platform runtime beyond Linux, provider streaming, download/offline, Spotify, advanced DSP, backend, Apple screenshot similarity score, glass, gesture/motion trajectories, reduced-motion equivalence or performance benchmarks. No public CI run has occurred; the workflow is configured only.

Build emits a Gradle `Configuration.setVisible` deprecation warning, scheduled for Gradle 11 removal (from Compose/Gradle build integration). No public release or installable Android/TV/Wear artifacts produced.

## Independent review and packaging limit
Review reproduced two bugs: unhandled automatic-advance failures terminated monitoring, and import after failed restore overwrote the original index. Both regressions failed before the fixes and now pass. Follow-up review found both resolved. DIY editor status was corrected to unverified.

`packageDeb` was attempted and failed because `fakeroot` is absent; jpackage explicitly reported this prerequisite. No Debian installer is available. The development JAR remains the runnable artifact.
