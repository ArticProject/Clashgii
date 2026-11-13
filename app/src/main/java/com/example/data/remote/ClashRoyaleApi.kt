package com.example.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

interface ClashRoyaleApi {
    @GET("players/{tag}")
    suspend fun getPlayer(@Path("tag") encodedTag: String): PlayerDto

    @GET("players/{tag}/battlelog")
    suspend fun getPlayerBattleLog(@Path("tag") encodedTag: String): List<BattleLogEntryDto>

    @GET("clans")
    suspend fun searchClans(
        @Query("name") name: String,
        @Query("limit") limit: Int = 12
    ): ClanSearchResponseDto

    @GET("clans/{tag}/members")
    suspend fun getClanMembers(@Path("tag") encodedClanTag: String): ClanMembersResponseDto

    @GET("cards")
    suspend fun getAllCards(): CardsListResponseDto
}

class DynamicApiConfigHolder {
    private val tokenRef = AtomicReference("")
    private val baseUrlRef = AtomicReference("https://proxy.royaleapi.dev/v1/")

    fun update(token: String, baseUrl: String) {
        tokenRef.set(token.trim())
        baseUrlRef.set(baseUrl.trim().ifBlank { "https://proxy.royaleapi.dev/v1/" })
    }

    fun currentToken(): String = tokenRef.get()
    fun currentBaseUrl(): String = baseUrlRef.get()
}

object ClashApiFactory {
    fun create(configHolder: DynamicApiConfigHolder, moshi: Moshi): ClashRoyaleApi {
        val dynamicRoutingInterceptor = Interceptor { chain ->
            val original = chain.request()
            val targetBase = configHolder.currentBaseUrl().toHttpUrlOrNull()
            val builder = original.newBuilder()

            if (targetBase != null) {
                val baseSegments = targetBase.pathSegments.filter { it.isNotEmpty() }
                val requestSegments = original.url.pathSegments.filter { it.isNotEmpty() }
                // Strip the placeholder "v1" prefix from original URL if base already has path segments
                val strippedRequestSegments = if (requestSegments.firstOrNull() == "v1") {
                    requestSegments.drop(1)
                } else {
                    requestSegments
                }

                val newUrlBuilder = targetBase.newBuilder()
                    .encodedPath("/")
                baseSegments.forEach { newUrlBuilder.addPathSegment(it) }
                strippedRequestSegments.forEach { newUrlBuilder.addPathSegment(it) }
                newUrlBuilder.encodedQuery(original.url.encodedQuery)
                builder.url(newUrlBuilder.build())
            }

            val token = configHolder.currentToken()
            if (token.isNotBlank()) {
                builder.header("Authorization", "Bearer $token")
            }
            builder.header("Accept", "application/json")
            chain.proceed(builder.build())
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(dynamicRoutingInterceptor)
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl("https://proxy.royaleapi.dev/v1/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ClashRoyaleApi::class.java)
    }

    fun createMoshi(): Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
}
