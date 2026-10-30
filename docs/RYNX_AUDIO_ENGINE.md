# Rynx audio engine: current Android implementation and recovery audit

Status: 2026-10-30. This describes the existing `bxanedot/rynx` project and the proposed Media3 audio-path patch. It is **not** a finished audio quality or device certification report.

## Where the previous work stopped

The owner's Codex session notes report a large **local, uncommitted and unpushed** worktree, including desktop MP3 decoding, desktop DSP, desktop/Android folder scanning, platform/setting documentation, and Android output-switcher work. The last action before the usage limit was **investigating a desktop FLAC Java Sound decoder**; the log does not show that task being completed. Those local files are not recoverable from the current GitHub default branch. Do **not** pretend this branch includes them or recreate the old worktree automatically.

The current GitHub project uses `app/android`, `app/desktop`, `shared/playback` and `shared/ui`, rather than the earlier local worktree's `apps/` paths. Follow `AGENTS.md`: Android phones are first priority, and never copy source from unapproved reference projects.

## Current Android signal path

```text
Local file or permitted remote source
    -> Media3 extractor and platform-supported decoder
    -> DefaultAudioSink PCM processing chain
    -> RynxMedia3AudioProcessor
       -> shared RynxAudioDspRuntime (Float32 processing internally)
       -> optional TPDF dither when quantizing to PCM 8/16/24 integer
    -> Android AudioTrack / OS audio stack
    -> wired / Bluetooth / USB / device output (OS-managed)
```

There is **one ExoPlayer** owned by `PlaybackService`, which also owns the existing MediaSession, queue restore, notifications and background audio. `RynxAudioRenderersFactory` installs the Rynx PCM processor into the *same* audio sink. No second player, downloader, audio service or queue manager is introduced.

### Why the float-output setting changed

Media3 documents that its `DefaultAudioSink.Builder.setEnableFloatOutput(true)` path can route 24/32-bit/high-resolution PCM through an output path where normal custom audio processing is unavailable. The former Rynx factory requested that path, risking bypass of the custom DSP. This patch requests `setEnableFloatOutput(false)` so the supported PCM processor chain is used.

**Fidelity trade-off:** Media3 may convert high-resolution integer/float sources to **16-bit integer PCM** before reaching the Rynx processor. The new diagnostics show what actually reaches that processor; the original decoder format or source codec is not proof of 24-/32-bit DAC output. A future high-resolution DSP-capable output backend requires its own implementation and device tests. Do not advertise bit-perfect or end-to-end hi-res audio on this path.

### Built-in effects already in the default branch

- Shared, original Float32 DSP: 10-band, 31-band, and adjustable parametric biquad EQ; low/high shelves and high-/low-pass filters.
- Bass, treble, preamp and estimated headroom reduction.
- Mid/side stereo width, balance, headphone crossfeed.
- Linked compressor and interpolated sample/intersample peak guard. The latter is **not** a standards-certified true-peak limiter.
- Android settings persistence, Rynx sound panel and Pure Mode.

The new `TPDF output dither` switch is **off by default** and is persisted using the existing Android DSP JSON store. It produces two independent pseudorandom uniform values per integer sample, subtracts them to form triangular noise at about one target least-significant-bit peak, then quantizes and saturates. Dither applies to **processed 8-, 16-, or 24-bit integer PCM**. It does not apply to floating-point or 32-bit integer output. It cannot recover precision lost earlier in a decoder/sink conversion.

Pure Mode still copies the PCM bytes presented to the processor unchanged, including with dither enabled. This bypass is **not evidence of bit-perfect physical output**: Android routing, sample-rate conversion, system effects and Bluetooth codecs remain outside Rynx's control.

### Truthful diagnostics

The existing Sound and EQ panel separately displays:
1. Source/selected media-track information reported by Media3 (which can be absent).
2. **Actual PCM format passed into Rynx DSP**, recorded when the processor is configured (sample rate, bit depth, channels, processing/bypass state).
3. An Android native-rate *hint* and available route names, which are not equivalent to a hardware DAC format.
4. The existing saturation counter, which detects out-of-range processed samples, not all downstream clipping.

When unavailable, data remains unknown instead of inferring physical DAC precision from a source file.

