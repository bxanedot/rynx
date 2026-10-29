package dev.bxane.rynx.playback.catalog

import kotlin.coroutines.cancellation.CancellationException

/** Uses the primary provider first and consults the user-approved fallback only when needed. */
class AudioResolutionChain(
    private val primary: AudioStreamResolver,
    private val fallback: AudioStreamResolver,
) : AudioStreamResolver {
    override suspend fun resolveAudio(videoId: String): ResolvedAudioStream? {
        val primaryResult = try {
            primary.resolveAudio(videoId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
        return primaryResult ?: fallback.resolveAudio(videoId)
    }
}
