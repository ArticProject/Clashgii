package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ApiReadableException
import com.example.data.ClashRoyaleRepository
import com.example.data.SavedOpponentDeck
import com.example.data.local.AppSettings
import com.example.data.local.SettingsStore
import com.example.domain.ApiEndpointMode
import com.example.domain.BattleOpponentSummary
import com.example.domain.ClanMemberMatch
import com.example.domain.ClanSummary
import com.example.domain.DeckCard
import com.example.domain.ElixirCycleEngine
import com.example.domain.ElixirPhase
import com.example.domain.ElixirTrackerState
import com.example.domain.PlayerScoutProfile
import com.example.domain.UiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ScoutSearchMode(val label: String) {
    TAG("По тегу #"),
    CLAN_NICK("Клан + Ник"),
    MY_BATTLES("Мои бои")
}

data class ScoutUiState(
    val searchMode: ScoutSearchMode = ScoutSearchMode.TAG,
    val tagQuery: String = "",
    val clanQuery: String = "",
    val nicknameQuery: String = "",
    val profileResult: UiResult<PlayerScoutProfile> = UiResult.Idle,
    val clanSearchResults: List<ClanSummary> = emptyList(),
    val clanMemberMatches: List<ClanMemberMatch> = emptyList(),
    val clanSearchState: UiResult<Unit> = UiResult.Idle,
    val myBattleOpponents: UiResult<List<BattleOpponentSummary>> = UiResult.Idle,
    val statusMessage: String? = null
)

