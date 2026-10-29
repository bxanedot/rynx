# Dependency audit

| Dependency | Pinned revision/version | Use | State |
|---|---|---|---|
| InnerTubeX | v0.11.2 / `c863ddf15be01a1abfa4630329bdc0a1e95f1531` | KMP YouTube Music catalog search and Android-first audio resolution | Public API integrated in Rynx `commonMain`; accepts supported direct HTTPS audio. Catalog parser/resolver tests and Android build pass. Live extraction/device playback unverified. GPL-3.0 source notice retained. |
| NewPipeExtractor | v0.26.5 | Android fallback audio resolution after InnerTubeX | Android-only public API adapter; Android compilation passes; live fallback and device playback unverified. GPL-3.0-or-later notice retained. |
| QuackJS | 0.1.2 / `5af5261d31e50b578e4c81434f6fce8dfd81db93` | InnerTubeX's transitive player transform dependency | JitPack transitive dependency; GPL-3.0 license and upstream notices retained. Full transitive release audit remains open. |
| Apple iTunes/RSS metadata | Public endpoints; no binary SDK | Song search, lookup, storefront charts, genre data and cover URLs | Rynx-owned KMP Ktor parser/client. Song lookup and cover parsing are covered by deterministic tests; live artwork terms remain a separate release review. |
| GlyphStudio `:sfsymbols` | pinned submodule commit | Shared KMP semantic symbol renderer | Owner states implementation is Apache licensed; Apple SF Symbols artwork terms are separate. iOS target has not been built on an Apple toolchain. |
| `youtubei.js` (YouTube.js) | npm `18.3.0` inspected; not added | JavaScript provider candidate | Not a Rynx dependency. No Android/Kotlin runtime or tested host exists, so it is not in the Android provider chain. |
| Kotlin Multiplatform | 2.4.21 | Shared application code | Root build. |
| Gradle | 9.8.2 | Reproducible build | Wrapper pins the official distribution checksum. |
| JDK | 27 | JVM 26 desktop target and build tooling | Workspace tooling; not committed. |

The active Android resolver chain is InnerTubeX → NewPipeExtractor. Provider refusal and cancellation semantics prevent fallback from bypassing explicit access restrictions. Apple search metadata and artwork are separate from playback media; no Apple audio preview is used.

GlyphStudio declares Kotlin 2.4.20 and Compose 1.12.1. Rynx includes its pinned `sfsymbols` module as `:glyphstudioSfsymbols`; optional generated wrappers are excluded while the dynamic catalog API is retained. Do not claim iOS build verification before an Apple toolchain run.

Current desktop runtime uses Compose Multiplatform 1.12.1, InnerTubeX and the Apple metadata module, plus Ktor CIO. The Android app uses Media3 1.11.1, Coil 3.6.3, Ktor OkHttp, SQLDelight 2.4.1 and AndroidX lifecycle/media components. NewPipe's API 33+ desugaring requirement is met for min API 26; its optional JSR-223 Rhino adapter is excluded because Android does not ship JavaBeans/JSR-223/Dynalink.

The complete transitive license/security audit, image-service terms, and signed release identity remain open. eXcor and its eXcor-specific dependencies/notices are no longer part of the active Rynx build; see `docs/archive/EXCOR.md`.
