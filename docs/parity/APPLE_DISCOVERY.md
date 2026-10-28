# Apple-first discovery

## Working code

- The approved VyonEx `appleMusic` API supplies keyless Apple song search, typed song lookup and cover URLs through Rynx adapters. Artist-image support is exposed by the adapter. No Apple audio/preview URL becomes a playback source.
- Search first requests Apple metadata. Empty/error results fall back to VyonEx YouTube Music metadata. Choosing an Apple song conservatively matches title, artist, recording variants and available duration against YouTube Music before stream resolution. A mismatch reports an error rather than playing an arbitrary first result.
- Shared `RynxRecommendations` is independently authored local ranking: decayed artist/genre affinity, chart popularity, release freshness, known favorites input, replay penalties and artist diversity. The Media3 service records persisted listening signals after 30 seconds of advancing playback, independently of Activity publication; pauses and seeks do not count as listening, and repeats create new sessions; favorite/skip training is not yet wired. No private Apple recommendation algorithm or account history is used.
- Home includes Made for you, local Recently Played, global Daily 100, regional/nearby storefront 100 snapshots, Best new songs from New Music Daily, a release-date-filtered New this week, rising-rank Trending, City Charts and current Apple tour set-list collections. Country snapshots are explicitly distinguished from Apple's exact daily playlist rankings. Trending requires a prior daily snapshot; tour playlists do not represent concert dates.
- Explore loads actual Apple Browse by Genre curator categories, with the canonical genre taxonomy as fallback. Curator IDs and genre IDs remain separate. Category detail opens real Apple genre collections; taxonomy fallback loads a labelled iTunes genre chart. Square artwork geometry, captions, spacing and sampled colors follow the inspected public mobile-web reference; tile illustrations are original vectors.
- City/tour lists are actual visible Apple playlist links. Opening a collection parses only public JSON-LD and enriches song artists by public lookup, preserving rank by ID. These pages are not a stable documented API and may become unavailable.
- Region is persisted and user-selectable. Visible discovery refreshes across UTC days; failed requests back off for five minutes. Neighbors are Rynx-authored geographic choices. Unsupported country feeds report unavailable. Apple failures can populate discovery with explicitly labelled YouTube Music suggestions, while unavailable charts stay unavailable.
- Metadata documents have bounded size/concurrency/timeouts, private disk caching and HTTP cache-directive handling. Must-revalidate responses are not silently reused after failures. Cancellation never becomes stale success.

## Evidence

Opt-in live checks on 2026-10-28 passed for VyonEx Apple search, typed lookup and cover artwork, plus 51 genres, 100 US storefront songs, 100 Global playlist songs, 50 city links and 50 Pop genre-chart songs. Live RSS initially returned a transient 502; a subsequent request passed. Deterministic parser/ranking/matching/UI tests remain the default CI contract, with live checks enabled explicitly using `RYNX_APPLE_LIVE=1`.

No 95% visual score, exact Apple recommendations, complete Apple catalog, nearby concert-date service, native playback acceptance, Spotify completion or production readiness is claimed. Artwork availability from an API is separate from artwork/service usage terms; release auditing remains required. See `docs/cleanroom/APPLE_CATALOG_EVIDENCE.md` for official sources and constraints.

## Decisions

“Next this week” is interpreted as New this week, derived from real release dates; no future releases are fabricated. Region/neighbor country charts use real Apple storefront feeds, not a synthetic global aggregate. Original genre graphics avoid copying Apple editorial imagery. The public JSON-LD adapter does not inspect application script state or acquire web bearer tokens.
