# Persistent Playlists Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give Android users persistent, editable playlists backed by shared KMP data and the existing Rynx Library UI.

**Architecture:** Add a `shared:library` KMP module with a versioned SQLDelight SQLite schema, common models, repository, and edit rules. Android supplies `AndroidSqliteDriver`; JVM tests use SQLDelight's SQLite JDBC driver. The shared Compose Library renders playlist list/detail and sends edits through typed callbacks. No signed streams or credentials are stored.

**Tech Stack:** Kotlin Multiplatform 2.4.21, Compose Multiplatform 1.12.1, SQLDelight 2.4.1 (Apache-2.0), Coroutines/Flow, Android SQLite, JUnit/Kotlin test.

**Spec:** `docs/superpowers/specs/2026-10-28-persistent-playlists-design.md`

## Global Constraints

- Android phones remain P0; do not expand desktop/TV/Wear/iOS product work before the Android completion gate.
- Shared models, edit rules, and repository code belong in `commonMain`; Android SQLite driver code belongs in `androidMain`.
- Preserve existing local tracks, favorites, recent history, queue, player, appearance settings, and merged Rynx vector assets.
- Store stable local/provider references and display metadata only. Never persist signed URLs, request headers, cookies, or auth tokens.
- Do not copy competitor implementation source, protected Apple artwork, or reference screenshots into the product.
- SQLDelight must resolve/build with the existing Kotlin/AGP toolchain before adoption; otherwise record the precise failure and remove the incomplete dependency/configuration.
- Device acceptance remains open until the current branch runs on a working API 36 emulator or real device.

## Review Focus

- Blank/whitespace names, overlong names, and rename attempts must fail without corrupting existing playlists.
- Reorder operations on empty, one-entry, boundary, and out-of-range positions must produce contiguous stable order.
- Duplicate track identities may exist as separate playlist entries; removing one entry must not remove its sibling.
- Local file references may be revoked; playlist metadata must survive while playback presents a recoverable error.
- A failed database write must not publish stale in-memory playlist state as persisted success.

---

### Task 1: Shared library database and playlist rules

**Files:**
- Modify: `build.gradle.kts`
- Modify: `settings.gradle.kts`
- Create: `shared/library/build.gradle.kts`
- Create: `shared/library/src/commonMain/sqldelight/dev/bxane/rynx/library/db/Playlist.sq`
- Create: `shared/library/src/commonMain/kotlin/dev/bxane/rynx/library/PlaylistModels.kt`
- Create: `shared/library/src/commonMain/kotlin/dev/bxane/rynx/library/PlaylistRepository.kt`
- Create: `shared/library/src/commonTest/kotlin/dev/bxane/rynx/library/PlaylistRepositoryTest.kt`
- Create: `shared/library/src/jvmTest/kotlin/dev/bxane/rynx/library/PlaylistDatabaseTest.kt`

**Interfaces:**
- Consumes: `TrackItem`-equivalent stable IDs and provider/local metadata; SQLDelight `SqlDriver` supplied by the host.
- Produces: `Playlist(id: Long, name: String, entries: List<PlaylistEntry>, createdAtMillis: Long, updatedAtMillis: Long)`, `PlaylistEntry(id: Long, mediaId: String, title: String, artist: String, artworkUrl: String?, source: String, localUri: String?, position: Long)`, and `PlaylistRepository` create/rename/delete/add/remove/move/list APIs backed by `Flow<List<Playlist>>`.

- [x] **Step 1: Write repository tests** for trim/reject blank names, rename, delete, duplicate preservation, entry removal, reorder, contiguous positions, missing parents and persistence after reopen.
- [ ] **Step 2: Verify a red test run** before implementing the repository. The final tests pass, but the expected failure was not captured in this turn.
- [x] **Step 3: Add SQLDelight plugin/module/schema** with separate entry IDs, foreign-key cascade, provider/local stable references and ordered positions. Resolved stream URLs and headers are not stored.
- [x] **Step 4: Implement common repository operations** with transactions and observable Flow mapping; repository state is derived from persisted rows.
- [x] **Step 5: Add a JDBC SQLite persistence test** proving playlists and entry order survive closing/reopening the database.
- [x] **Step 6: Run `:shared:library:jvmTest :shared:library:compileAndroidMain`**; the common rule/database suite passes.

### Task 2: Android library persistence adapter

