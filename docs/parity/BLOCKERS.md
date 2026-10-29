# Android-first blockers

| ID | Blocker | Impact |
|---|---|---|
| AND-B001 | Platform-specific Apple Music measurements and motion recordings incomplete | Apple Music is the authorized direct reference; the lack of Vyon media does not block UI work. |
| AND-B002 | No attached physical Android phone and no `/dev/kvm` locally | Verify on `49abec7` passed. Hosted API 36 phone run `40883902747` on `07b3017` failed queue restoration: the test could read its seeded queue before startup, but the service read no snapshot. The test now flushes its seed synchronously with `commit()` before service startup; current-head connected verification is pending. Physical-device output, accessibility, and performance results remain unavailable. |
| AND-B003 | Spotify registered client/callback, entitlements and authorized test account unavailable | Authentication and supported native playback validation remain blocked. |
| AND-B004 | GlyphStudio implementation Apache license confirmed by user; Apple artwork eligibility not resolved | The shared app now renders GlyphStudio symbols on Android, iOS, and desktop per user instruction; complete the separate artwork distribution review before release. |
| AND-B005 | Source licensing resolved: Rynx selects GPL-3.0-or-later and includes corresponding source, dependency pins and retained notices | Full release audit and signed release identity remain separate gates. |
| AND-B006 | eXcor/InnerTubeX live extraction, fallback invocation and audible device tests remain unverified | The latest opt-in YouTube check failed before a provider response with a network `IOException` (`ConnectPlan.kt:472`, connection refused). InnertubeX has only deterministic adapter/build coverage so far; search-to-playback remains runtime-unverified. |
| AND-B007 | DSP has deterministic PCM tests and Media3 integration, but no physical-device audio, output-route, underrun or long-session test | Audiophile output claims, device stability and performance remain unverified; ReplayGain and overlapping crossfade are not implemented. |
| AND-B008 | No actual download queue or download completion pipeline is implemented | The shared “Downloaded by Rynx” attribution record and Android atomic sidecar writer are tested, but no download path invokes them automatically. |
| AND-B009 | No separate public `bxanedot/glyphsymbols` repository resolves (GitHub API 404; bxanedot account lists `GlyphStudio` only, checked 2026-10-29) | The available bxane symbol library is GlyphStudio's KMP `:sfsymbols` module, now used from `commonMain` on every app target. If a separate `GlyphSymbols` artifact was intended, its public coordinate is still unknown. |

Android SDK tooling was provisioned outside the repository. API 37 is available through AGP's package resolver; this is no longer an SDK blocker. Compose 1.12.1 requires compile API 37.

Historical desktop packaging/renderer limitations remain recorded in `REGRESSION_RESULTS.md`; desktop development is paused.

Apple public data and engine contracts passed live checks. This does not resolve full music playback, artwork/service release audit, complete recommendation training, native foreground/background acceptance or the Android completion gate. See `REGRESSION_RESULTS.md` for the exact hosted commit results and limitations.
