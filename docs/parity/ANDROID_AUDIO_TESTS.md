# Android audio tests

Media3 ExoPlayer is owned by MediaSessionService. The session implements focus/noisy-output handling and foreground notifications. UI transport uses MediaController. Queue edits/reordering preserve the active item and the actual shuffle permutation through app-owned session commands. Volume uses Android music-stream quantization; output opens system controls.

Passed local checks include queue/order invariants, synchronized/enhanced LRC parsing, seek interaction/cancellation and latest-request ownership. A superseded network resolution cannot commit over a newer selection or clear its loading state. These do not prove audible output.

Compiled instrumentation:

- `PlaybackSessionTest`: generated silent PCM advances/pauses through the actual service.
- `PlayerDeviceTest`: actual player controls reach system volume and Media3, LRC tap seeks, shuffle/reorder/clear retain correct identity, rendered frames captured.
- `LocalAudioMetadataTest`: independently generated silent MP3 with original embedded cover/title and fallback metadata.
- `LyricsImportRecreationTest`: recreates the host while the player is active, then imports a real timed-lyrics file and verifies it attaches to that track. It does not verify a result returning from a picker that remained open through recreation.
- `AppearancePersistenceTest` and `LaunchTest`: preference normalization and recreation/metadata behavior.

Runtime status: not yet passed. The local unaccelerated emulator crashed before app installation, so no app test outcome can be inferred. CI uses the existing repository's hardware-accelerated phone runner. Even a silent-PCM pass would not establish audible signal quality.

Still required: background survival, focus interruptions, headset changes, notification interaction, live buffering/recovery, audio device changes, long sessions, glitches, DSP correctness, battery and first-audio latency. No performance numbers or bit-perfect claim are available.


The playback service now owns qualifying listening signals and recent history. A shared tracker verifies advancing playback, pause exclusion, seek discontinuities and repeat sessions. The new BackgroundListeningTest generates 45 seconds of original PCM and waits for a real service-produced signal after stopping the Activity; it compiles but is not yet a runtime pass. The hosted launch/recreation test reproduced TLS teardown on the UI thread; cleanup moved to a process-owned IO scope, with its native rerun pending.


Hosted run 40689777924 progressed past recreation; playback/controller tests then failed to start generated PCM and the lyric test had no current item. Inspection of Media3 1.11.1's public callback API identified that the deprecated onConnect default is an empty-command sentinel. Rynx's override had converted it into an accepted connection with no player commands. The callback now uses onConnectAsync and explicitly grants full default player commands only to same-package/same-UID controllers. Other controllers retain Media3's trust-based defaults. Existing native PCM tests plus explicit command assertions verify the regression on rerun; no playback pass is inferred from compilation.

Hosted phone run 40695300876 confirms generated PCM session playback and service-owned background listening after the Media3 command fix; launch/recreation, metadata and preference checks also proceed. Two accessibility workflows remain failing at compact-player activation. The surface now exposes its expansion action explicitly on its named semantic node; native helpers resolve an actionable ancestor and merged labels, including system DocumentsUI rows. Full native rerun remains required before declaring these workflows verified.
