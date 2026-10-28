# Apple Music reference lock

Authorized by user's follow-up on 2026-10-27. Use documented Apple Music product UI, with original rynx branding and authorized provider content. This changes visual direction, not providers: no Apple Music catalog/playback integration was requested.

| Evidence | Public source | Findings | Limits |
|---|---|---|---|
| APPLE-001 | https://support.apple.com/guide/iphone/use-the-music-player-controls-iph676daac9b/ios | Library title, artwork grid, category rows, compact player above floating five-destination navigation; full-height Now Playing artwork, transport, timeline, volume and queue | Public screenshot inspected; device dp and motion unmeasured |
| APPLE-002 | https://support.apple.com/guide/music-windows/use-the-miniplayer-mus71d7dcfce/windows | Artwork opens MiniPlayer; hover controls, seek/volume/lyrics/queue; full screen and always-on-top setting | Guide read; playback strip screenshot inspected |
| APPLE-003 | https://support.apple.com/guide/music-windows/play-songs-from-your-library-mus36265ad9/windows | Library sidebar categories; playlist grouping; search, double-click to play | Documentation only; no full-window screenshot measured |
| APPLE-004 | https://support.apple.com/guide/music/use-miniplayer-mus71d7dcfce/mac | Separate compact artwork window, hover controls, queue, lyrics, size options, window menu | Guide read; geometry/motion unmeasured |
| APPLE-005 | https://support.apple.com/guide/tv/control-music-playback-atvbef12564e/tvos | Remote focus navigation, Now Playing, queue, lyrics, left restart/right skip and timeline | Guide read; no TV runtime capture |
| APPLE-006 | https://support.apple.com/guide/watch/play-music-apd70768b20b/watchos | Center artwork/title/artist, bottom transport, top back/more, queue under more, crown volume | Guide read; adaptation to Wear pending |
| APPLE-007 | https://play.google.com/store/apps/details?id=com.apple.android.music&hl=en_US | Android listing screenshots show Home/Browse/Radio/Library/Search navigation, a profile entry, Home recommendation/recent shelves, compact playback controls, browse artwork shelves and Radio features | Public promotional screenshots inspected 2026-10-28; compressed/noninteractive, so exact geometry, live states and motion remain unmeasured |

## Direction and roles
Preserve artwork-led hierarchy, strong library heading, quiet neutral canvas, red/pink control accent, compact playback strip and destination-appropriate navigation. Desktop uses sidebar; narrow windows use compact navigation. Use platform sans fallback, never bundle Apple's font without permission.

The first phone shell now exposes Home, Browse, Radio, Library and Search, matching the Apple Music Android Play Store screenshots inspected on 2026-10-28. Home shows persisted recently played items from actual playback; Browse shortcuts issue real catalog searches, and Search is connected to VyonEx. Radio is explicitly marked as not connected; it is not counted as implemented. Do not populate Spotify or cloud destinations with fabricated content or nonfunctional actions.

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
