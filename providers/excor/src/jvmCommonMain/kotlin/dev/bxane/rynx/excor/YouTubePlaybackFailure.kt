package dev.bxane.rynx.excor

import com.bxane.excor.ExcorUnplayableException
import com.bxane.excor.ExcorUnplayableReason
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

object ExcorFailureMapper {
    fun classify(error: Throwable): YouTubePlaybackFailure {
        if (error is CancellationException) throw error
        return when (error) {
            is ExcorUnplayableException -> when (error.reason) {
                ExcorUnplayableReason.AGE_RESTRICTED -> YouTubePlaybackFailure.AgeRestricted
                ExcorUnplayableReason.SIGN_IN_REQUIRED -> YouTubePlaybackFailure.SignInRequired
                ExcorUnplayableReason.UNAVAILABLE -> YouTubePlaybackFailure.VideoUnavailable
                ExcorUnplayableReason.UPCOMING -> YouTubePlaybackFailure.StreamNotStarted
                ExcorUnplayableReason.GEO_RESTRICTED -> YouTubePlaybackFailure.UnavailableInRegion
                ExcorUnplayableReason.OTHER -> YouTubePlaybackFailure.ProviderRefused
            }
            is IOException -> YouTubePlaybackFailure.NetworkUnavailable
            else -> YouTubePlaybackFailure.Unknown
        }
    }
}
