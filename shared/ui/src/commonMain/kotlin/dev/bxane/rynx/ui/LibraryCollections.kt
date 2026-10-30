package dev.bxane.rynx.ui

/** Collection groupings use real local tags; missing albums are not invented. */
internal data class LibraryAlbum(
    val title: String,
    val artist: String,
    val tracks: List<TrackItem>,
) {
    val artworkUrl: String? get() = tracks.firstNotNullOfOrNull(TrackItem::artworkUrl)
}

internal data class LibraryArtist(
    val name: String,
    val tracks: List<TrackItem>,
    val albums: List<LibraryAlbum>,
) {
    val artworkUrl: String? get() = tracks.firstNotNullOfOrNull(TrackItem::artworkUrl)
}

internal fun libraryAlbums(tracks: List<TrackItem>): List<LibraryAlbum> {
    val grouped = tracks.distinctBy(TrackItem::id).mapNotNull { track ->
        val album = track.albumTitle?.trim()?.takeIf(String::isNotEmpty) ?: return@mapNotNull null
        val artist = track.subtitle.trim().takeUnless { it.isEmpty() || it == "Local audio" || it == "Unknown artist" }
            ?: "Unknown Artist"
        (artist to album) to track
    }.groupBy({ it.first }, { it.second })
    return grouped.map { (key, songs) -> LibraryAlbum(key.second, key.first, songs) }
        .sortedWith(compareBy<LibraryAlbum>({ it.title.lowercase() }, { it.artist.lowercase() }))
}

internal fun libraryArtists(tracks: List<TrackItem>): List<LibraryArtist> {
    val distinct = tracks.distinctBy(TrackItem::id)
    val albums = libraryAlbums(distinct)
    return distinct.filter { track ->
        val name = track.subtitle.trim()
        name.isNotEmpty() && name != "Unknown artist" && name != "Local audio"
    }.groupBy { it.subtitle.trim() }
        .map { (artist, songs) ->
            LibraryArtist(artist, songs, albums.filter { it.artist == artist })
        }
        .sortedBy { it.name.lowercase() }
}
