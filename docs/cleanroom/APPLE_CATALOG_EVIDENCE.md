# Rynx Apple catalog research

Observed 2026-10-28 (UTC), unauthenticated HTTPS requests unless explicitly stated. Sources were official Apple documentation and public Apple pages/data only. No Vyon, Convx, BitChord, or Vivi Music code, app JavaScript implementation, private application state, developer-token extraction, or private protocols were read. No production files or builds were touched.

## Implementable keyless sources

| Source | Observed result | Appropriate label / limitation |
| --- | --- | --- |
| `https://itunes.apple.com/search?term=Jack%20Johnson&country=US&media=music&entity=song&limit=2` | 200; `resultCount:2` | iTunes catalog search; not complete Apple Music catalog |
| `https://itunes.apple.com/lookup?id=909253&entity=album&country=US&limit=2` | 200; 3 results: artist then two albums | Parent resource can precede children; match IDs/types |
| `https://itunes.apple.com/lookup?id=6822497774,6796864754,6818209990&country=US` | 200; all three songs with artist names | Practical batch enrichment for public playlist JSON-LD |
| `https://itunes.apple.com/WebObjects/MZStoreServices.woa/ws/genres?id=34&cc=us` | 200; Music root 34, 51 immediate children, nested subgenres | Legacy public data; no current official endpoint support contract found |
| `https://rss.applemarketingtools.com/api/v2/us/music/most-played/100/songs.json` | 200; 100 results | Storefront Top Songs; not interchangeable with exact Top 100: USA playlist |
| Same RSS path with `in` | 200; 100 results | India storefront Top Songs |
| `https://itunes.apple.com/us/rss/topsongs/limit=50/genre=14/json` | 200; 50 entries; title “iTunes Store: Top Songs in Pop” | Genuine genre-specific **iTunes** chart; not an Apple Music streaming chart |

RSS v2 response self-links use the canonical host `rss.marketingtools.apple.com`. The old host above works. Official generator: https://rss.marketingtools.apple.com/. Its rendered interface offered 10/25/50 limits, while the tested existing 100-result URL returned 100. Do not infer other feeds such as `new-releases`, a `global` country, or future-release feeds.

Minimal schemas:

- Search/lookup: `{resultCount,results:[{wrapperType,kind,artistId,collectionId,trackId,artistName,collectionName,trackName,releaseDate,trackTimeMillis,primaryGenreName,trackViewUrl,...}]}`. Search returned artwork/preview URLs too; ignore those for the agreed implementation. No ISRC was present in tested iTunes results. Keep IDs as strings in domain contracts. Artist/album/song lookups return different shapes.
- Genres: `{"34":{id:"34",name:"Music",url,rssUrls,chartUrls,subgenres:{"14":{id:"14",name:"Pop",...,subgenres:{...}},...}}}`. There are no genre tile images in this response. Do not assume its legacy `chartUrls` are supported APIs.
- RSS v2: `{feed:{title,country,updated,copyright,results:[{id,name,artistName,artistId,artistUrl,releaseDate,kind:"songs",genres:[{genreId,name,url}],url,artworkUrl100}]}}`. Ordered results give rank `index+1`. There is no explicit rank, duration, or ISRC.
- Legacy genre RSS: `{feed:{title:{label},updated:{label},rights,entry:[{"im:name":{label},"im:artist":{label,attributes:{href}},id:{label,attributes:{"im:id":string}},category:{attributes:{"im:id":string,label,term,scheme}},"im:collection":{"im:name":{label}},"im:releaseDate":{label},link:[{attributes:{rel:"alternate",href}},...]}]}}`. Use `im:id` and the alternate link; skip `im:image` and enclosure/preview links. Rank is array order. First observed Pop row: Babydoll, Jamie Miller & Antonio Cipriano, song ID 6812888327.

iTunes documentation specifies limits 1–200, approximately 20 requests/minute (subject to change), and recommends metadata caching for larger sites. Search/lookup replies had `Cache-Control:max-age=86400`. RSS v2 replies had `max-age=0, private, must-revalidate`; follow origin HTTP caching rules. Source: https://performance-partners.apple.com/search-api (the archived developer.apple.com Search API URL redirects here).

## Exact Daily 100, city charts, and editorial collections

Ordinary GETs to these playlist pages returned 200 and public `<script type="application/ld+json">`:

