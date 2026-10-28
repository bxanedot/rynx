package dev.bxane.rynx.android

import kotlinx.coroutines.CancellationException

/** Uses the primary extractor first and consults the permitted fallback only when needed. */
internal class AudioResolutionChain<T : Any>(
    private val primary: suspend (videoId: String) -> T?,
    private val fallback: suspend (videoId: String) -> T?,
) {
    suspend fun resolve(videoId: String): T? {
        val primaryResult = try {
            primary(videoId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
        return primaryResult ?: fallback(videoId)
    }
}
