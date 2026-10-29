# Rynx implementation plan

Audit updated 2026-10-29 against the existing repository and the latest Android branch. This is an incremental KMP product; preserve completed work and keep Android phones as the first release gate. The user-authorized visual reference is Apple Music on iOS. No Vyon build is available, and no exact visual parity claim is made.

## Repository and branch assessment

- Repository: `bxanedot/rynx`; existing application implementation is Kotlin Multiplatform with Compose Multiplatform in `shared/ui`, an Android Media3 host, JVM/headless UI tests, and independently versioned provider modules.
- Current in-progress workspace branch: `feature/scrobbling`, current head `b006a6f`, based on merged application-host move `515ee51`. Verify run [40967462478](https://github.com/bxanedot/rynx/actions/runs/40967462478) passed on production-code commit `8718a0f`. Hosted Android phone run [40967462418](https://github.com/bxanedot/rynx/actions/runs/40967462418) failed because the Android launcher, rather than Rynx, was foreground during navigation. Head `b006a6f` adds test-only foreground recovery; exact-head runs [40968737648](https://github.com/bxanedot/rynx/actions/runs/40968737648) and [40968738046](https://github.com/bxanedot/rynx/actions/runs/40968738046) are pending. The slice adds KMP Last.fm/ListenBrainz clients and policy, Android credential storage, Settings controls, and service-owned submission. Live account behavior remains unverified without owner credentials.
- PR [#5](https://github.com/bxanedot/rynx/pull/5) fixes selected-track fallback classification. Latest head `daaf075` contains test-only foreground recovery. Verify run [40967326940](https://github.com/bxanedot/rynx/actions/runs/40967326940) and API 36 hosted phone run [40967327024](https://github.com/bxanedot/rynx/actions/runs/40967327024) passed on prior test commit `01c0b42`; exact-head runs [40968632064](https://github.com/bxanedot/rynx/actions/runs/40968632064) and [40968631913](https://github.com/bxanedot/rynx/actions/runs/40968631913) are pending. The prior phone suite passed Search navigation after Library; its debug APK is an installable CI artifact (not a production release).
- Merged PRs #1–#4 supply original navigation motion, brand vectors, the former eXcor integration, and Android playback work; eXcor has since been removed from the active build at the user's request.
- A separate local worktree, `feature/shared-playback-preparation`, has uncommitted changes. Treat it as in-progress, do not count its extra files as delivered, and do not merge or discard them without reviewing and testing them against this branch.
- Clean-room boundary and dependency pins are recorded in `AGENTS.md` and `docs/cleanroom/`. Active provider surfaces are InnerTubeX, NewPipeExtractor, Apple's public metadata endpoints, and GlyphStudio under the recorded license conditions. eXcor is archived at the user's request.

## Implementation inventory

| Area | Assessment | Next action |
|---|---|---|
| Shared domain and provider contracts | Working and covered by common/JVM tests | Preserve and add regression coverage with each vertical slice |
| Discovery | Apple public-catalog metadata, Explore genres, charts, regional and neighboring charts, New Music Daily, city/tour collections, recent plays, and original local ranking are implemented | Add missing detail flows and verify failure/offline states on Android; do not represent the local ranker as Apple’s private algorithm |
| Search and playback preparation | Apple public metadata/artwork plus InnerTubeX catalog search and primary KMP resolver; Android NewPipe fallback | Build and test the new catalog/artwork mapping, then run authorized live search-to-audio playback and provider recovery |
| Android playback | Media3 service/session, local files, transport, seek, repeat/shuffle, editable queue, notifications/session hooks, and shared PCM DSP integration exist | Verify audible playback, focus/interruption, routes, underruns, and long sessions on physical hardware |
| Queue restoration | Versioned KMP resume snapshot and Android Media3 service restoration retain eligible local content URIs, position, repeat, and shuffle order while restoring paused | Generated-PCM MediaStore restoration instrumentation passed on hosted API 36 at `187511c`; still verify permission revocation and actual process-death behavior. Remote/provider playback deliberately is not persisted |
| Player and design system | Shared Compose navigation, search transition, player morph, queue, lyrics, glass/solid/AMOLED modes, splash motion, and vector brand are implemented with headless/UI tests | Measure against Apple Music iOS references; complete Android accessibility and device rendering checks |
| Library | Favorites, recent metadata, and SQLDelight playlists with create/rename/add/remove/reorder/delete are implemented and tested | Finish artist/album/library detail, sort/filter, import/export, and device workflows |
| Lyrics | LRC import, synchronized lines, enhanced word timing, follow, and seek behavior are implemented | Verify picker/device lifecycle and document source/translation limitations |
| Downloads | KMP attribution metadata (`Downloaded by Rynx`) and an Android sidecar writer are tested; no product download queue invokes them | Add downloads only for media the selected source permits; wire attribution into completion and test cancellation/retry/integrity before calling downloads complete |
| Spotify | No user-facing integration is implemented | Block on registered client/callback, authorized test account, current API scope and playback eligibility; then implement real supported functionality |
| Scrobbling | KMP clients and policy, Android Settings, Keystore storage, and service submission implemented; Verify passed on `8718a0f`, later hosted phone run failed due to launcher focus; exact-head recovery runs pending | Confirm current Android phone instrumentation; live account acceptance still requires owner credentials/tokens |
| Other platforms | JVM/Linux development renderer exists; iOS, Windows, macOS, TV, and Wear product targets are not release-ready | Keep work limited until the Android phone gate passes, then add native system/audio integration per platform |

No feature is marked complete solely because a contract, screen, or settings control exists. Detailed feature and screen states are maintained in `docs/parity/ANDROID_FEATURE_MATRIX.md` and `docs/parity/ANDROID_SCREEN_MATRIX.md`.

## Delivery sequence

1. **Close Android playback acceptance.** Hosted API 36 queue-restore instrumentation passes at `187511c`. Next verify on a physical phone: local and authorized remote playback, background service, MediaSession controls, focus/headset changes, output routing, and recovery. Exercise InnerTubeX → NewPipe behavior without logging signed URLs, cookies, or headers. The local emulator remains unavailable, and a hosted emulator pass is not physical-audio evidence.
2. **Finish discovery and core catalog journeys.** Implement real artist/album detail and navigation using authorized Apple metadata, keep playable media resolution distinct from catalog artwork, and test loading, empty, unavailable, and offline states.
3. **Complete library workflows.** Test persistence after process restart and finish sorting/filtering/import/export. Build a permitted download pipeline only after the provider’s access rules and available API explicitly support the requested media; invoke `DownloadBrandingMetadata` from the real completion path.
4. **Complete supported Spotify integration.** Resolve official credentials, callbacks, account access, and actual supported playback capabilities before enabling UI. Do not simulate blocked capabilities.
5. **Finish Android release readiness.** Complete visual/accessibility comparisons against authorized Apple Music iOS evidence, test long playback and performance on hardware, resolve artwork/dependency licensing, and produce a release signed with the owner’s key.
6. **Expand platforms after the Android gate.** Add Android tablets, desktop, TV, Wear, and optional iOS/iPadOS with shared KMP domain/UI where appropriate and native playback/media-session adapters on each target.
7. **Add advanced features selectively.** Prioritize high-value, licensed, testable additions such as replay gain, true overlapping crossfade, audio-route diagnostics, lyrics sources, social sync, and recognition only after reliable core journeys.

## Acceptance rules

- Keep application behavior and domain logic in KMP where platform APIs allow; keep Media3, permissions, file pickers, and audio routing in Android source sets.
- Preserve merged branding and working features; do not expand other platforms before the Android phone completion gate.
- Use only approved library public APIs/documentation for integrations. Do not inspect prohibited music-app implementation sources.
- Distinguish compile, unit-test, hosted-device, physical-device, live-provider, and visual-reference evidence.
- Do not claim Apple Music fidelity without platform-specific measurements and screenshot/motion comparisons.
- Record blocked work with its exact prerequisite, then continue on independently actionable work.
