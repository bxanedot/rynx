package dev.bxane.rynx.desktop

import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import dev.bxane.rynx.ui.*
import dev.bxane.rynx.excor.createLoggedOutExcorCatalog
import java.awt.FileDialog
import javax.swing.SwingUtilities

fun main() = application {
    val player=remember { DesktopPlayer(catalogSearch=createLoggedOutExcorCatalog()) }
    DisposableEffect(player) { onDispose { player.close() } }
    val state by player.state.collectAsState()
    Window(onCloseRequest=::exitApplication,title="rynx",state=rememberWindowState(width=1120.dp,height=760.dp)) {
        val actions=remember(player) { object:PlayerActions {
            override fun importFiles() {
                SwingUtilities.invokeLater {
                    FileDialog(window,"Add music",FileDialog.LOAD).apply {
                        isMultipleMode=true
                        setFilenameFilter { _,name -> name.substringAfterLast('.').lowercase() in setOf("wav","wave","aif","aiff","au") }
                        isVisible=true
                        val selected=files.toList()
                        dispose()
                        if(selected.isNotEmpty()) player.import(selected)
                    }
                }
            }
            override fun play(id:String) = player.play(id)
            override fun toggle() = player.toggle()
            override fun previous() = player.previous()
            override fun next() = player.next()
            override fun seek(fraction:Float) = player.seek(fraction)
            override fun cycleRepeat() = player.cycleRepeat()
            override fun removeFromQueue(id:String) = player.remove(id)
            override fun dismissError() = player.dismissError()
            override fun searchYouTubeMusic(query:String) = player.searchYouTubeMusic(query)
            override fun loadMoreYouTubeMusic() = player.loadMoreYouTubeMusic()
        } }
        RynxApp(state,actions)
    }
}
