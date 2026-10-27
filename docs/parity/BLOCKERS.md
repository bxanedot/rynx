# Blockers
| ID | Blocker | Affected work | Required resolution |
|---|---|---|---|
| B-001 | Vyon references unavailable; Apple Music authorized instead | Original Vyon parity cannot be established | Resolved direction change; Apple public UI guides now usable. Platform captures/motion measurements still absent |
| B-002 | No Android SDK, emulator or adb in initial environment | Android/TV/Wear runtime verification | Provision target SDKs and devices |
| B-003 | GlyphStudio implementation license not found; artwork terms unaudited | Shipping glyph library/artwork | Explicit implementation license and separate applicable Apple terms review |
| B-004 | Spotify configuration/entitlements and observed behavior absent | Auth/native playback and feature parity | Registered client/callback, supported SDK/API and authorized test account |
| B-005 | VyonEx requires JDK 27/JVM 26 and Android API 37 upstream | Runtime dependency integration | Compatible build environment or approved independently published artifacts |
| B-006 | No macOS/Windows target environments | Native runtime/package validation | Actual target environments |

These are prerequisites, not claims that the remaining implementation has been completed. Advanced DSP, backend, providers, AI and other scope are still unimplemented.

B-007: Debian native installer packaging requires fakeroot, absent from this environment. packageDeb failed; development JAR build succeeds.

B-008: Default Skiko GL fallback was unreliable in Xvfb on repeat launch; explicit SOFTWARE renderer works. Real GPU runtime/performance remains unverified.
