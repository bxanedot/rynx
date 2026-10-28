package dev.bxane.rynx.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import dev.bxane.rynx.design.RynxMaterialMode
import dev.bxane.rynx.playback.discovery.*
import dev.bxane.rynx.ui.material.*

private val DiscoveryAccent=Color(0xFFFA2D48)
@Composable private fun ink():Color=if(LocalRynxAppearance.current.darkTheme || LocalRynxAppearance.current.material==RynxMaterialMode.Amoled)Color.White else Color(0xFF18181A)
@Composable private fun Text(value:String,size:Int=14,bold:Boolean=false,muted:Boolean=false,modifier:Modifier=Modifier,maxLines:Int=2) {
    BasicText(value,modifier,style=TextStyle(color=ink().copy(alpha=if(muted).62f else 1f),fontSize=size.sp,fontWeight=if(bold)FontWeight.Bold else FontWeight.Normal),maxLines=maxLines,overflow=TextOverflow.Ellipsis)
}
@Composable internal fun DiscoveryHeader(feed:DiscoveryState,actions:PlayerActions) {
    Column(Modifier.padding(bottom=12.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {Text("Find your next favorite",23,true);Text(if(feed.stale)"Saved discovery · reconnect to refresh" else "Apple catalog. Your Rynx mix.",12,muted=true)}
            RynxButton(if(feed.loading)"Updating…" else "Refresh",actions::refreshDiscovery,enabled=!feed.loading)
        }
        Spacer(Modifier.height(12.dp))
        RynxButton("Region · ${feed.regions.firstOrNull {it.code==feed.region}?.name?:feed.region.uppercase()}",{actions.openDiscoverySection("regions")})
        feed.error?.let {Text(it,13,modifier=Modifier.padding(top=12.dp))}
    }

}
internal fun LazyListScope.homeDiscoverySections(feed:DiscoveryState,actions:PlayerActions) {
    if(feed.loading && feed.sections.isEmpty())item {Text("Refreshing your music…",16,modifier=Modifier.padding(vertical=24.dp))}
    items(feed.sections,key={"shelf:${it.id}"}) {section->
        Column(Modifier.fillMaxWidth().padding(vertical=12.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {Text(section.title,22,true);Text(section.subtitle,12,muted=true)}
                if(section.tracks.isNotEmpty() || section.collections.isNotEmpty()) RynxButton("See all",{actions.openDiscoverySection(section.id)},Modifier.semantics {contentDescription="Open ${section.title}"})
            }
            Spacer(Modifier.height(12.dp))
            section.unavailableReason?.let {Text(it,13,muted=true)}
            section.availabilityNote?.let {Text(it,12,muted=true)}
            if(section.tracks.isNotEmpty())LazyRow(horizontalArrangement=Arrangement.spacedBy(13.dp)) {
                items(section.tracks.take(12),key={it.id}) {track->DiscoverySongCard(track,actions,section.kind==HomeSectionKind.Daily100)}
            }
            if(section.collections.isNotEmpty())LazyRow(horizontalArrangement=Arrangement.spacedBy(13.dp)) {
                items(section.collections.take(12),key={it.id}) {collection->
                    Column(Modifier.width(190.dp).clip(RoundedCornerShape(16.dp)).rynxMaterial(16.dp)
                        .discoveryPress("Open ${collection.title}"){actions.openDiscoveryCollection(collection)}.padding(14.dp)) {
                        GenreDecoration(collection.id,Modifier.fillMaxWidth().height(100.dp))
                        Text(collection.title,18,true,modifier=Modifier.padding(top=12.dp));Text("Apple Music",12,muted=true)
                    }
                }
            }
        }
    }
}
@Composable private fun DiscoverySongCard(track:MusicMetadata,actions:PlayerActions,ranked:Boolean) {
    Column(Modifier.width(152.dp)) {
        Column(Modifier.clip(RoundedCornerShape(12.dp)).discoveryPress("Play discovery ${track.title}"){actions.playDiscovery(track)}) {
            Box(Modifier.size(152.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(Color(0xFF60304E),Color(0xFF20243F)))) ,contentAlignment=Alignment.Center) {
                if(track.artworkUrl!=null)AsyncImage(track.artworkUrl,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                else RynxGlyph(Glyph.Brand,Color.White.copy(alpha=.55f),Modifier.size(65.dp))
                if(ranked)track.chartRank?.let {rank->BasicText(rank.toString(),Modifier.align(Alignment.BottomStart).padding(10.dp),style=TextStyle(color=Color.White,fontSize=28.sp,fontWeight=FontWeight.Bold))}
            }
            Text(track.title,14,true,modifier=Modifier.padding(top=8.dp),maxLines=1);Text(track.artist,12,muted=true,maxLines=1)
        }
        track.sourceUrl?.let {url->RynxButton("Apple Music",{actions.openMusicSource(url)},Modifier.padding(top=6.dp).semantics {contentDescription="Open ${track.title} in Apple Music"})}
    }
}
@Composable internal fun AppleExploreContent(feed:DiscoveryState,actions:PlayerActions,modifier:Modifier) {
    LazyColumn(modifier.padding(horizontal=20.dp),contentPadding=PaddingValues(bottom=24.dp)) {
        item {Text("Explore",26,true);Text("Browse by genre",18,true,modifier=Modifier.padding(top=14.dp,bottom=16.dp))}
        if(feed.genres.isEmpty())item {
            Text(if(feed.loading)"Loading Apple genres…" else "Genres could not load. Refresh when connected.",14,muted=true)
            RynxButton("Refresh genres",actions::refreshDiscovery,Modifier.padding(top=12.dp),enabled=!feed.loading)
        }
        items(feed.genres.chunked(2),key={row->row.joinToString {it.id}}) {row->
            Row(Modifier.fillMaxWidth().padding(bottom=13.dp),horizontalArrangement=Arrangement.spacedBy(13.dp)) {
                row.forEach {genre->
                    val interaction=remember {androidx.compose.foundation.interaction.MutableInteractionSource()}
                    Box(Modifier.weight(1f).height(112.dp).clip(RoundedCornerShape(12.dp))
                        .rynxPressFeedback(interaction)
                        .clickable(interactionSource=interaction,indication=null,role=Role.Button){actions.openGenre(genre)}.semantics {contentDescription="Explore ${genre.name}"}) {
                        GenreDecoration(genre.id,Modifier.fillMaxSize())
                        BasicText(genre.name,Modifier.align(Alignment.BottomStart).padding(14.dp),style=TextStyle(color=Color.White,fontSize=21.sp,fontWeight=FontWeight.Bold),maxLines=2,overflow=TextOverflow.Ellipsis)
                    }
                }
                if(row.size==1)Spacer(Modifier.weight(1f))
            }
        }
        item {RynxButton("Browse genres in Apple Music",{actions.openMusicSource("https://music.apple.com/${feed.region}/room/6456176470")})}
    }
}
/** Original geometric artwork; category names and hierarchy are obtained from Apple, not hard-coded. */
@Composable private fun GenreDecoration(id:String,modifier:Modifier) {
    val colors=listOf(Color(0xFFC33B53),Color(0xFF7759C1),Color(0xFF1C887D),Color(0xFFB35A29),Color(0xFF306EB9),Color(0xFF9E3F8D))
    val base=colors[(id.hashCode().toUInt()%colors.size.toUInt()).toInt()]
    Canvas(modifier.background(Brush.linearGradient(listOf(base,base.copy(red=base.red*.55f,green=base.green*.55f,blue=base.blue*.55f))))) {
        val radius=size.maxDimension*.58f
        repeat(4){i->drawCircle(Color.White.copy(alpha=.055f+i*.015f),radius-i*size.minDimension*.12f,androidx.compose.ui.geometry.Offset(size.width*.93f,size.height*.1f))}
        drawLine(Color.White.copy(alpha=.13f),androidx.compose.ui.geometry.Offset(size.width*.48f,0f),androidx.compose.ui.geometry.Offset(size.width,size.height*.76f),size.minDimension*.16f)
    }
}
@Composable internal fun DiscoveryTrackRow(track:MusicMetadata,actions:PlayerActions) {
    Row(Modifier.fillMaxWidth().padding(vertical=5.dp).rynxMaterial(12.dp,amount=.5f).padding(10.dp),verticalAlignment=Alignment.CenterVertically) {
        Row(Modifier.weight(1f).discoveryPress("Play discovery ${track.title}"){actions.playDiscovery(track)},verticalAlignment=Alignment.CenterVertically) {
            track.chartRank?.let {rank->Text(rank.toString(),14,muted=true,modifier=Modifier.widthIn(min=24.dp).padding(end=8.dp))}
            Artwork(48.dp,artworkUrl=track.artworkUrl)
            Column(Modifier.padding(start=12.dp).weight(1f)) {Text(track.title,14,true);Text(track.artist,12,muted=true)}
        }
        track.sourceUrl?.let {url->RynxButton("Apple Music",{actions.openMusicSource(url)},Modifier.semantics {contentDescription="Open ${track.title} in Apple Music"})}
    }
}
@Composable internal fun DiscoveryDetail(state:PlayerUiState,actions:PlayerActions) {
    val section=state.discoverySelection?:return
    DiscoveryModal(section.title,actions::closeDiscovery) {
        LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=28.dp)) {
            item {Text(section.subtitle,13,muted=true,modifier=Modifier.padding(bottom=12.dp))}
            if(state.busy)item {Text("Finding and preparing this recording…",14,modifier=Modifier.padding(vertical=12.dp).semantics {contentDescription="Preparing music";liveRegion=LiveRegionMode.Polite})}
            state.error?.let {message->item {
                Text(message,14,modifier=Modifier.padding(vertical=12.dp).semantics {contentDescription=message;liveRegion=LiveRegionMode.Polite})
                RynxButton("Dismiss error",actions::dismissError)
            }}
            section.availabilityNote?.let {item {Text(it,13,muted=true)}}
            if(state.discoverySelectionLoading)item {Text("Loading songs…",16,modifier=Modifier.padding(vertical=20.dp))}
            section.unavailableReason?.let {item {Text(it,15,modifier=Modifier.padding(vertical=18.dp))}}
            if(section.id=="regions")items(state.discovery.regions,key={it.code}) {region->
                RynxButton(region.name,{actions.changeDiscoveryRegion(region.code);actions.closeDiscovery()},Modifier.fillMaxWidth().padding(vertical=5.dp),selected=region.code==state.discovery.region)
            }
            items(section.tracks,key={it.id}) {track->DiscoveryTrackRow(track,actions)}
            items(section.collections,key={it.id}) {collection->
                RynxButton(collection.title,{actions.openDiscoveryCollection(collection)},Modifier.fillMaxWidth().padding(vertical=5.dp))
            }
            section.sourceUrl?.let {url->item {RynxButton("Open in Apple Music",{actions.openMusicSource(url)},Modifier.padding(top=18.dp))}}
        }
    }
}
@Composable private fun DiscoveryModal(title:String,onClose:()->Unit,content:@Composable ColumnScope.()->Unit) {
    PlayerBackHandler(true,onClose)
    CompositionLocalProvider(LocalRynxMaterialLayer provides 5f) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.42f)).pointerInput(Unit){detectTapGestures {onClose()}})
        RynxSurface(Modifier.fillMaxSize().padding(top=18.dp).semantics {paneTitle=title}.pointerInput(Unit){detectTapGestures{}},28.dp,shadow=true) {
            Column(Modifier.fillMaxSize().padding(20.dp)) {
                Row(Modifier.fillMaxWidth().padding(bottom=15.dp),verticalAlignment=Alignment.CenterVertically) {Text(title,25,true,modifier=Modifier.weight(1f));RynxButton("Done",onClose)}
                content()
            }
        }
    }
}

private fun Modifier.discoveryPress(label:String,onClick:()->Unit):Modifier=composed {
    val interaction=remember {androidx.compose.foundation.interaction.MutableInteractionSource()}
    rynxPressFeedback(interaction).clickable(interactionSource=interaction,indication=null,role=Role.Button,onClick=onClick).semantics {contentDescription=label}
}
