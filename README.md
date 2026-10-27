# rynx
Author: **bxane**.

Independent Kotlin Multiplatform + Compose Multiplatform music application. Apple Music is the visual reference, per the user's follow-up replacing the unavailable Vyon reference. No prohibited Vyon, BitChord or Convx implementation was consulted.

**Early desktop vertical slice, not the complete requested rewrite.** Present: local WAV/AIFF/AU import, persisted file library, local search, native Java Sound playback code, play/pause/seek/next/previous/repeat, editable queue, adaptive library/player/queue views, light/dark themes and original vector controls. Real audible playback, Apple visual/motion parity and other platform runtimes remain unverified.

```sh
git submodule update --init
python3 tools/audit_workspace.py
./gradlew check :apps:desktop:packageUberJarForCurrentOS
./gradlew :apps:desktop:run
```

Requires a full JDK 21 (including javac), a graphical desktop and a Java Sound-compatible output device for playback. On Linux the development JAR can be run with `java -jar apps/desktop/build/compose/jars/rynx-linux-x64-0.1.0.jar`.

On a virtual/headless Linux display without a GL context, launch with `java -Dskiko.renderApi=SOFTWARE -jar apps/desktop/build/compose/jars/rynx-linux-x64-0.1.0.jar`. Automatic renderer fallback produced a black window on a repeat launch in this environment; explicit software rendering was verified.

The baseline decoder accepts formats supported by Java Sound, converts to 16-bit PCM and limits source files to 64 MiB/decoded data to 128 MiB. It is not a lossless/bit-perfect or streaming engine. File references persist under `~/.rynx/library-v1.txt`; source media is never copied or modified. If the saved index cannot be restored, imports remain session-only and the original index is preserved for recovery. Queue, position and appearance are currently session-only.

VyonEx and GlyphStudio are pinned approved submodules, **not runtime-linked yet**. No YouTube replacement extractor exists. Spotify, Android/TV/Wear, network providers, lyrics, advanced DSP, social/backend and the rest of the master scope are unimplemented. iOS remains a later expansion. Narrow desktop renders do not establish Android support.

See [current results](docs/parity/REGRESSION_RESULTS.md), [reference decisions](docs/parity/APPLE_MUSIC_REFERENCE.md), [feature inventory](docs/parity/FEATURE_MATRIX.md), [blockers](docs/parity/BLOCKERS.md), and [source isolation](docs/cleanroom/SOURCE_ISOLATION.md).
