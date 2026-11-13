package com.example.data

import com.example.data.local.CardCatalogEntity
import com.example.data.local.RecentOpponentEntity
import com.example.data.local.RoyaleDao
import com.example.data.remote.CardDto
import com.example.data.remote.ClashRoyaleApi
import com.example.data.remote.DynamicApiConfigHolder
import com.example.domain.BattleOpponentSummary
import com.example.domain.ClanMemberMatch
import com.example.domain.ClanSummary
import com.example.domain.DeckCard
import com.example.domain.PlayerScoutProfile
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

data class SavedOpponentDeck(
    val tag: String,
    val name: String,
    val clanName: String?,
    val trophies: Int,
    val avgElixir: Double,
    val cards: List<DeckCard>,
    val updatedAt: Long
)

class ClashRoyaleRepository(
    private val api: ClashRoyaleApi,
    private val dao: RoyaleDao,
    private val configHolder: DynamicApiConfigHolder,
    private val moshi: Moshi
) {
    private val deckListType = Types.newParameterizedType(List::class.java, DeckCard::class.java)
    private val deckJsonAdapter = moshi.adapter<List<DeckCard>>(deckListType)

    val recentOpponentsFlow: Flow<List<SavedOpponentDeck>> =
        dao.observeRecentOpponents().map { rows ->
            rows.mapNotNull { row ->
                val cards = runCatching { deckJsonAdapter.fromJson(row.deckJson) }.getOrNull()
                    ?: return@mapNotNull null
                SavedOpponentDeck(
                    tag = row.tag,
                    name = row.name,
                    clanName = row.clanName,
                    trophies = row.trophies,
                    avgElixir = row.avgElixir,
                    cards = cards,
                    updatedAt = row.updatedAt
                )
            }
        }

    val cardCatalogFlow: Flow<List<DeckCard>> =
        dao.observeCardCatalog().map { rows ->
            rows.map { row ->
                DeckCard(
                    id = row.id,
                    name = row.name,
                    elixirCost = row.elixirCost,
                    maxLevel = row.maxLevel,
                    iconUrl = row.iconUrl,
                    evolutionIconUrl = row.evolutionIconUrl
                )
            }
        }

    fun normalizeTag(rawInput: String): String {
        val cleaned = rawInput
            .trim()
            .uppercase()
            .removePrefix("HTTPS://LINK.CLASHROYALE.COM/PLAY/RU?TAG=")
            .removePrefix("HTTPS://LINK.CLASHROYALE.COM/?TAG=")
            .substringAfter("TAG=", missingDelimiterValue = rawInput.trim().uppercase())
            .substringBefore("&")
            .replace("#", "")
            .replace(" ", "")
        return if (cleaned.isBlank()) "" else "#$cleaned"
    }

    fun applyApiConfigImmediately(token: String, baseUrl: String) {
        configHolder.update(token = token, baseUrl = baseUrl)
    }

    suspend fun syncCardCatalog(): Result<Int> = withContext(Dispatchers.IO) {
        if (configHolder.currentToken().isBlank()) {
            return@withContext Result.failure(ApiReadableException("Сначала укажите API-токен в настройках.", isAuthOrIp = true))
        }
        try {
            val response = api.getAllCards()
            val entities = response.items.map { dto ->
                CardCatalogEntity(
                    id = dto.id,
                    name = dto.name,
                    elixirCost = dto.elixirCost ?: 3,
                    maxLevel = dto.maxLevel ?: 15,
                    iconUrl = dto.iconUrls?.medium.orEmpty(),
                    evolutionIconUrl = dto.iconUrls?.evolutionMedium
                )
            }
            if (entities.isNotEmpty()) {
                dao.upsertCards(entities)
            }
            Result.success(entities.size)
        } catch (t: Throwable) {
            Result.failure(mapException(t))
        }
    }

    suspend fun scoutPlayerByTag(rawTag: String): Result<PlayerScoutProfile> = withContext(Dispatchers.IO) {
        if (configHolder.currentToken().isBlank()) {
            return@withContext Result.failure(ApiReadableException("Не задан API-токен Clash Royale. Откройте вкладку «API и HUD».", isAuthOrIp = true))
        }
        val normalized = normalizeTag(rawTag)
        if (normalized.length < 4) {
            return@withContext Result.failure(ApiReadableException("Введите корректный тег игрока, например #2PP или #C0G20PR2."))
        }

        try {
            ensureCatalogLoadedSilently()
            val catalogMap = dao.getAllCards().associateBy { it.id }

            val playerDto = api.getPlayer(normalized)
            val battleLogDtos = runCatching { api.getPlayerBattleLog(normalized) }.getOrDefault(emptyList())

            val mappedDeck = (playerDto.currentDeck ?: emptyList()).map { dto ->
                mapCard(dto, catalogMap)
            }

            val mappedBattles = battleLogDtos.mapNotNull { entry ->
                val opponent = entry.opponent?.firstOrNull() ?: return@mapNotNull null
                val playerSide = entry.team?.firstOrNull()
                val cards = (opponent.cards ?: emptyList()).map { mapCard(it, catalogMap) }
                if (cards.isEmpty()) return@mapNotNull null
                BattleOpponentSummary(
                    tag = opponent.tag,
                    name = opponent.name,
                    clanName = opponent.clan?.name,
                    trophies = opponent.startingTrophies,
                    playerCrowns = playerSide?.crowns ?: 0,
                    opponentCrowns = opponent.crowns ?: 0,
                    battleTime = entry.battleTime.orEmpty(),
                    gameMode = entry.gameMode?.name ?: entry.type ?: "Бой",
                    cards = cards
                )
            }

            // Fallback to most recent battle deck used by the scouted player if currentDeck is empty
            val fallbackSelfDeck = if (mappedDeck.size < 8) {
                battleLogDtos.firstOrNull()?.team?.firstOrNull()?.cards?.map { mapCard(it, catalogMap) }.orEmpty()
            } else {
                emptyList()
            }

            val finalDeck = if (mappedDeck.size == 8) mappedDeck else fallbackSelfDeck.ifEmpty { mappedDeck }
            val deckLabel = if (mappedDeck.size == 8) {
                "Активная колода профиля"
            } else {
                "Колода из последнего боя"
            }

            val profile = PlayerScoutProfile(
                tag = playerDto.tag,
                name = playerDto.name,
                expLevel = playerDto.expLevel,
                trophies = playerDto.trophies,
                bestTrophies = playerDto.bestTrophies,
                wins = playerDto.wins,
                losses = playerDto.losses,
                clanName = playerDto.clan?.name,
                clanTag = playerDto.clan?.tag,
                arenaName = playerDto.arena?.name,
                currentDeck = finalDeck,
                deckSourceLabel = deckLabel,
                battleLogDecks = mappedBattles.take(8)
            )

            if (finalDeck.isNotEmpty()) {
                saveRecentOpponent(
                    tag = profile.tag,
                    name = profile.name,
                    clanName = profile.clanName,
                    trophies = profile.trophies,
                    avgElixir = profile.averageElixir,
                    cards = finalDeck
                )
            }

            Result.success(profile)
        } catch (t: Throwable) {
            Result.failure(mapException(t))
        }
    }

    suspend fun searchClans(clanNameQuery: String): Result<List<ClanSummary>> = withContext(Dispatchers.IO) {
        if (configHolder.currentToken().isBlank()) {
            return@withContext Result.failure(ApiReadableException("Не задан API-токен Clash Royale.", isAuthOrIp = true))
        }
        val query = clanNameQuery.trim()
        if (query.length < 3) {
            return@withContext Result.failure(ApiReadableException("Название клана должно содержать минимум 3 символа."))
        }
        try {
            val res = api.searchClans(query, limit = 12)
            Result.success(
                res.items.map {
                    ClanSummary(
                        tag = it.tag,
                        name = it.name,
                        membersCount = it.members,
                        clanScore = it.clanScore
                    )
                }
            )
        } catch (t: Throwable) {
            Result.failure(mapException(t))
        }
    }

    suspend fun searchMembersInClan(
        clanTag: String,
        clanName: String,
        nicknameFilter: String
    ): Result<List<ClanMemberMatch>> = withContext(Dispatchers.IO) {
        if (configHolder.currentToken().isBlank()) {
            return@withContext Result.failure(ApiReadableException("Не задан API-токен Clash Royale.", isAuthOrIp = true))
        }
        try {
            val normalizedClanTag = normalizeTag(clanTag)
            val response = api.getClanMembers(normalizedClanTag)
            val filter = nicknameFilter.trim()
            val mapped = response.items
                .filter { filter.isBlank() || it.name.contains(filter, ignoreCase = true) }
                .map {
                    ClanMemberMatch(
                        tag = it.tag,
                        name = it.name,
                        role = it.role,
                        trophies = it.trophies,
                        clanName = clanName,
                        clanTag = normalizedClanTag
                    )
                }
            Result.success(mapped)
        } catch (t: Throwable) {
            Result.failure(mapException(t))
        }
    }

    suspend fun findMembersByClanAndNickname(
        clanQuery: String,
        nicknameQuery: String
    ): Result<Pair<List<ClanSummary>, List<ClanMemberMatch>>> = withContext(Dispatchers.IO) {
        if (configHolder.currentToken().isBlank()) {
            return@withContext Result.failure(ApiReadableException("Не задан API-токен Clash Royale.", isAuthOrIp = true))
        }
        val clanTrim = clanQuery.trim()
        val nickTrim = nicknameQuery.trim()
        if (clanTrim.length < 3) {
            return@withContext Result.failure(ApiReadableException("Укажите название клана (от 3 символов), чтобы найти игрока по нику."))
        }

        try {
            val clans = api.searchClans(clanTrim, limit = 8).items.map {
                ClanSummary(
                    tag = it.tag,
                    name = it.name,
                    membersCount = it.members,
                    clanScore = it.clanScore
                )
            }
            if (clans.isEmpty()) {
                return@withContext Result.success(emptyList<ClanSummary>() to emptyList())
            }

            if (nickTrim.isBlank()) {
                return@withContext Result.success(clans to emptyList())
            }

            val topClans = clans.take(4)
            val matchedMembers = coroutineScope {
                topClans.map { clan ->
                    async {
                        runCatching {
                            api.getClanMembers(clan.tag).items
                                .filter { it.name.contains(nickTrim, ignoreCase = true) }
                                .map { member ->
                                    ClanMemberMatch(
                                        tag = member.tag,
                                        name = member.name,
                                        role = member.role,
                                        trophies = member.trophies,
                                        clanName = clan.name,
                                        clanTag = clan.tag
                                    )
                                }
                        }.getOrDefault(emptyList())
                    }
                }.awaitAll().flatten()
            }.sortedByDescending { it.name.equals(nickTrim, ignoreCase = true) }

            Result.success(clans to matchedMembers)
        } catch (t: Throwable) {
            Result.failure(mapException(t))
        }
    }

    suspend fun fetchBattleOpponents(myTag: String): Result<List<BattleOpponentSummary>> = withContext(Dispatchers.IO) {
        if (configHolder.currentToken().isBlank()) {
            return@withContext Result.failure(ApiReadableException("Не задан API-токен Clash Royale.", isAuthOrIp = true))
        }
        val normalized = normalizeTag(myTag)
        if (normalized.length < 4) {
            return@withContext Result.failure(ApiReadableException("Укажите ваш собственный #TAG для загрузки истории боёв."))
        }

        try {
            ensureCatalogLoadedSilently()
            val catalogMap = dao.getAllCards().associateBy { it.id }
            val logs = api.getPlayerBattleLog(normalized)
            val opponents = logs.mapNotNull { entry ->
                val opponent = entry.opponent?.firstOrNull() ?: return@mapNotNull null
                val playerSide = entry.team?.firstOrNull()
                val cards = (opponent.cards ?: emptyList()).map { mapCard(it, catalogMap) }
                if (cards.isEmpty()) return@mapNotNull null
                BattleOpponentSummary(
                    tag = opponent.tag,
                    name = opponent.name,
                    clanName = opponent.clan?.name,
                    trophies = opponent.startingTrophies,
                    playerCrowns = playerSide?.crowns ?: 0,
                    opponentCrowns = opponent.crowns ?: 0,
                    battleTime = entry.battleTime.orEmpty(),
                    gameMode = entry.gameMode?.name ?: entry.type ?: "Бой",
                    cards = cards
                )
            }
            Result.success(opponents)
        } catch (t: Throwable) {
            Result.failure(mapException(t))
        }
    }

    suspend fun saveRecentOpponent(
        tag: String,
        name: String,
        clanName: String?,
        trophies: Int,
        avgElixir: Double,
        cards: List<DeckCard>
    ) = withContext(Dispatchers.IO) {
        val json = deckJsonAdapter.toJson(cards)
        dao.upsertRecentOpponent(
            RecentOpponentEntity(
                tag = tag,
                name = name,
                clanName = clanName,
                trophies = trophies,
                avgElixir = avgElixir,
                deckJson = json,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteRecentOpponent(tag: String) = withContext(Dispatchers.IO) {
        dao.deleteRecentOpponent(tag)
    }

    private suspend fun ensureCatalogLoadedSilently() {
        if (dao.getAllCards().isEmpty()) {
            runCatching {
                val res = api.getAllCards()
                val entities = res.items.map { dto ->
                    CardCatalogEntity(
                        id = dto.id,
                        name = dto.name,
                        elixirCost = dto.elixirCost ?: 3,
                        maxLevel = dto.maxLevel ?: 15,
                        iconUrl = dto.iconUrls?.medium.orEmpty(),
                        evolutionIconUrl = dto.iconUrls?.evolutionMedium
                    )
                }
                if (entities.isNotEmpty()) {
                    dao.upsertCards(entities)
                }
            }
        }
    }

    private fun mapCard(dto: CardDto, catalog: Map<Int, CardCatalogEntity>): DeckCard {
        val catalogEntry = catalog[dto.id]
        val cost = dto.elixirCost ?: catalogEntry?.elixirCost ?: 3
        val icon = dto.iconUrls?.medium ?: catalogEntry?.iconUrl.orEmpty()
        val evoIcon = dto.iconUrls?.evolutionMedium ?: catalogEntry?.evolutionIconUrl
        return DeckCard(
            id = dto.id,
            name = dto.name,
            elixirCost = cost,
            level = dto.level ?: 11,
            maxLevel = dto.maxLevel ?: catalogEntry?.maxLevel ?: 15,
            evolutionLevel = dto.evolutionLevel ?: 0,
            iconUrl = icon,
            evolutionIconUrl = evoIcon
        )
    }

    private fun mapException(t: Throwable): ApiReadableException {
        return when (t) {
            is ApiReadableException -> t
            is HttpException -> {
                when (t.code()) {
                    400 -> ApiReadableException("Некорректный формат запроса или тега (#TAG).")
                    401, 403 -> ApiReadableException(
                        "Ошибка доступа API (${t.code()}). Проверьте токен и привязку IP прокси RoyaleAPI: 45.79.218.79.",
                        isAuthOrIp = true
                    )
                    404 -> ApiReadableException("Игрок или клан с такими данными не найден.")
                    429 -> ApiReadableException("Превышен лимит запросов API. Подождите пару секунд.")
                    503 -> ApiReadableException("Серверы Clash Royale API временно на обслуживании.")
                    else -> ApiReadableException("Ошибка сервера API: HTTP ${t.code()}.")
                }
            }
            is IOException -> ApiReadableException("Нет соединения с сервером или прокси. Проверьте интернет.")
            else -> ApiReadableException(t.localizedMessage ?: "Неизвестная ошибка при обращении к API.")
        }
    }
}

class ApiReadableException(
    override val message: String,
    val isAuthOrIp: Boolean = false
) : Exception(message)
