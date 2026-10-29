# Android-first blockers

| ID | Blocker | Impact |
|---|---|---|
| AND-B001 | Platform-specific Apple Music measurements and motion recordings incomplete | Apple Music is now the authorized direct reference; lack of Vyon media does not block UI work |
| AND-B002 | No attached physical Android phone and no /dev/kvm locally | Historical hosted API 36 KVM run 28 passed 10/10 device tests on an earlier commit. For the current worktree, the local API 36 software emulator remained offline and never reached `sys.boot_completed`; instrumentation only compiled. No current-branch device test or physical-device result is available. |
| AND-B003 | Spotify registered client/callback, entitlements and authorized test account unavailable | Authentication and supported native playback validation |
| AND-B004 | GlyphStudio implementation Apache license confirmed by user; Apple artwork eligibility not resolved | Original vectors remain in runtime until artwork review |
| AND-B005 | Resolved for source licensing: Rynx selects GPL-3.0-or-later and includes corresponding source, dependency pins and retained notices | Full release audit and tested release identity remain separate gates |
| AND-B006 | Excor live search/extraction and audible device tests remain unverified | The current opt-in YouTube check failed before a provider response with a network `IOException` (`ConnectPlan.kt:472`, connection refused); current-branch Android instrumentation did not run because the local emulator failed to boot. Search-to-playback remains runtime-unverified. |

Android SDK tooling was provisioned outside the repository. API 37 is available through AGP's newer package resolver, despite standalone sdkmanager initially failing to locate it. This is no longer an SDK blocker. Compile API 37 is needed by Compose 1.12.1. Other required features remain incomplete; documentation does not satisfy acceptance.

Historical desktop packaging/renderer limitations remain recorded in REGRESSION_RESULTS.md; desktop development is paused.

Apple public data/engine contracts passed live checks. This does not resolve full music playback, artwork/service release audit, complete recommendation training, native foreground/background acceptance or the Android completion gate. Native CI results must be assessed after the current fixes, not inferred from successful APK packaging.