## Files directly involved

| File | Responsibility |
| --- | --- |
| `app/android/src/main/kotlin/dev/bxane/rynx/android/PlaybackService.kt` | Existing Media3 player and MediaSession owner; retained |
| `app/android/src/main/kotlin/dev/bxane/rynx/android/RynxAudioRenderersFactory.kt` | Single Media3 audio sink and processor attachment; float-bypass fix |
| `app/android/src/main/kotlin/dev/bxane/rynx/android/RynxMedia3AudioProcessor.kt` | PCM conversion, dither, live format diagnostics, Pure Mode bypass |
| `shared/playback/src/commonMain/kotlin/dev/bxane/rynx/playback/audio/AudioDsp.kt` | Existing platform-independent DSP; new dither preference |
| `shared/playback/src/commonMain/kotlin/dev/bxane/rynx/playback/audio/AudioSignalInfo.kt` | Separates DSP PCM input metadata from source/output metadata |
| `app/android/src/main/kotlin/dev/bxane/rynx/android/AndroidAudioDspSettingsStore.kt` | Existing persistence; new optional key with default false |
| `app/android/src/main/kotlin/dev/bxane/rynx/android/MainActivity.kt` | Reads the current PCM diagnostics |
| `shared/ui/src/commonMain/kotlin/dev/bxane/rynx/ui/AudioDspSheet.kt` | Functional dither toggle and reported PCM format |

## Android playback reliability work in this branch

- **Live effect changes:** the PCM processor crossfades the actual previous filter output into the newly configured output for a short transition. Returning to flat sound also blends rather than jumping to dry PCM. All working buffers are retained across blocks; no scratch arrays are constructed in `queueInput`. Pure Mode explicitly bypasses processing.
- **Parametric EQ editing:** the previous normalized-settings method padded every parametric filter list to 12 bands; the Add Filter button could never add a filter. Parametric lists now permit one to twelve filters, while 10-/31-band layouts keep their fixed counts.
- **Output routing:** the Audio Output action uses Android's native `MediaRouter2.showSystemOutputSwitcher()` on API 34+ and falls back to Android volume/sound settings when the switcher is unavailable. This deliberately leaves actual routing with Android rather than inventing an independent device engine.
- **Skip long silences:** an off-by-default toggle uses ExoPlayer's built-in, real silence-skipping AudioProcessor, not a simulated DSP. It persists via the same settings store and automatically yields to Pure Mode. The Media3 default thresholds are currently used; no custom quiet-music protection or threshold UI is claimed.
- **Headless UI test reliability:** navigation semantics lookups retry with the virtual Compose frame clock to avoid transient asynchronous availability under software rendering.

## Verification checklist

**Tests added; execution status must come from actual CI or device results, not from source inspection alone.**

- `:app:android:testDebugUnitTest`: PCM dither noise is bounded at digital silence, a noninert switch, and Pure Mode bypass remains byte-for-byte.
- `:app:android:connectedDebugAndroidTest`: DSP settings survive persistence store recreation.
- `:shared:playback:jvmTest`: existing equalizer response, compressor, and Pure Mode DSP tests.
- `:app:android:lintDebug` and `:app:android:assembleDebug`.
- Hardware-needed: inspect actual PCM and output route on multiple Android devices; listen for clicks/distortion; connect/disconnect wired, Bluetooth and USB DAC outputs; confirm background and MediaSession controls; measure underruns, DSP CPU and battery.

### Not implemented or not verified

This branch does not implement ReplayGain/LUFS analysis, automatic track/album normalization, sample-rate-matched exclusive USB, certified true-peak limiting, surround/Atmos decoding, adaptive resampling, crossfade/Automix, advanced spatial audio, or desktop high-resolution playback. No 192-kHz compatibility claim is made. Format decoding depends on Media3/platform support; ALAC and AIFF are not universally supported. Bit-perfect delivery and the Android mixer/device output format remain unverified.

The highest-priority next audio milestones are: **(1)** demonstrate Android processing with the installed sink on device, **(2)** measure PCM and output path/underruns, **(3)** implement safe transition smoothing, **(4)** add real ReplayGain track/album metadata ingestion, **(5)** design a true high-precision platform output path only after comparisons and tests.
