# Feature matrix

182 requested scope items; this is not a complete observed reference inventory. Apple Music replaced Vyon as visual reference by explicit user follow-up. Partial rows identify initial local desktop code, never verified full parity.

Statuses: implemented = code present; verified = relevant tests and reference comparison pass; partial = subset present; blocked = known unmet prerequisite; unverified = no evidence. No row is currently verified.

| ID | Requested feature | Status | Reference | Acceptance |
|---|---|---|---|---|
| DISC-001 | Listen Now | unverified | Apple Music iOS is the visual target; the screen still needs a permitted iPhone capture/spec | Record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| DISC-002 | Explore | unverified | REQ-001; not observed | Observe explore; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| DISC-003 | Search | partial | REQ-001; Apple reference adaptation; complete parity unverified | Apple metadata search and real cover mapping use Rynx KMP code; YouTube Music catalog search/pagination uses InnerTubeX. Deterministic tests and the local Android build pass; one live Apple search/image fetch passed. YouTube live playback, visual parity, artwork rights and other platforms remain unverified. |
| DISC-004 | Charts | unverified | REQ-001; not observed | Observe charts; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| DISC-005 | Genres | unverified | REQ-001; not observed | Observe genres; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| DISC-006 | Moods | unverified | REQ-001; not observed | Observe moods; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| DISC-007 | Recommendations | unverified | REQ-001; not observed | Observe recommendations; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| DISC-008 | New releases | unverified | REQ-001; not observed | Observe new releases; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| DISC-009 | Radios | unverified | REQ-001; not observed | Observe radios; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| DISC-010 | Artists | unverified | REQ-001; not observed | Observe artists; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| DISC-011 | Albums | unverified | REQ-001; not observed | Observe albums; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| DISC-012 | Playlists | unverified | REQ-001; not observed | Observe playlists; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| DISC-013 | Videos | unverified | REQ-001; not observed | Observe videos; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| DISC-014 | Regional discovery | unverified | REQ-001; not observed | Observe regional discovery; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| DISC-015 | Suggestions | unverified | REQ-001; not observed | Observe suggestions; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| DISC-016 | Personalized shelves | unverified | REQ-001; not observed | Observe personalized shelves; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| PLAY-001 | Background playback | unverified | REQ-001; not observed | Observe background playback; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| PLAY-002 | Mini-player | partial | REQ-001; Apple reference adaptation; complete parity unverified | Observe mini-player; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| PLAY-003 | Expanded player | partial | REQ-001; Apple reference adaptation; complete parity unverified | Observe expanded player; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| PLAY-004 | Player variants | unverified | REQ-001; not observed | Observe player variants; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| PLAY-005 | Queue | partial | REQ-001; Apple reference adaptation; complete parity unverified | Observe queue; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| PLAY-006 | Shuffle | unverified | REQ-001; not observed | Observe shuffle; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| PLAY-007 | Repeat | partial | REQ-001; Apple reference adaptation; complete parity unverified | Observe repeat; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| PLAY-008 | Seeking | partial | REQ-001; Apple reference adaptation; complete parity unverified | Observe seeking; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| PLAY-009 | Playback speed | unverified | REQ-001; not observed | Observe playback speed; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| PLAY-010 | Sleep timer | unverified | REQ-001; not observed | Observe sleep timer; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| PLAY-011 | Quality selection | unverified | REQ-001; not observed | Observe quality selection; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| PLAY-012 | Video playback | unverified | REQ-001; not observed | Observe video playback; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| PLAY-013 | Media controls | unverified | REQ-001; not observed | Observe media controls; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| PLAY-014 | Notifications | unverified | REQ-001; not observed | Observe notifications; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| PLAY-015 | Playback recovery | partial | REQ-001; Apple reference adaptation; complete parity unverified | Observe playback recovery; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| LIB-001 | Favorites | partial | Apple Music is the authorized UI reference; the owner describes Vyon as matching it, but specific feature parity remains unverified | Android favorite IDs and track metadata persist; library/player toggles, remote-track restoration, and deterministic UI/shared tests are implemented. Device persistence and replay validation remain pending. |
| LIB-002 | Playlists | unverified | REQ-001; not observed | Observe playlists; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| LIB-003 | History | unverified | REQ-001; not observed | Observe history; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| LIB-004 | Recent tracks | partial | The iOS Apple Music Listen Now screen is the visual target; recent-shelf order and contents are not yet captured or verified | Android stores up to 50 actually playing tracks and restores metadata to Listen Now. Device playback-history validation and cross-platform behavior remain pending. |
| LIB-005 | Local songs | partial | REQ-001; Apple reference adaptation; complete parity unverified | Observe local songs; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| LIB-006 | Local folders | unverified | REQ-001; not observed | Observe local folders; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| LIB-007 | Albums | unverified | REQ-001; not observed | Observe albums; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| LIB-008 | Artists | unverified | REQ-001; not observed | Observe artists; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| LIB-009 | Downloads | unverified | REQ-001; not observed | Observe downloads; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| LIB-010 | Network libraries | unverified | REQ-001; not observed | Observe network libraries; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| LIB-011 | Cached content | unverified | REQ-001; not observed | Observe cached content; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| LIB-012 | Automatic playlists | unverified | REQ-001; not observed | Observe automatic playlists; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| LIB-013 | Import | partial | Android local audio import and versioned Rynx playlist-archive import are implemented; local codec/database tests and a headless UI action test pass | Android document-picker file round trip, local-reference relinking, and non-Android import remain unverified. |
| LIB-014 | Export | partial | Android exports playlist metadata/order to versioned Rynx JSON through the document picker; codec/database and headless UI action tests pass | Android document-picker file round trip and non-Android export remain unverified; no audio files or playable stream URLs are included. |
| LIB-015 | Sorting | unverified | REQ-001; not observed | Observe sorting; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| LIB-016 | Filtering | unverified | REQ-001; not observed | Observe filtering; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| LYR-001 | Synchronized lyrics | unverified | REQ-001; not observed | Observe synchronized lyrics; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| LYR-002 | Word timing | unverified | REQ-001; not observed | Observe word timing; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| LYR-003 | Karaoke | unverified | REQ-001; not observed | Observe karaoke; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| LYR-004 | Translation | unverified | REQ-001; not observed | Observe translation; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| LYR-005 | Romanization | unverified | REQ-001; not observed | Observe romanization; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| LYR-006 | Lyrics sources | unverified | REQ-001; not observed | Observe lyrics sources; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| LYR-007 | Font customization | unverified | REQ-001; not observed | Observe font customization; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| LYR-008 | Lyrics motion | unverified | REQ-001; not observed | Observe lyrics motion; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| AUD-001 | Graphic EQ | unverified | REQ-001; not observed | Observe graphic eq; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| AUD-002 | Parametric EQ | unverified | REQ-001; not observed | Observe parametric eq; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| AUD-003 | AutoEQ | unverified | REQ-001; not observed | Observe autoeq; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| AUD-004 | Presets | unverified | REQ-001; not observed | Observe presets; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| AUD-005 | Crossfade | unverified | REQ-001; not observed | Observe crossfade; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| AUD-006 | Automix | unverified | REQ-001; not observed | Observe automix; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| AUD-007 | Loudness handling | unverified | REQ-001; not observed | Observe loudness handling; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| AUD-008 | Lossless output | unverified | REQ-001; not observed | Observe lossless output; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| AUD-009 | Spatial audio | unverified | REQ-001; not observed | Observe spatial audio; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| AUD-010 | Vocal suppression | unverified | REQ-001; not observed | Observe vocal suppression; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| AUD-011 | Output configuration | unverified | REQ-001; not observed | Observe output configuration; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| AUD-012 | Gapless playback | unverified | REQ-001; not observed | Observe gapless playback; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| AUD-013 | High resolution output | unverified | REQ-001; not observed | Observe high resolution output; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| AUD-014 | Audio telemetry | unverified | REQ-001; not observed | Observe audio telemetry; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| DJ-001 | Beat detection | unverified | REQ-001; not observed | Observe beat detection; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| DJ-002 | BPM | unverified | REQ-001; not observed | Observe bpm; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| DJ-003 | Musical key | unverified | REQ-001; not observed | Observe musical key; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| DJ-004 | Energy profiling | unverified | REQ-001; not observed | Observe energy profiling; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| DJ-005 | Stems | unverified | REQ-001; not observed | Observe stems; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| DJ-006 | Transition planning | unverified | REQ-001; not observed | Observe transition planning; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| DJ-007 | DJ effects | unverified | REQ-001; not observed | Observe dj effects; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| DJ-008 | Smart mixing | unverified | REQ-001; not observed | Observe smart mixing; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| DJ-009 | Automated sets | unverified | REQ-001; not observed | Observe automated sets; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| REC-001 | Microphone recognition | unverified | REQ-001; not observed | Observe microphone recognition; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| REC-002 | Fingerprint handling | unverified | REQ-001; not observed | Observe fingerprint handling; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| REC-003 | Recognition history | unverified | REQ-001; not observed | Observe recognition history; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| REC-004 | Recognition results | unverified | REQ-001; not observed | Observe recognition results; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| DOWN-001 | Track download | unverified | REQ-001; not observed | Observe track download; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| DOWN-002 | Batch download | unverified | REQ-001; not observed | Observe batch download; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| DOWN-003 | Caching | unverified | REQ-001; not observed | Observe caching; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| DOWN-004 | Offline playback | unverified | REQ-001; not observed | Observe offline playback; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| DOWN-005 | Transfer status | unverified | REQ-001; not observed | Observe transfer status; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| DOWN-006 | Retry | unverified | REQ-001; not observed | Observe retry; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| DOWN-007 | Cancellation | unverified | REQ-001; not observed | Observe cancellation; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| DOWN-008 | Integrity checking | unverified | REQ-001; not observed | Observe integrity checking; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| DOWN-009 | Local export | unverified | REQ-001; not observed | Observe local export; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| DOWN-010 | Resume | unverified | REQ-001; not observed | Observe resume; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| SOC-001 | Listen Together | unverified | REQ-001; not observed | Observe listen together; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| SOC-002 | Synchronized rooms | unverified | REQ-001; not observed | Observe synchronized rooms; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| SOC-003 | Jam | unverified | REQ-001; not observed | Observe jam; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| SOC-004 | Presence | unverified | REQ-001; not observed | Observe presence; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| SOC-005 | Participant controls | unverified | REQ-001; not observed | Observe participant controls; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| SOC-006 | Discord Rich Presence | unverified | REQ-001; not observed | Observe discord rich presence; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| SOC-007 | Last.fm | unverified | REQ-001; not observed | Observe last.fm; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| SOC-008 | ListenBrainz | unverified | REQ-001; not observed | Observe listenbrainz; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| SOC-009 | Clock correction | unverified | REQ-001; not observed | Observe clock correction; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| SOC-010 | Reconnection | unverified | REQ-001; not observed | Observe reconnection; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| SOC-011 | Room expiry | unverified | REQ-001; not observed | Observe room expiry; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| SOC-012 | Abuse prevention | unverified | REQ-001; not observed | Observe abuse prevention; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| AI-001 | AI DJ | unverified | REQ-001; not observed | Observe ai dj; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| AI-002 | AI music tools | unverified | REQ-001; not observed | Observe ai music tools; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| AI-003 | AI playlist editing | unverified | REQ-001; not observed | Observe ai playlist editing; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| AI-004 | AI configuration | unverified | REQ-001; not observed | Observe ai configuration; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| STAT-001 | Listening history | unverified | REQ-001; not observed | Observe listening history; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| STAT-002 | Statistics | unverified | REQ-001; not observed | Observe statistics; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| STAT-003 | Song insights | unverified | REQ-001; not observed | Observe song insights; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| STAT-004 | Replay presentations | unverified | REQ-001; not observed | Observe replay presentations; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| STAT-005 | Replay motion | unverified | REQ-001; not observed | Observe replay motion; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| CUSTOM-001 | Themes | partial | REQ-001; Apple reference adaptation; complete parity unverified | Observe themes; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| CUSTOM-002 | Colors | unverified | REQ-001; not observed | Observe colors; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| CUSTOM-003 | Fonts | unverified | REQ-001; not observed | Observe fonts; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| CUSTOM-004 | App icons | unverified | REQ-001; not observed | Observe app icons; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| CUSTOM-005 | Player selector | unverified | REQ-001; not observed | Observe player selector; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| CUSTOM-006 | DIY player editor | unverified | REQ-001; not observed | Observe diy player editor; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| CUSTOM-007 | Glass settings | unverified | REQ-001; not observed | Observe glass settings; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| CUSTOM-008 | Backgrounds | unverified | REQ-001; not observed | Observe backgrounds; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| CUSTOM-009 | Home customization | unverified | REQ-001; not observed | Observe home customization; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| CUSTOM-010 | Advanced appearance | unverified | REQ-001; not observed | Observe advanced appearance; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| CUSTOM-011 | Reduced motion | unverified | REQ-001; not observed | Observe reduced motion; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| UTIL-001 | Accounts | unverified | REQ-001; not observed | Observe accounts; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| UTIL-002 | Privacy | unverified | REQ-001; not observed | Observe privacy; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| UTIL-003 | Onboarding | unverified | REQ-001; not observed | Observe onboarding; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| UTIL-004 | Backup | unverified | REQ-001; not observed | Observe backup; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| UTIL-005 | Restore | unverified | REQ-001; not observed | Observe restore; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| UTIL-006 | Settings search | unverified | REQ-001; not observed | Observe settings search; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| UTIL-007 | Downloads management | unverified | REQ-001; not observed | Observe downloads management; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| UTIL-008 | Error logs | unverified | REQ-001; not observed | Observe error logs; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| UTIL-009 | Updates | unverified | REQ-001; not observed | Observe updates; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| UTIL-010 | Diagnostics | unverified | REQ-001; not observed | Observe diagnostics; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| PROV-001 | InnerTubeX primary catalog/resolver | partial | User-authorized KMP integration; no live provider observation | Search/pagination and stream resolution use the public KMP API. Supports only direct HTTPS audio in the current Media3 path. Current tests/build pending; live resolution and device playback unverified. |
| PROV-007 | NewPipeExtractor fallback | partial | User-authorized Android fallback | Android-only resolver after InnerTubeX; live fallback and device playback remain unverified. |
| PROV-008 | YouTube.js | blocked | User requested package; public npm package inspected | JavaScript-only package is not included; no tested Android/Kotlin host exists. |
| PROV-002 | Spotify | unverified | REQ-001; not observed | Observe spotify; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| PROV-003 | Local files | partial | REQ-001; Apple reference adaptation; complete parity unverified | Observe local files; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| PROV-004 | SMB | unverified | REQ-001; not observed | Observe smb; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| PROV-005 | WebDAV | unverified | REQ-001; not observed | Observe webdav; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| PROV-006 | External modules | unverified | REQ-001; not observed | Observe external modules; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| SPOT-001 | Login | unverified | REQ-001; not observed | Observe login; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| SPOT-002 | Account linking | unverified | REQ-001; not observed | Observe account linking; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| SPOT-003 | Token persistence | unverified | REQ-001; not observed | Observe token persistence; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| SPOT-004 | Token refresh | unverified | REQ-001; not observed | Observe token refresh; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| SPOT-005 | Catalog | unverified | REQ-001; not observed | Observe catalog; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| SPOT-006 | Home feed | unverified | REQ-001; not observed | Observe home feed; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| SPOT-007 | Artists | unverified | REQ-001; not observed | Observe artists; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| SPOT-008 | Albums | unverified | REQ-001; not observed | Observe albums; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| SPOT-009 | Tracks | unverified | REQ-001; not observed | Observe tracks; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| SPOT-010 | Playlists | unverified | REQ-001; not observed | Observe playlists; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| SPOT-011 | Library | unverified | REQ-001; not observed | Observe library; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| SPOT-012 | Liked songs | unverified | REQ-001; not observed | Observe liked songs; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| SPOT-013 | Playlist import | unverified | REQ-001; not observed | Observe playlist import; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| SPOT-014 | Playlist management | unverified | REQ-001; not observed | Observe playlist management; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| SPOT-015 | Artwork | unverified | REQ-001; not observed | Observe artwork; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| SPOT-016 | Canvas-style visuals | unverified | REQ-001; not observed | Observe canvas-style visuals; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| SPOT-017 | Playback | unverified | REQ-001; not observed | Observe playback; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| SPOT-018 | Play pause | unverified | REQ-001; not observed | Observe play pause; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| SPOT-019 | Seek | unverified | REQ-001; not observed | Observe seek; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| SPOT-020 | Skip previous | unverified | REQ-001; not observed | Observe skip previous; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| SPOT-021 | Queue | unverified | REQ-001; not observed | Observe queue; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| SPOT-022 | Shuffle repeat | unverified | REQ-001; not observed | Observe shuffle repeat; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| SPOT-023 | Remote playback | unverified | REQ-001; not observed | Observe remote playback; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| SPOT-024 | Connect | unverified | REQ-001; not observed | Observe connect; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| SPOT-025 | Authorized offline | unverified | REQ-001; not observed | Observe authorized offline; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| SPOT-026 | DJ | unverified | REQ-001; not observed | Observe dj; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| SPOT-027 | Reconnection | unverified | REQ-001; not observed | Observe reconnection; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| SPOT-028 | Errors | unverified | REQ-001; not observed | Observe errors; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| MOD-001 | Discovery | unverified | REQ-001; not observed | Observe discovery; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| MOD-002 | Manifests | unverified | REQ-001; not observed | Observe manifests; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| MOD-003 | Installation | unverified | REQ-001; not observed | Observe installation; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| MOD-004 | Settings | unverified | REQ-001; not observed | Observe settings; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| MOD-005 | Source priorities | unverified | REQ-001; not observed | Observe source priorities; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| MOD-006 | Search capabilities | unverified | REQ-001; not observed | Observe search capabilities; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| MOD-007 | Stream capabilities | unverified | REQ-001; not observed | Observe stream capabilities; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| MOD-008 | Errors | unverified | REQ-001; not observed | Observe errors; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| MOD-009 | Timeouts | unverified | REQ-001; not observed | Observe timeouts; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| MOD-010 | Cancellation | unverified | REQ-001; not observed | Observe cancellation; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| MOD-011 | Permissions | unverified | REQ-001; not observed | Observe permissions; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| MOD-012 | Sandbox | unverified | REQ-001; not observed | Observe sandbox; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| MOD-013 | Health | unverified | REQ-001; not observed | Observe health; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
| MOD-014 | Format compatibility | unverified | REQ-001; not observed | Observe format compatibility; record input, output, persistence, cancellation and failure cases; implement and verify on relevant platforms. |
