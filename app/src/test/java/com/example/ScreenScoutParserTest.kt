package com.example

import com.example.overlay.ScreenTextParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenScoutParserTest {

    @Test
    fun filtersNoiseAndBuildsClanNickPairs() {
        val raw = listOf("VS", "2:45", "x2", "8542", "DarkKnight", "Alpha Clan", "10")
        val filtered = ScreenTextParser.filterCandidateLines(raw)
        assertEquals(listOf("DarkKnight", "Alpha Clan"), filtered)

        val pairs = ScreenTextParser.buildSearchPairs(filtered)
        assertTrue(pairs.contains("Alpha Clan" to "DarkKnight"))
        assertTrue(pairs.contains("DarkKnight" to "Alpha Clan"))
    }

    @Test
    fun extractsDirectPlayerTagFromOcrText() {
        val raw = listOf("Opponent", "#C0G20PR2", "VS")
        assertEquals("#C0G20PR2", ScreenTextParser.extractTag(raw))
    }
}
