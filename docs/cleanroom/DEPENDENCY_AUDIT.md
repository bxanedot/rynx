# Dependency audit
| Dependency | Pinned revision/version | Use | State |
|---|---|---|---|
| VyonEx | 239a8d056c90b2101bf04de27a639d591acc69a3 | Exclusive YouTube extraction | Submodule pinned; runtime integration pending |
| GlyphStudio | 7db5de2e81c6687f9e3b483eeaac43a145e9704c | sfsymbols | Submodule pinned; license gate; runtime integration pending |
| Kotlin Multiplatform | 2.4.21 | Shared application code | Root build |
| Gradle | 9.8.2 | Reproducible build | Wrapper retained from approved engine |
| JDK | 21 | Independent shared foundation | Installed environment |

VyonEx upstream declares Kotlin 2.4.21, JDK toolchain 27/JVM target 26, AGP 9.4.1, Android API 37/min 26; Ktor 3.6.0, coroutines 1.11.0, serialization 1.12.0, QuickJS 1.0.15. These are documented requirements, not verified Rynx integrations.
GlyphStudio declares Kotlin 2.4.20, Compose 1.12.1, AGP 9.2.1, Android API 37. Do not silently rewrite upstream builds or downgrade dependencies.
Full transitive license/security audit and dependency locking remain open until runtime modules are linked.

Current desktop baseline links Compose Multiplatform 1.12.1 (including Skiko 0.150.1 via Compose Desktop) and coroutines 1.11.0, in addition to Kotlin stdlib/test. The downloaded full JDK is external tooling, not committed. Source dependency pins remain unchanged. `./gradlew :apps:desktop:dependencies --configuration jvmRuntimeClasspath` lists the desktop runtime graph. Full redistribution notices/transitive audit remain pending.
