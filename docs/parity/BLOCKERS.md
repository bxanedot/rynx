# Android-first blockers

| ID | Blocker | Impact |
|---|---|---|
| AND-B001 | Platform-specific Apple Music measurements and motion recordings incomplete | Apple Music is now the authorized direct reference; lack of Vyon media does not block UI work |
| AND-B002 | No attached physical Android phone and no /dev/kvm locally | Hosted API 36 KVM run works. Run 20 failed when the lyrics-picker test blocked on a stopped ActivityScenario, leaving the following Search test without a resumed host. Test harness now requests recreation through the retained Activity; hosted rerun is pending. Physical phone remains unavailable. |
| AND-B003 | Spotify registered client/callback, entitlements and authorized test account unavailable | Authentication and supported native playback validation |
| AND-B004 | GlyphStudio implementation Apache license confirmed by user; Apple artwork eligibility not resolved | Original vectors remain in runtime until artwork review |
| AND-B005 | Resolved for source licensing: Rynx selects GPL-3.0-or-later and includes corresponding source, dependency pins and retained notices | Full release audit and tested release identity remain separate gates |
| AND-B006 | VyonEx search and both stream-resolution adapters compile, but live provider and audible device tests have not run | Search-to-playback remains runtime-unverified |

Android SDK tooling was provisioned outside the repository. API 37 is available through AGP's newer package resolver, despite standalone sdkmanager initially failing to locate it. This is no longer an SDK blocker. Compile API 37 is needed by Compose 1.12.1. Other required features remain incomplete; documentation does not satisfy acceptance.

Historical desktop packaging/renderer limitations remain recorded in REGRESSION_RESULTS.md; desktop development is paused.

Apple public data/engine contracts passed live checks. This does not resolve full music playback, artwork/service release audit, complete recommendation training, native foreground/background acceptance or the Android completion gate. Native CI results must be assessed after the current fixes, not inferred from successful APK packaging.