| Collection | Verified playlist ID | Observed JSON-LD |
| --- | --- | --- |
| Top 100: Global | `pl.d25f5d1181894928af76c85c967f8f31` | MusicPlaylist, 100 tracks |
| Top 100: USA | `pl.606afcbb70264d2eb2b51d8dbcfa6a12` | MusicPlaylist, 100 tracks |
| Top 25: New York City | `pl.a88b5c26caea48a59484370b6f79c9df` | MusicPlaylist, 25 tracks |
| New Music Daily | `pl.2b0e6e332fdf4b7a91164da3162127b5` | MusicPlaylist, 100 tracks |

URLs follow the **observed** public links: https://music.apple.com/us/playlist/top-100-global/pl.d25f5d1181894928af76c85c967f8f31 , https://music.apple.com/us/playlist/top-100-usa/pl.606afcbb70264d2eb2b51d8dbcfa6a12 , https://music.apple.com/us/playlist/top-25-new-york-city/pl.a88b5c26caea48a59484370b6f79c9df , https://music.apple.com/us/playlist/new-music-daily/pl.2b0e6e332fdf4b7a91164da3162127b5 .

The structured schema is `MusicPlaylist{name,description,datePublished,numTracks,author,track:[MusicRecording{name,url,duration,audio:{thumbnailUrl,...}}],url}`. Song ID is the final numeric component of `track.url`; duration is ISO 8601; array order matched visible ranking. **No artist/byArtist is supplied on these JSON-LD tracks.** Enrich by ID via lookup, preserve original order with an ID map, and allow missing lookups/catalog availability. No Apple audio URL is needed. Do not interpret schema.org “free” offers as a content license or permission for full playback.

The optional public adapter can select only JSON-LD and ordinary semantic anchors/text. Do not read other script bodies or serialized app state. These are publicly accessible structured pages, with **no published API stability contract**. Return partial/unavailable on schema changes; do not silently invent song artists or replace an exact chart with a differently ranked feed.

Public collection discovery:

- https://music.apple.com/us/new/top-charts/city-charts returned 200, 50 unique city playlist links in this page response (100 duplicated semantic anchors). This is not an exhaustive city catalog. Delhi: `pl.8f35027eb4f5434691799f18390d885f`; Bengaluru: `pl.d77516976c03485186561deba65ef931`.
- https://music.apple.com/in/new/top-charts visibly links Top 100: India `pl.c0e98d2423e54c39b3df955c24df3cc5`.
- https://music.apple.com/us/new returned 200 with real semantic playlist anchors, including New Music Daily and tour Set Lists. Raw HTML anchor counts vary from rendered page content; treat initial response as a partial collection.
- https://music.apple.com/us/room/6456176470 returned 200: Browse by Genre, with 97 unique public curator links observed. This is a reference for original genre tile categories, **not** a source of reusable artwork. Curator IDs (e.g. Alternative 976439526, Bollywood 982307152, Electronic 976439536) are not music genre IDs. It includes editorial categories such as Alpha Women and Behind the Songs.
- Captured public city, genre-curator, and editorial playlist links are in `rynx-apple-public-links.json` alongside this report.

RSS US and the actual USA playlist differed in the same research window: first three RSS songs were Housewife / BbY WOW / Choosin’ Texas; playlist order was Housewife / Choosin’ Texas / BbY WOW. Only 13/100 positions matched. Preserve source labels rather than equating charts.

## Official Apple Music API with the app's own developer token

https://developer.apple.com/documentation/applemusicapi/generating-developer-tokens states **every Apple Music API request requires a developer token**, signed ES256 with the app developer’s MusicKit private key and issued through Apple Developer Program membership. Maximum token lifetime is about six months. Keep the signing private key off the APK. A direct request to `https://api.music.apple.com/v1/catalog/us/charts?types=songs&limit=1` without Authorization returned **401**, empty body.

Documented capabilities suitable for an optional authorized provider:

