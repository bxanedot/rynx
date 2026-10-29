# Last.fm and ListenBrainz Scrobbling

## Intent

Add real account linking and track scrobbling to Rynx's existing Android player, with playback rules, metadata mapping, API clients, settings state and user-visible controls shared in Kotlin Multiplatform. A successful account connection enables that service by default; users can turn it off or disconnect later.

## Goals

- Support Last.fm browser authorization using `auth.getToken` → user approval in a browser → `auth.getSession`, without collecting a Last.fm password.
- Support ListenBrainz's documented user-token flow: open its settings page, accept a pasted token, validate it, then store it securely.
- Send now-playing and completed-listen events from the foreground playback service, not from the Activity, so screen-off playback remains covered.
- Scrobble once a track has played for at least half its known duration or four minutes, whichever is shorter. For unknown-duration tracks, use four minutes. Ignore known tracks shorter than 30 seconds; count only actual advancing playback, not seeks or pauses.
- Make one submission attempt per provider and playback attempt. Surface failures, but do not automatically retry: neither provider offers an idempotency key, so blind retries risk duplicate listens.
- Keep API models, signature construction, track eligibility, service clients and preference rules in KMP `commonMain`; isolate Android browser launch and encrypted storage in `androidMain`.
- Keep playback independent of service failures and never include credentials or signed request data in logs.

## Non-goals

- Importing or exporting a user's existing listening history.
- Syncing login sessions between devices, scrobble analytics, social feeds or a backend service.
- Implementing an iOS host in this change. Shared code must retain the existing iOS targets; iOS Keychain and browser integration remain follow-up work.
- Claiming Last.fm login is production-ready until Rynx has its own approved API key and shared secret.

## Approaches considered

1. **Android-only service and settings.** Fastest initial UI, but duplicates protocol and scrobble rules when iOS is added. Rejected because bxane explicitly wants the application code in KMP.
2. **Use third-party scrobble SDKs.** Could save protocol work, but introduces another dependency/license and hides auth/signature behavior. Rejected because Ktor and the provider contracts already exist in Rynx and the APIs are small.
3. **Implement provider clients and the scrobble state machine in KMP, with narrow native adapters.** Chosen: common code owns request/signature/domain behavior; Android owns KeyStore encryption, browser launch and service lifecycle. iOS can provide Keychain/browser adapters later without rewriting the rules.

## Authentication and service constraints

- Last.fm requires an API key and shared secret issued for the Rynx application. The mobile app must sign authenticated calls with that shared secret. Read these from Gradle properties or environment variables; never commit them. If they are absent, the Last.fm connection action is disabled with a clear setup message.
- ListenBrainz's current public API documents personal user tokens in the `Authorization: Token …` header, not an app OAuth client flow. Rynx will open `listenbrainz.org/settings/`, let the user paste the token, validate it, and store it encrypted.
- Last.fm's browser approval is completed with an explicit “Finish login” action after the user returns to Rynx; the app does not need a custom URI callback.
- A successful new login writes `enabled=true` for that provider. Existing explicit off preferences remain off across process restarts. Disconnect removes the token/session and disables scrobbling.
- Provider network errors are isolated from playback. Credentials and Last.fm signatures never appear in logs or plaintext preferences.

## Acceptance

Unit tests cover auth signatures, API request parameters, invalid/empty credentials, token validation, auto-enable state, pause/seek handling, duration thresholds, one-attempt behavior and multiple providers. Android verification covers encrypted credential persistence, browser handoff/login completion, Settings controls, and service-owned scrobbling while the Activity is stopped. Live account tests require owner-provided API credentials and test accounts; they must be opt-in and must not fabricate a passed live result.
