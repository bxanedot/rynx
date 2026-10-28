package dev.bxane.rynx.playback

import kotlin.test.*

class TimedLyricsTest {
    @Test fun lrcAppliesOffsetAndMultipleTimestampsWithoutLosingEnhancedWordTiming() {
        val lyrics=parseTimedLyrics("""
            [ar:bxane]
            [offset:-250]
            [00:01.00][00:03.50]Hello
            [00:02.12]<00:02.12>Good <00:02.75>night
        """.trimIndent())
        assertEquals(listOf(750L,1870L,3250L),lyrics.map { it.startMillis })
        assertEquals("Good night",lyrics[1].text)
        assertEquals(listOf(1870L,2500L),lyrics[1].words.map { it.startMillis })
        assertEquals(listOf("Good ","night"),lyrics[1].words.map { it.text })
    }
    @Test fun invalidTimingCannotCreateSeekTargetsAndNegativeOffsetClampsToZero() {
        val lyrics=parseTimedLyrics("[offset:-1000]\n[00:00.50]Beginning\n[00:99.00]bad\n[999999999999999999999:00]bad\nuntimed")
        assertEquals(1,lyrics.size)
        assertEquals(0L,lyrics.single().startMillis)
        assertEquals("Beginning",lyrics.single().text)
    }
    @Test fun followingSelectsTheLineAtItsBoundaryAndHandlesAnEmptyFile() {
        val lyrics=parseTimedLyrics("[00:02]First\n[00:05]Second")
        assertEquals(-1,activeLyricIndex(lyrics,0))
        assertEquals(0,activeLyricIndex(lyrics,2000))
        assertEquals(0,activeLyricIndex(lyrics,4999))
        assertEquals(1,activeLyricIndex(lyrics,5000))
        assertEquals(-1,activeLyricIndex(emptyList(),5000))
    }
}
