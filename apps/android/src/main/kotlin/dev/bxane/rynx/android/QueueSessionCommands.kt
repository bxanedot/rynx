package dev.bxane.rynx.android

import android.os.Bundle
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import dev.bxane.rynx.playback.moveUpcomingQueueEntry
import dev.bxane.rynx.playback.normalizedPlaybackOrder

internal object QueueSessionCommands {
    val Move = SessionCommand("dev.bxane.rynx.queue.MOVE", Bundle.EMPTY)
    val Clear = SessionCommand("dev.bxane.rynx.queue.CLEAR_UPCOMING", Bundle.EMPTY)
    const val Source = "source"
    const val Destination = "destination"
}

/** Timeline indexes remain stable while shuffle changes their playback order. */
internal fun playbackOrder(timeline: Timeline, shuffle: Boolean): List<Int> {
    val order = ArrayList<Int>(timeline.windowCount)
    var next = timeline.getFirstWindowIndex(shuffle)
    while (next != C.INDEX_UNSET && order.size < timeline.windowCount && next !in order) {
        order += next
        next = timeline.getNextWindowIndex(next, Player.REPEAT_MODE_OFF, shuffle)
    }
    return normalizedPlaybackOrder(timeline.windowCount, order)
}

/** Queue editing stays in the service, where ExoPlayer's real shuffle permutation is available. */
@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
internal class QueueSessionCallback(private val packageName: String) : MediaSession.Callback {
    private fun ownsController(controller:MediaSession.ControllerInfo)=
        controller.packageName==packageName && controller.uid==android.os.Process.myUid()

    override fun onConnectAsync(session:MediaSession,controller:MediaSession.ControllerInfo):com.google.common.util.concurrent.ListenableFuture<MediaSession.ConnectionResult> {
        val result=MediaSession.ConnectionResult.AcceptedResultBuilder(session,controller)
        if(ownsController(controller)) {
            result.setAvailableSessionCommands(MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(QueueSessionCommands.Move).add(QueueSessionCommands.Clear).build())
                .setAvailablePlayerCommands(MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS)
        }
        return Futures.immediateFuture(result.build())
    }

    override fun onCustomCommand(session: MediaSession, controller: MediaSession.ControllerInfo, command: SessionCommand, args: Bundle): com.google.common.util.concurrent.ListenableFuture<SessionResult> {
        if (command != QueueSessionCommands.Move && command != QueueSessionCommands.Clear) return super.onCustomCommand(session, controller, command, args)
        if (!ownsController(controller)) return Futures.immediateFuture(SessionResult(SessionError.ERROR_PERMISSION_DENIED))
        val player = session.player as? ExoPlayer
            ?: return Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
        if (command == QueueSessionCommands.Clear) {
            val order = playbackOrder(player.currentTimeline, player.shuffleModeEnabled)
            val current = order.indexOf(player.currentMediaItemIndex)
            if (current < 0) return Futures.immediateFuture(SessionResult(SessionError.ERROR_INVALID_STATE))
            order.drop(current + 1).sortedDescending().forEach(player::removeMediaItem)
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
        val sourceId = args.getString(QueueSessionCommands.Source)
        val destinationId = args.getString(QueueSessionCommands.Destination)
        val source = (0 until player.mediaItemCount).firstOrNull { player.getMediaItemAt(it).mediaId == sourceId }
        val destination = (0 until player.mediaItemCount).firstOrNull { player.getMediaItemAt(it).mediaId == destinationId }
        val order = if (source != null && destination != null) moveUpcomingQueueEntry(
            playbackOrder(player.currentTimeline, player.shuffleModeEnabled), player.currentMediaItemIndex, source, destination,
        ) else null
        if (order == null) return Futures.immediateFuture(SessionResult(SessionError.ERROR_BAD_VALUE))
        if (player.shuffleModeEnabled) player.setShuffleOrder(DefaultShuffleOrder(order.toIntArray(), System.nanoTime()))
        else player.moveMediaItem(source!!, destination!!)
        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
    }
}
