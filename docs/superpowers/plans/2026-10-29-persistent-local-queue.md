# Persist and restore local playback queues

## Goal

Keep a resumable, paused playback queue across Android process/service recreation when every queued item is a durable user-granted local `content://` URI. Never persist temporary provider stream URLs or remote provider queues.

## Design

- Add a platform-neutral, versioned playback resume value and codec in `shared/playback` (`commonMain`), with strict validation for queue length, position, shuffle permutation, and item text.
- Add an Android preferences adapter that stores only validated snapshots. Queue items contain stable media IDs and display metadata, not resolved stream URLs or request headers.
- Restore queue, current item, position, repeat mode, and shuffle order into the existing Media3 player paused before exposing its session.
- Persist changes from Media3 events and a modest periodic progress checkpoint; clear the saved snapshot whenever playback contains a non-persistable item or the queue is cleared.
- Test codec and URI eligibility in common tests; test Android preference round-trip and restore behavior through existing Android tests where the device runner is available.

## Tasks

1. [x] Add common tests first for round-trip, malformed/unsupported data, validation bounds, and local-only eligibility.
2. [x] Implement the common snapshot and codec; run focused shared playback tests.
3. [x] Implement the Android storage adapter; exercise it through the service restoration instrumentation.
4. [x] Integrate save/restore with the current `PlaybackService` Media3 player without changing playback ownership.
5. [x] Run shared tests, Android unit tests, Android instrumentation compilation, debug and unsigned release APK builds, lint; the local software AVD remained offline and did not run tests.
6. [x] Update feature/parity/implementation-plan status and record hardware/runtime limits.

## Verification record

- Red/green: focused common test compile first failed because the snapshot API did not exist; after implementation, focused tests passed.
- Full local checks: 50 shared playback tests, 9 Android JVM tests, `lintDebug`, `assembleDebug`, `assembleRelease`, and `assembleDebugAndroidTest` passed. Instrumentation was compiled, not executed locally.
- Debug APK SHA-256: `1ea5fdb4d631bec0f4f36cc679875770010fef85969d653f961aa327fe529ade`.
- Unsigned release APK SHA-256: `9e3b4f89443a84bcbbaa3dd833f98a122fa66bece2ecdbe09c982726b27de77a`.
- Android test APK SHA-256: `f19d20cee5bade9432d8df5d1fcbefdc30b80f414ae94fd7aecd66c21be8f84b`.
- Local API 35 software AVD with acceleration disabled remained `offline`; no device runtime result is claimed. Hosted API 36 instrumentation is pending on the pushed revision.

## Acceptance

- Restored audio is paused and never starts unexpectedly.
- The resumed queue preserves current item, position, repeat mode, and shuffle traversal order.
- Provider stream URLs, authorization headers, and remote IDs never enter the persisted snapshot.
- Invalid or stale snapshots fail closed and do not prevent service startup.
- Existing queue, MediaSession, and transport behavior remains on the same Media3 player.

