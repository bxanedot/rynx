# Android-first blockers

| ID | Blocker | Impact |
|---|---|---|
| AND-B001 | Platform-specific Apple Music measurements and motion recordings incomplete | Apple Music is the authorized direct reference; the lack of Vyon media does not block UI work. |
| AND-B002 | No attached physical Android phone in the current workspace | The prior local API 36 emulator did not boot; hosted runs provide emulator evidence only. This workspace currently has no `adb` executable or USB device path, so authorized visible-app inspection and physical-device audio, accessibility, and performance results await an accessible local device. |
| AND-B003 | Spotify registered client/callback, entitlements and authorized test account unavailable | Authentication and supported native playback validation remain blocked. |
| AND-B004 | GlyphStudio implementation Apache license confirmed by user; Apple artwork eligibility not resolved | The shared app now renders GlyphStudio symbols on Android, iOS, and desktop per user instruction; complete the separate artwork distribution review before release. |
| AND-B005 | Source licensing resolved: Rynx selects GPL-3.0-or-later and includes corresponding source, dependency pins and retained notices | Full release audit and signed release identity remain separate gates. |
| AND-B006 | InnerTubeX/NewPipeExtractor live extraction, fallback invocation and audible provider playback remain unverified | Current provider tests/builds pass, and API 36 device instrumentation passes for generated local PCM. A prior opt-in YouTube check failed before any provider response with a network `IOException` (`ConnectPlan.kt:472`, connection refused); the hosted device suite does not verify provider search-to-playback. |
| AND-B007 | DSP has deterministic PCM tests and Media3 integration, but no physical-device audio, output-route, underrun or long-session test | Audiophile output claims, device stability and performance remain unverified; ReplayGain and overlapping crossfade are not implemented. |
| AND-B008 | No actual download queue or download completion pipeline is implemented | The shared “Downloaded by Rynx” attribution record and Android atomic sidecar writer are tested, but no download path invokes them automatically. |
| AND-B009 | No separate public `bxanedot/glyphsymbols` repository resolves (GitHub API 404; bxanedot account lists `GlyphStudio` only, checked 2026-10-29) | The available bxane symbol library is GlyphStudio's KMP `:sfsymbols` module, now used from `commonMain` on every app target. If a separate `GlyphSymbols` artifact was intended, its public coordinate is still unknown. |
| AND-B010 | YouTube.js has no Android/Kotlin host in Rynx | The official `youtubei.js` package targets JavaScript runtimes and is not in the current Android dependency graph. Android currently uses InnerTubeX → NewPipeExtractor; adding a JavaScript host would require separate integration and device tests. |
| AND-B011 | Live scrobbling credentials and account tests are unavailable | Last.fm requires Rynx's registered app API key/shared secret; ListenBrainz needs an owner-authorized test token. Build, protocol tests, secure persistence, and service behavior must pass CI before these integrations can be verified beyond implementation status. |

Android SDK tooling was provisioned outside the repository. API 37 is available through AGP's package resolver; this is no longer an SDK blocker. Compose 1.12.1 requires compile API 37.

Historical desktop packaging/renderer limitations remain recorded in `REGRESSION_RESULTS.md`; desktop development is paused.

Apple public data and engine contracts passed live checks. This does not resolve full music playback, artwork/service release audit, complete recommendation training, native foreground/background acceptance or the Android completion gate. See `REGRESSION_RESULTS.md` for the exact hosted commit results and limitations.
