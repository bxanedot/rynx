package dev.bxane.rynx.vyonex

import com.vyonex.VyonExUnplayableException
import com.vyonex.VyonExUnplayableReason
import kotlinx.coroutines.CancellationException
import java.io.IOException

/** Stable, URL-free failures for application state and user-facing copy. */
sealed interface YouTubePlaybackFailure {
    data object AgeRestricted : YouTubePlaybackFailure
    data object SignInRequired : YouTubePlaybackFailure
    data object VideoUnavailable : YouTubePlaybackFailure
    data object StreamNotStarted : YouTubePlaybackFailure
    data object UnavailableInRegion : YouTubePlaybackFailure
    data object ProviderRefused : YouTubePlaybackFailure
    data object NetworkUnavailable : YouTubePlaybackFailure
    data object Unknown : YouTubePlaybackFailure
}

object VyonExFailureMapper {
    fun classify(error: Throwable): YouTubePlaybackFailure {
        if (error is CancellationException) throw error
        return when (error) {
            is VyonExUnplayableException -> when (error.reason) {
                VyonExUnplayableReason.AGE_RESTRICTED -> YouTubePlaybackFailure.AgeRestricted
                VyonExUnplayableReason.SIGN_IN_REQUIRED -> YouTubePlaybackFailure.SignInRequired
                VyonExUnplayableReason.UNAVAILABLE -> YouTubePlaybackFailure.VideoUnavailable
                VyonExUnplayableReason.UPCOMING -> YouTubePlaybackFailure.StreamNotStarted
                VyonExUnplayableReason.GEO_RESTRICTED -> YouTubePlaybackFailure.UnavailableInRegion
                VyonExUnplayableReason.OTHER -> YouTubePlaybackFailure.ProviderRefused
            }
            is IOException -> YouTubePlaybackFailure.NetworkUnavailable
            else -> YouTubePlaybackFailure.Unknown
        }
    }
}
