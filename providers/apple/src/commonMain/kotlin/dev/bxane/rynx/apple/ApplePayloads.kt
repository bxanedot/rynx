package dev.bxane.rynx.apple

import dev.bxane.rynx.playback.discovery.*
import kotlinx.serialization.json.*

internal fun JsonObject.text(key:String)=get(key)?.jsonPrimitive?.contentOrNull
internal fun JsonObject.array(key:String)=get(key) as? JsonArray ?: JsonArray(emptyList())
internal fun JsonObject.obj(key:String)=get(key) as? JsonObject ?: JsonObject(emptyMap())
data class ApplePlaylistLink(val id:String,val title:String,val url:String)

/** Parses provider documents and only public JSON-LD/visible links, never application script state. */
object ApplePayloads {
    private val json=Json {ignoreUnknownKeys=true;isLenient=false}
    fun feed(body:String,region:String):ChartCollection {
        val feed=json.parseToJsonElement(body).jsonObject.obj("feed")
        require(feed.containsKey("results")){"Apple chart feed is missing results"}
        val tracks=feed.array("results").mapIndexedNotNull {index,value->
            val r=value as? JsonObject ?: return@mapIndexedNotNull null
            val id=r.text("id")?.takeIf {it.all(Char::isDigit)}?:return@mapIndexedNotNull null
            val title=r.text("name")?.takeIf {it.isNotBlank()}?:return@mapIndexedNotNull null
            val genres=r.array("genres").mapNotNull {it as? JsonObject}.filter {it.text("genreId")!="34"}
            MusicMetadata("apple:$id",title,r.text("artistName").orEmpty(),genreIds=genres.mapNotNull {it.text("genreId")},
                genreNames=genres.mapNotNull {it.text("name")},releaseDate=r.text("releaseDate"),sourceUrl=r.text("url"),chartRank=index+1)
        }
        return ChartCollection("storefront:$region",feed.text("title")?:"Top Songs",tracks,
            "https://music.apple.com/$region/new/top-charts",feed.text("updated"))
    }
    fun lookup(body:String):List<MusicMetadata> = json.parseToJsonElement(body).jsonObject.array("results").mapNotNull {value->
        val r=value as? JsonObject ?: return@mapNotNull null
        if(r.text("kind")!="song")return@mapNotNull null
        val id=r.text("trackId")?:return@mapNotNull null
        val title=r.text("trackName")?.takeIf {it.isNotBlank()}?:return@mapNotNull null
        MusicMetadata("apple:$id",title,r.text("artistName").orEmpty(),album=r.text("collectionName"),artworkUrl=r.text("artworkUrl100"),
            genreNames=listOfNotNull(r.text("primaryGenreName")),releaseDate=r.text("releaseDate"),
            durationMillis=r["trackTimeMillis"]?.jsonPrimitive?.longOrNull,sourceUrl=r.text("trackViewUrl"))
    }
    fun genreFeed(body:String,region:String,genre:MusicGenre):ChartCollection {
        val feed=json.parseToJsonElement(body).jsonObject.obj("feed")
        val tracks=feed.array("entry").mapIndexedNotNull {index,value->
            val r=value as? JsonObject ?: return@mapIndexedNotNull null
            val id=r.obj("id").obj("attributes").text("im:id")?:return@mapIndexedNotNull null
            val title=r.obj("im:name").text("label")?:return@mapIndexedNotNull null
            val url=r.array("link").mapNotNull {it as? JsonObject}.map {it.obj("attributes")}.firstOrNull {it.text("rel")=="alternate"}?.text("href")
            MusicMetadata("apple:$id",title,r.obj("im:artist").text("label").orEmpty(),album=r.obj("im:collection").obj("im:name").text("label"),
                genreIds=listOf(genre.id),genreNames=listOf(genre.name),releaseDate=r.obj("im:releaseDate").text("label"),sourceUrl=url,chartRank=index+1)
        }
        return ChartCollection("genre:${genre.id}",genre.name,tracks,"https://music.apple.com/$region/room/6456176470",feed.obj("updated").text("label"))
    }
    fun genres(body:String):List<MusicGenre> {
        fun node(id:String,r:JsonObject):MusicGenre? {
            val name=r.text("name")?.takeIf {it.isNotBlank()}?:return null
            val key=r.text("id")?:id
            if(key.isBlank())return null
            return MusicGenre(key,name,r.obj("subgenres").mapNotNull {(childId,child)->(child as? JsonObject)?.let {node(childId,it)}}.sortedBy {it.name})
        }
        return json.parseToJsonElement(body).jsonObject.obj("34").obj("subgenres").mapNotNull {(id,value)->(value as? JsonObject)?.let {node(id,it)}}.sortedBy {it.name}
    }
    fun playlist(html:String,id:String,url:String):ChartCollection {
        val scripts=Regex("<script\\b([^>]*)>([\\s\\S]*?)</script\\s*>",RegexOption.IGNORE_CASE)
        fun find(value:JsonElement):JsonObject?=when(value) {
            is JsonArray->value.firstNotNullOfOrNull(::find)
            is JsonObject->if(value["@type"]?.toString()?.contains("MusicPlaylist")==true)value else value["@graph"]?.let(::find)
            else->null
        }
        val data=scripts.findAll(html).filter {Regex("type\\s*=\\s*[\"']application/ld\\+json[\"']",RegexOption.IGNORE_CASE).containsMatchIn(it.groupValues[1])}
            .mapNotNull {runCatching {find(json.parseToJsonElement(it.groupValues[2]))}.getOrNull()}.firstOrNull()
            ?:error("Apple's public playlist data is unavailable")
        val rawTracks=(data["track"] as? JsonArray) ?: data.obj("track").array("itemListElement")
        val tracks=rawTracks.mapIndexedNotNull {index,value->
            val entry=value as? JsonObject ?: return@mapIndexedNotNull null
            val row=if(entry.containsKey("item"))entry.obj("item") else entry
            val source=row.text("url")?.takeIf {it.startsWith("https://music.apple.com/")}?:return@mapIndexedNotNull null
            val trackId=source.substringBefore('?').trimEnd('/').substringAfterLast('/').takeIf {it.isNotBlank()&&it.all(Char::isDigit)}?:return@mapIndexedNotNull null
            val title=row.text("name")?.takeIf {it.isNotBlank()}?:return@mapIndexedNotNull null
            val duration=row.text("duration")?.let {Regex("PT(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?").matchEntire(it)}?.let {m->
                ((m.groupValues[1].toLongOrNull()?:0)*3600+(m.groupValues[2].toLongOrNull()?:0)*60+(m.groupValues[3].toLongOrNull()?:0))*1000
            }
            MusicMetadata("apple:$trackId",decode(title),row.obj("byArtist").text("name").orEmpty(),durationMillis=duration,sourceUrl=source,chartRank=index+1)
        }
        require(tracks.isNotEmpty()){ "Apple's public playlist has no songs" }
        return ChartCollection(id,decode(data.text("name")?:"Apple Music playlist"),tracks,url,data.text("datePublished"),maxOf(tracks.size,data["numTracks"]?.jsonPrimitive?.intOrNull?:0))
    }
    fun playlistLinks(html:String):List<ApplePlaylistLink> {
        val visible=html.replace(Regex("<script\\b[^>]*>[\\s\\S]*?</script\\s*>",RegexOption.IGNORE_CASE),"")
        return Regex("<a\\b[^>]*href\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>([\\s\\S]*?)</a>",RegexOption.IGNORE_CASE).findAll(visible).mapNotNull {match->
            val href=decode(match.groupValues[1]);val url=when {href.startsWith("https://music.apple.com/")->href;href.startsWith('/')&&!href.startsWith("//")->"https://music.apple.com$href";else->return@mapNotNull null}.substringBefore('?')
            val id=url.substringAfterLast('/').takeIf {it.startsWith("pl.")}?:return@mapNotNull null
            val title=decode(match.groupValues[2].replace(Regex("<[^>]*>")," ")).replace(Regex("\\s+")," ").trim()
            ApplePlaylistLink(id,title.ifBlank {url.substringBeforeLast('/').substringAfterLast('/').replace('-',' ')},url)
        }.distinctBy {it.id}.toList()
    }
    private fun decode(value:String)=value.replace("&amp;","&").replace("&quot;","\"").replace("&#39;","'").replace("&apos;","'").replace("&lt;","<").replace("&gt;",">").replace("&nbsp;"," ")
}
