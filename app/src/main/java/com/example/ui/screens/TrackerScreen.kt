package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.DeckCard
import com.example.domain.ElixirPhase
import com.example.domain.ElixirTrackerState
import com.example.overlay.ElixirTenPipBar
import com.example.ui.components.DeckCardTile
import com.example.ui.components.OverlayControlAction
import com.example.ui.theme.ActionLight
import com.example.ui.theme.ActionLightInk
import com.example.ui.theme.ElixirAccent
import com.example.ui.theme.GraphiteActive
import com.example.ui.theme.GraphiteHairline
import com.example.ui.theme.GraphiteHairlineStrong
import com.example.ui.theme.GraphiteRaised
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StatusReady
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerScreen(
    state: ElixirTrackerState,
    cardCatalog: List<DeckCard>,
    effectiveCostProvider: (DeckCard) -> Int,
    onTogglePlayPause: () -> Unit,
    onReset: () -> Unit,
    onSelectPhase: (ElixirPhase) -> Unit,
    onAdjustElixir: (Float) -> Unit,
    onPlayCard: (DeckCard) -> Unit,
    onPlayGenericCost: (Int) -> Unit,
    onUndo: () -> Unit,
    onClearDeck: () -> Unit,
    onLoadManualDeck: (List<DeckCard>) -> Unit,
    onSyncCatalog: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeckPickerSheet by remember { mutableStateOf(false) }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
        ) {
            item {
                OverlayControlAction(isOverlayActive = state.isOverlayActive)
            }

            // Live Elixir Readout Card (16dp panel radius, 20dp padding, 8dp inner controls)
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = GraphiteSurface,
                    border = BorderStroke(1.dp, GraphiteHairlineStrong),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = state.opponentName,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = InkPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = buildString {
                                        if (state.opponentTag.isNotBlank()) {
                                            append(state.opponentTag)
                                            append(" · ")
                                        }
                                        append(state.formattedMatchClock)
                                        append(" · ")
                                        append(state.phase.cadenceLabel)
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = InkSecondary
                                )
                            }
                            if (state.deckOrder.isNotEmpty()) {
                                TextButton(onClick = onClearDeck) {
                                    Icon(
                                        imageVector = Icons.Outlined.Clear,
                                        contentDescription = null,
                                        tint = InkSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Сброс колоды", color = InkSecondary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(ElixirAccent)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = String.format(Locale.US, "%.1f", state.currentElixir),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 40.sp,
                                    color = InkPrimary,
                                    modifier = Modifier.testTag("tracker_elixir_value")
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "/ 10.0",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 16.sp,
                                    color = InkMuted,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                ElixirPhase.entries.forEach { phase ->
                                    val selected = state.phase == phase
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (selected) GraphiteActive else GraphiteRaised,
                                        border = BorderStroke(
                                            1.dp,
                                            if (selected) GraphiteHairlineStrong else GraphiteHairline
                                        ),
                                        modifier = Modifier
                                            .height(36.dp)
                                            .minimumInteractiveComponentSize()
                                            .clickable { onSelectPhase(phase) }
                                            .testTag("phase_${phase.label}")
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(horizontal = 12.dp)
                                        ) {
                                            Text(
                                                text = phase.label,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 13.sp,
                                                color = if (selected) InkPrimary else InkSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        ElixirTenPipBar(elixir = state.currentElixir)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Match Playback & Calibration Controls
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = onTogglePlayPause,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (state.isRunning) GraphiteActive else ActionLight,
                                    contentColor = if (state.isRunning) InkPrimary else ActionLightInk
                                ),
                                modifier = Modifier
                                    .weight(1.6f)
                                    .height(48.dp)
                                    .testTag("tracker_play_pause")
                            ) {
                                Icon(
                                    imageVector = if (state.isRunning) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (state.isRunning) "Пауза" else "Старт боя",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            OutlinedButton(
                                onClick = onUndo,
                                enabled = state.canUndo,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, GraphiteHairlineStrong),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("tracker_undo")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Undo,
                                    contentDescription = "Отмена",
                                    tint = InkPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            OutlinedButton(
                                onClick = onReset,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, GraphiteHairlineStrong),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("tracker_reset")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = "Сброс",
                                    tint = InkSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Fine Calibration Nudges
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(-1.0f to "−1.0", -0.5f to "−0.5", 0.5f to "+0.5", 1.0f to "+1.0").forEach { (delta, label) ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = GraphiteRaised,
                                    border = BorderStroke(1.dp, GraphiteHairline),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .clickable { onAdjustElixir(delta) }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = label,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = InkPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Deck Cycle Interactive Area OR Universal Keypad (Sentence case labels, no ALL CAPS)
            if (state.deckOrder.isNotEmpty()) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(StatusReady)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "В руке соперника · нажмите при выходе карты",
                            style = MaterialTheme.typography.titleMedium,
                            color = InkPrimary
                        )
                    }
                }

                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        state.handCards.forEach { card ->
                            DeckCardTile(
                                card = card,
                                displayCost = effectiveCostProvider(card),
                                isInHand = true,
                                onClick = { onPlayCard(card) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                if (state.cycleQueue.isNotEmpty()) {
                    item {
                        Text(
                            text = "Очередь цикла · следующие 4 карты",
                            style = MaterialTheme.typography.titleMedium,
                            color = InkSecondary
                        )
                    }
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(0.64f)
                        ) {
                            state.cycleQueue.forEach { card ->
                                DeckCardTile(
                                    card = card,
                                    displayCost = effectiveCostProvider(card),
                                    isInHand = false,
                                    onClick = { onPlayCard(card) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            } else {
                item {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = GraphiteSurface,
                        border = BorderStroke(1.dp, GraphiteHairline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (state.isAutoVisionEnabled) StatusReady else InkMuted)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (state.isAutoVisionEnabled) {
                                        "Авто-анализ арены активен"
                                    } else {
                                        "Автоматический анализ боя"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = InkPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Оверлей автоматически распознаёт ник и клан соперника в начале боя, подгружает его 8 карт через API и отслеживает высадку вражеских юнитов на арене.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkSecondary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = {
                                        if (android.provider.Settings.canDrawOverlays(context)) {
                                            com.example.overlay.ScreenCapturePermissionActivity.launch(context)
                                        } else {
                                            com.example.ui.components.openOverlayPermissionSettings(context)
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ActionLight,
                                        contentColor = ActionLightInk
                                    ),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(44.dp)
                                ) {
                                    Text(
                                        text = if (state.isAutoVisionEnabled) "Перезапустить скан" else "Включить авто-анализ",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        if (cardCatalog.isEmpty()) onSyncCatalog()
                                        showDeckPickerSheet = true
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, GraphiteHairlineStrong),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Style,
                                        contentDescription = null,
                                        tint = InkSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Собрать 8 карт", style = MaterialTheme.typography.labelLarge, color = InkPrimary)
                                }
                            }
                        }
                    }
                }
            }

            if (state.lastPlayedCardName != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = GraphiteSurface,
                        border = BorderStroke(1.dp, GraphiteHairline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Последний ход: ${state.lastPlayedCardName} · −${state.lastPlayedCost ?: 0} эл",
                            style = MaterialTheme.typography.labelMedium,
                            color = InkSecondary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showDeckPickerSheet) {
        ManualDeckPickerSheet(
            catalog = cardCatalog,
            onDismiss = { showDeckPickerSheet = false },
            onConfirmDeck = { selectedCards ->
                onLoadManualDeck(selectedCards)
                showDeckPickerSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualDeckPickerSheet(
    catalog: List<DeckCard>,
    onDismiss: () -> Unit,
    onConfirmDeck: (List<DeckCard>) -> Unit
) {
    var filterText by remember { mutableStateOf("") }
    val selected = remember { mutableStateListOf<DeckCard>() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = GraphiteSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Выбор карт · ${selected.size}/8",
                    style = MaterialTheme.typography.titleLarge,
                    color = InkPrimary,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { onConfirmDeck(selected.toList()) },
                    enabled = selected.isNotEmpty(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ActionLight,
                        contentColor = ActionLightInk
                    )
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Готово", style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = filterText,
                onValueChange = { filterText = it },
                placeholder = { Text("Поиск карты") },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (catalog.isEmpty()) {
                Text(
                    text = "Каталог карт пуст. Нажмите «Проверить API» во вкладке «API и HUD»",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            } else {
                val filtered = catalog.filter {
                    filterText.isBlank() || it.name.contains(filterText, ignoreCase = true)
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(340.dp)
                ) {
                    items(filtered, key = { it.id }) { card ->
                        val isPicked = selected.any { it.id == card.id }
                        Box(
                            modifier = Modifier.alpha(if (isPicked) 0.44f else 1f)
                        ) {
                            DeckCardTile(
                                card = card,
                                isInHand = isPicked,
                                onClick = {
                                    if (isPicked) {
                                        selected.removeAll { it.id == card.id }
                                    } else if (selected.size < 8) {
                                        selected.add(card)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
