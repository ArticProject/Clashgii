package com.example.data.remote

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PlayerDto(
    val tag: String,
    val name: String,
    val expLevel: Int = 1,
    val trophies: Int = 0,
    val bestTrophies: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val clan: ClanRefDto? = null,
    val arena: ArenaDto? = null,
    val currentDeck: List<CardDto>? = null
)

@JsonClass(generateAdapter = true)
data class ClanRefDto(
    val tag: String = "",
    val name: String = ""
)

@JsonClass(generateAdapter = true)
data class ArenaDto(
    val id: Int = 0,
    val name: String = ""
)

@JsonClass(generateAdapter = true)
data class CardDto(
    val id: Int,
    val name: String,
    val level: Int? = null,
    val maxLevel: Int? = null,
    val evolutionLevel: Int? = null,
    val elixirCost: Int? = null,
    val iconUrls: IconUrlsDto? = null
)

@JsonClass(generateAdapter = true)
data class IconUrlsDto(
    val medium: String? = null,
    val evolutionMedium: String? = null
)

@JsonClass(generateAdapter = true)
data class BattleLogEntryDto(
    val type: String? = null,
    val battleTime: String? = null,
    val gameMode: GameModeDto? = null,
    val team: List<BattleParticipantDto>? = null,
    val opponent: List<BattleParticipantDto>? = null
)

@JsonClass(generateAdapter = true)
data class GameModeDto(
    val id: Int = 0,
    val name: String = ""
)

@JsonClass(generateAdapter = true)
data class BattleParticipantDto(
    val tag: String = "",
    val name: String = "",
    val startingTrophies: Int? = null,
    val crowns: Int? = null,
    val clan: ClanRefDto? = null,
    val cards: List<CardDto>? = null
)

@JsonClass(generateAdapter = true)
data class ClanSearchResponseDto(
    val items: List<ClanSearchItemDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ClanSearchItemDto(
    val tag: String,
    val name: String,
    val members: Int = 0,
    val clanScore: Int = 0
)

@JsonClass(generateAdapter = true)
data class ClanMembersResponseDto(
    val items: List<ClanMemberDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ClanMemberDto(
    val tag: String,
    val name: String,
    val role: String = "member",
    val trophies: Int = 0
)

@JsonClass(generateAdapter = true)
data class CardsListResponseDto(
    val items: List<CardDto> = emptyList()
)
