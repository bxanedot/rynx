# VyonEx integration
Pinned commit: 239a8d056c90b2101bf04de27a639d591acc69a3. Approved engine only; no Rynx extractor exists.
Verified documentation API names: VyonEx(host), init(), extract(videoId, maxKbps), extractVideo(videoId, maxHeight), parse(url), metadata(videoId), formats(videoId), download(format, headers, sink), onRefused(url), playlist/playlistMore, album, artist, mix/mixMore, related/relatedMore, search/searchMusic/searchMore, manifest, captionText, chapters, close.
These names were read in the approved library README; Rynx adapters are not yet implemented or compile checked.
Adapter acceptance: typed ID mapping; nullable metadata; headers preserved with URLs; cancellation propagated; exceptions classified without leaking URLs/tokens; refusal informs engine; retry bounded; pagination preserves continuation; resume verifies server range/integrity; expired streams re-resolved.
Use library public declarations before writing adapter signatures. Do not reuse its sample app queue or controller. No live extraction test run.
