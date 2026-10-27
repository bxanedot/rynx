# Platform matrix
| Target | Build | Runtime | Visual/motion | Native requirements |
|---|---|---|---|---|
| Android phone/tablet FOSS/GMS | Not configured | Not tested | unverified | Media3, session, foreground service, focus, MediaStore, downloads, lifecycle |
| Android TV | Not configured | Not tested | unverified | D-pad, focus, TV layouts/onboarding |
| Wear OS | Not configured | Not tested | unverified | Phone sync, reconnection, supported playback |
| Windows | Desktop sources configured, not built on Windows | Not tested | unverified | Native shell/JVM package verification pending |
| macOS | Desktop sources configured, not built on macOS | Not tested | unverified | Native shell/JVM package verification pending |
| Linux | Desktop JAR built | Window launched under Xvfb/software rendering; audible playback unverified | Partial adaptive views, no fidelity comparison | Java Sound baseline; system media integration pending |
| iOS/iPadOS | Planned expansion | Not tested | unverified | Native audio/system integration after original targets |

The foundation's commonMain is platform-neutral Kotlin. A JVM test target is not a delivered desktop app or proof of other platform support.
