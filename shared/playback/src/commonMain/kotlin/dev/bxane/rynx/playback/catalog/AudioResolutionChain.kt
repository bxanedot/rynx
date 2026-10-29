package dev.bxane.rynx.playback.catalog

import kotlin.coroutines.cancellation.CancellationException

/** Tries providers in priority order and returns the first usable audio stream. */
class AudioResolutionChain(
    private val primary: AudioStreamResolver,
    private val fallback: AudioStreamResolver,
    vararg additionalFallbacks: AudioStreamResolver,
) : AudioStreamResolver {
    private val resolvers = listOf(primary, fallback) + additionalFallbacks

    override suspend fun resolveAudio(videoId: String): ResolvedAudioStream? {
        for (resolver in resolvers) {
            val result = try {
                resolver.resolveAudio(videoId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (refused: ProviderAccessRefusalException) {
                throw refused
            } catch (_: Exception) {
                null
            }
            if (result != null) return result
        }
        return null
    }
}
