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

## Direction and roles
Preserve artwork-led hierarchy, strong library heading, quiet neutral canvas, red/pink control accent, compact playback strip and destination-appropriate navigation. Desktop uses sidebar; narrow windows use compact navigation. Use platform sans fallback, never bundle Apple's font without permission.

First vertical slice is local library/import/search, queue and transport. Do not populate Home, Radio, Spotify or cloud tabs with fabricated content or nonfunctional actions.

## Decision ledger
| Decision | Source | Role / limitation |
|---|---|---|
| Sidebar on desktop | APPLE-003 | Library/search grouping, adapted dimensions pending full reference |
| Compact player opens expanded player | APPLE-001/002 | Real shared playback controls |
| Artwork with title/artist and timeline | APPLE-001/002 | Local filenames until metadata available; original note vector when artwork absent |
| Light/dark neutral surfaces with red accent | APPLE-001 | Independently chosen token approximations; not measured color parity |
| Original geometric glyphs initially | License blocker B-003 | No unlicensed SF Symbols assets shipped |
| Native local file picker | Platform API requirement | Desktop adaptation |

Current implementation values are proposals, not measurements. No blur or motion fidelity may be claimed from these stills.

## First-slice deviations and omissions
Current local-file views are an adaptation of the documented patterns. They have no album-art extraction, artwork background, five-destination navigation, glass refraction/blur, shared-element morph or measured Apple animation. Only implemented Library/Search destinations are exposed. Full-height Now Playing currently uses a vector fallback and compact transport. This is incomplete work, not a fidelity claim or permission to omit remaining requirements.
