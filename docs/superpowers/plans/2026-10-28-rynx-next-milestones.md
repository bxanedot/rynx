# Rynx next milestones

> Historical plan; provider details were superseded on 2026-10-29 when eXcor was archived by request. Use `docs/parity/PROVIDER_UPDATES.md` for current provider policy.

**Goal:** Continue the existing Rynx app toward a reliable Android-first release while keeping application logic and reusable UI in Kotlin Multiplatform for a later iOS port. Use Apple Music on iOS as the visual and interaction reference. This plan extends the current repository; it does not restart it.

**Current baseline:** `feature/shared-playback-preparation` is one commit ahead of merged `main` (`c38ac0d`) with local playback-preparation commit `e971132`. Navigation and brand PRs #1 and #2 are merged. A post-rebase offline build stopped before compilation because `components-resources:1.12.1` is not in the local Gradle cache. Local API 36 emulator boot previously failed. Playback preparation, provider live behavior, and audible Android playback have not yet been verified on this branch/device. See `docs/IMPLEMENTATION_STATUS.md` for the detailed evidence inventory.

## Rules for every milestone

- Keep shared models, rules, repository contracts, state, and reusable Compose UI in KMP `commonMain` where the platform APIs allow it. Put Android system services in Kotlin `androidMain`; add iOS-specific Kotlin/native adapters only when the iOS target is ready. Do not treat JVM compilation as iOS support.
- Keep Android phones as P0. Preserve existing desktop code, but do not expand other platform products until the Android completion gate passes.
- Use Apple Music's iOS experience as the design reference. Do not claim pixel or motion parity without reference captures and an actual comparison. Preserve the current Rynx layout and branding.
- Keep eXcor primary and the already approved fallback chain. The hourly eXcor refresh workflow remains in place; adopt new pins only when the workflow's checks pass.
- Do not inspect prohibited music-app source. Do not store signed stream URLs, headers, cookies, or tokens in the library or logs.
- Report implemented, unit-tested, integration-tested, device-tested, live-verified, and visually-compared states separately.

## Milestone 1 — Re-establish a trustworthy Android baseline

1. Resolve the current Compose artifact through the configured online Gradle path; do not change dependency versions just to bypass an offline cache miss.
2. Run the full existing Android debug/release/lint/unit-test build and shared/provider JVM suites on the current rebased branch.
3. Run the generated-audio Media3 service and provider-header instrumentation tests on a working API 36 emulator or a real Android device. The earlier software emulator failure means this requires a functioning device/runner.
4. Retry opt-in Apple and YouTube provider checks only when network access permits. Separate connection failures from product failures and keep provider URLs/headers redacted.
5. Update the status and regression docs with exact commands, test counts, warnings, device/API, and results. Stop this milestone on a source/build failure and fix that failure before adding features.

**Exit gate:** Current-branch build and deterministic tests pass; service instrumentation has executed on Android. Live provider and audible physical-device results remain individually labeled if unavailable.

## Milestone 2 — Persistent KMP playlists and Library editing

1. Prove SQLDelight plugin/artifact compatibility with the repository's Kotlin, Gradle, and Android toolchain before adopting it. If incompatible, remove the trial configuration and choose a compatible KMP store rather than leaving the build broken.
2. Test-first implement shared playlist models, validation, stable entry identity, duplicate-entry behavior, ordering, and a repository contract in `commonMain`.
3. Add a versioned persistent store and Android driver. Persist stable provider/local references and display metadata only; never persist resolved streams or credentials.
4. Wire the existing Library UI to create, rename, delete, open, add, remove, and reorder playlists. Reuse the existing Rynx layout/material/icon system.
5. Test persistence after close/reopen, failed writes, duplicate entries, invalid names, and revoked local-file references. Add an Android instrumentation restart test.

**Exit gate:** Shared repository and UI tests pass; Android app compiles; restart persistence test runs successfully on device/emulator.

## Milestone 3 — Complete the core discovery-to-playback journeys

1. Verify Apple catalog metadata/artwork with YouTube Music fallback and strict recording matching before stream resolution.
2. Audit the requested Home and Explore shelves against available public catalog data: Daily 100 (global/region/neighboring countries), Best New Songs, Next This Week, Recent Plays, Trending Songs, City Charts, New on Tour, and Apple Music-derived genres.
3. Implement each supported shelf as real provider-backed or locally ranked content, with loading, empty, offline, and error states. Label unsupported/editorial-only data as blocked or unverified instead of manufacturing results.
4. Finish artist, album, playlist, search, queue, and Library detail flows, including navigation state restoration and accessibility semantics.
5. Add end-to-end tests for search-to-play, album-to-queue, playlist editing-to-restart, and provider failure recovery.

**Exit gate:** Core browse/search/library/playback journeys work end-to-end on Android; all shown controls have real behavior and error states.

## Milestone 4 — Apply and validate the supplied vector icons

1. Inspect the uploaded RAR archives in isolation; inventory formats, source licenses, and whether artwork is already vector.
2. Convert eligible user-provided icons into actual SVG/vector geometry, not raster images wrapped in SVG. Preserve originals and record provenance/license evidence.
3. Add a central semantic icon registry and route shared Compose controls through it. Keep platform-specific artwork licensing separate from the implementation-library license.
4. Compare icon scale, optical alignment, selected states, and animation in the actual Android screens. Keep album artwork and photos raster.

**Exit gate:** Every adopted supplied asset has an identified license/provenance and vector master where eligible; Android UI uses the shared registry without regressions.

## Milestone 5 — Finish Apple Music iOS-inspired visual and motion behavior

1. Capture or obtain authorized Apple Music iOS reference states at matching dimensions; record unavailable states as unverified.
2. Compare Home, Explore, Search, Library, album/artist/playlist pages, mini-player, expanded player, queue, lyrics, menus, and settings.
3. Tune shared layout, typography, colors, glass/solid/AMOLED materials, icon geometry, and motion from evidence. Keep reduced-motion and font-scaling behavior.
4. Add deterministic screenshot and transition tests for representative Android sizes. Validate gestures and interruption behavior on-device, not only screenshots.

**Exit gate:** All Android screens are inventoried; critical visual/motion differences are fixed or documented with evidence; no unsupported 95% fidelity claim.

## Milestone 6 — Authorized integrations and Android release readiness

1. Audit official Spotify APIs and playback terms; configure a registered app/callback and authorized test account before implementation. Implement only capabilities the authorized interfaces support.
2. Complete local library/offline flows, lyrics, playback recovery, notification/headset/audio-focus, accessibility, performance, dependency/artwork notices, and release signing.
3. Run the full functional, instrumentation, screenshot, accessibility, performance, and security gates. Produce a reproducible release-candidate APK and publish exact limitations.
4. Pass the Android completion gate in `docs/IMPLEMENTATION_STATUS.md` before substantial tablet, desktop, TV, Wear, or iOS product work.

## After Android acceptance

Add iOS/iPadOS as a real KMP target, then implement and test native audio/system adapters while reusing common domain/state and shared Compose UI where supported. Expand desktop, TV, and Wear in separate platform-specific milestones with their own device/CI evidence.

## Current immediate task

Start at Milestone 1, step 1: resolve the Gradle artifact online, then continue only after a verified current-branch build. Milestone 2's detailed test-first checklist is in `docs/superpowers/plans/2026-10-28-persistent-playlists.md`.
