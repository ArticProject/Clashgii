package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.BuildConfig
import com.example.domain.ApiEndpointMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "royalepulse_settings")

data class AppSettings(
    val apiToken: String = "",
    val endpointMode: ApiEndpointMode = ApiEndpointMode.ROYALE_API_PROXY,
    val customBaseUrl: String = "",
    val myPlayerTag: String = "",
    val overlayOpacity: Float = 0.56f,
    val overlayScale: Float = 0.88f,
    val startingElixir: Float = 5.0f,
    val autoPhaseSwitch: Boolean = true
) {
    val activeBaseUrl: String
        get() = when (endpointMode) {
            ApiEndpointMode.ROYALE_API_PROXY -> ApiEndpointMode.ROYALE_API_PROXY.baseUrl
            ApiEndpointMode.OFFICIAL_DIRECT -> ApiEndpointMode.OFFICIAL_DIRECT.baseUrl
            ApiEndpointMode.CUSTOM_PROXY -> customBaseUrl.trim().let { raw ->
                when {
                    raw.isBlank() -> ApiEndpointMode.ROYALE_API_PROXY.baseUrl
                    !raw.endsWith("/") -> "$raw/"
                    else -> raw
                }
            }
        }

    val hasToken: Boolean
        get() = apiToken.isNotBlank()
}

class SettingsStore(private val context: Context) {

    private object Keys {
        val API_TOKEN = stringPreferencesKey("api_token")
        val ENDPOINT_MODE = stringPreferencesKey("endpoint_mode")
        val CUSTOM_BASE_URL = stringPreferencesKey("custom_base_url")
        val MY_PLAYER_TAG = stringPreferencesKey("my_player_tag")
        val OVERLAY_OPACITY = floatPreferencesKey("overlay_opacity")
        val OVERLAY_SCALE = floatPreferencesKey("overlay_scale")
        val STARTING_ELIXIR = floatPreferencesKey("starting_elixir")
        val AUTO_PHASE_SWITCH = booleanPreferencesKey("auto_phase_switch")
    }

    private fun buildConfigFallbackToken(): String {
        val raw = BuildConfig.CLASH_ROYALE_API_KEY.trim()
        return if (raw.isBlank() || raw == "MY_CLASH_ROYALE_API_KEY") "" else raw
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val storedToken = prefs[Keys.API_TOKEN].orEmpty().trim()
        val effectiveToken = storedToken.ifBlank { buildConfigFallbackToken() }
        AppSettings(
            apiToken = effectiveToken,
            endpointMode = ApiEndpointMode.fromId(prefs[Keys.ENDPOINT_MODE]),
            customBaseUrl = prefs[Keys.CUSTOM_BASE_URL] ?: "",
            myPlayerTag = prefs[Keys.MY_PLAYER_TAG] ?: "",
            overlayOpacity = (prefs[Keys.OVERLAY_OPACITY] ?: 0.56f).coerceIn(0.25f, 0.95f),
            overlayScale = (prefs[Keys.OVERLAY_SCALE] ?: 0.88f).coerceIn(0.75f, 1.15f),
            startingElixir = (prefs[Keys.STARTING_ELIXIR] ?: 5.0f).coerceIn(0f, 10f),
            autoPhaseSwitch = prefs[Keys.AUTO_PHASE_SWITCH] ?: true
        )
    }

    suspend fun saveApiConfig(
        token: String,
        mode: ApiEndpointMode,
        customBaseUrl: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.API_TOKEN] = token.trim()
            prefs[Keys.ENDPOINT_MODE] = mode.id
            prefs[Keys.CUSTOM_BASE_URL] = customBaseUrl.trim()
        }
    }

    suspend fun saveMyPlayerTag(tag: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MY_PLAYER_TAG] = tag.trim()
        }
    }

    suspend fun saveOverlayPreferences(
        opacity: Float,
        scale: Float,
        startingElixir: Float,
        autoPhaseSwitch: Boolean
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.OVERLAY_OPACITY] = opacity.coerceIn(0.25f, 0.95f)
            prefs[Keys.OVERLAY_SCALE] = scale.coerceIn(0.75f, 1.15f)
            prefs[Keys.STARTING_ELIXIR] = startingElixir.coerceIn(0f, 10f)
            prefs[Keys.AUTO_PHASE_SWITCH] = autoPhaseSwitch
        }
    }
}
