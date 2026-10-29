# Platform matrix
| Target | Build | Runtime | Visual/motion | Native requirements |
|---|---|---|---|---|
| Android phone P0 | Development APK and test APK built; lint zero errors | Connected tests blocked: emulator offline; no actual-device verification | Provisional UI; Vyon comparison absent | Media3 session/service and document picker implemented; background/audio validation pending |
| Android TV | Not configured | Not tested | unverified | D-pad, focus, TV layouts/onboarding |
| Wear OS | Not configured | Not tested | unverified | Phone sync, reconnection, supported playback |
| Windows | Desktop sources configured, not built on Windows | Not tested | unverified | Native shell/JVM package verification pending |
| macOS | Desktop sources configured, not built on macOS | Not tested | unverified | Native shell/JVM package verification pending |
| Linux | Desktop uber-JAR built with JDK 27; provider runtime linked | Current Java 26+ package not launched; Excor live search and audible playback unverified | Partial adaptive views, no fidelity comparison | Java Sound baseline; system media integration pending |
| iOS/iPadOS | Shared design, playback, library and UI Kotlin/Native targets declared; no iOS host app, Apple build or runtime test | Not tested | iOS Apple Music is the design direction; runtime parity unverified | After the Android gate, add the host app and verify Apple-only renderers/native audio and system adapters on macOS |

The foundation's commonMain is platform-neutral Kotlin. A JVM test target is not a delivered desktop app or proof of other platform support.


Rynx UI and domain logic are authored in Kotlin KMP. Shared Compose components and GlyphStudio symbol rendering are in `commonMain`; Android platform services and back behavior are Kotlin platform adapters. Shared iOS target declarations now exist to support later porting, but an iOS host app and Apple-toolchain build remain future work. The renderer is not runtime-verified on Apple targets.
