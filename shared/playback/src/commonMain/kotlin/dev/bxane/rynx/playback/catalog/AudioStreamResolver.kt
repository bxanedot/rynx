package dev.bxane.rynx.playback.catalog

/** Platform/provider adapter for resolving an ephemeral stream from a catalog media ID. */
fun interface AudioStreamResolver {
    suspend fun resolveAudio(videoId: String): ResolvedAudioStream?
}
