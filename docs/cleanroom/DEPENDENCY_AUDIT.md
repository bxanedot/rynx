# Dependency audit
| Dependency | Pinned revision/version | Use | State |
|---|---|---|---|
| Excor | deb51bafc219645133f3f9145a68719cade7165e / 1.0.0 | Primary YouTube Music search and stream resolution | Composite-linked JVM/Android adapter compiles; opt-in live probe was blocked by the environment proxy (`CONNECT 403`), so live provider/playback remains unverified |
| NewPipeExtractor | v0.26.5 | Android-only last audio-resolution fallback after Excor and InnerTubeX | JitPack dependency, public API integration compiles; GPL-3.0-or-later release obligation documented |
| InnerTubeX | v0.11.2 / c863ddf15be01a1abfa4630329bdc0a1e95f1531 | KMP audio-resolution fallback after Excor and before NewPipe | Pinned JitPack dependency resolved; KMP/JVM and Android debug builds pass. Live extraction/device playback remain unverified. GPL-3.0 source license retained |
| QuackJS | 0.1.2 / 5af5261d31e50b578e4c81434f6fce8dfd81db93 | InnerTubeX's transitive Kotlin player-transform dependency | JitPack transitive dependency; GPL-3.0 license and upstream third-party notice retained. Full dependency/artifact attribution audit remains open |
| GlyphStudio | v1.0.0 / pinned submodule commit | sfsymbols | Apache implementation per owner; Apple SF artwork terms separately restrict use; not linked into Android runtime, which uses original Rynx vectors |
| Kotlin Multiplatform | 2.4.21 | Shared application code | Root build |
| Gradle | 9.8.2 | Reproducible build | Wrapper retained from approved engine |
| JDK | 27 | JVM 26 provider and desktop target | Required by Excor; tests and Linux uber-JAR package pass |

Excor upstream declares Kotlin 2.4.21, JDK toolchain 27/JVM target 26, AGP 9.4.1, Android API 37/min 26; Ktor 3.6.0, coroutines 1.11.0, serialization 1.12.0, QuickJS 1.0.15. Its JVM artifact and documented search APIs compile through the Rynx adapter; Android integration and all live extraction/download features remain unverified.
GlyphStudio declares Kotlin 2.4.20, Compose 1.12.1, AGP 9.2.1, Android API 37. Do not silently rewrite upstream builds or downgrade dependencies.
Full transitive license/security audit and dependency locking remain open, including for newly linked InnertubeX/QuackJS artifacts.

Current desktop runtime links Compose Multiplatform 1.12.1 (including Skiko 0.150.1 via Compose Desktop), coroutines 1.11.0, the Excor 1.0.0 composite artifact, and OkHttp 5.5.0, in addition to Kotlin stdlib/test and Excor transitives. The full OpenJDK Temurin 27.0.1 is external tooling, not committed. Source dependency pins remain unchanged. `./gradlew :apps:desktop:dependencies --configuration jvmRuntimeClasspath` lists the desktop runtime graph. Full redistribution notices/transitive audit remain pending.

Android-first additions: AGP 9.4.1, Android compile API 37 / target 36 / min 26, Media3 ExoPlayer/datasource/session 1.11.1, Activity Compose 1.13.0, Lifecycle runtime Compose 2.11.0, Core KTX 1.19.1, AndroidX Test runner/core 1.7.0 and ext JUnit 1.3.0. Shared UI uses Coil 3.6.3 with Ktor 3.6.0 engines (OkHttp on Android, CIO on desktop). NewPipe's documented API 33+ desugaring requirement is met with `desugar_jdk_libs_nio` for min API 26; its optional JSR-223 Rhino adapter is excluded because Android lacks JavaBeans/JSR-223/Dynalink. SDK/emulator tools are external workspace tooling. Shared JVM bytecode stays 21; Android shared targets use JVM 17. Excor, InnerTubeX and NewPipe provider modules compile; live extraction/playback remain unverified.

Apple metadata/discovery continuation uses Excor's approved public AppleMusicSong/ExcorAppleMusic APIs through typed adapters. Public Apple chart/genre/page data use Ktor 3.6.0, kotlinx.serialization-json 1.12.0 and coroutines 1.11.0, already present in the runtime graph. Android transport is OkHttp. No Apple proprietary binary, web token, preview media or original music-app source is embedded. Provider data/artwork terms are documented separately in APPLE_CATALOG_EVIDENCE.md.

Android media/library continuation adds SQLDelight 2.4.1 and its Android SQLite driver for playlist persistence. The DSP implementation uses the existing Media3 1.11.1 audio processor hook and original Kotlin DSP code; no separate audio engine dependency was added. The Android settings store uses platform SharedPreferences. The download attribution sidecar uses only the JDK file APIs and shared Kotlin JSON encoding; it does not alter audio/video bytes and is not yet wired to an actual download pipeline.

Playlist backup uses the already-approved `kotlinx-serialization-json` 1.12.0 runtime in `shared/library` for a bounded, versioned KMP JSON archive. It adds no platform or provider dependency; the runtime uses Apache-2.0 and was already present through the Apple metadata module.
