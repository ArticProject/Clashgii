package com.example.overlay

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.domain.ClanMemberMatch
import com.example.domain.DeckCard
import com.example.domain.ElixirPhase
import com.example.domain.ElixirTrackerState
import com.example.ui.theme.ElixirAccent
import com.example.ui.theme.GraphiteActive
import com.example.ui.theme.GraphiteBg
import com.example.ui.theme.GraphiteHairline
import com.example.ui.theme.GraphiteHairlineStrong
import com.example.ui.theme.GraphiteRaised
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StatusReady
import java.util.Locale

@Composable
fun OverlayHudContent(
    state: ElixirTrackerState,
    isMinimized: Boolean,
    opacity: Float,
    scale: Float,
    effectiveCostProvider: (DeckCard) -> Int,
    onToggleMinimize: () -> Unit,
    onCloseOverlay: () -> Unit,
    onDragDelta: (dx: Float, dy: Float) -> Unit,
    onTogglePlayPause: () -> Unit,
    onReset: () -> Unit,
    onUndo: () -> Unit,
    onSelectPhase: (ElixirPhase) -> Unit,
    onPlayCard: (DeckCard) -> Unit,
    onAutoScanClick: () -> Unit,
    onSelectCandidate: (ClanMemberMatch) -> Unit,
    onDismissScanFeedback: () -> Unit
) {
    if (isMinimized) {
        MinimizedOverlayPill(
            state = state,
            opacity = opacity,
            onToggleMinimize = onToggleMinimize,
            onTogglePlayPause = onTogglePlayPause,
            onAutoScanClick = onAutoScanClick,
            onDragDelta = onDragDelta
        )
    } else {
        ExpandedOverlayPanel(
            state = state,
            opacity = opacity,
            scale = scale,
            effectiveCostProvider = effectiveCostProvider,
            onToggleMinimize = onToggleMinimize,
            onCloseOverlay = onCloseOverlay,
            onDragDelta = onDragDelta,
            onTogglePlayPause = onTogglePlayPause,
            onReset = onReset,
            onUndo = onUndo,
            onSelectPhase = onSelectPhase,
            onPlayCard = onPlayCard,
            onAutoScanClick = onAutoScanClick,
            onSelectCandidate = onSelectCandidate,
            onDismissScanFeedback = onDismissScanFeedback
        )
    }
}

