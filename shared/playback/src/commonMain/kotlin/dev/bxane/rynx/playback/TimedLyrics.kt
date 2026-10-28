package dev.bxane.rynx.playback

data class LyricWord(val startMillis:Long,val text:String)
data class LyricLine(val startMillis:Long,val text:String,val words:List<LyricWord> = emptyList())

private val lineTime=Regex("\\[(\\d{1,8}):([0-5]\\d)(?:[.:](\\d{1,3}))?]")
private val wordTime=Regex("<(\\d{1,8}):([0-5]\\d)(?:[.:](\\d{1,3}))?>")
private val offsetTag=Regex("(?im)^\\[offset:([+-]?\\d{1,12})]\\s*$")

/** Independently written parser for standard and enhanced LRC; untimed text is not a seek target. */
fun parseTimedLyrics(source:String):List<LyricLine> {
    val offset=offsetTag.findAll(source).lastOrNull()?.groupValues?.get(1)?.toLongOrNull() ?: 0L
    return buildList {
        source.lineSequence().forEach { raw ->
            val line=raw.trim();val timestamps=mutableListOf<Long>();var position=0
            while(true) {
                val match=lineTime.find(line,position)?.takeIf { it.range.first==position } ?: break
                timestamps+=lyricTimestamp(match);position=match.range.last+1
            }
            if(timestamps.isEmpty()) return@forEach
            val content=line.substring(position).trim()
            val words=wordTime.findAll(content).toList()
            val text=content.replace(wordTime,"").trim()
            if(text.isBlank())return@forEach
            val enhanced=words.mapIndexed { index,match ->
                val next=words.getOrNull(index+1)?.range?.first ?: content.length
                LyricWord((lyricTimestamp(match)+offset).coerceAtLeast(0),content.substring(match.range.last+1,next))
            }
            timestamps.forEach { add(LyricLine((it+offset).coerceAtLeast(0),text,enhanced)) }
        }
    }.sortedBy(LyricLine::startMillis)
}

private fun lyricTimestamp(match:MatchResult):Long {
    val fraction=match.groupValues[3].padEnd(3,'0')
    return match.groupValues[1].toLong()*60_000+match.groupValues[2].toLong()*1000+(fraction.toLongOrNull() ?: 0)
}

fun activeLyricIndex(lines:List<LyricLine>,positionMillis:Long):Int {
    var low=0;var high=lines.lastIndex;var active=-1
    while(low<=high) {
        val middle=(low+high) ushr 1
        if(lines[middle].startMillis<=positionMillis) { active=middle;low=middle+1 } else high=middle-1
    }
    return active
}
