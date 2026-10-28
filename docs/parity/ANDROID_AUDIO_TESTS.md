# Android audio tests

Media3 ExoPlayer is owned by MediaSessionService. The session implements focus/noisy-output handling and foreground notifications. UI transport uses MediaController. Queue edits/reordering preserve the active item and the actual shuffle permutation through app-owned session commands. Volume uses Android music-stream quantization; output opens system controls.

Passed local checks include queue/order invariants, synchronized/enhanced LRC parsing, seek interaction/cancellation and latest-request ownership. A superseded network resolution cannot commit over a newer selection or clear its loading state. These do not prove audible output.

Compiled instrumentation:

- `PlaybackSessionTest`: generated silent PCM advances/pauses through the actual service.
- `PlayerDeviceTest`: actual player controls reach system volume and Media3, LRC tap seeks, shuffle/reorder/clear retain correct identity, rendered frames captured.
- `LocalAudioMetadataTest`: independently generated silent MP3 with original embedded cover/title and fallback metadata.
- `LyricsImportRecreationTest`: real document picker result survives activity recreation and attaches to the selected track.
- `AppearancePersistenceTest` and `LaunchTest`: preference normalization and recreation/metadata behavior.

Runtime status: not yet passed. The local unaccelerated emulator crashed before app installation, so no app test outcome can be inferred. CI uses the existing repository's hardware-accelerated phone runner. Even a silent-PCM pass would not establish audible signal quality.

Still required: background survival, focus interruptions, headset changes, notification interaction, live buffering/recovery, audio device changes, long sessions, glitches, DSP correctness, battery and first-audio latency. No performance numbers or bit-perfect claim are available.
