package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.data.ClashRoyaleRepository
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsStore
import com.example.data.remote.ClashApiFactory
import com.example.data.remote.DynamicApiConfigHolder
import com.example.domain.ElixirCycleEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RoyalePulseApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        createOverlayNotificationChannel()
        container = AppContainer(this)

        // Keep API interceptor config and engine defaults synced with DataStore
        appScope.launch {
            container.settingsStore.settingsFlow.collectLatest { settings ->
                container.configHolder.update(
                    token = settings.apiToken,
                    baseUrl = settings.activeBaseUrl
                )
                container.elixirEngine.configureSettings(
                    startingElixir = settings.startingElixir,
                    autoPhaseSwitch = settings.autoPhaseSwitch
                )
            }
        }
    }

    private fun createOverlayNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                OVERLAY_CHANNEL_ID,
                getString(R.string.notification_channel_overlay),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_overlay_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val OVERLAY_CHANNEL_ID = "royalepulse_overlay_channel"
    }
}

class AppContainer(application: Application) {
    val settingsStore = SettingsStore(application)
    val database = AppDatabase.build(application)
    val configHolder = DynamicApiConfigHolder()
    private val moshi = ClashApiFactory.createMoshi()
    private val api = ClashApiFactory.create(configHolder, moshi)

    val repository = ClashRoyaleRepository(
        api = api,
        dao = database.royaleDao(),
        configHolder = configHolder,
        moshi = moshi
    )

    val elixirEngine = ElixirCycleEngine()
}
