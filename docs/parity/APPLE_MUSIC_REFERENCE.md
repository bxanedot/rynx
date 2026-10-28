# Apple Music reference lock

Authorized by the user as a behavioral/design reference. The current visual target is the Apple Music iPhone/iOS app, not Apple Music for Android. Keep original rynx branding and licensed assets. This changes visual direction, not providers: no Apple Music playback integration was requested. The user wants all Rynx application code in Kotlin/KMP for a later iOS port.

| Evidence | Public source | Findings | Limits |
|---|---|---|---|
| APPLE-001 | https://support.apple.com/guide/iphone/use-the-music-player-controls-iph676daac9b/ios | Library title, artwork grid, category rows, compact player above floating five-destination navigation; full-height Now Playing artwork, transport, timeline, volume and queue | Public screenshot inspected; device dp and motion unmeasured |
| APPLE-002 | https://support.apple.com/guide/music-windows/use-the-miniplayer-mus71d7dcfce/windows | Artwork opens MiniPlayer; hover controls, seek/volume/lyrics/queue; full screen and always-on-top setting | Guide read; playback strip screenshot inspected |
| APPLE-003 | https://support.apple.com/guide/music-windows/play-songs-from-your-library-mus36265ad9/windows | Library sidebar categories; playlist grouping; search, double-click to play | Documentation only; no full-window screenshot measured |
| APPLE-004 | https://support.apple.com/guide/music/use-miniplayer-mus71d7dcfce/mac | Separate compact artwork window, hover controls, queue, lyrics, size options, window menu | Guide read; geometry/motion unmeasured |
| APPLE-005 | https://support.apple.com/guide/tv/control-music-playback-atvbef12564e/tvos | Remote focus navigation, Now Playing, queue, lyrics, left restart/right skip and timeline | Guide read; no TV runtime capture |
| APPLE-006 | https://support.apple.com/guide/watch/play-music-apd70768b20b/watchos | Center artwork/title/artist, bottom transport, top back/more, queue under more, crown volume | Guide read; adaptation to Wear pending |
| APPLE-007 | https://play.google.com/store/apps/details?id=com.apple.android.music&hl=en_US | Android listing screenshots show Home/Browse/Radio/Library/Search navigation, a profile entry, recommendation/recent shelves, compact playback controls and browse shelves | Discovery only. Explicitly excluded as a visual or interaction reference by the user on 2026-10-28; these screenshots must not drive Rynx UI decisions. |

## Direction and roles
Preserve artwork-led hierarchy, strong library heading, quiet neutral canvas, red/pink control accent, compact playback strip and destination-appropriate navigation. Desktop uses sidebar; narrow windows use compact navigation. Use platform sans fallback, never bundle Apple's font without permission.

Rynx navigation uses the iOS-style “Listen Now”, Browse, Radio, Library and Search labels. The first item was corrected from “Home” after the owner clarified that Apple Music for iOS is the visual target. Listen Now displays persisted recent playback plus discovery sections; Browse shortcuts issue real catalog searches, and Search is connected to the catalog providers. Radio is explicitly marked as not connected; it is not counted as implemented. APPLE-007 is not used for screen geometry or behavior. Do not populate Spotify or cloud destinations with fabricated content or nonfunctional actions.

## Decision ledger
| Decision | Source | Role / limitation |
|---|---|---|
| Sidebar on desktop | APPLE-003 | Library/search grouping, adapted dimensions pending full reference |
| Compact player opens expanded player | APPLE-001/002 | Real shared playback controls |
| Artwork with title/artist and timeline | APPLE-001/002 | Local filenames until metadata is available; original Rynx vector mark when artwork is absent |
| Light/dark neutral surfaces with red accent | APPLE-001 | Independently chosen token approximations; not measured color parity |
| Original geometric glyphs initially | License blocker B-003 | No unlicensed SF Symbols assets shipped |
| Native local file picker | Platform API requirement | Desktop adaptation |

Current implementation values are proposals, not measurements. No blur or motion fidelity may be claimed from these stills.

## First-slice deviations and omissions
Current local-file views are an adaptation of the documented patterns. They have no album-art extraction, artwork-driven background, measured glass refraction/blur, shared-element morph or measured Apple animation. The five destinations are present, but Radio and the complete Apple catalog/player workflows are not connected. Full-height Now Playing uses the vector fallback, transport, seek and repeat. This is incomplete work, not a fidelity claim or permission to omit remaining requirements.

Latest user confirmation: Apple Music is again the direct interface and interaction target across devices, with Android phones implemented first. No Vyon build is required to proceed with authorized Apple reference capture. This is a user-directed reference choice, not independent verification that the two products are identical.
