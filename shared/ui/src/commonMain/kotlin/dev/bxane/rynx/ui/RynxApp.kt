package dev.bxane.rynx.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*

private data class Palette(val bg: Color, val surface: Color, val text: Color, val secondary: Color, val line: Color)
private val Accent = Color(0xFFFA2D48)
private val LocalPalette = staticCompositionLocalOf { Palette(Color.White,Color(0xFFF5F5F7),Color(0xFF18181A),Color(0xFF747478),Color(0xFFE5E5E8)) }

@Composable private fun Label(text: String, size: Int = 14, color: Color = LocalPalette.current.text, bold: Boolean = false, modifier: Modifier = Modifier) {
    BasicText(text, modifier, style=TextStyle(color=color,fontSize=size.sp,fontFamily=FontFamily.SansSerif,fontWeight=if(bold) FontWeight.SemiBold else FontWeight.Normal), maxLines=1, overflow=TextOverflow.Ellipsis)
}

@Composable private fun IconButton(glyph: Glyph, description: String, enabled: Boolean = true, tint: Color = LocalPalette.current.text, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).clickable(enabled=enabled,role=Role.Button,onClick=onClick).semantics{contentDescription=description},contentAlignment=Alignment.Center) {
        RynxGlyph(glyph,if(enabled) tint else tint.copy(alpha=.28f))
    }
}

@Composable private fun Action(text: String, action: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(9.dp)).background(Accent).clickable(role=Role.Button,onClick=action).padding(horizontal=18.dp,vertical=11.dp)) { Label(text,color=Color.White,bold=true) }
}

@Composable private fun Artwork(size: Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(RoundedCornerShape(size*.09f)).background(LocalPalette.current.line),contentAlignment=Alignment.Center) {
        RynxGlyph(Glyph.Note,LocalPalette.current.secondary,Modifier.size(size*.38f))
    }
}

/** Apple Music reference patterns; dimensions are independently selected, not measured parity. */
@Composable
fun RynxApp(state: PlayerUiState, actions: PlayerActions) {
    var dark by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    val palette = if(dark) Palette(Color(0xFF171719),Color(0xFF222225),Color(0xFFF5F5F7),Color(0xFFA4A4AA),Color(0xFF353538)) else LocalPalette.current
    CompositionLocalProvider(LocalPalette provides palette) {
        BoxWithConstraints(Modifier.fillMaxSize().background(palette.bg)) {
            val wide = maxWidth >= 760.dp
            Row(Modifier.fillMaxSize()) {
                if(wide) Column(Modifier.width(216.dp).fillMaxHeight().background(palette.surface).padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Label("rynx",28,bold=true,modifier=Modifier.padding(bottom=30.dp))
                    NavItem("Search",Glyph.Search,searching){searching=true}
                    Label("LIBRARY",11,palette.secondary,bold=true,modifier=Modifier.padding(top=28.dp,bottom=4.dp))
                    NavItem("Songs",Glyph.Library,!searching){searching=false;query=""}
                    Spacer(Modifier.weight(1f))
                    Label("by bxane",12,palette.secondary)
                }
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    if(wide && state.current!=null) CompactPlayer(state,actions,{expanded=true},{showQueue=!showQueue})
                    Row(Modifier.fillMaxWidth().padding(horizontal=if(wide)32.dp else 24.dp,vertical=20.dp),verticalAlignment=Alignment.CenterVertically) {
                        Label(if(searching) "Search" else "Library",32,bold=true,modifier=Modifier.weight(1f))
                        IconButton(if(dark) Glyph.Sun else Glyph.Moon,"Switch to ${if(dark) "light" else "dark"} theme"){dark=!dark}
                        IconButton(Glyph.Add,"Import audio files",onClick=actions::importFiles)
                    }
                    if(searching) Row(Modifier.padding(horizontal=24.dp).fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(palette.surface).padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
                        RynxGlyph(Glyph.Search,palette.secondary)
                        Box(Modifier.padding(start=10.dp).weight(1f)) {
                            if(query.isEmpty()) Label("Search your library",color=palette.secondary)
                            BasicTextField(query,{query=it},Modifier.fillMaxWidth().semantics{contentDescription="Search your library"},singleLine=true,textStyle=TextStyle(color=palette.text,fontSize=15.sp))
                        }
                        if(query.isNotEmpty()) IconButton(Glyph.Close,"Clear search"){query=""}
                    }
                    if(state.error!=null) Row(Modifier.fillMaxWidth().background(Accent.copy(alpha=.1f)).padding(horizontal=20.dp),verticalAlignment=Alignment.CenterVertically) {
                        BasicText(state.error,Modifier.weight(1f),style=TextStyle(color=palette.text,fontSize=13.sp))
                        IconButton(Glyph.Close,"Dismiss error",onClick=actions::dismissError)
                    }
                    if(state.busy) Label("Opening audio…",color=palette.secondary,modifier=Modifier.padding(24.dp))
                    val visible = state.tracks.filter { it.title.contains(query,ignoreCase=true) }
                    if(state.tracks.isEmpty()) Column(Modifier.weight(1f).fillMaxWidth().padding(32.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally) {
                        RynxGlyph(Glyph.Library,palette.secondary,Modifier.size(52.dp))
                        Spacer(Modifier.height(18.dp));Label("Your music, right here",24,bold=true)
                        Spacer(Modifier.height(10.dp));Label("Add WAV, AIFF or AU files to your library.",color=palette.secondary)
                        Spacer(Modifier.height(24.dp));Action("Add music",actions::importFiles)
                    } else LazyColumn(Modifier.weight(1f).padding(horizontal=if(wide)32.dp else 20.dp),contentPadding=PaddingValues(vertical=20.dp)) {
                        item { Row(Modifier.fillMaxWidth().padding(bottom=14.dp)) { Label(if(searching) "${visible.size} results" else "Songs",20,bold=true);Spacer(Modifier.weight(1f));Label("${state.tracks.size} songs",color=palette.secondary) } }
                        items(visible,key={it.id}) { track -> TrackRow(track,state.currentId==track.id,state.playing,{actions.play(track.id)}) }
                        if(visible.isEmpty()) item { Label("No songs found",color=palette.secondary,modifier=Modifier.padding(vertical=24.dp)) }
                    }
                    if(!wide) {
                        if(state.current!=null) Box(Modifier.padding(horizontal=12.dp).clip(RoundedCornerShape(24.dp)).background(palette.surface)) { CompactPlayer(state,actions,{expanded=true},{showQueue=!showQueue}) }
                        Row(Modifier.fillMaxWidth().padding(12.dp).clip(RoundedCornerShape(30.dp)).background(palette.surface).padding(4.dp),horizontalArrangement=Arrangement.SpaceEvenly) {
                            NavItem("Library",Glyph.Library,!searching){searching=false;query=""}
                            NavItem("Search",Glyph.Search,searching){searching=true}
                        }
                    }
                }
                if(showQueue && wide) QueuePanel(state,actions,{showQueue=false},Modifier.width(280.dp).fillMaxHeight())
            }
            if(expanded) ExpandedPlayer(state,actions,{expanded=false},{showQueue=!showQueue})
            if(showQueue && (!wide || expanded)) QueuePanel(state,actions,{showQueue=false},Modifier.align(Alignment.CenterEnd).widthIn(max=360.dp).fillMaxSize())
        }
    }
}

