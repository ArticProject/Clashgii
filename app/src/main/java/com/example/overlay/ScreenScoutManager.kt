package com.example.overlay

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import com.example.data.ClashRoyaleRepository
import com.example.domain.ClanMemberMatch
import com.example.domain.DeckCard
import com.example.domain.PlayerScoutProfile
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.math.abs

sealed interface ScreenScanOutcome {
    data class AutoLoaded(val profile: PlayerScoutProfile) : ScreenScanOutcome
    data class CandidatesFound(val candidates: List<ClanMemberMatch>, val detectedLines: List<String>) : ScreenScanOutcome
    data class NeedsManualClarification(val detectedLines: List<String>, val reason: String) : ScreenScanOutcome
}

data class ArenaSpawnEvent(
    val estimatedElixirCost: Int,
    val matchedHandCard: DeckCard? = null
)

class ScreenScoutManager(
    private val context: Context,
    private val repository: ClashRoyaleRepository
) {
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var captureWidth: Int = 0
    private var captureHeight: Int = 0
    private val mainHandler = Handler(Looper.getMainLooper())
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Arena motion & red healthbar/level badge detector state
    private var previousArenaGrid: IntArray? = null
    private var lastSpawnTimestampMs: Long = 0L
    private var lastOcrAttemptTimestampMs: Long = 0L

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            releaseProjection()
        }
    }

    fun hasActiveProjection(): Boolean = mediaProjection != null

    fun setProjectionResult(resultCode: Int, data: Intent) {
        releaseProjection()
        val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val projection = manager.getMediaProjection(resultCode, data) ?: return
        projection.registerCallback(projectionCallback, mainHandler)
        mediaProjection = projection
        ensurePersistentVirtualDisplay(projection)
    }

    fun releaseProjection() {
        runCatching { virtualDisplay?.release() }
        runCatching { imageReader?.close() }
        runCatching { mediaProjection?.unregisterCallback(projectionCallback) }
        runCatching { mediaProjection?.stop() }
        virtualDisplay = null
        imageReader = null
        mediaProjection = null
        previousArenaGrid = null
    }

    private fun ensurePersistentVirtualDisplay(projection: MediaProjection) {
        if (virtualDisplay != null && imageReader != null) return
        val (fullW, fullH, densityDpi) = getScreenMetrics()
        if (fullW <= 0 || fullH <= 0) return
        // Downscale by 2 for fast real-time arena frame analysis without battery drain
        val targetW = (fullW / 2).coerceAtLeast(360)
        val targetH = (fullH / 2).coerceAtLeast(640)
        captureWidth = targetW
        captureHeight = targetH

        val reader = ImageReader.newInstance(targetW, targetH, PixelFormat.RGBA_8888, 2)
        imageReader = reader
        virtualDisplay = projection.createVirtualDisplay(
            "RoyalePulseLiveVision",
            targetW,
            targetH,
            (densityDpi / 2).coerceAtLeast(120),
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            mainHandler
        )
    }

    /**
     * Real-time frame step:
     * 1. If opponent deck is not loaded yet, periodically runs OCR on the top banner (every 3.5s) to auto-detect opponent.
     * 2. Analyzes the opponent's half of the arena (18%..52% vertical) for newly spawned enemy unit badges/health bars & deploy bursts.
     */
    suspend fun analyzeLiveArenaStep(
        deckLoaded: Boolean,
        isMatchRunning: Boolean,
        handCards: List<DeckCard>
    ): Pair<ScreenScanOutcome?, ArenaSpawnEvent?> = withContext(Dispatchers.Default) {
        val projection = mediaProjection ?: return@withContext null to null
        withContext(Dispatchers.Main) {
            ensurePersistentVirtualDisplay(projection)
        }

        val frame = acquireLatestBitmapFrame() ?: return@withContext null to null
        try {
            val now = System.currentTimeMillis()
            var scanOutcome: ScreenScanOutcome? = null

            // Auto-scout opponent deck if not loaded yet
            if (!deckLoaded && now - lastOcrAttemptTimestampMs >= 3500L) {
                lastOcrAttemptTimestampMs = now
                val cropH = (frame.height * 0.45f).toInt().coerceIn(1, frame.height)
                val topBanner = Bitmap.createBitmap(frame, 0, 0, frame.width, cropH)
                try {
                    val outcome = runOcrAndResolveOpponent(topBanner)
                    if (outcome is ScreenScanOutcome.AutoLoaded || outcome is ScreenScanOutcome.CandidatesFound) {
                        scanOutcome = outcome
                    }
                } finally {
                    topBanner.recycle()
                }
            }

            // Detect opponent card deployment on the top arena half
            val spawnEvent = if (isMatchRunning) {
                detectOpponentDeployInArena(frame, now, handCards)
            } else {
                null
            }

            scanOutcome to spawnEvent
        } finally {
            frame.recycle()
        }
    }

    suspend fun captureAndScoutOpponent(
        onPrepareCapture: suspend () -> Unit = {},
        onCaptureFinished: suspend () -> Unit = {}
    ): ScreenScanOutcome = withContext(Dispatchers.Default) {
        val projection = mediaProjection
            ?: return@withContext ScreenScanOutcome.NeedsManualClarification(
                detectedLines = emptyList(),
                reason = "Нет доступа к захвату экрана"
            )

        withContext(Dispatchers.Main) {
            ensurePersistentVirtualDisplay(projection)
        }

        onPrepareCapture()
        val fullFrame = try {
            var bmp: Bitmap? = null
            for (i in 0 until 6) {
                delay(60L)
                bmp = acquireLatestBitmapFrame()
                if (bmp != null) break
            }
            bmp
        } finally {
            onCaptureFinished()
        } ?: return@withContext ScreenScanOutcome.NeedsManualClarification(
            detectedLines = emptyList(),
            reason = "Не удалось снять кадр экрана"
        )

        val cropH = (fullFrame.height * 0.46f).toInt().coerceIn(1, fullFrame.height)
        val topCrop = Bitmap.createBitmap(fullFrame, 0, 0, fullFrame.width, cropH)
        fullFrame.recycle()
        try {
            runOcrAndResolveOpponent(topCrop)
        } finally {
            topCrop.recycle()
        }
    }

    private suspend fun runOcrAndResolveOpponent(topBitmap: Bitmap): ScreenScanOutcome {
        val rawLines = recognizeLines(topBitmap)
        val candidates = ScreenTextParser.filterCandidateLines(rawLines)

        if (candidates.isEmpty()) {
            return ScreenScanOutcome.NeedsManualClarification(
                detectedLines = rawLines,
                reason = "Текст не найден на экране"
            )
        }

        // 1. Direct #TAG detection
        val tagMatch = ScreenTextParser.extractTag(rawLines)
        if (!tagMatch.isNullOrBlank()) {
            val profile = repository.scoutPlayerByTag(tagMatch).getOrNull()
            if (profile != null && profile.currentDeck.isNotEmpty()) {
                return ScreenScanOutcome.AutoLoaded(profile)
            }
        }

        // 2. Pairwise Clan + Nickname matching
        val pairs = ScreenTextParser.buildSearchPairs(candidates)
        val aggregatedMatches = linkedMapOf<String, ClanMemberMatch>()

        for ((clanQuery, nickQuery) in pairs) {
            val result = repository.findMembersByClanAndNickname(
                clanQuery = clanQuery,
                nicknameQuery = nickQuery
            ).getOrNull() ?: continue

            val (_, members) = result
            members.forEach { aggregatedMatches[it.tag] = it }

            val exactMatch = members.firstOrNull {
                it.name.equals(nickQuery, ignoreCase = true)
            }
            if (exactMatch != null && members.size <= 2) {
                val profile = repository.scoutPlayerByTag(exactMatch.tag).getOrNull()
                if (profile != null && profile.currentDeck.isNotEmpty()) {
                    return ScreenScanOutcome.AutoLoaded(profile)
                }
            }
        }

        val allMatches = aggregatedMatches.values.toList()
        return when {
            allMatches.size == 1 -> {
                val only = allMatches.first()
                val profile = repository.scoutPlayerByTag(only.tag).getOrNull()
                if (profile != null && profile.currentDeck.isNotEmpty()) {
                    ScreenScanOutcome.AutoLoaded(profile)
                } else {
                    ScreenScanOutcome.CandidatesFound(allMatches, candidates)
                }
            }
            allMatches.isNotEmpty() -> {
                ScreenScanOutcome.CandidatesFound(allMatches.take(4), candidates)
            }
            else -> {
                ScreenScanOutcome.NeedsManualClarification(
                    detectedLines = candidates,
                    reason = "Считано: ${candidates.take(2).joinToString(" · ")}"
                )
            }
        }
    }

    /**
     * Inspects an 8x6 spatial grid over the opponent's half of the arena (y = 18%..54%, x = 8%..92%)
     * for newly appearing enemy red level badges / healthbars + sudden deploy animation deltas.
     */
    private fun detectOpponentDeployInArena(
        frame: Bitmap,
        nowMs: Long,
        handCards: List<DeckCard>
    ): ArenaSpawnEvent? {
        val cols = 8
        val rows = 6
        val currentGrid = IntArray(cols * rows)
        val redSignatureGrid = IntArray(cols * rows)

        val startX = (frame.width * 0.08f).toInt()
        val endX = (frame.width * 0.92f).toInt()
        val startY = (frame.height * 0.18f).toInt()
        val endY = (frame.height * 0.54f).toInt()

        val cellW = ((endX - startX) / cols).coerceAtLeast(1)
        val cellH = ((endY - startY) / rows).coerceAtLeast(1)

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                var lumaSum = 0
                var redBadgePixels = 0
                var samples = 0
                val y0 = startY + r * cellH
                val x0 = startX + c * cellW
                var y = y0
                while (y < y0 + cellH && y < frame.height) {
                    var x = x0
                    while (x < x0 + cellW && x < frame.width) {
                        val px = frame.getPixel(x, y)
                        val red = Color.red(px)
                        val green = Color.green(px)
                        val blue = Color.blue(px)
                        lumaSum += (red * 3 + green * 4 + blue) shr 3
                        // Clash Royale enemy level badge / health bar signature: bright saturated crimson-red
                        if (red > 195 && green < 75 && blue < 85 && (red - green) > 125) {
                            redBadgePixels++
                        }
                        samples++
                        x += 4
                    }
                    y += 4
                }
                val idx = r * cols + c
                currentGrid[idx] = if (samples > 0) lumaSum / samples else 0
                redSignatureGrid[idx] = redBadgePixels
            }
        }

        val prev = previousArenaGrid
        previousArenaGrid = currentGrid
        if (prev == null) return null

        // Enforce cooldown of 2.1s between auto-detected card spawns so walking units don't re-trigger
        if (nowMs - lastSpawnTimestampMs < 2100L) return null

        var activeBurstCells = 0
        var redSpawnCells = 0
        for (i in currentGrid.indices) {
            val lumaDelta = abs(currentGrid[i] - prev[i])
            if (lumaDelta > 28 && redSignatureGrid[i] >= 3) {
                redSpawnCells++
            }
            if (lumaDelta > 36) {
                activeBurstCells++
            }
        }

        // Trigger when a new enemy unit badge/healthbar bursts onto the opponent arena side
        if (redSpawnCells >= 1 && activeBurstCells >= 2) {
            lastSpawnTimestampMs = nowMs
            val matchedCard = if (handCards.isNotEmpty()) {
                // Pick the card in hand matching the deploy footprint (swarm/heavy vs cycle unit)
                val sortedHand = handCards.sortedBy { it.elixirCost }
                when {
                    redSpawnCells >= 3 || activeBurstCells >= 8 -> sortedHand.last()
                    redSpawnCells == 2 || activeBurstCells >= 5 -> sortedHand[sortedHand.size / 2]
                    else -> sortedHand.first()
                }
            } else {
                null
            }

            val estimatedCost = matchedCard?.elixirCost ?: when {
                redSpawnCells >= 3 || activeBurstCells >= 8 -> 5
                redSpawnCells == 2 || activeBurstCells >= 5 -> 4
                else -> 3
            }

            return ArenaSpawnEvent(
                estimatedElixirCost = estimatedCost,
                matchedHandCard = matchedCard
            )
        }

        return null
    }

    private suspend fun acquireLatestBitmapFrame(): Bitmap? = withContext(Dispatchers.Main) {
        val reader = imageReader ?: return@withContext null
        val w = captureWidth
        val h = captureHeight
        if (w <= 0 || h <= 0) return@withContext null

        val image = runCatching { reader.acquireLatestImage() }.getOrNull() ?: return@withContext null
        try {
            val plane = image.planes[0]
            val buffer = plane.buffer
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val rowPadding = rowStride - pixelStride * w
            val rawBitmap = Bitmap.createBitmap(
                w + rowPadding / pixelStride,
                h,
                Bitmap.Config.ARGB_8888
            )
            rawBitmap.copyPixelsFromBuffer(buffer)
            if (rowPadding == 0) {
                rawBitmap
            } else {
                val cropped = Bitmap.createBitmap(rawBitmap, 0, 0, w, h)
                rawBitmap.recycle()
                cropped
            }
        } finally {
            image.close()
        }
    }

    private fun getScreenMetrics(): Triple<Int, Int, Int> {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val densityDpi = context.resources.displayMetrics.densityDpi
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.maximumWindowMetrics.bounds
            Triple(bounds.width(), bounds.height(), densityDpi)
        } else {
            val dm = context.resources.displayMetrics
            Triple(dm.widthPixels, dm.heightPixels, densityDpi)
        }
    }

    private suspend fun recognizeLines(bitmap: Bitmap): List<String> =
        suspendCancellableCoroutine { cont ->
            val finished = AtomicBoolean(false)
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    if (finished.compareAndSet(false, true)) {
                        val lines = visionText.textBlocks
                            .flatMap { it.lines }
                            .sortedBy { it.boundingBox?.top ?: 0 }
                            .map { it.text.trim() }
                            .filter { it.isNotBlank() }
                        cont.resume(lines)
                    }
                }
                .addOnFailureListener {
                    if (finished.compareAndSet(false, true)) {
                        cont.resume(emptyList())
                    }
                }
        }
}