@Composable
private fun MinimizedOverlayPill(
    state: ElixirTrackerState,
    opacity: Float,
    onToggleMinimize: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onAutoScanClick: () -> Unit,
    onDragDelta: (dx: Float, dy: Float) -> Unit
) {
    val surfaceAlpha = opacity.coerceIn(0.25f, 0.95f)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = GraphiteBg.copy(alpha = surfaceAlpha),
        border = BorderStroke(1.dp, GraphiteHairlineStrong),
        shadowElevation = 4.dp,
        modifier = Modifier
            .height(32.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDragDelta(dragAmount.x, dragAmount.y)
                }
            }
            .testTag("overlay_minimized_pill")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            state.isAutoVisionEnabled && state.isRunning -> StatusReady
                            state.isRunning -> ElixirAccent
                            else -> InkMuted
                        }
                    )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = String.format(Locale.US, "%.1f", state.currentElixir),
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = InkPrimary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = state.phase.label,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                color = InkSecondary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(GraphiteSurface.copy(alpha = surfaceAlpha))
                    .clickable(enabled = !state.isScanningScreen, onClick = onAutoScanClick)
            ) {
                if (state.isScanningScreen) {
                    CircularProgressIndicator(
                        color = InkPrimary,
                        strokeWidth = 1.5.dp,
                        modifier = Modifier.size(12.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.CenterFocusStrong,
                        contentDescription = "Авто-анализ экрана",
                        tint = if (state.isAutoVisionEnabled) StatusReady else InkSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (state.isRunning) GraphiteActive.copy(alpha = surfaceAlpha)
                        else GraphiteSurface.copy(alpha = surfaceAlpha)
                    )
                    .clickable(onClick = onTogglePlayPause)
            ) {
                Icon(
                    imageVector = if (state.isRunning) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = null,
                    tint = InkPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(2.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onToggleMinimize)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = stringResource(R.string.cd_overlay_minimize),
                    tint = InkSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun ExpandedOverlayPanel(
    state: ElixirTrackerState,
    opacity: Float,
    scale: Float,
    effectiveCostProvider: (DeckCard) -> Int,
    onToggleMinimize: () -> Unit,
    onCloseOverlay: () -> Unit,
    onDragDelta: (dx: Float, dy: Float) -> Unit,
    onTogglePlayPause: () -> Unit,
    onReset: () -> Unit,
    onUndo: () -> Unit,
    onSelectPhase: (ElixirPhase) -> Unit,
    onPlayCard: (DeckCard) -> Unit,
    onAutoScanClick: () -> Unit,
    onSelectCandidate: (ClanMemberMatch) -> Unit,
    onDismissScanFeedback: () -> Unit
) {
    // Ultra-compact translucent HUD (196dp base width, 12dp outer radius, 8dp padding, 6dp inner radius)
    val panelWidth = (196 * scale).dp
    val surfaceAlpha = opacity.coerceIn(0.25f, 0.95f)
    val innerAlpha = (surfaceAlpha + 0.12f).coerceAtMost(0.90f)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = GraphiteBg.copy(alpha = surfaceAlpha),
        border = BorderStroke(1.dp, GraphiteHairlineStrong),
        shadowElevation = 6.dp,
        modifier = Modifier
            .width(panelWidth)
            .testTag("overlay_expanded_panel")
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Row 1: Compact Draggable Header + Live Elixir + Controls (28dp height)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(GraphiteSurface.copy(alpha = innerAlpha))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onDragDelta(dragAmount.x, dragAmount.y)
                        }
                    }
                    .padding(horizontal = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.DragIndicator,
                    contentDescription = stringResource(R.string.cd_overlay_drag),
                    tint = InkMuted,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                state.isAutoVisionEnabled && state.isRunning -> StatusReady
                                state.isRunning -> ElixirAccent
                                else -> InkMuted
                            }
                        )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = String.format(Locale.US, "%.1f", state.currentElixir),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = InkPrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                // Clickable phase badge (cycles x1 -> x2 -> x3 -> x1)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(GraphiteRaised.copy(alpha = innerAlpha))
                        .clickable {
                            val next = when (state.phase) {
                                ElixirPhase.SINGLE -> ElixirPhase.DOUBLE
                                ElixirPhase.DOUBLE -> ElixirPhase.TRIPLE
                                ElixirPhase.TRIPLE -> ElixirPhase.SINGLE
                            }
                            onSelectPhase(next)
                        }
                        .padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = state.phase.label,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        color = InkSecondary
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Auto-Vision / Scan toggle button
                OverlayIconButton(
                    onClick = onAutoScanClick,
                    enabled = !state.isScanningScreen,
                    testTag = "overlay_auto_scan_button"
                ) {
                    if (state.isScanningScreen) {
                        CircularProgressIndicator(
                            color = InkPrimary,
                            strokeWidth = 1.5.dp,
                            modifier = Modifier.size(12.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.CenterFocusStrong,
                            contentDescription = "Авто-анализ",
                            tint = if (state.isAutoVisionEnabled) StatusReady else InkSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Play / Pause
                OverlayIconButton(onClick = onTogglePlayPause) {
                    Icon(
                        imageVector = if (state.isRunning) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        tint = if (state.isRunning) ElixirAccent else InkPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Undo (only when available)
                if (state.canUndo) {
                    OverlayIconButton(onClick = onUndo) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Undo,
                            contentDescription = "Отменить",
                            tint = InkSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                } else {
                    OverlayIconButton(onClick = onReset) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Сброс",
                            tint = InkMuted,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                // Minimize
                OverlayIconButton(onClick = onToggleMinimize) {
                    Icon(
                        imageVector = Icons.Outlined.ExpandLess,
                        contentDescription = stringResource(R.string.cd_overlay_minimize),
                        tint = InkSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Close
                OverlayIconButton(onClick = onCloseOverlay) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.cd_overlay_close),
                        tint = InkMuted,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            ElixirTenPipBar(elixir = state.currentElixir, barHeight = 4.dp)
            Spacer(modifier = Modifier.height(6.dp))

            // Row 2: Opponent 4-card hand + next card in cycle OR compact Auto-Vision bar
            if (state.deckOrder.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    state.handCards.forEach { card ->
                        OverlayDeckCardTile(
                            card = card,
                            effectiveCost = effectiveCostProvider(card),
                            isInHand = true,
                            surfaceAlpha = innerAlpha,
                            onClick = { onPlayCard(card) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    val nextCard = state.cycleQueue.firstOrNull()
                    if (nextCard != null) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(28.dp)
                                .background(GraphiteHairlineStrong)
                        )
                        OverlayDeckCardTile(
                            card = nextCard,
                            effectiveCost = effectiveCostProvider(nextCard),
                            isInHand = false,
                            surfaceAlpha = innerAlpha,
                            onClick = { onPlayCard(nextCard) },
                            modifier = Modifier.weight(0.85f)
                        )
                    }
                }
            } else {
                // No manual keypad — single compact auto-analyzer status / activation bar
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = GraphiteSurface.copy(alpha = innerAlpha),
                    border = BorderStroke(
                        1.dp,
                        if (state.isAutoVisionEnabled) StatusReady.copy(alpha = 0.45f) else GraphiteHairlineStrong
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .clickable(enabled = !state.isScanningScreen, onClick = onAutoScanClick)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (state.isAutoVisionEnabled) StatusReady else InkMuted)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                state.isScanningScreen -> "Сканируем арену..."
                                state.isAutoVisionEnabled && state.lastPlayedCardName != null ->
                                    "${state.lastPlayedCardName} · −${state.lastPlayedCost ?: 0}"
                                state.isAutoVisionEnabled -> "Авто-анализ боя активен"
                                else -> "Включить авто-анализ боя"
                            },
                            fontFamily = InterFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = InkPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Optional compact status feedback or disambiguation candidates
            if (!state.scanStatusMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(GraphiteSurface.copy(alpha = innerAlpha))
                        .clickable(onClick = onDismissScanFeedback)
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = state.scanStatusMessage,
                        fontSize = 10.sp,
                        color = InkSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Скрыть",
                        tint = InkMuted,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            if (state.scanCandidates.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    state.scanCandidates.take(3).forEach { candidate ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = GraphiteRaised.copy(alpha = innerAlpha),
                            border = BorderStroke(1.dp, GraphiteHairlineStrong),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                                .clickable { onSelectCandidate(candidate) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            ) {
                                Text(
                                    text = "${candidate.name} · ${candidate.clanName}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = InkPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = candidate.trophies.toString(),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    color = InkSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverlayIconButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    testTag: String? = null,
    content: @Composable () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(4.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .let { mod -> if (testTag != null) mod.testTag(testTag) else mod }
    ) {
        content()
    }
}

@Composable
fun ElixirTenPipBar(
    elixir: Float,
    modifier: Modifier = Modifier,
    barHeight: Dp = 8.dp
) {
    // Solid single accent bar per unslop Pass 2 & Pass 7 (no hue gradient)
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        for (i in 0 until 10) {
            val fill = (elixir - i).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(barHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(GraphiteRaised)
            ) {
                if (fill > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fill)
                            .height(barHeight)
                            .background(ElixirAccent)
                    )
                }
            }
        }
    }
}

@Composable
private fun OverlayDeckCardTile(
    card: DeckCard,
    effectiveCost: Int,
    isInHand: Boolean,
    surfaceAlpha: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isInHand) GraphiteHairlineStrong else GraphiteHairline
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isInHand) GraphiteSurface.copy(alpha = surfaceAlpha)
                else GraphiteBg.copy(alpha = surfaceAlpha * 0.75f)
            )
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .alpha(if (isInHand) 1f else 0.62f)
            .clickable(onClick = onClick)
    ) {
        if (card.activeIconUrl.isNotBlank()) {
            AsyncImage(
                model = card.activeIconUrl,
                contentDescription = stringResource(R.string.cd_card_icon, card.name),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .padding(1.dp)
            )
        } else {
            Text(
                text = card.name.take(3),
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                color = InkPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(1.dp)
                .size(14.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(ElixirAccent)
        ) {
            Text(
                text = effectiveCost.toString(),
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 9.sp,
                color = Color.White
            )
        }
    }
}
