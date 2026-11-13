package com.example.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class TrackerSnapshot(
    val currentElixir: Float,
    val deckOrder: List<DeckCard>,
    val lastPlayedCost: Int?,
    val lastPlayedCardName: String?
)

data class ElixirTrackerState(
    val opponentName: String = "Анализ арены",
    val opponentTag: String = "",
    val currentElixir: Float = 5.0f,
    val startingElixir: Float = 5.0f,
    val phase: ElixirPhase = ElixirPhase.SINGLE,
    val autoPhaseSwitch: Boolean = true,
    val isRunning: Boolean = false,
    val elapsedMillis: Long = 0L,
    val deckOrder: List<DeckCard> = emptyList(),
    val lastPlayedCost: Int? = null,
    val lastPlayedCardName: String? = null,
    val canUndo: Boolean = false,
    val isOverlayActive: Boolean = false,
    val isAutoVisionEnabled: Boolean = false,
    val isScanningScreen: Boolean = false,
    val scanStatusMessage: String? = null,
    val scanCandidates: List<ClanMemberMatch> = emptyList()
) {
    val handCards: List<DeckCard>
        get() = deckOrder.take(4)

    val cycleQueue: List<DeckCard>
        get() = deckOrder.drop(4)

    val elapsedSeconds: Int
        get() = (elapsedMillis / 1000L).toInt()

    val formattedMatchClock: String
        get() {
            val totalSec = elapsedSeconds
            val min = totalSec / 60
            val sec = totalSec % 60
            return "%d:%02d".format(min, sec)
        }
}

class ElixirCycleEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickerJob: Job? = null
    private val undoStack = ArrayDeque<TrackerSnapshot>()

    private val _state = MutableStateFlow(ElixirTrackerState())
    val state: StateFlow<ElixirTrackerState> = _state.asStateFlow()

    fun setOverlayActive(active: Boolean) {
        _state.update { it.copy(isOverlayActive = active) }
    }

    fun setAutoVisionEnabled(enabled: Boolean) {
        _state.update { it.copy(isAutoVisionEnabled = enabled) }
    }

    fun setScanningState(scanning: Boolean, statusMessage: String? = null) {
        _state.update {
            it.copy(
                isScanningScreen = scanning,
                scanStatusMessage = statusMessage,
                scanCandidates = if (scanning) emptyList() else it.scanCandidates
            )
        }
    }

    fun setScanCandidates(candidates: List<ClanMemberMatch>, statusMessage: String?) {
        _state.update {
            it.copy(
                isScanningScreen = false,
                scanCandidates = candidates,
                scanStatusMessage = statusMessage
            )
        }
    }

    fun clearScanFeedback() {
        _state.update {
            it.copy(
                isScanningScreen = false,
                scanStatusMessage = null,
                scanCandidates = emptyList()
            )
        }
    }

    fun configureSettings(startingElixir: Float, autoPhaseSwitch: Boolean) {
        _state.update { current ->
            val updatedElixir = if (!current.isRunning && current.elapsedMillis == 0L) {
                startingElixir.coerceIn(0f, 10f)
            } else {
                current.currentElixir
            }
            current.copy(
                startingElixir = startingElixir.coerceIn(0f, 10f),
                autoPhaseSwitch = autoPhaseSwitch,
                currentElixir = updatedElixir
            )
        }
    }

    fun loadOpponentDeck(
        opponentName: String,
        opponentTag: String,
        cards: List<DeckCard>,
        resetTimer: Boolean = true
    ) {
        undoStack.clear()
        _state.update { current ->
            if (resetTimer) {
                stopTicker()
                current.copy(
                    opponentName = opponentName,
                    opponentTag = opponentTag,
                    deckOrder = cards.take(8),
                    currentElixir = current.startingElixir,
                    phase = ElixirPhase.SINGLE,
                    isRunning = false,
                    elapsedMillis = 0L,
                    lastPlayedCost = null,
                    lastPlayedCardName = null,
                    canUndo = false
                )
            } else {
                current.copy(
                    opponentName = opponentName,
                    opponentTag = opponentTag,
                    deckOrder = cards.take(8),
                    lastPlayedCost = null,
                    lastPlayedCardName = null,
                    canUndo = false
                )
            }
        }
    }

    fun clearDeck() {
        undoStack.clear()
        _state.update {
            it.copy(
                opponentName = "Универсальный режим",
                opponentTag = "",
                deckOrder = emptyList(),
                lastPlayedCost = null,
                lastPlayedCardName = null,
                canUndo = false
            )
        }
    }

    fun toggleRunning() {
        val currentlyRunning = _state.value.isRunning
        if (currentlyRunning) {
            stopTicker()
            _state.update { it.copy(isRunning = false) }
        } else {
            _state.update { it.copy(isRunning = true) }
            startTicker()
        }
    }

    fun startMatch() {
        if (!_state.value.isRunning) {
            _state.update { it.copy(isRunning = true) }
            startTicker()
        }
    }

    fun pauseMatch() {
        stopTicker()
        _state.update { it.copy(isRunning = false) }
    }

    fun resetMatch() {
        stopTicker()
        undoStack.clear()
        _state.update { current ->
            current.copy(
                currentElixir = current.startingElixir,
                phase = ElixirPhase.SINGLE,
                isRunning = false,
                elapsedMillis = 0L,
                lastPlayedCost = null,
                lastPlayedCardName = null,
                canUndo = false
            )
        }
    }

    fun setPhase(phase: ElixirPhase) {
        _state.update { it.copy(phase = phase) }
    }

    fun adjustElixir(delta: Float) {
        _state.update { current ->
            pushUndo(current)
            current.copy(
                currentElixir = (current.currentElixir + delta).coerceIn(0f, 10f),
                canUndo = undoStack.isNotEmpty()
            )
        }
    }

    fun playGenericCost(cost: Int) {
        if (cost <= 0) return
        _state.update { current ->
            pushUndo(current)
            current.copy(
                currentElixir = (current.currentElixir - cost).coerceAtLeast(0f),
                lastPlayedCost = cost,
                lastPlayedCardName = "Карта ($cost)",
                canUndo = undoStack.isNotEmpty()
            )
        }
    }

    fun effectiveCardCost(card: DeckCard): Int {
        val lastCost = _state.value.lastPlayedCost
        return if (card.isMirror && lastCost != null) {
            (lastCost + 1).coerceAtMost(10)
        } else {
            card.elixirCost.coerceAtLeast(1)
        }
    }

    fun playCard(card: DeckCard) {
        _state.update { current ->
            pushUndo(current)
            val costToSpend = if (card.isMirror && current.lastPlayedCost != null) {
                (current.lastPlayedCost + 1).coerceAtMost(10)
            } else {
                card.elixirCost.coerceAtLeast(1)
            }

            val updatedOrder = if (current.deckOrder.any { it.id == card.id }) {
                current.deckOrder.filterNot { it.id == card.id } + card
            } else {
                current.deckOrder
            }

            current.copy(
                currentElixir = (current.currentElixir - costToSpend).coerceAtLeast(0f),
                deckOrder = updatedOrder,
                lastPlayedCost = costToSpend,
                lastPlayedCardName = card.name,
                canUndo = undoStack.isNotEmpty()
            )
        }
    }

    fun undoLastAction() {
        val snapshot = undoStack.removeLastOrNull() ?: return
        _state.update { current ->
            current.copy(
                currentElixir = snapshot.currentElixir,
                deckOrder = snapshot.deckOrder,
                lastPlayedCost = snapshot.lastPlayedCost,
                lastPlayedCardName = snapshot.lastPlayedCardName,
                canUndo = undoStack.isNotEmpty()
            )
        }
    }

    private fun pushUndo(current: ElixirTrackerState) {
        if (undoStack.size >= 25) {
            undoStack.removeFirst()
        }
        undoStack.addLast(
            TrackerSnapshot(
                currentElixir = current.currentElixir,
                deckOrder = current.deckOrder,
                lastPlayedCost = current.lastPlayedCost,
                lastPlayedCardName = current.lastPlayedCardName
            )
        )
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            var lastTickNs = System.nanoTime()
            while (isActive) {
                delay(50L)
                val nowNs = System.nanoTime()
                val dtSec = ((nowNs - lastTickNs).coerceIn(1_000_000L, 500_000_000L)) / 1_000_000_000f
                lastTickNs = nowNs

                _state.update { current ->
                    if (!current.isRunning) return@update current
                    val newElapsedMs = current.elapsedMillis + (dtSec * 1000f).toLong()
                    val elapsedSec = newElapsedMs / 1000L
                    val activePhase = if (current.autoPhaseSwitch) {
                        when {
                            elapsedSec >= 240L -> ElixirPhase.TRIPLE
                            elapsedSec >= 120L -> ElixirPhase.DOUBLE
                            else -> ElixirPhase.SINGLE
                        }
                    } else {
                        current.phase
                    }
                    val gained = dtSec * activePhase.elixirPerSecond
                    val nextElixir = (current.currentElixir + gained).coerceAtMost(10.0f)
                    current.copy(
                        currentElixir = nextElixir,
                        phase = activePhase,
                        elapsedMillis = newElapsedMs
                    )
                }
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }
}
