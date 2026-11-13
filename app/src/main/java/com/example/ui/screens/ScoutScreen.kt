package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SavedOpponentDeck
import com.example.data.local.AppSettings
import com.example.domain.BattleOpponentSummary
import com.example.domain.ClanMemberMatch
import com.example.domain.ClanSummary
import com.example.domain.PlayerScoutProfile
import com.example.domain.UiResult
import com.example.ui.ScoutSearchMode
import com.example.ui.ScoutUiState
import com.example.ui.components.DeckCardTile
import com.example.ui.components.MissingTokenBanner
import com.example.ui.components.OverlayControlAction
import com.example.ui.components.ensureAndStartOverlay
import com.example.ui.theme.ActionLight
import com.example.ui.theme.ActionLightInk
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
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusDangerSurface
import com.example.ui.theme.StatusReady
import java.util.Locale

@Composable
fun ScoutScreen(
    scoutState: ScoutUiState,
    settings: AppSettings,
    isOverlayActive: Boolean,
    recentOpponents: List<SavedOpponentDeck>,
    onSelectSearchMode: (ScoutSearchMode) -> Unit,
    onTagQueryChange: (String) -> Unit,
    onClanQueryChange: (String) -> Unit,
    onNicknameQueryChange: (String) -> Unit,
    onScoutByTag: (String) -> Unit,
    onSearchClanNick: () -> Unit,
    onSelectClan: (ClanSummary) -> Unit,
    onLoadMyBattles: (String?) -> Unit,
    onLoadProfileToTracker: (PlayerScoutProfile, Boolean) -> Unit,
    onLoadBattleOpponentToTracker: (BattleOpponentSummary, Boolean) -> Unit,
    onLoadSavedOpponent: (SavedOpponentDeck, Boolean) -> Unit,
    onDeleteSavedOpponent: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTracker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var myTagInput by remember(settings.myPlayerTag) { mutableStateOf(settings.myPlayerTag) }

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
            // Quiet Top Header (unslop Pass 8: no decorative generated hero banner)
            item {
                ScoutTopHeader(
                    hasToken = settings.hasToken,
                    endpointLabel = settings.endpointMode.label
                )
            }

            if (!settings.hasToken) {
                item {
                    MissingTokenBanner(onOpenSettings = onOpenSettings)
                }
            }

            // Overlay Quick Control
            item {
                OverlayControlAction(isOverlayActive = isOverlayActive)
            }

            // Search Mode Segmented Switcher
            item {
                SearchModeSelector(
                    selected = scoutState.searchMode,
                    onSelect = onSelectSearchMode
                )
            }

            // Mode-specific Search Card
            item {
                when (scoutState.searchMode) {
                    ScoutSearchMode.TAG -> {
                        TagSearchCard(
                            tagQuery = scoutState.tagQuery,
                            onTagQueryChange = onTagQueryChange,
                            onPasteFromClipboard = {
                                val text = clipboard.getText()?.text.orEmpty()
                                if (text.isNotBlank()) {
                                    onTagQueryChange(text)
                                    onScoutByTag(text)
                                }
                            },
                            onSearch = { onScoutByTag(scoutState.tagQuery) },
                            isLoading = scoutState.profileResult is UiResult.Loading
                        )
                    }

                    ScoutSearchMode.CLAN_NICK -> {
                        ClanNickSearchCard(
                            clanQuery = scoutState.clanQuery,
                            nickQuery = scoutState.nicknameQuery,
                            onClanQueryChange = onClanQueryChange,
                            onNickQueryChange = onNicknameQueryChange,
                            onSearch = onSearchClanNick,
                            isLoading = scoutState.clanSearchState is UiResult.Loading
                        )
                    }

                    ScoutSearchMode.MY_BATTLES -> {
                        MyBattlesCard(
                            myTag = myTagInput,
                            onMyTagChange = { myTagInput = it },
                            onFetch = { onLoadMyBattles(myTagInput) },
                            isLoading = scoutState.myBattleOpponents is UiResult.Loading
                        )
                    }
                }
            }

            // Clan / Member Search Results (when in CLAN_NICK mode)
            if (scoutState.searchMode == ScoutSearchMode.CLAN_NICK) {
                when (val clanState = scoutState.clanSearchState) {
                    is UiResult.Error -> {
                        item {
                            ErrorBannerCard(
                                message = clanState.message,
                                showSettingsButton = clanState.isIpOrAuthError,
                                onOpenSettings = onOpenSettings
                            )
                        }
                    }

                    is UiResult.Success -> {
                        if (scoutState.clanMemberMatches.isNotEmpty()) {
                            item {
                                SectionHeaderRow(
                                    title = "Игроки",
                                    meta = scoutState.clanMemberMatches.size.toString()
                                )
                            }
                            items(scoutState.clanMemberMatches, key = { it.tag }) { member ->
                                MemberMatchRow(
                                    member = member,
                                    onClick = { onScoutByTag(member.tag) }
                                )
                            }
                        } else if (scoutState.clanSearchResults.isNotEmpty()) {
                            item {
                                SectionHeaderRow(
                                    title = "Кланы",
                                    meta = scoutState.clanSearchResults.size.toString()
                                )
                            }
                            items(scoutState.clanSearchResults, key = { it.tag }) { clan ->
                                ClanResultRow(
                                    clan = clan,
                                    onClick = { onSelectClan(clan) }
                                )
                            }
                        } else {
                            item {
                                EmptyHintCard(
                                    title = "Совпадений не найдено",
                                    subtitle = "Проверьте название клана (от 3 символов) или оставьте ник пустым для просмотра участников"
                                )
                            }
                        }
                    }

                    else -> Unit
                }
            }

            // My Battlelog Opponents (when in MY_BATTLES mode)
            if (scoutState.searchMode == ScoutSearchMode.MY_BATTLES) {
                when (val battlesState = scoutState.myBattleOpponents) {
                    is UiResult.Error -> {
                        item {
                            ErrorBannerCard(
                                message = battlesState.message,
                                showSettingsButton = battlesState.isIpOrAuthError,
                                onOpenSettings = onOpenSettings
                            )
                        }
                    }

                    is UiResult.Success -> {
                        if (battlesState.data.isEmpty()) {
                            item {
                                EmptyHintCard(
                                    title = "История боёв пуста",
                                    subtitle = "Сыграйте матч и обновите список для просмотра колод соперников"
                                )
                            }
                        } else {
                            item {
                                SectionHeaderRow(
                                    title = "Недавние соперники",
                                    meta = battlesState.data.size.toString()
                                )
                            }
                            items(battlesState.data.take(10)) { opp ->
                                BattleOpponentCard(
                                    opponent = opp,
                                    onScoutFullProfile = { onScoutByTag(opp.tag) },
                                    onLoadToOverlay = {
                                        onLoadBattleOpponentToTracker(opp, true)
                                        ensureAndStartOverlay(context)
                                    }
                                )
                            }
                        }
                    }

                    else -> Unit
                }
            }

            // Main Scouted Player Profile Result
            when (val profileRes = scoutState.profileResult) {
                is UiResult.Loading -> {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = GraphiteSurface,
                            border = BorderStroke(1.dp, GraphiteHairline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(20.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = InkPrimary,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 1.5.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Загрузка профиля и колоды",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = InkPrimary
                                )
                            }
                        }
                    }
                }

                is UiResult.Error -> {
                    item {
                        ErrorBannerCard(
                            message = profileRes.message,
                            showSettingsButton = profileRes.isIpOrAuthError,
                            onOpenSettings = onOpenSettings
                        )
                    }
                }

                is UiResult.Success -> {
                    item {
                        ScoutedProfileResultCard(
                            profile = profileRes.data,
                            onLoadAndStartOverlay = {
                                onLoadProfileToTracker(profileRes.data, true)
                                ensureAndStartOverlay(context)
                            },
                            onLoadToInAppTracker = {
                                onLoadProfileToTracker(profileRes.data, true)
                                onOpenTracker()
                            }
                        )
                    }
                }

                UiResult.Idle -> Unit
            }

            // Saved / Recent Opponents History
            if (recentOpponents.isNotEmpty()) {
                item {
                    SectionHeaderRow(
                        title = "История колод",
                        meta = recentOpponents.size.toString()
                    )
                }
                items(recentOpponents, key = { it.tag }) { saved ->
                    SavedOpponentRow(
                        saved = saved,
                        onOpenProfile = { onScoutByTag(saved.tag) },
                        onLoadToOverlay = {
                            onLoadSavedOpponent(saved, true)
                            ensureAndStartOverlay(context)
                        },
                        onDelete = { onDeleteSavedOpponent(saved.tag) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun ScoutTopHeader(
    hasToken: Boolean,
    endpointLabel: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "RoyalePulse",
                style = MaterialTheme.typography.headlineMedium,
                color = InkPrimary,
                modifier = Modifier.weight(1f)
            )
            // Signature live status dot + mono cadence/endpoint meta
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (hasToken) StatusReady else StatusDanger)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (hasToken) endpointLabel else "Токен не задан",
                    style = MaterialTheme.typography.labelMedium,
                    color = InkSecondary
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Разведка колоды соперника и счётчик эликсира поверх игры",
            style = MaterialTheme.typography.bodyMedium,
            color = InkSecondary
        )
    }
}

