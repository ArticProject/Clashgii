package com.example.domain

enum class ApiEndpointMode(val id: String, val label: String, val baseUrl: String) {
    ROYALE_API_PROXY(
        id = "royaleapi",
        label = "Прокси RoyaleAPI",
        baseUrl = "https://proxy.royaleapi.dev/v1/"
    ),
    OFFICIAL_DIRECT(
        id = "official",
        label = "Прямой официальный API",
        baseUrl = "https://api.clashroyale.com/v1/"
    ),
    CUSTOM_PROXY(
        id = "custom",
        label = "Свой прокси-сервер",
        baseUrl = ""
    );

    companion object {
        fun fromId(id: String?): ApiEndpointMode =
            entries.firstOrNull { it.id == id } ?: ROYALE_API_PROXY
    }
}

enum class ElixirPhase(
    val label: String,
    val cadenceLabel: String,
    val multiplier: Int,
    val elixirPerSecond: Float
) {
    SINGLE("x1", "x1 · 2.8с", 1, 1.0f / 2.8f),
    DOUBLE("x2", "x2 · 1.4с", 2, 1.0f / 1.4f),
    TRIPLE("x3", "x3 · 0.9с", 3, 1.0f / 0.93333334f)
}

data class DeckCard(
    val id: Int,
    val name: String,
    val elixirCost: Int,
    val level: Int = 11,
    val maxLevel: Int = 15,
    val evolutionLevel: Int = 0,
    val iconUrl: String = "",
    val evolutionIconUrl: String? = null,
    val isMirror: Boolean = name.equals("Mirror", ignoreCase = true) || id == 28000006
) {
    val activeIconUrl: String
        get() = if (evolutionLevel > 0 && !evolutionIconUrl.isNullOrBlank()) {
            evolutionIconUrl
        } else {
            iconUrl
        }
}

data class PlayerScoutProfile(
    val tag: String,
    val name: String,
    val expLevel: Int,
    val trophies: Int,
    val bestTrophies: Int,
    val wins: Int,
    val losses: Int,
    val clanName: String?,
    val clanTag: String?,
    val arenaName: String?,
    val currentDeck: List<DeckCard>,
    val deckSourceLabel: String = "Текущая колода профиля",
    val battleLogDecks: List<BattleOpponentSummary> = emptyList()
) {
    val averageElixir: Double
        get() {
            val validCosts = currentDeck.filter { it.elixirCost > 0 }.map { it.elixirCost }
            return if (validCosts.isEmpty()) 0.0 else validCosts.average()
        }

    val fourCardCycleCost: Int
        get() = currentDeck
            .map { if (it.elixirCost > 0) it.elixirCost else 1 }
            .sorted()
            .take(4)
            .sum()
}

data class BattleOpponentSummary(
    val tag: String,
    val name: String,
    val clanName: String?,
    val trophies: Int?,
    val playerCrowns: Int,
    val opponentCrowns: Int,
    val battleTime: String,
    val gameMode: String,
    val cards: List<DeckCard>
) {
    val averageElixir: Double
        get() {
            val validCosts = cards.filter { it.elixirCost > 0 }.map { it.elixirCost }
            return if (validCosts.isEmpty()) 0.0 else validCosts.average()
        }
}

data class ClanSummary(
    val tag: String,
    val name: String,
    val membersCount: Int,
    val clanScore: Int
)

data class ClanMemberMatch(
    val tag: String,
    val name: String,
    val role: String,
    val trophies: Int,
    val clanName: String,
    val clanTag: String
)

sealed interface UiResult<out T> {
    data object Idle : UiResult<Nothing>
    data object Loading : UiResult<Nothing>
    data class Success<T>(val data: T) : UiResult<T>
    data class Error(val message: String, val isIpOrAuthError: Boolean = false) : UiResult<Nothing>
}
