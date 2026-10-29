# Android audio tests

Media3 ExoPlayer is owned by MediaSessionService. The session implements focus/noisy-output handling and foreground notifications. UI transport uses MediaController. Queue edits/reordering preserve the active item and the actual shuffle permutation through app-owned session commands. Volume uses Android music-stream quantization; output opens system controls.

Passed local checks include queue/order invariants, synchronized/enhanced LRC parsing, seek interaction/cancellation and latest-request ownership. A superseded network resolution cannot commit over a newer selection or clear its loading state. These do not prove audible output.

Instrumentation:

- `PlaybackSessionTest`: generated silent PCM advances, exposes the expected media metadata, and pauses/resumes through the actual service. Passed in the hosted API 36 connected run for commit `a407e1c`.
- `ProviderHeaderResolverTest`: verifies URL-scoped provider headers and immutable input `DataSpec`; passed in the hosted API 36 connected run.
- `PlayerDeviceTest`: actual player controls reach system volume and Media3, LRC tap seeks, shuffle/reorder/clear retain correct identity, rendered frames captured.
- `LocalAudioMetadataTest`: independently generated silent MP3 with original embedded cover/title and fallback metadata.
- `LyricsImportRecreationTest`: recreates the host while the player is active, then imports a real timed-lyrics file and verifies it attaches to that track. It does not verify a result returning from a picker that remained open through recreation.
- `AppearancePersistenceTest` and `LaunchTest`: preference normalization and recreation/metadata behavior.

Runtime status: the hosted API 36 Pixel 6 emulator installed the debug APK and passed `connectedDebugAndroidTest` on commit `a407e1c` ([run 40838202274](https://github.com/bxanedot/rynx/actions/runs/40838202274)). This includes the generated-PCM service playback/pause-resume and Android SQLite playlist close/reopen/order tests. The local software emulator still does not boot; no physical phone is attached. Even generated silent-PCM playback passing does not establish audible signal quality.

Still required: process/OS recovery beyond the tested Activity-stop case, focus interruptions, headset changes, notification interaction, live buffering/recovery, audio device changes, long audible sessions, glitches, physical DSP behavior, battery and first-audio latency. No performance numbers or bit-perfect claim are available.


The playback service now owns qualifying listening signals and recent history. A shared tracker verifies advancing playback, pause exclusion, seek discontinuities and repeat sessions. `BackgroundListeningTest` generates original PCM and observes a service-produced listening signal after stopping the Activity; it is included in the successful hosted API 36 connected run above.


Hosted run 40689777924 progressed past recreation; playback/controller tests then failed to start generated PCM and the lyric test had no current item. Inspection of Media3 1.11.1's public callback API identified that the deprecated onConnect default is an empty-command sentinel. Rynx's override had converted it into an accepted connection with no player commands. The callback now uses onConnectAsync and explicitly grants full default player commands only to same-package/same-UID controllers. Other controllers retain Media3's trust-based defaults. Existing native PCM tests plus explicit command assertions verify the regression on rerun; no playback pass is inferred from compilation.

Hosted phone run 40695300876 confirms generated PCM session playback and service-owned background listening after the Media3 command fix; launch/recreation, metadata and preference checks also proceed. Two accessibility workflows remain failing at compact-player activation. The surface now exposes its expansion action explicitly on its named semantic node; native helpers resolve an actionable ancestor and merged labels, including system DocumentsUI rows. Full native rerun remains required before declaring these workflows verified.

## Rynx DSP slice — 2026-10-28

The existing `PlaybackService` still owns the sole ExoPlayer/MediaSession. `RynxAudioRenderersFactory` adds a Rynx `AudioProcessor` to Media3's sink; common PCM math/settings are in `shared/playback` and the shared sound sheet is connected to the existing Android controls. Settings are stored locally in app-private preferences. No account or network dependency was added for DSP.

The common processor implements 10/31-band and parametric RBJ biquads, low/high shelves and pass filters, automatic EQ headroom, bass/treble, stereo width/balance, low-pass crossfeed, stereo-linked compression with attack/release smoothing, and a 4x cubic-interpolated inter-sample peak guard. The Android bridge accepts PCM 8/16/24/32-bit integer and 32-bit float encodings (including byte-order variants), keeps the Media3 input format/rate, uses preallocated float scratch, and writes processed audio back at that PCM width. Pure Mode and a neutral settings state copy the incoming PCM bytes unchanged at this processor boundary. This is not evidence of end-to-end bit-perfect delivery: Media3 decoders, Android's mixer, the active route and DAC can resample or convert the signal.

Automated evidence for this slice: shared `:shared:playback:jvmTest` passed 43 tests, including flat/bypass transparency, 1 kHz EQ gain, limiter ceiling, layout count, and above-threshold compression with stereo-link balance. Android `:apps:android:testDebugUnitTest` passed 8 tests, including exact PCM16 Pure Mode copying, active PCM16 EQ, active 24-bit integer gain and active 32-bit float gain. `:apps:android:compileDebugKotlin`, `:apps:android:compileDebugAndroidTestKotlin` and `:apps:android:assembleDebug` passed. Debug artifact: `apps/android/build/outputs/apk/debug/android-debug.apk` (22,331,218 bytes). Desktop/headless Compose tests passed 54 cases, including real compressor-control callback wiring. `git diff --check` passed.

No Android device is attached. The available software AVDs have not booted in this workspace and `/dev/kvm` is absent, so no installed-app/audio-route/listening result is claimed. No frame-time, CPU, battery, underrun, long-session, USB DAC, Bluetooth, or acoustic measurement was run.

Known scope limits: ReplayGain tag parsing/normalization, overlapping crossfade, explicit resampling, output channel remapping, dithering controls, AutoEQ, spatial rendering, and exclusive USB output are not implemented. Media3/platform codec support determines which source formats can be decoded; PCM-format support at this processor does not guarantee every named container or 192 kHz end-to-end output. The peak guard is an interpolated approximation, not a certified standards-compliant true-peak limiter. Current output information is best-effort Media3/device-route data, not an active DAC measurement.