class MainViewModel(
    private val repository: ClashRoyaleRepository,
    private val settingsStore: SettingsStore,
    private val elixirEngine: ElixirCycleEngine
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsStore.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    val trackerState: StateFlow<ElixirTrackerState> = elixirEngine.state

    val recentOpponents: StateFlow<List<SavedOpponentDeck>> = repository.recentOpponentsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val cardCatalog: StateFlow<List<DeckCard>> = repository.cardCatalogFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _scoutState = MutableStateFlow(ScoutUiState())
    val scoutState: StateFlow<ScoutUiState> = _scoutState.asStateFlow()

    private val _apiTestResult = MutableStateFlow<UiResult<String>>(UiResult.Idle)
    val apiTestResult: StateFlow<UiResult<String>> = _apiTestResult.asStateFlow()

    fun setSearchMode(mode: ScoutSearchMode) {
        _scoutState.update { it.copy(searchMode = mode, statusMessage = null) }
    }

    fun updateTagQuery(value: String) {
        _scoutState.update { it.copy(tagQuery = value) }
    }

    fun updateClanQuery(value: String) {
        _scoutState.update { it.copy(clanQuery = value) }
    }

    fun updateNicknameQuery(value: String) {
        _scoutState.update { it.copy(nicknameQuery = value) }
    }

    fun clearStatusMessage() {
        _scoutState.update { it.copy(statusMessage = null) }
    }

    fun scoutByTag(rawTag: String = _scoutState.value.tagQuery) {
        val cleanTag = repository.normalizeTag(rawTag)
        _scoutState.update {
            it.copy(
                tagQuery = cleanTag,
                profileResult = UiResult.Loading,
                statusMessage = null
            )
        }
        viewModelScope.launch {
            val result = repository.scoutPlayerByTag(cleanTag)
            result.fold(
                onSuccess = { profile ->
                    _scoutState.update { it.copy(profileResult = UiResult.Success(profile)) }
                },
                onFailure = { err ->
                    val apiErr = err as? ApiReadableException
                    _scoutState.update {
                        it.copy(
                            profileResult = UiResult.Error(
                                message = err.message ?: "Ошибка поиска профиля",
                                isIpOrAuthError = apiErr?.isAuthOrIp == true
                            )
                        )
                    }
                }
            )
        }
    }

    fun searchByClanAndNick() {
        val current = _scoutState.value
        _scoutState.update {
            it.copy(
                clanSearchState = UiResult.Loading,
                clanSearchResults = emptyList(),
                clanMemberMatches = emptyList()
            )
        }
        viewModelScope.launch {
            val res = repository.findMembersByClanAndNickname(
                clanQuery = current.clanQuery,
                nicknameQuery = current.nicknameQuery
            )
            res.fold(
                onSuccess = { (clans, members) ->
                    _scoutState.update {
                        it.copy(
                            clanSearchState = UiResult.Success(Unit),
                            clanSearchResults = clans,
                            clanMemberMatches = members
                        )
                    }
                    // If an exact unique member match was found, auto-load their deck!
                    if (members.size == 1) {
                        scoutByTag(members.first().tag)
                    }
                },
                onFailure = { err ->
                    val apiErr = err as? ApiReadableException
                    _scoutState.update {
                        it.copy(
                            clanSearchState = UiResult.Error(
                                message = err.message ?: "Ошибка поиска клана",
                                isIpOrAuthError = apiErr?.isAuthOrIp == true
                            )
                        )
                    }
                }
            )
        }
    }

    fun loadClanMembers(clan: ClanSummary) {
        val nickFilter = _scoutState.value.nicknameQuery
        _scoutState.update { it.copy(clanSearchState = UiResult.Loading) }
        viewModelScope.launch {
            val res = repository.searchMembersInClan(clan.tag, clan.name, nickFilter)
            res.fold(
                onSuccess = { members ->
                    _scoutState.update {
                        it.copy(
                            clanSearchState = UiResult.Success(Unit),
                            clanMemberMatches = members
                        )
                    }
                },
                onFailure = { err ->
                    val apiErr = err as? ApiReadableException
                    _scoutState.update {
                        it.copy(
                            clanSearchState = UiResult.Error(
                                message = err.message ?: "Не удалось загрузить участников клана",
                                isIpOrAuthError = apiErr?.isAuthOrIp == true
                            )
                        )
                    }
                }
            )
        }
    }

    fun loadMyBattleOpponents(customTag: String? = null) {
        val tagToUse = customTag ?: settings.value.myPlayerTag
        _scoutState.update { it.copy(myBattleOpponents = UiResult.Loading) }
        viewModelScope.launch {
            if (!customTag.isNullOrBlank()) {
                settingsStore.saveMyPlayerTag(repository.normalizeTag(customTag))
            }
            val res = repository.fetchBattleOpponents(tagToUse)
            res.fold(
                onSuccess = { opponents ->
                    _scoutState.update { it.copy(myBattleOpponents = UiResult.Success(opponents)) }
                },
                onFailure = { err ->
                    val apiErr = err as? ApiReadableException
                    _scoutState.update {
                        it.copy(
                            myBattleOpponents = UiResult.Error(
                                message = err.message ?: "Не удалось загрузить историю боёв",
                                isIpOrAuthError = apiErr?.isAuthOrIp == true
                            )
                        )
                    }
                }
            )
        }
    }

    fun loadDeckIntoTracker(
        opponentName: String,
        opponentTag: String,
        cards: List<DeckCard>,
        resetTimer: Boolean = true
    ) {
        elixirEngine.loadOpponentDeck(
            opponentName = opponentName,
            opponentTag = opponentTag,
            cards = cards,
            resetTimer = resetTimer
        )
        _scoutState.update {
            it.copy(statusMessage = "Колода $opponentName загружена в трекер")
        }
    }

    fun loadSavedOpponent(saved: SavedOpponentDeck) {
        loadDeckIntoTracker(saved.name, saved.tag, saved.cards, resetTimer = true)
    }

    fun deleteSavedOpponent(tag: String) {
        viewModelScope.launch {
            repository.deleteRecentOpponent(tag)
        }
    }

    // Tracker controls
    fun toggleMatchRunning() = elixirEngine.toggleRunning()
    fun resetMatch() = elixirEngine.resetMatch()
    fun setPhase(phase: ElixirPhase) = elixirEngine.setPhase(phase)
    fun adjustElixir(delta: Float) = elixirEngine.adjustElixir(delta)
    fun playCard(card: DeckCard) = elixirEngine.playCard(card)
    fun playGenericCost(cost: Int) = elixirEngine.playGenericCost(cost)
    fun undoLastAction() = elixirEngine.undoLastAction()
    fun clearTrackerDeck() = elixirEngine.clearDeck()
    fun effectiveCardCost(card: DeckCard): Int = elixirEngine.effectiveCardCost(card)

    // Settings & API test
    fun saveApiConfig(token: String, mode: ApiEndpointMode, customBaseUrl: String, myTag: String) {
        val resolvedUrl = resolveBaseUrl(mode, customBaseUrl)
        repository.applyApiConfigImmediately(token, resolvedUrl)
        viewModelScope.launch {
            settingsStore.saveApiConfig(token, mode, customBaseUrl)
            if (myTag.isNotBlank()) {
                settingsStore.saveMyPlayerTag(repository.normalizeTag(myTag))
            } else {
                settingsStore.saveMyPlayerTag("")
            }
            _apiTestResult.value = UiResult.Success("Настройки сохранены")
        }
    }

    fun testApiAndSyncCards(token: String, mode: ApiEndpointMode, customBaseUrl: String) {
        val resolvedUrl = resolveBaseUrl(mode, customBaseUrl)
        repository.applyApiConfigImmediately(token, resolvedUrl)
        _apiTestResult.value = UiResult.Loading
        viewModelScope.launch {
            settingsStore.saveApiConfig(token, mode, customBaseUrl)
            val res = repository.syncCardCatalog()
            res.fold(
                onSuccess = { count ->
                    _apiTestResult.value = UiResult.Success("Подключено · $count карт в кэше")
                },
                onFailure = { err ->
                    val apiErr = err as? ApiReadableException
                    _apiTestResult.value = UiResult.Error(
                        message = err.message ?: "Ошибка подключения",
                        isIpOrAuthError = apiErr?.isAuthOrIp == true
                    )
                }
            )
        }
    }

    private fun resolveBaseUrl(mode: ApiEndpointMode, customBaseUrl: String): String {
        return when (mode) {
            ApiEndpointMode.ROYALE_API_PROXY, ApiEndpointMode.OFFICIAL_DIRECT -> mode.baseUrl
            ApiEndpointMode.CUSTOM_PROXY -> customBaseUrl.trim().let { raw ->
                when {
                    raw.isBlank() -> ApiEndpointMode.ROYALE_API_PROXY.baseUrl
                    !raw.endsWith("/") -> "$raw/"
                    else -> raw
                }
            }
        }
    }

    fun syncCardsInBackground() {
        viewModelScope.launch {
            repository.syncCardCatalog()
        }
    }

    fun saveOverlayPreferences(
        opacity: Float,
        scale: Float,
        startingElixir: Float,
        autoPhaseSwitch: Boolean
    ) {
        viewModelScope.launch {
            settingsStore.saveOverlayPreferences(opacity, scale, startingElixir, autoPhaseSwitch)
        }
    }

    class Factory(
        private val repository: ClashRoyaleRepository,
        private val settingsStore: SettingsStore,
        private val elixirEngine: ElixirCycleEngine
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository, settingsStore, elixirEngine) as T
        }
    }
}
