# Dependency audit
| Dependency | Pinned revision/version | Use | State |
|---|---|---|---|
| VyonEx | 4e8c8c5f683d75d83dabe4a9ca0c6ab18d8f92e1 / 0.2.0 | Primary YouTube Music search and stream resolution | Composite-linked JVM/Android adapter compiles; live provider/playback unverified |
| NewPipeExtractor | v0.26.5 | Android-only audio resolution fallback after VyonEx | JitPack dependency, public API integration compiles; GPL-3.0-or-later release obligation documented |
| GlyphStudio | v1.0.0 / pinned submodule commit | sfsymbols | Apache implementation per owner; Apple SF artwork terms separately restrict use; not linked into Android runtime, which uses original Rynx vectors |
| Kotlin Multiplatform | 2.4.21 | Shared application code | Root build |
| Gradle | 9.8.2 | Reproducible build | Wrapper retained from approved engine |
| JDK | 27 | JVM 26 provider and desktop target | Required by VyonEx; tests and Linux uber-JAR package pass |

VyonEx upstream declares Kotlin 2.4.21, JDK toolchain 27/JVM target 26, AGP 9.4.1, Android API 37/min 26; Ktor 3.6.0, coroutines 1.11.0, serialization 1.12.0, QuickJS 1.0.15. Its JVM artifact and documented search APIs compile through the Rynx adapter; Android integration and all live extraction/download features remain unverified.
GlyphStudio declares Kotlin 2.4.20, Compose 1.12.1, AGP 9.2.1, Android API 37. Do not silently rewrite upstream builds or downgrade dependencies.
Full transitive license/security audit and dependency locking remain open until runtime modules are linked.

Current desktop runtime links Compose Multiplatform 1.12.1 (including Skiko 0.150.1 via Compose Desktop), coroutines 1.11.0, the VyonEx 0.2.0 composite artifact, and OkHttp 5.5.0, in addition to Kotlin stdlib/test and VyonEx transitives. The full OpenJDK Temurin 27.0.1 is external tooling, not committed. Source dependency pins remain unchanged. `./gradlew :apps:desktop:dependencies --configuration jvmRuntimeClasspath` lists the desktop runtime graph. Full redistribution notices/transitive audit remain pending.

Android-first additions: AGP 9.4.1, Android compile API 37 / target 36 / min 26, Media3 ExoPlayer/datasource/session 1.11.1, Activity Compose 1.13.0, Lifecycle runtime Compose 2.11.0, Core KTX 1.19.1, AndroidX Test runner/core 1.7.0 and ext JUnit 1.3.0. Shared UI uses Coil 3.6.3 with Ktor 3.6.0 engines (OkHttp on Android, CIO on desktop). NewPipe's documented API 33+ desugaring requirement is met with `desugar_jdk_libs_nio` for min API 26; its optional JSR-223 Rhino adapter is excluded because Android lacks JavaBeans/JSR-223/Dynalink. SDK/emulator tools are external workspace tooling. Shared JVM bytecode stays 21; Android shared targets use JVM 17. The Android VyonEx and NewPipe provider modules compile, but live extraction/playback remain unverified.
