# Last.fm and ListenBrainz

Rynx's scrobbling domain policy and both provider API clients are in KMP `commonMain`. Android settings and Media3 service integration use an Android Keystore-backed AES-GCM credential store. There is no iOS host or Keychain adapter in the current repository; iOS can reuse the shared clients and implement the same secure-store contract later.

## Linking accounts

- **Last.fm:** register Rynx as a Last.fm API application and configure `LASTFM_API_KEY` and `LASTFM_SHARED_SECRET` in the build environment, or Gradle properties `rynxLastFmApiKey` and `rynxLastFmSharedSecret`. No credentials are committed. Without both values, the Settings action is disabled. Linking uses Last.fm browser approval and a manual **Finish Last.fm login** step after returning to Rynx; the app never asks for a Last.fm password.
- **ListenBrainz:** open the user's ListenBrainz settings, create a personal API token, paste it into Rynx, and validate it with the documented token-validation endpoint. The token is stored encrypted by Android Keystore.

Successful linking turns scrobbling on for that provider. A later explicit off switch persists across restarts. Disconnecting deletes the stored session/token and turns scrobbling off.

Example debug build invocation:

```sh
LASTFM_API_KEY=... LASTFM_SHARED_SECRET=... ./gradlew :app:android:assembleDebug
```

The Last.fm shared secret is necessarily present in a client application that signs Last.fm requests; configure an application credential intended for the Rynx client. ListenBrainz's per-user token and Last.fm session key are encrypted at rest and are never included in logs.

## Playback behavior

The Android playback service observes the existing Media3 player, including when the Activity is stopped. It submits now-playing when a valid track begins and one scrobble attempt per connected, enabled provider once actual advancing playback reaches half of a known track's duration or four minutes, whichever is shorter. Unknown-duration tracks use four minutes; tracks under 30 seconds are excluded. Pauses and seek jumps do not contribute to the threshold.

Provider errors are shown in Settings and never stop or pause playback. Rynx does not automatically retry failed scrobbles, since the providers do not provide an idempotency key and retrying after an ambiguous network failure can create a duplicate listen. Android tests and KMP protocol tests do not establish a live account result; live verification requires owner-supplied credentials and accounts.