@Composable
private fun SectionHeaderRow(
    title: String,
    meta: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = InkPrimary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = meta,
            style = MaterialTheme.typography.labelMedium,
            color = InkMuted
        )
    }
}

@Composable
private fun SearchModeSelector(
    selected: ScoutSearchMode,
    onSelect: (ScoutSearchMode) -> Unit
) {
    // Outer 12dp radius, 4dp padding -> inner 8dp radius (unslop Pass 7)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = GraphiteSurface,
        border = BorderStroke(1.dp, GraphiteHairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            ScoutSearchMode.entries.forEach { mode ->
                val isSelected = selected == mode
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) GraphiteActive else Color.Transparent,
                    border = if (isSelected) BorderStroke(1.dp, GraphiteHairlineStrong) else null,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .minimumInteractiveComponentSize()
                        .clickable { onSelect(mode) }
                        .testTag("scout_mode_${mode.name.lowercase()}")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) InkPrimary else InkSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TagSearchCard(
    tagQuery: String,
    onTagQueryChange: (String) -> Unit,
    onPasteFromClipboard: () -> Unit,
    onSearch: () -> Unit,
    isLoading: Boolean
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = GraphiteSurface,
        border = BorderStroke(1.dp, GraphiteHairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Поиск по тегу",
                    style = MaterialTheme.typography.titleMedium,
                    color = InkPrimary,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.CenterFocusStrong,
                        contentDescription = null,
                        tint = InkMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "В бою доступен Авто-скан на оверлее",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkMuted
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = tagQuery,
                onValueChange = onTagQueryChange,
                placeholder = {
                    Text(
                        text = "#C0G20PR2",
                        fontFamily = JetBrainsMonoFontFamily,
                        color = InkMuted
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                trailingIcon = {
                    IconButton(
                        onClick = onPasteFromClipboard,
                        modifier = Modifier.testTag("paste_tag_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentPaste,
                            contentDescription = "Вставить из буфера",
                            tint = InkSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GraphiteHairlineStrong,
                    unfocusedBorderColor = GraphiteHairline,
                    focusedTextColor = InkPrimary,
                    unfocusedTextColor = InkPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scout_tag_input")
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onSearch,
                enabled = !isLoading,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ActionLight,
                    contentColor = ActionLightInk
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("scout_search_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Найти колоду", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ClanNickSearchCard(
    clanQuery: String,
    nickQuery: String,
    onClanQueryChange: (String) -> Unit,
    onNickQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    isLoading: Boolean
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = GraphiteSurface,
        border = BorderStroke(1.dp, GraphiteHairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Поиск по клану и нику",
                style = MaterialTheme.typography.titleMedium,
                color = InkPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Название клана с экрана начала боя и ник соперника",
                style = MaterialTheme.typography.bodyMedium,
                color = InkSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = clanQuery,
                onValueChange = onClanQueryChange,
                label = { Text("Название клана") },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = InkSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GraphiteHairlineStrong,
                    unfocusedBorderColor = GraphiteHairline,
                    focusedTextColor = InkPrimary,
                    unfocusedTextColor = InkPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scout_clan_input")
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = nickQuery,
                onValueChange = onNickQueryChange,
                label = { Text("Ник соперника") },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.PersonSearch,
                        contentDescription = null,
                        tint = InkSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GraphiteHairlineStrong,
                    unfocusedBorderColor = GraphiteHairline,
                    focusedTextColor = InkPrimary,
                    unfocusedTextColor = InkPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scout_nick_input")
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onSearch,
                enabled = !isLoading,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ActionLight,
                    contentColor = ActionLightInk
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("scout_clan_search_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = ActionLightInk,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 1.5.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Поиск в клане", style = MaterialTheme.typography.labelLarge)
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Groups,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Найти в клане", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun MyBattlesCard(
    myTag: String,
    onMyTagChange: (String) -> Unit,
    onFetch: () -> Unit,
    isLoading: Boolean
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = GraphiteSurface,
        border = BorderStroke(1.dp, GraphiteHairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Соперники из ваших боёв",
                style = MaterialTheme.typography.titleMedium,
                color = InkPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Точные колоды и теги оппонентов из вашего журнала боёв",
                style = MaterialTheme.typography.bodyMedium,
                color = InkSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = myTag,
                onValueChange = onMyTagChange,
                label = { Text("Ваш тег") },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GraphiteHairlineStrong,
                    unfocusedBorderColor = GraphiteHairline,
                    focusedTextColor = InkPrimary,
                    unfocusedTextColor = InkPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("my_battlelog_tag_input")
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onFetch,
                enabled = !isLoading,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ActionLight,
                    contentColor = ActionLightInk
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("fetch_my_battles_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = ActionLightInk,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 1.5.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Загрузка боёв", style = MaterialTheme.typography.labelLarge)
                } else {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Загрузить мои бои", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun ScoutedProfileResultCard(
    profile: PlayerScoutProfile,
    onLoadAndStartOverlay: () -> Unit,
    onLoadToInAppTracker: () -> Unit
) {
    var showRecentDecks by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = GraphiteSurface,
        border = BorderStroke(1.dp, GraphiteHairlineStrong),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("scouted_profile_card")
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = InkPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = buildString {
                            append(profile.tag)
                            if (!profile.clanName.isNullOrBlank()) {
                                append(" · ")
                                append(profile.clanName)
                            }
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = InkSecondary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = profile.trophies.toString(),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp,
                        color = InkPrimary
                    )
                    Text(
                        text = "трофеи",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = GraphiteHairline)
            Spacer(modifier = Modifier.height(12.dp))

            // Asymmetric Telemetry Strip (One primary figure in ElixirAccent, others in quiet neutral steps)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1.2f)) {
                    Text(
                        text = "Средний эликсир",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = String.format(Locale.US, "%.1f", profile.averageElixir),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                        color = ElixirAccent
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Цикл 4 карт",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${profile.fourCardCycleCost} эл",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = InkPrimary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Победы",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = profile.wins.toString(),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = InkSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = profile.deckSourceLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = InkMuted
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Moment: 2 rows x 4 cards grid carrying the real visual richness
            if (profile.currentDeck.isNotEmpty()) {
                val topRow = profile.currentDeck.take(4)
                val bottomRow = profile.currentDeck.drop(4).take(4)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    topRow.forEach { card ->
                        DeckCardTile(card = card, modifier = Modifier.weight(1f))
                    }
                }
                if (bottomRow.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        bottomRow.forEach { card ->
                            DeckCardTile(card = card, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary action + 1 secondary action side by side
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onLoadAndStartOverlay,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ActionLight,
                        contentColor = ActionLightInk
                    ),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(48.dp)
                        .testTag("load_deck_overlay_button")
                ) {
                    Text("В оверлей", style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = onLoadToInAppTracker,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, GraphiteHairlineStrong),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("load_deck_tracker_button")
                ) {
                    Text("В трекер", style = MaterialTheme.typography.labelLarge, color = InkPrimary)
                }
            }

            if (profile.battleLogDecks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { showRecentDecks = !showRecentDecks },
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Text(
                        text = if (showRecentDecks) {
                            "Скрыть недавние бои"
                        } else {
                            "Недавние бои игрока · ${profile.battleLogDecks.size}"
                        },
                        color = InkSecondary
                    )
                }
                AnimatedVisibility(visible = showRecentDecks) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        profile.battleLogDecks.take(4).forEach { b ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = GraphiteRaised,
                                border = BorderStroke(1.dp, GraphiteHairline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Против ${b.name}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = InkPrimary
                                        )
                                        Text(
                                            text = "${b.gameMode} · ${b.tag}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = InkSecondary
                                        )
                                    }
                                    Text(
                                        text = "${b.playerCrowns}:${b.opponentCrowns}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = InkPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClanResultRow(
    clan: ClanSummary,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = GraphiteSurface,
        border = BorderStroke(1.dp, GraphiteHairline),
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = InkSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(clan.name, style = MaterialTheme.typography.titleMedium, color = InkPrimary)
                Text(
                    text = "${clan.tag} · ${clan.membersCount}/50",
                    style = MaterialTheme.typography.labelMedium,
                    color = InkSecondary
                )
            }
            Text(
                text = clan.clanScore.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = InkSecondary
            )
        }
    }
}

@Composable
private fun MemberMatchRow(
    member: ClanMemberMatch,
    onClick: () -> Unit
) {
    // No emoji! Tabular right-aligned figure
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = GraphiteSurface,
        border = BorderStroke(1.dp, GraphiteHairlineStrong),
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(member.name, style = MaterialTheme.typography.titleMedium, color = InkPrimary)
                Text(
                    text = "${member.tag} · ${member.clanName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = InkSecondary
                )
            }
            Text(
                text = "${member.trophies} кубков",
                style = MaterialTheme.typography.labelMedium,
                color = InkSecondary
            )
        }
    }
}

@Composable
private fun BattleOpponentCard(
    opponent: BattleOpponentSummary,
    onScoutFullProfile: () -> Unit,
    onLoadToOverlay: () -> Unit
) {
    // No emoji! Clean tabular average elixir figure
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = GraphiteSurface,
        border = BorderStroke(1.dp, GraphiteHairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = opponent.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = InkPrimary
                    )
                    Text(
                        text = buildString {
                            append(opponent.tag)
                            if (!opponent.clanName.isNullOrBlank()) append(" · ${opponent.clanName}")
                            append(" · ${opponent.gameMode}")
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = InkSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "${String.format(Locale.US, "%.1f", opponent.averageElixir)} эл",
                    style = MaterialTheme.typography.labelMedium,
                    color = ElixirAccent
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                opponent.cards.take(8).forEach { card ->
                    DeckCardTile(card = card, modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onScoutFullProfile,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, GraphiteHairlineStrong),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .minimumInteractiveComponentSize()
                ) {
                    Text("Профиль", style = MaterialTheme.typography.labelLarge, color = InkPrimary)
                }
                Button(
                    onClick = onLoadToOverlay,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ActionLight,
                        contentColor = ActionLightInk
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .minimumInteractiveComponentSize()
                ) {
                    Text("В оверлей", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun SavedOpponentRow(
    saved: SavedOpponentDeck,
    onOpenProfile: () -> Unit,
    onLoadToOverlay: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = GraphiteSurface,
        border = BorderStroke(1.dp, GraphiteHairline),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenProfile)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(saved.name, style = MaterialTheme.typography.titleMedium, color = InkPrimary)
                Text(
                    text = "${saved.tag} · ${String.format(Locale.US, "%.1f", saved.avgElixir)} эл",
                    style = MaterialTheme.typography.labelMedium,
                    color = InkSecondary
                )
            }
            OutlinedButton(
                onClick = onLoadToOverlay,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, GraphiteHairlineStrong),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(36.dp)
                    .minimumInteractiveComponentSize()
            ) {
                Text("В оверлей", style = MaterialTheme.typography.labelLarge, color = InkPrimary)
            }
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Удалить из истории",
                    tint = InkMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ErrorBannerCard(
    message: String,
    showSettingsButton: Boolean,
    onOpenSettings: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = StatusDangerSurface,
        border = BorderStroke(1.dp, StatusDanger.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = InkPrimary
            )
            if (showSettingsButton) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onOpenSettings,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, GraphiteHairlineStrong),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text("Настройки API", style = MaterialTheme.typography.labelLarge, color = InkPrimary)
                }
            }
        }
    }
}

@Composable
private fun EmptyHintCard(
    title: String,
    subtitle: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = GraphiteSurface,
        border = BorderStroke(1.dp, GraphiteHairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = InkPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
        }
    }
}