- Catalog search, songs/albums/artists/playlists and relationships: developer token; no Music User Token needed for public catalog.
- `GET /v1/catalog/{storefront}/genres`: top-chart genres; https://developer.apple.com/documentation/applemusicapi/get-all-genres .
- `GET /v1/catalog/{storefront}/charts`: supports `types`, `genre`, `limit`, `offset`, and documented `with=cityCharts` or `with=globalTopCharts`. Discover actual `chart` identifiers by omitting `chart`; do not guess names. https://developer.apple.com/documentation/applemusicapi/charts .
- `GET /v1/catalog/{storefront}/playlists?filter[storefront-chart]=...`: documented chart-playlist discovery; filter values should come from official usage, not guesses. https://developer.apple.com/documentation/applemusicapi/get-charts-playlists-by-storefront-value .
- Personal recommendations `/v1/me/recommendations` and recent-history endpoints require **both developer token and authorized Music User Token**. https://developer.apple.com/documentation/applemusicapi/get-all-recommendations ; https://developer.apple.com/documentation/applemusicapi/history ; https://developer.apple.com/documentation/applemusicapi/user-authentication-for-musickit .
- Apple offers MusicKit for Android sign-in/playback libraries; full Apple Music playback involves authorized subscription capabilities. The general REST API can back shared KMP metadata contracts. https://developer.apple.com/musickit/ .
- Apple Music Feed is a separate rich-catalog offering and also requires developer credentials; it is not a free keyless replacement. https://developer.apple.com/documentation/applemusicfeed/ .

Do not use or extract Apple's web bearer tokens.

## What the planned Home sections can honestly represent

- Daily 100: actual public playlist JSON-LD where available; RSS is a separately labelled storefront chart. “Neighbors” is Rynx’s own geographic country selection, not an Apple neighboring-country endpoint. Some countries/catalog items may be unavailable.
- Best new songs: Apple’s genuine New Music Daily is an editorial candidate source; Rynx can independently rank candidates. Do not attribute Rynx’s ranking to Apple.
- New this week: a Rynx date filter over real `releaseDate` metadata is possible, with incomplete candidate coverage. No verified universal keyless Apple “New This Week” or future releases feed was found. Exact-title search only identified third-party curated playlists (Topsify/Gospel Music Association), not Apple editorial.
- Recently played: use Rynx’s local playback events. Apple account listening history is unavailable keylessly.
- Trending: most-played is popularity, not measured growth. Rynx may compute its own deltas from permitted factual snapshots and clearly label the result.
- Recommendations: use Rynx play/skip/favorite history, genre affinity, recency, popularity and diversity locally. Apple’s personalized recommendations are not available without user authorization.
- New on tour: public Set List collection cards can link/open real playlists. Examples verified on /us/new: Bob Dylan’s Lights of Autumn Tour `pl.21aac669a76d440190386782d8a2f543`; SIENNA SPIRO’s My House Tour `pl.2084f36ef22347d58ddb1489788c84a0`. Playlist presence does not prove dates, ticket availability, or a nearby upcoming show.
- Genuine concert destination: https://music.apple.com/us/concerts returned 200 and visible artist/venue/date links, with default results dependent on fetch location. No documented public Apple Music concert API was found in the current API reference. Apple says dates originate from Bandsintown: https://artists.apple.com/5605-concerts-on-apple-music . Linking to Apple concert discovery is supportable; do not invent events or use its internal calls.

## Artwork and attribution boundary

The official iTunes Search API terms explicitly constrain promotional content including album art and previews to promoting the referenced store content, proximate approved store badges/direct purchase links; previews also require “provided courtesy of iTunes,” streaming only (no downloading/caching), no independent entertainment use, and no promotion of other goods/services. The RSS feed contains Apple copyright/all-rights-reserved text and did not establish a broader artwork license.

Apple Music’s identity guidelines explain links and badges (and explicitly allow multiple music-service badges in marketing), but do **not** establish that a “View in Apple Music” link alone licenses RSS/iTunes art decorating playback from another service: https://www.apple.com/itunes/marketing-on-music/identity-guidelines.html . The initial research proposed factual metadata and original vector tiles/placeholders. The user then explicitly requested real cover artwork. Current Rynx parses Apple cover URLs directly from public iTunes/RSS metadata and normalizes standard small-image size tokens to request a larger variant; catalog cards and playback metadata can use those URLs with Apple source/deep links. API availability is not artwork-usage permission, and this change does not establish a completed service/artwork release audit. No iTunes preview audio is used or cached, and Rynx does not imply Apple endorsement.

Raw observation files in scratch include `apple-api-observations.json`, `apple-legacy-genre-chart.json`, and `apple-public-page-*.json` (public JSON-LD only, not application state).



Implementation boundary: current selected-song search, lookup and image parsing are Rynx-owned KMP code over Apple's public iTunes API; chart, genre, playlist and RSS parsing remain public-data adapters. The Excor-based implementation used in an earlier branch is archived and is not active; see `../archive/EXCOR.md`. Apple audio/preview URLs never enter playback resolution.