@Composable private fun NavItem(text:String,glyph:Glyph,selected:Boolean,onClick:()->Unit) {
    val p=LocalPalette.current
    Row(Modifier.clip(RoundedCornerShape(10.dp)).background(if(selected) Accent.copy(alpha=.1f) else Color.Transparent).clickable(role=Role.Tab,onClick=onClick).semantics{this.selected=selected}.padding(horizontal=12.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
        RynxGlyph(glyph,if(selected) Accent else p.secondary);Label(text,color=if(selected) Accent else p.text,bold=selected)
    }
}

@Composable private fun TrackRow(track:TrackItem,selected:Boolean,playing:Boolean,onClick:()->Unit) {
    val p=LocalPalette.current
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if(selected) Accent.copy(alpha=.08f) else Color.Transparent).clickable(role=Role.Button,onClick=onClick).padding(10.dp),verticalAlignment=Alignment.CenterVertically) {
        Artwork(44.dp)
        Column(Modifier.padding(start=14.dp).weight(1f)) { Label(track.title,bold=selected,color=if(selected) Accent else p.text);Spacer(Modifier.height(4.dp));Label(track.subtitle,12,p.secondary) }
        if(selected) RynxGlyph(if(playing) Glyph.Pause else Glyph.Play,Accent)
    }
}

@Composable private fun CompactPlayer(state:PlayerUiState,actions:PlayerActions,open:()->Unit,queue:()->Unit) {
    val track=state.current?:return
    val p=LocalPalette.current
    Row(Modifier.fillMaxWidth().height(72.dp).background(p.surface).padding(horizontal=10.dp),verticalAlignment=Alignment.CenterVertically) {
        Row(Modifier.weight(1f).clickable(role=Role.Button,onClick=open).semantics{contentDescription="Open Now Playing"}.padding(4.dp),verticalAlignment=Alignment.CenterVertically) {
            Artwork(44.dp)
            Column(Modifier.padding(start=12.dp).weight(1f)){Label(track.title,bold=true);Label(track.subtitle,12,p.secondary)}
        }
        IconButton(if(state.playing) Glyph.Pause else Glyph.Play,if(state.playing) "Pause" else "Play",!state.busy,onClick=actions::toggle)
        IconButton(Glyph.Next,"Next track",!state.busy,onClick=actions::next)
        IconButton(Glyph.Queue,"Show queue",onClick=queue)
    }
}

