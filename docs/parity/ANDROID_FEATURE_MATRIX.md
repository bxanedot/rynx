# Android feature matrix

Requested scope inventory, not an observed Vyon inventory. No Vyon Android build/captures are available. No complete feature/reference/device parity is verified. Local tests verify narrower behaviors documented below. Existing desktop implementation does not establish Android completion.

| ID | Feature | Android status | Evidence / next acceptance |
|---|---|---|---|
| DISC-001 | Home | incomplete | Real Apple-backed Home shelves and local service-owned Recently Played. Global/region/neighbors, editorial releases, date-filtered week, rising ranks, city/tour collections and local recommendations implemented; provider/UI tests pass; native acceptance pending. |
| DISC-002 | Explore | incomplete | Explore renders dynamically fetched Apple genre IDs/names with original vector tiles and a genuine genre-chart drilldown; headless interaction test passes. |
| DISC-003 | Search | incomplete | Apple metadata search through VyonEx with YouTube Music fallback; conservative variant/artist/duration matching before playback. Live Apple and deterministic tests pass; native stream path pending. |
| DISC-004 | Charts | incomplete | Actual Apple Global chart and labelled storefront snapshots; City Charts from public links/JSON-LD. Live data contract checks pass; source availability may vary. |
| DISC-005 | Genres | incomplete | 51 actual Apple genres verified live; nested taxonomy retained; original tile artwork and real iTunes genre charts. |
| DISC-006 | Moods | unverified | Observe permitted reference, implement, test and compare |
| DISC-007 | Recommendations | incomplete | Original local content-based ranker with decayed artist/genre affinity, popularity, freshness, diversity and replay penalty; qualifying service-owned playback signals persist; favorite/skip training incomplete. |
| DISC-008 | New releases | incomplete | Best new songs from actual New Music Daily; New this week filters actual release dates. Not a complete upcoming-release service. |
| DISC-009 | Radios | unverified | Observe permitted reference, implement, test and compare |
| DISC-010 | Artists | unverified | Observe permitted reference, implement, test and compare |
| DISC-011 | Albums | unverified | Observe permitted reference, implement, test and compare |
| DISC-012 | Playlists | unverified | Observe permitted reference, implement, test and compare |
| DISC-013 | Videos | unverified | Observe permitted reference, implement, test and compare |
| DISC-014 | Regional discovery | unverified | Observe permitted reference, implement, test and compare |
| DISC-015 | Suggestions | unverified | Observe permitted reference, implement, test and compare |
| DISC-016 | Personalized shelves | incomplete | Personalized local candidate ranking and service-owned listening history; no Apple account-history or private algorithm access. |
| PLAY-001 | Background playback | incomplete | Media3 foreground session/service code present; generated local audio session metadata and pause/resume instrumentation compiles; current-branch device runtime, audio-focus and interruption tests remain pending |
| PLAY-002 | Mini-player | incomplete | One capsule-to-sheet surface, shared artwork/transport and navigation handoff pass headless interaction/continuity tests; Android gesture/render acceptance pending |
| PLAY-003 | Expanded player | incomplete | Reference-proportioned transport, volume, output, seek and menus implemented in all material modes; landscape and large-font harness passes; native/reference gates pending |
| PLAY-004 | Player variants | unverified | Observe permitted reference, implement, test and compare |
| PLAY-005 | Queue | incomplete | Playback-order aware queue with shuffle-safe reorder/remove/clear; headless tests pass; actual Media3 instrumentation compiled, not run |
| PLAY-006 | Shuffle | incomplete | Native Media3 shuffle and shuffle-order editing wired; headless/order tests pass, device test pending |
| PLAY-007 | Repeat | incomplete | Android host code present; actual-device validation and reference comparison pending |
| PLAY-008 | Seeking | incomplete | Android host code present; actual-device validation and reference comparison pending |
| PLAY-009 | Playback speed | unverified | Observe permitted reference, implement, test and compare |
| PLAY-010 | Sleep timer | unverified | Observe permitted reference, implement, test and compare |
| PLAY-011 | Quality selection | unverified | Observe permitted reference, implement, test and compare |
| PLAY-012 | Video playback | unverified | Observe permitted reference, implement, test and compare |
| PLAY-013 | Media controls | unverified | Observe permitted reference, implement, test and compare |
| PLAY-014 | Notifications | unverified | Observe permitted reference, implement, test and compare |
| PLAY-015 | Playback recovery | unverified | Observe permitted reference, implement, test and compare |
| LIB-001 | Favorites | incomplete | Compose actions update shared favorite IDs; Android persists IDs plus local/YouTube Music display metadata and can resolve saved YouTube Music items again. Shared/UI tests pass and Android unit/instrumentation tests compile; device persistence/playback validation remains pending. |
| LIB-002 | Playlists | unverified | Observe permitted reference, implement, test and compare |
| LIB-003 | History | unverified | Observe permitted reference, implement, test and compare |
| LIB-004 | Recent tracks | incomplete | Android persists up to 50 actually playing tracks and restores metadata for the Home shelf; playback-history device test and comparison remain pending. |
| LIB-005 | Local songs | incomplete | Android host code present; actual-device validation and reference comparison pending |
| LIB-006 | Local folders | unverified | Observe permitted reference, implement, test and compare |
| LIB-007 | Albums | unverified | Observe permitted reference, implement, test and compare |
| LIB-008 | Artists | unverified | Observe permitted reference, implement, test and compare |
| LIB-009 | Downloads | unverified | Observe permitted reference, implement, test and compare |
| LIB-010 | Network libraries | unverified | Observe permitted reference, implement, test and compare |
| LIB-011 | Cached content | unverified | Observe permitted reference, implement, test and compare |
| LIB-012 | Automatic playlists | unverified | Observe permitted reference, implement, test and compare |
| LIB-013 | Import | incomplete | Android host code present; actual-device validation and reference comparison pending |
| LIB-014 | Export | unverified | Observe permitted reference, implement, test and compare |
| LIB-015 | Sorting | unverified | Observe permitted reference, implement, test and compare |
| LIB-016 | Filtering | unverified | Observe permitted reference, implement, test and compare |
| LYR-001 | Synchronized lyrics | incomplete | Imported timed LRC, current-line following and seek implemented; parser/UI tests pass; native picker/device verification pending |
| LYR-002 | Word timing | incomplete | Enhanced LRC word timing parsed and highlighted; boundary tests pass; reference/device verification pending |
| LYR-003 | Karaoke | incomplete | Real timed word progress in shared lyric pane; no translation/romanization provider |
| LYR-004 | Translation | unverified | Observe permitted reference, implement, test and compare |
| LYR-005 | Romanization | unverified | Observe permitted reference, implement, test and compare |
| LYR-006 | Lyrics sources | incomplete | Local LRC picker/import and private persistence implemented; online lyric providers absent |
| LYR-007 | Font customization | unverified | Observe permitted reference, implement, test and compare |
| LYR-008 | Lyrics motion | incomplete | Auto-follow, user-scroll pause/recovery and line/word emphasis; headless tests pass, exact motion unmeasured |
| AUD-001 | Graphic EQ | unverified | Observe permitted reference, implement, test and compare |
| AUD-002 | Parametric EQ | unverified | Observe permitted reference, implement, test and compare |
| AUD-003 | AutoEQ | unverified | Observe permitted reference, implement, test and compare |
| AUD-004 | Presets | unverified | Observe permitted reference, implement, test and compare |
| AUD-005 | Crossfade | unverified | Observe permitted reference, implement, test and compare |
| AUD-006 | Automix | unverified | Observe permitted reference, implement, test and compare |
| AUD-007 | Loudness handling | unverified | Observe permitted reference, implement, test and compare |
| AUD-008 | Lossless output | unverified | Observe permitted reference, implement, test and compare |
| AUD-009 | Spatial audio | unverified | Observe permitted reference, implement, test and compare |
| AUD-010 | Vocal suppression | unverified | Observe permitted reference, implement, test and compare |
| AUD-011 | Output configuration | unverified | Observe permitted reference, implement, test and compare |
| AUD-012 | Gapless playback | unverified | Observe permitted reference, implement, test and compare |
| AUD-013 | High resolution output | unverified | Observe permitted reference, implement, test and compare |
| AUD-014 | Audio telemetry | unverified | Observe permitted reference, implement, test and compare |
| DJ-001 | Beat detection | unverified | Observe permitted reference, implement, test and compare |
| DJ-002 | BPM | unverified | Observe permitted reference, implement, test and compare |
| DJ-003 | Musical key | unverified | Observe permitted reference, implement, test and compare |
| DJ-004 | Energy profiling | unverified | Observe permitted reference, implement, test and compare |
| DJ-005 | Stems | unverified | Observe permitted reference, implement, test and compare |
| DJ-006 | Transition planning | unverified | Observe permitted reference, implement, test and compare |
| DJ-007 | DJ effects | unverified | Observe permitted reference, implement, test and compare |
| DJ-008 | Smart mixing | unverified | Observe permitted reference, implement, test and compare |
| DJ-009 | Automated sets | unverified | Observe permitted reference, implement, test and compare |
| REC-001 | Microphone recognition | unverified | Observe permitted reference, implement, test and compare |
| REC-002 | Fingerprint handling | unverified | Observe permitted reference, implement, test and compare |
| REC-003 | Recognition history | unverified | Observe permitted reference, implement, test and compare |
| REC-004 | Recognition results | unverified | Observe permitted reference, implement, test and compare |
| DOWN-001 | Track download | unverified | Observe permitted reference, implement, test and compare |
| DOWN-002 | Batch download | unverified | Observe permitted reference, implement, test and compare |
| DOWN-003 | Caching | unverified | Observe permitted reference, implement, test and compare |
| DOWN-004 | Offline playback | unverified | Observe permitted reference, implement, test and compare |
| DOWN-005 | Transfer status | unverified | Observe permitted reference, implement, test and compare |
| DOWN-006 | Retry | unverified | Observe permitted reference, implement, test and compare |
| DOWN-007 | Cancellation | unverified | Observe permitted reference, implement, test and compare |
| DOWN-008 | Integrity checking | unverified | Observe permitted reference, implement, test and compare |
| DOWN-009 | Local export | unverified | Observe permitted reference, implement, test and compare |
| DOWN-010 | Resume | unverified | Observe permitted reference, implement, test and compare |
| SOC-001 | Listen Together | unverified | Observe permitted reference, implement, test and compare |
| SOC-002 | Synchronized rooms | unverified | Observe permitted reference, implement, test and compare |
| SOC-003 | Jam | unverified | Observe permitted reference, implement, test and compare |
| SOC-004 | Presence | unverified | Observe permitted reference, implement, test and compare |
| SOC-005 | Participant controls | unverified | Observe permitted reference, implement, test and compare |
| SOC-006 | Discord Rich Presence | unverified | Observe permitted reference, implement, test and compare |
| SOC-007 | Last.fm | unverified | Observe permitted reference, implement, test and compare |
| SOC-008 | ListenBrainz | unverified | Observe permitted reference, implement, test and compare |
| SOC-009 | Clock correction | unverified | Observe permitted reference, implement, test and compare |
| SOC-010 | Reconnection | unverified | Observe permitted reference, implement, test and compare |
| SOC-011 | Room expiry | unverified | Observe permitted reference, implement, test and compare |
| SOC-012 | Abuse prevention | unverified | Observe permitted reference, implement, test and compare |
| AI-001 | AI DJ | unverified | Observe permitted reference, implement, test and compare |
| AI-002 | AI music tools | unverified | Observe permitted reference, implement, test and compare |
| AI-003 | AI playlist editing | unverified | Observe permitted reference, implement, test and compare |
| AI-004 | AI configuration | unverified | Observe permitted reference, implement, test and compare |
| STAT-001 | Listening history | incomplete | Service-owned recent history and qualifying listening signals implemented, including background sessions; native background regression compiles and awaits CI. |
| STAT-002 | Statistics | unverified | Observe permitted reference, implement, test and compare |
| STAT-003 | Song insights | unverified | Observe permitted reference, implement, test and compare |
| STAT-004 | Replay presentations | unverified | Observe permitted reference, implement, test and compare |
| STAT-005 | Replay motion | unverified | Observe permitted reference, implement, test and compare |
| CUSTOM-001 | Themes | incomplete | Live light/dark plus Liquid Glass/Solid/AMOLED; mode policy and UI tests pass; Android runtime pending |
| CUSTOM-002 | Colors | unverified | Observe permitted reference, implement, test and compare |
| CUSTOM-003 | Fonts | unverified | Observe permitted reference, implement, test and compare |
| CUSTOM-004 | App icons | unverified | Observe permitted reference, implement, test and compare |
| CUSTOM-005 | Player selector | unverified | Observe permitted reference, implement, test and compare |
| CUSTOM-006 | DIY player editor | unverified | Observe permitted reference, implement, test and compare |
| CUSTOM-007 | Glass settings | incomplete | Central Haze renderer, all optical controls/live preview, nested source layering and Android persistence implemented; render tests pass, device cost unmeasured |
| CUSTOM-008 | Backgrounds | unverified | Observe permitted reference, implement, test and compare |
| CUSTOM-009 | Home customization | unverified | Observe permitted reference, implement, test and compare |
| CUSTOM-010 | Advanced appearance | incomplete | Shared material contracts across existing screens; unimplemented product screens remain absent |
| CUSTOM-011 | Reduced motion | incomplete | Full/reduced/follow-system and motion intensity; snap alternatives implemented; native accessibility service verification pending |
| UTIL-001 | Accounts | unverified | Observe permitted reference, implement, test and compare |
| UTIL-002 | Privacy | unverified | Observe permitted reference, implement, test and compare |
| UTIL-003 | Onboarding | unverified | Observe permitted reference, implement, test and compare |
| UTIL-004 | Backup | unverified | Observe permitted reference, implement, test and compare |
| UTIL-005 | Restore | unverified | Observe permitted reference, implement, test and compare |
| UTIL-006 | Settings search | unverified | Observe permitted reference, implement, test and compare |
| UTIL-007 | Downloads management | unverified | Observe permitted reference, implement, test and compare |
| UTIL-008 | Error logs | unverified | Observe permitted reference, implement, test and compare |
| UTIL-009 | Updates | unverified | Observe permitted reference, implement, test and compare |
| UTIL-010 | Diagnostics | unverified | Observe permitted reference, implement, test and compare |
| PROV-001 | VyonEx | incomplete | Android/JVM adapter implements shared KMP catalog/stream contracts; deterministic tests and release builds pass. Live YouTube request was refused by the environment before provider response; device playback is unverified |
| PROV-002 | Spotify | unverified | Observe permitted reference, implement, test and compare |
| PROV-003 | Local files | incomplete | Android host code present; actual-device validation and reference comparison pending |
| PROV-004 | SMB | unverified | Observe permitted reference, implement, test and compare |
| PROV-005 | WebDAV | unverified | Observe permitted reference, implement, test and compare |
| PROV-006 | External modules | unverified | Observe permitted reference, implement, test and compare |
| PROV-007 | NewPipeExtractor fallback | incomplete | Pinned Android fallback implements the shared KMP resolver contract and compiles; VyonEx-first/cancellation behavior tests pass. Live fallback and device acceptance remain pending |
| SPOT-001 | Login | unverified | Observe permitted reference, implement, test and compare |
| SPOT-002 | Account linking | unverified | Observe permitted reference, implement, test and compare |
| SPOT-003 | Token persistence | unverified | Observe permitted reference, implement, test and compare |
| SPOT-004 | Token refresh | unverified | Observe permitted reference, implement, test and compare |
| SPOT-005 | Catalog | unverified | Observe permitted reference, implement, test and compare |
| SPOT-006 | Home feed | unverified | Observe permitted reference, implement, test and compare |
| SPOT-007 | Artists | unverified | Observe permitted reference, implement, test and compare |
| SPOT-008 | Albums | unverified | Observe permitted reference, implement, test and compare |
| SPOT-009 | Tracks | unverified | Observe permitted reference, implement, test and compare |
| SPOT-010 | Playlists | unverified | Observe permitted reference, implement, test and compare |
| SPOT-011 | Library | unverified | Observe permitted reference, implement, test and compare |
| SPOT-012 | Liked songs | unverified | Observe permitted reference, implement, test and compare |
| SPOT-013 | Playlist import | unverified | Observe permitted reference, implement, test and compare |
| SPOT-014 | Playlist management | unverified | Observe permitted reference, implement, test and compare |
| SPOT-015 | Artwork | unverified | Observe permitted reference, implement, test and compare |
| SPOT-016 | Canvas-style visuals | unverified | Observe permitted reference, implement, test and compare |
| SPOT-017 | Playback | unverified | Observe permitted reference, implement, test and compare |
| SPOT-018 | Play pause | unverified | Observe permitted reference, implement, test and compare |
| SPOT-019 | Seek | unverified | Observe permitted reference, implement, test and compare |
| SPOT-020 | Skip previous | unverified | Observe permitted reference, implement, test and compare |
| SPOT-021 | Queue | incomplete | Android host code present; actual-device validation and reference comparison pending |
| SPOT-022 | Shuffle repeat | unverified | Observe permitted reference, implement, test and compare |
| SPOT-023 | Remote playback | unverified | Observe permitted reference, implement, test and compare |
| SPOT-024 | Connect | unverified | Observe permitted reference, implement, test and compare |
| SPOT-025 | Authorized offline | unverified | Observe permitted reference, implement, test and compare |
| SPOT-026 | DJ | unverified | Observe permitted reference, implement, test and compare |
| SPOT-027 | Reconnection | unverified | Observe permitted reference, implement, test and compare |
| SPOT-028 | Errors | unverified | Observe permitted reference, implement, test and compare |
| MOD-001 | Discovery | unverified | Observe permitted reference, implement, test and compare |
| MOD-002 | Manifests | unverified | Observe permitted reference, implement, test and compare |
| MOD-003 | Installation | unverified | Observe permitted reference, implement, test and compare |
| MOD-004 | Settings | unverified | Observe permitted reference, implement, test and compare |
| MOD-005 | Source priorities | unverified | Observe permitted reference, implement, test and compare |
| MOD-006 | Search capabilities | unverified | Observe permitted reference, implement, test and compare |
| MOD-007 | Stream capabilities | unverified | Observe permitted reference, implement, test and compare |
| MOD-008 | Errors | unverified | Observe permitted reference, implement, test and compare |
| MOD-009 | Timeouts | unverified | Observe permitted reference, implement, test and compare |
| MOD-010 | Cancellation | unverified | Observe permitted reference, implement, test and compare |
| MOD-011 | Permissions | unverified | Observe permitted reference, implement, test and compare |
| MOD-012 | Sandbox | unverified | Observe permitted reference, implement, test and compare |
| MOD-013 | Health | unverified | Observe permitted reference, implement, test and compare |
| MOD-014 | Format compatibility | unverified | Observe permitted reference, implement, test and compare |

Latest reference clarification: the user describes Vyon as a copy of Apple Music across devices and authorizes Apple Music as the direct UI/UX reference. Earlier missing-Vyon evidence notes are historical, not a prerequisite to further UI work. Existing implementation and verification statuses are unchanged; platform-specific Apple measurements/comparisons remain necessary. Android stays P0.
