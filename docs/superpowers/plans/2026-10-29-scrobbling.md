# Last.fm and ListenBrainz scrobbling

> **For implementation agents:** Follow the TDD sequence in each task. Complete the listed test before its implementation. The Android service must remain the only playback owner.

**Goal:** Add KMP provider clients, account state, playback tracking and settings for Last.fm and ListenBrainz; authenticate with documented flows, enable each provider after successful login, and submit real scrobbles from background playback.

**Architecture:** Put provider contracts, API clients, Last.fm signing, ListenBrainz payloads, eligibility and playback progress policy in `shared/playback` `commonMain`, which already has Android, JVM and iOS targets. Put KeyStore-backed credential persistence, Android browser launch, service polling, and preference adapters in `app/android`. Extend the existing common Settings UI through `PlayerUiState` and `PlayerActions`; no separate player or service.

**Tech stack:** Kotlin Multiplatform, Compose Multiplatform, Coroutines, Ktor 3.6, kotlinx.serialization, Media3, Android Keystore.

## Tasks

1. **Add KMP domain policy and tests.** Add common tests for the 30-second short-track exclusion, 50%-or-four-minute threshold, unknown-duration threshold, pause/seek accounting, track reset, and one submission event. Add the immutable account/track/event models and tracker only after the tests fail as expected.
2. **Add provider protocol clients and tests.** Test RFC-known MD5 vectors for the Last.fm signature primitive, parameter ordering/exclusions, browser-token/session flow, ListenBrainz token validation, now-playing and scrobble JSON/auth headers, HTTP errors, and credential-free errors using Ktor MockEngine. Then implement `LastFmClient`, `ListenBrainzClient`, common DTOs and requests.
3. **Add Android account storage and auth bridge.** Add tests for preference defaults (success links enabled; later toggles persist), encrypted credential write/read/delete and unconfigured Last.fm behavior. Implement the Android Keystore AES-GCM secret store, non-secret account preferences, build-time-only Last.fm API key/secret configuration, browser actions, and explicit Last.fm “Finish login” / ListenBrainz token validation flows.
4. **Add common Settings UI.** Add state/action tests or Compose behavior checks for disconnected, pending, connected, disabled, provider error, and disconnect states. Add the Listening History group to the existing Settings screen; do not change unrelated visual sections.
5. **Wire background playback scrobbling.** Test the service adapter with a fake player/client/account store where practical. Integrate a recorder alongside the existing history recorder in `PlaybackService`, mapping current Media3 metadata and duration, sending now-playing on track/account changes, and making one asynchronous submission attempt when the common tracker becomes eligible. Provider failures must never interrupt playback; no automatic retry.
6. **Verify and document.** Run focused and full shared playback tests, Android unit/compile/lint/APK checks, Android instrumentation if the emulator is available, and workspace audit. Record exact results and note that real Last.fm calls require owner-supplied registered app credentials and real account verification.

## Review focus

- Shared logic stays in `commonMain`; only Android lifecycle, Keystore, preferences, and browser launch are platform-specific.
- Do not ask for, log, or persist plaintext passwords, session keys, or ListenBrainz tokens. Last.fm browser return is an explicit manual completion action.
- Successful login enables the provider once; an explicit user opt-out persists. Disconnect disables and erases credentials.
- Do not block playback on network requests. Do not claim exactly-once network delivery; this implementation makes one attempt per playback attempt and does not retry.
- Protect the existing player, MediaSession, queue, notifications, and discovery behavior from scrobbling failures.
- Keep Apple Music layout, design tokens, or current UI behavior otherwise untouched.

## Constraints

- Never inspect Vyon, BitChord, Convx or Vivi Music implementation source.
- Do not add a second playback system or third-party scrobbling SDK.
- Do not commit Last.fm credentials. Missing credentials disable Last.fm linking with an explanatory state.
- Unknown track duration uses a four-minute scrobble threshold; known tracks shorter than 30 seconds are not scrobbled.
- Provider failures are surfaced in settings and are not retried automatically.

## Verification log

- Pending implementation.