object ScreenTextParser {
    private val noiseWords = setOf(
        "vs", "elixir", "overtime", "sudden death", "2x", "3x", "x1", "x2", "x3",
        "royalepulse", "tower", "king", "battle", "tiebreaker"
    )

    fun extractTag(rawLines: List<String>): String? {
        return Regex("#[0289PYLQGRJCUV]{4,10}")
            .find(rawLines.joinToString(" ").uppercase())
            ?.value
    }

    fun filterCandidateLines(lines: List<String>): List<String> {
        return lines
            .map { it.replace("\n", " ").trim() }
            .filter { line ->
                val lower = line.lowercase()
                val hasLetters = line.any { it.isLetter() }
                val isTimerOrNumber = Regex("^\\d+[:./]?\\d*$").matches(line)
                line.length >= 3 &&
                    hasLetters &&
                    !isTimerOrNumber &&
                    lower !in noiseWords
            }
            .distinct()
            .take(6)
    }

    fun buildSearchPairs(candidates: List<String>): List<Pair<String, String>> {
        val pairs = mutableListOf<Pair<String, String>>()
        for (i in candidates.indices) {
            for (j in candidates.indices) {
                if (i != j && candidates[j].length >= 3) {
                    pairs.add(candidates[j] to candidates[i])
                }
            }
        }
        return pairs.take(6)
    }
}
