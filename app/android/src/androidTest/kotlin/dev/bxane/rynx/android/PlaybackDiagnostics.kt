package dev.bxane.rynx.android

import androidx.media3.session.MediaController
import androidx.test.platform.app.InstrumentationRegistry

internal fun playbackDiagnostics(controller:MediaController):String {
    var result=""
    InstrumentationRegistry.getInstrumentation().runOnMainSync {
        val commands=controller.availableCommands
        result="connected=${controller.isConnected}, state=${controller.playbackState}, playWhenReady=${controller.playWhenReady}, playing=${controller.isPlaying}, count=${controller.mediaItemCount}, position=${controller.currentPosition}, duration=${controller.duration}, commands=${(0 until commands.size()).map {commands.get(it)}}, error=${controller.playerError?.errorCodeName}, cause=${controller.playerError?.cause?.javaClass?.simpleName}"
    }
    return result
}