**Files:**
- Create: `apps/android/src/main/kotlin/dev/bxane/rynx/android/AndroidLibraryDatabase.kt`
- Modify: `apps/android/build.gradle.kts`
- Modify: `apps/android/src/main/kotlin/dev/bxane/rynx/android/MainActivity.kt`
- Test: `apps/android/src/androidTest/kotlin/dev/bxane/rynx/android/PlaylistPersistenceTest.kt`

**Interfaces:**
- Consumes: `PlaylistRepository` and `RynxLibraryDatabase` from Task 1; Android `Context` only for the `AndroidSqliteDriver` factory.
- Produces: one lazily opened, app-owned database/repository, lifecycle-safe `Flow` collection into immutable UI state, and a deterministic database close path.

- [x] **Step 1: Add an instrumentation regression** that creates a playlist, adds two generated local entries, closes/reopens the database, and verifies IDs/order.
- [ ] **Step 2: Run the instrumentation test before the adapter exists**; the red test-first run was not captured.
- [x] **Step 3: Implement the Android driver factory** with application context and an app-owned database; SQLite work runs on `Dispatchers.IO`, media bytes are not copied.
- [x] **Step 4: Wire lifecycle** to one lazy database and collect playlist updates only while the activity is started.
- [x] **Step 5: Compile Android instrumentation and unit tests**; the instrumentation APK was produced, but no emulator/device execution was possible here.

### Task 3: Shared Library playlist workflows

**Files:**
- Modify: `shared/ui/src/commonMain/kotlin/dev/bxane/rynx/ui/PlayerUiState.kt`
- Modify: `shared/ui/src/commonMain/kotlin/dev/bxane/rynx/ui/RynxApp.kt`
- Modify: `apps/android/src/main/kotlin/dev/bxane/rynx/android/MainActivity.kt`
- Modify: `apps/desktop/src/jvmTest/kotlin/dev/bxane/rynx/desktop/AppRenderingTest.kt` only for shared component callbacks
- Test: `apps/desktop/src/jvmTest/kotlin/dev/bxane/rynx/desktop/LibraryPlaylistUiTest.kt`

**Interfaces:**
- Consumes: repository models and callbacks from Tasks 1–2; existing `PlayerActions`, `TrackItem`, player queue/playback APIs, and Rynx material components.
- Produces: common UI state for playlist collection/selection/loading/error; create/rename/delete/add/remove/reorder actions; Library list and playlist-detail surfaces that preserve current geometry/material behavior.

- [x] **Step 1: Add focused UI interaction tests** for playlist create/add, rename, detail, reorder and removal. Accessible empty/error-state coverage remains future work.
- [x] **Step 2: Run the focused UI tests** in the desktop/headless Compose harness.
- [x] **Step 3: Add shared playlist state and typed callbacks**; Android callbacks use the repository and existing player without database logic in Compose.
- [x] **Step 4: Add playlist list/detail surfaces** inside the current Library flow, reusing Rynx rows/material and retaining Favorites/Songs.
- [x] **Step 5: Add create/rename/delete/add/remove/reorder affordances** with name validation feedback and stable entry identity.
- [x] **Step 6: Run the shared UI/JVM harness**; playlist list/detail/name-edit and entry-action regressions pass.

### Task 4: Acceptance and status

**Files:**
- Update: `docs/IMPLEMENTATION_STATUS.md`
- Update: `docs/parity/ANDROID_FEATURE_MATRIX.md`
- Update: `docs/parity/ANDROID_SCREEN_MATRIX.md`
- Update: `docs/parity/ANDROID_AUDIO_TESTS.md`
- Update: `docs/parity/BLOCKERS.md`

**Interfaces:**
- Consumes: Tasks 1–3, current Android CI workflows, and the current SQLite/provider/network/device outcomes.
- Produces: reproducible status separating common tests, Android compilation, instrumentation execution, and physical-device behavior.

- [x] **Step 1: Run deterministic verification** for the shared library/UI/app modules and Android debug/release/instrumentation packaging. The targeted 77-test set passes.
- [ ] **Step 2: Run connected API 36 instrumentation** when a hardware-accelerated runner or real device is available; do not mark restart behavior verified from compilation alone.
- [x] **Step 3: Record test counts, database migration state, warnings, and known failures** in implementation/parity status and regression results.

## Follow-up after this slice

1. Complete user-owned import/export and local folder/library browsing.
2. Add authorized offline/download behavior and integrity/recovery tests.
3. Finish Android provider/device acceptance and Apple Music visual/motion comparison.
4. Implement Spotify only after credentials/callback and current API authorization are confirmed.
5. Pass the Android completion gate before substantive iOS, desktop, TV or Wear product work.
