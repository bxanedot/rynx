package dev.bxane.rynx.playback.discovery

import dev.bxane.rynx.playback.catalog.*
import kotlin.math.abs

/** Conservative cross-provider matching: matching title alone never identifies a recording. */
object MetadataMatching {
    private val decoration=setOf("official","audio","video","lyrics","lyric","hd","4k","visualizer")
    private val variants=setOf("live","remix","cover","karaoke","instrumental","sped","slowed","remastered","remaster","acoustic","unplugged","edit","extended","mix","version","piano","orchestral","reverb","mono","stereo","clean")
    private fun tokens(text:String)=text.lowercase().map {if(it.isLetterOrDigit())it else ' '}.joinToString("")
        .split(' ').filter {it.isNotBlank() && it !in decoration}.toSet()
    private fun overlap(a:Set<String>,b:Set<String>):Double=if(a.isEmpty()||b.isEmpty())0.0 else a.intersect(b).size.toDouble()/a.union(b).size
    fun confidence(target:MusicMetadata,candidate:CatalogItem):Double {
        if(candidate.type!=CatalogItemType.Track)return 0.0
        val title=tokens(target.title);val other=tokens(candidate.title)
        if(title.intersect(variants)!=other.intersect(variants))return 0.0
        val titleScore=overlap(title,other)
        val artist=tokens(target.artist);val candidateArtist=tokens(candidate.artist.orEmpty())-setOf("topic","vevo")
        val artistScore=overlap(artist,candidateArtist)
        if(titleScore<.72 || artistScore<.4)return 0.0
        val duration=candidate.durationSeconds?.times(1000)
        if(target.durationMillis!=null && duration!=null && abs(target.durationMillis-duration)>maxOf(8000L,target.durationMillis/8))return 0.0
        return titleScore*.65+artistScore*.35
    }
    fun bestPlaybackMatch(target:MusicMetadata,candidates:List<CatalogItem>):CatalogItem? = candidates.map {it to confidence(target,it)}
        .filter {it.second>=.78}.maxByOrNull {it.second}?.first
    fun bestMetadataMatch(source:CatalogItem,candidates:List<MusicMetadata>):MusicMetadata?=candidates.map {it to confidence(it,source)}
        .filter {it.second>=.78}.maxByOrNull {it.second}?.first
}
