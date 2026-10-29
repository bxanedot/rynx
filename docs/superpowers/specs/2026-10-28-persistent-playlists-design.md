# Persistent playlists design

## Intent

Complete the first real Android Library collection workflow without replacing the existing Rynx app. Users should be able to create and rename playlists, add/remove tracks, reorder entries, reopen a playlist, and keep it after an app restart. Common models and rules must be portable to a later iOS target.

## Current state

`PlayerUiState` exposes local songs, favorites and recent tracks. The Library screen only renders favorites and local songs; it has no playlist model, storage, creation flow, or detail screen. `MainActivity` persists small values through Android `SharedPreferences`/JSON. The shared modules configure Android and JVM targets; iOS is not configured.

## Approaches

1. **Extend `SharedPreferences` JSON.** Fast and low-dependency, but playlist entries become another Activity-owned Android serialization format, with weak transactional/migration support and no reusable database for the iOS port.
2. **SQLDelight KMP database (chosen).** SQLDelight 2.4.1 is Apache-2.0 and generates typed Kotlin APIs for Android, JVM, and native targets. Put schema, queries, repository contract and playlist rules in shared modules; wire an Android SQLite driver now and add the iOS native driver only after Android acceptance. This adds plugin/schema/migration setup but keeps library data and queries reusable.
3. **Android Room database.** Mature Android persistence, but it would keep the initial implementation Android-specific and require reworking the data layer for the user's later iOS port. The current project has no Room dependency.

SQLDelight was selected after inspecting existing dependencies: there is currently no database library. Its official README documents Android/JVM/Native(SQLite) support and Apache-2.0 licensing; the version 2.4.1 release is published 2026-10-06. Dependency/artifact resolution and its compatibility with the repository's Kotlin 2.4.21 toolchain must be proven by the build before it is treated as adopted.

## Goals

- Store user playlists and ordered entries in a versioned SQLite schema, without copying or altering media files.
- Store stable provider/local media references and display metadata only. Never persist signed extraction URLs, request headers, cookies, or tokens.
- Put playlist models, validation, ordering rules, and repository interfaces in `commonMain`.
- Provide an Android driver/repository and connect it to the existing Compose Library.
- Cover create/rename/delete/add/remove/reorder, empty playlists, duplicate entries, persistence after reopening the database, and database errors with tests.
- Preserve existing local tracks, favorites, recent history, player queue, appearance, and branding.

## Non-goals

- Do not add downloads, source-account synchronization, Spotify playlist management, collaborative playlists, or cloud backup in this slice.
- Do not redesign the Library or imitate protected Apple assets. Use existing Rynx components, tokens, icons and material modes.
- Do not add iOS/TV/Wear application targets here. The shared database design should permit later platform drivers without claiming those targets work now.

## Data and behavior

- A playlist has a stable ID, trimmed non-empty title, creation/update times, and explicit ordering.
- A playlist entry has its own stable ID, a stable media identity/source, display metadata, and a contiguous position. Duplicate recordings are permitted as distinct entries, matching normal playlist editing behavior.
- Rename rejects blank titles. Delete cascades to entries. Reorder addresses entry IDs, clamps a target into the valid list, and persists a normalized position sequence in a transaction.
- A local file entry references its already-authorized URI; importing a playlist never copies media. A remote item stores its provider ID and must be resolved again before playback.
- Opening a playlist shows its persisted entries and offers play/add-to-queue through existing callbacks. Unsupported or expired sources stay visible with a clear retry/availability message.

## Acceptance

Common unit tests validate the rules and Android/JVM database integration verifies schema CRUD and reopen persistence. UI tests cover playlist creation, detail entry playback callback, and edit actions. Android instrumentation should verify restart persistence when a working API 36 emulator is available; compilation alone leaves this gate open. All existing shared/provider/UI tests must remain green.

## Remaining to the builder

Confirm the dependency resolves and the SQLDelight Gradle plugin works with the repository's Kotlin/AGP versions before wiring app UI. If not, record the exact artifact/build blocker and do not leave an unbuildable dependency in the project.
