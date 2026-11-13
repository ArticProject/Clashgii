package com.example

import com.example.domain.DeckCard
import com.example.domain.ElixirCycleEngine
import com.example.domain.ElixirPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ElixirCycleEngineTest {

    private fun sampleDeck(): List<DeckCard> = listOf(
        DeckCard(1, "Hog Rider", 4),
        DeckCard(2, "Musketeer", 4),
        DeckCard(3, "Ice Spirit", 1),
        DeckCard(4, "Skeletons", 1),
        DeckCard(5, "Fireball", 4),
        DeckCard(6, "The Log", 2),
        DeckCard(7, "Cannon", 3),
        DeckCard(8, "Mirror", 1, isMirror = true)
    )

    @Test
    fun playingCardDeductsElixirCyclesToBackAndSupportsMirrorAndUndo() {
        val engine = ElixirCycleEngine()
        engine.configureSettings(startingElixir = 8.0f, autoPhaseSwitch = true)
        val deck = sampleDeck()
        engine.loadOpponentDeck("TestPlayer", "#2PP", deck, resetTimer = true)

        assertEquals(8.0f, engine.state.value.currentElixir, 0.01f)
        assertEquals(4, engine.state.value.handCards.size)
        assertEquals(4, engine.state.value.cycleQueue.size)

        // Play Hog Rider (4 elixir)
        val hog = deck[0]
        engine.playCard(hog)
        assertEquals(4.0f, engine.state.value.currentElixir, 0.01f)
        assertEquals(hog.id, engine.state.value.cycleQueue.last().id)
        assertEquals("Musketeer", engine.state.value.handCards.first().name)
        assertTrue(engine.state.value.canUndo)

        // Mirror should now cost 4 + 1 = 5 (clamped at 0 when elixir is 4.0)
        val mirror = deck[7]
        assertEquals(5, engine.effectiveCardCost(mirror))

        // Adjust +2.0 elixir -> 6.0, play Mirror -> 1.0
        engine.adjustElixir(2.0f)
        assertEquals(6.0f, engine.state.value.currentElixir, 0.01f)
        engine.playCard(mirror)
        assertEquals(1.0f, engine.state.value.currentElixir, 0.01f)

        // Undo Mirror + Undo +2.0 + Undo Hog Rider -> back to 8.0 and original order
        engine.undoLastAction()
        engine.undoLastAction()
        engine.undoLastAction()
        assertEquals(8.0f, engine.state.value.currentElixir, 0.01f)
        assertEquals(hog.id, engine.state.value.handCards.first().id)
        assertFalse(engine.state.value.canUndo)
    }

    @Test
    fun phaseSelectionUpdatesElixirRate() {
        val engine = ElixirCycleEngine()
        engine.setPhase(ElixirPhase.DOUBLE)
        assertEquals(ElixirPhase.DOUBLE, engine.state.value.phase)
        engine.setPhase(ElixirPhase.TRIPLE)
        assertEquals(ElixirPhase.TRIPLE, engine.state.value.phase)
    }
}