@Composable private fun ExpandedPlayer(state:PlayerUiState,actions:PlayerActions,close:()->Unit,queue:()->Unit) {
    val p=LocalPalette.current
    Column(Modifier.fillMaxSize().background(p.bg).padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){IconButton(Glyph.Close,"Close Now Playing",onClick=close);Spacer(Modifier.weight(1f));Label("NOW PLAYING",12,p.secondary,bold=true);Spacer(Modifier.weight(1f));IconButton(Glyph.Queue,"Show queue",onClick=queue)}
        if(state.error!=null) BasicText(state.error,style=TextStyle(color=Accent,fontSize=14.sp))
        Column(Modifier.widthIn(max=440.dp).fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
            Artwork(240.dp);Spacer(Modifier.height(32.dp))
            Label(state.current?.title?:"Nothing playing",24,bold=true)
            Label(state.current?.subtitle?:"Choose a song from your library",17,p.secondary)
            Spacer(Modifier.height(28.dp))
            SeekBar(if(state.durationMillis>0) state.positionMillis.toFloat()/state.durationMillis else 0f,state.durationMillis>0,actions::seek)
            Row(Modifier.fillMaxWidth().padding(top=7.dp)){Label(time(state.positionMillis),12,p.secondary);Spacer(Modifier.weight(1f));Label("-${time((state.durationMillis-state.positionMillis).coerceAtLeast(0))}",12,p.secondary)}
            Row(Modifier.fillMaxWidth().padding(vertical=24.dp),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.CenterVertically){
                IconButton(Glyph.Previous,"Previous track",state.current!=null&&!state.busy,onClick=actions::previous)
                IconButton(if(state.playing) Glyph.Pause else Glyph.Play,if(state.playing) "Pause" else "Play",state.current!=null&&!state.busy,onClick=actions::toggle)
                IconButton(Glyph.Next,"Next track",state.current!=null&&!state.busy,onClick=actions::next)
            }
            Row(verticalAlignment=Alignment.CenterVertically){IconButton(Glyph.Repeat,"Change repeat mode, currently ${state.repeatLabel}",tint=if(state.repeatLabel=="Off") p.secondary else Accent,onClick=actions::cycleRepeat);Label("Repeat: ${state.repeatLabel}",13,p.secondary)}
        }
    }
}

@Composable private fun SeekBar(value:Float,enabled:Boolean,onSeek:(Float)->Unit) {
    val p=LocalPalette.current
    var width by remember{mutableFloatStateOf(1f)}
    Box(Modifier.fillMaxWidth().height(32.dp).onSizeChanged { width=it.width.toFloat().coerceAtLeast(1f) }.semantics {
        contentDescription="Playback position";progressBarRangeInfo=ProgressBarRangeInfo(value.coerceIn(0f,1f),0f..1f)
        if(enabled) setProgress { onSeek(it.coerceIn(0f,1f));true } else disabled()
    }.pointerInput(enabled){detectTapGestures{if(enabled)onSeek((it.x/width).coerceIn(0f,1f))}}.pointerInput(enabled){detectDragGestures{change,_->if(enabled){change.consume();onSeek((change.position.x/width).coerceIn(0f,1f))}}},contentAlignment=Alignment.CenterStart) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(5.dp)) {
            drawRoundRect(p.line,cornerRadius=androidx.compose.ui.geometry.CornerRadius(5f))
            drawRoundRect(Accent,size=androidx.compose.ui.geometry.Size(size.width*value.coerceIn(0f,1f),size.height),cornerRadius=androidx.compose.ui.geometry.CornerRadius(5f))
        }
    }
}

@Composable private fun QueuePanel(state:PlayerUiState,actions:PlayerActions,close:()->Unit,modifier:Modifier) {
    val p=LocalPalette.current
    Column(modifier.background(p.surface).padding(18.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically){Label("Playing Next",22,bold=true,modifier=Modifier.weight(1f));IconButton(Glyph.Close,"Close queue",onClick=close)}
        if(state.queue.isEmpty()) Label("Your queue is empty",color=p.secondary,modifier=Modifier.padding(vertical=24.dp))
        LazyColumn { items(state.queue,key={it.id}) { track -> Row(verticalAlignment=Alignment.CenterVertically) { Box(Modifier.weight(1f)){TrackRow(track,track.id==state.currentId,state.playing,{actions.play(track.id)})};IconButton(Glyph.Close,"Remove ${track.title} from queue"){actions.removeFromQueue(track.id)} } } }
    }
}
private fun time(millis:Long):String { val seconds=millis.coerceAtLeast(0)/1000;return "${seconds/60}:${(seconds%60).toString().padStart(2,'0')}" }
