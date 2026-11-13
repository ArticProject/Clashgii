package com.example.overlay

import android.app.Activity
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.R
import com.example.RoyalePulseApp
import com.example.domain.ClanMemberMatch
import com.example.ui.theme.RoyalePulseTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class ElixirOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private var overlayLayoutParams: WindowManager.LayoutParams? = null
    private var serviceLifecycleOwner: ServiceLifecycleOwner? = null
    private var isMinimized by mutableStateOf(false)

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var screenScoutManager: ScreenScoutManager? = null
    private var projectionActive = false
    private var liveVisionJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val appContainer = (application as RoyalePulseApp).container
        screenScoutManager = ScreenScoutManager(applicationContext, appContainer.repository)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopOverlayAndSelf()
                return START_NOT_STICKY
            }

            ACTION_PROJECTION_DENIED -> {
                val engine = (application as RoyalePulseApp).container.elixirEngine
                engine.setAutoVisionEnabled(false)
                engine.setScanningState(false, "Доступ к анализу экрана отклонён")
                return START_STICKY
            }

            ACTION_ENABLE_PROJECTION_AND_SCAN -> {
                if (!Settings.canDrawOverlays(this)) {
                    stopOverlayAndSelf()
                    return START_NOT_STICKY
                }
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                @Suppress("DEPRECATION")
                val data: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_DATA_INTENT, Intent::class.java)
                } else {
                    intent.getParcelableExtra(EXTRA_DATA_INTENT)
                }
                if (resultCode == Activity.RESULT_OK && data != null) {
                    projectionActive = true
                    startForegroundNotification(includeMediaProjection = true)
                    attachOverlayIfNeeded()
                    screenScoutManager?.setProjectionResult(resultCode, data)
                    val engine = (application as RoyalePulseApp).container.elixirEngine
                    engine.setOverlayActive(true)
                    engine.setAutoVisionEnabled(true)
                    startLiveArenaLoop()
                    performAutoScan()
                }
                return START_STICKY
            }
        }

        if (!Settings.canDrawOverlays(this)) {
            stopOverlayAndSelf()
            return START_NOT_STICKY
        }

        startForegroundNotification(includeMediaProjection = projectionActive)
        attachOverlayIfNeeded()
        (application as RoyalePulseApp).container.elixirEngine.setOverlayActive(true)
        return START_STICKY
    }

    override fun onDestroy() {
        liveVisionJob?.cancel()
        screenScoutManager?.releaseProjection()
        serviceScope.cancel()
        detachOverlay()
        (application as? RoyalePulseApp)?.container?.elixirEngine?.let {
            it.setAutoVisionEnabled(false)
            it.setOverlayActive(false)
        }
        super.onDestroy()
    }

    private fun startForegroundNotification(includeMediaProjection: Boolean) {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val openPending = PendingIntent.getActivity(
            this,
            100,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, ElixirOverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this,
            101,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, RoyalePulseApp.OVERLAY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.notification_overlay_title))
            .setContentText("Плавающий трекер и авто-анализ арены активны")
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openPending)
            .addAction(0, getString(R.string.notification_action_stop), stopPending)
            .build()

        val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (includeMediaProjection) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            }
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, fgsType)
    }

    private fun attachOverlayIfNeeded() {
        if (overlayView != null) return

        val appContainer = (application as RoyalePulseApp).container
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 16
            y = 110
        }
        overlayLayoutParams = params

        val lifecycleOwner = ServiceLifecycleOwner().also {
            it.performRestore()
            it.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            it.handleLifecycleEvent(Lifecycle.Event.ON_START)
            it.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
        serviceLifecycleOwner = lifecycleOwner

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            setContent {
                RoyalePulseTheme {
                    val engineState by appContainer.elixirEngine.state.collectAsStateWithLifecycle()
                    val settings by appContainer.settingsStore.settingsFlow
                        .collectAsStateWithLifecycle(initialValue = com.example.data.local.AppSettings())

                    OverlayHudContent(
                        state = engineState,
                        isMinimized = isMinimized,
                        opacity = settings.overlayOpacity,
                        scale = settings.overlayScale,
                        effectiveCostProvider = { card ->
                            appContainer.elixirEngine.effectiveCardCost(card)
                        },
                        onToggleMinimize = { isMinimized = !isMinimized },
                        onCloseOverlay = { stopOverlayAndSelf() },
                        onDragDelta = { dx, dy ->
                            val lp = overlayLayoutParams ?: return@OverlayHudContent
                            lp.x += dx.roundToInt()
                            lp.y += dy.roundToInt()
                            runCatching { windowManager?.updateViewLayout(this, lp) }
                        },
                        onTogglePlayPause = { appContainer.elixirEngine.toggleRunning() },
                        onReset = { appContainer.elixirEngine.resetMatch() },
                        onUndo = { appContainer.elixirEngine.undoLastAction() },
                        onSelectPhase = { phase -> appContainer.elixirEngine.setPhase(phase) },
                        onPlayCard = { card -> appContainer.elixirEngine.playCard(card) },
                        onAutoScanClick = { triggerAutoScan() },
                        onSelectCandidate = { candidate -> selectScanCandidate(candidate) },
                        onDismissScanFeedback = { appContainer.elixirEngine.clearScanFeedback() }
                    )
                }
            }
        }

        overlayView = composeView
        wm.addView(composeView, params)
    }

    private fun startLiveArenaLoop() {
        liveVisionJob?.cancel()
        val appContainer = (application as RoyalePulseApp).container
        val engine = appContainer.elixirEngine

        liveVisionJob = serviceScope.launch {
            while (isActive && projectionActive) {
                val scout = screenScoutManager
                if (scout != null && scout.hasActiveProjection() && !engine.state.value.isScanningScreen) {
                    val currentState = engine.state.value
                    val (ocrOutcome, spawnEvent) = scout.analyzeLiveArenaStep(
                        deckLoaded = currentState.deckOrder.isNotEmpty(),
                        isMatchRunning = currentState.isRunning,
                        handCards = currentState.handCards
                    )

                    if (ocrOutcome is ScreenScanOutcome.AutoLoaded) {
                        val shouldReset = !engine.state.value.isRunning &&
                            engine.state.value.elapsedMillis == 0L
                        engine.loadOpponentDeck(
                            opponentName = ocrOutcome.profile.name,
                            opponentTag = ocrOutcome.profile.tag,
                            cards = ocrOutcome.profile.currentDeck,
                            resetTimer = shouldReset
                        )
                        if (!engine.state.value.isRunning) {
                            engine.startMatch()
                        }
                        engine.setScanCandidates(
                            candidates = emptyList(),
                            statusMessage = "${ocrOutcome.profile.name} · колода загружена"
                        )
                    } else if (ocrOutcome is ScreenScanOutcome.CandidatesFound &&
                        engine.state.value.scanCandidates.isEmpty()
                    ) {
                        engine.setScanCandidates(
                            candidates = ocrOutcome.candidates,
                            statusMessage = "Выберите соперника (${ocrOutcome.candidates.size}):"
                        )
                    }

                    if (spawnEvent != null) {
                        if (!engine.state.value.isRunning) {
                            engine.startMatch()
                        }
                        val matchedCard = spawnEvent.matchedHandCard
                        if (matchedCard != null) {
                            engine.playCard(matchedCard)
                        } else {
                            engine.playGenericCost(spawnEvent.estimatedElixirCost)
                        }
                    }
                }
                delay(450L)
            }
        }
    }

    private fun triggerAutoScan() {
        val scout = screenScoutManager ?: return
        if (!scout.hasActiveProjection()) {
            ScreenCapturePermissionActivity.launch(this)
            return
        }
        if (liveVisionJob?.isActive != true) {
            (application as RoyalePulseApp).container.elixirEngine.setAutoVisionEnabled(true)
            startLiveArenaLoop()
        }
        performAutoScan()
    }

    private fun performAutoScan() {
        val appContainer = (application as RoyalePulseApp).container
        val scout = screenScoutManager ?: return

        serviceScope.launch {
            appContainer.elixirEngine.setScanningState(true, "Ищем ник и клан...")
            val outcome = scout.captureAndScoutOpponent(
                onPrepareCapture = {
                    withContext(Dispatchers.Main) {
                        overlayView?.visibility = View.INVISIBLE
                    }
                    delay(110L)
                },
                onCaptureFinished = {
                    withContext(Dispatchers.Main) {
                        overlayView?.visibility = View.VISIBLE
                    }
                }
            )

            when (outcome) {
                is ScreenScanOutcome.AutoLoaded -> {
                    val shouldReset = !appContainer.elixirEngine.state.value.isRunning &&
                        appContainer.elixirEngine.state.value.elapsedMillis == 0L
                    appContainer.elixirEngine.loadOpponentDeck(
                        opponentName = outcome.profile.name,
                        opponentTag = outcome.profile.tag,
                        cards = outcome.profile.currentDeck,
                        resetTimer = shouldReset
                    )
                    if (!appContainer.elixirEngine.state.value.isRunning) {
                        appContainer.elixirEngine.startMatch()
                    }
                    appContainer.elixirEngine.setScanCandidates(
                        candidates = emptyList(),
                        statusMessage = "${outcome.profile.name} (${outcome.profile.tag})"
                    )
                    isMinimized = false
                }

                is ScreenScanOutcome.CandidatesFound -> {
                    appContainer.elixirEngine.setScanCandidates(
                        candidates = outcome.candidates,
                        statusMessage = "Совпадений: ${outcome.candidates.size} — выберите:"
                    )
                    isMinimized = false
                }

                is ScreenScanOutcome.NeedsManualClarification -> {
                    appContainer.elixirEngine.setScanCandidates(
                        candidates = emptyList(),
                        statusMessage = outcome.reason
                    )
                    isMinimized = false
                }
            }
        }
    }

    private fun selectScanCandidate(candidate: ClanMemberMatch) {
        val appContainer = (application as RoyalePulseApp).container
        serviceScope.launch {
            appContainer.elixirEngine.setScanningState(true, "Загружаем ${candidate.name}...")
            val profile = appContainer.repository.scoutPlayerByTag(candidate.tag).getOrNull()
            if (profile != null && profile.currentDeck.isNotEmpty()) {
                val shouldReset = !appContainer.elixirEngine.state.value.isRunning &&
                    appContainer.elixirEngine.state.value.elapsedMillis == 0L
                appContainer.elixirEngine.loadOpponentDeck(
                    opponentName = profile.name,
                    opponentTag = profile.tag,
                    cards = profile.currentDeck,
                    resetTimer = shouldReset
                )
                if (!appContainer.elixirEngine.state.value.isRunning) {
                    appContainer.elixirEngine.startMatch()
                }
                appContainer.elixirEngine.setScanCandidates(
                    candidates = emptyList(),
                    statusMessage = "Загружено: ${profile.name}"
                )
            } else {
                appContainer.elixirEngine.setScanCandidates(
                    candidates = emptyList(),
                    statusMessage = "Не удалось загрузить колоду ${candidate.name}"
                )
            }
        }
    }

    private fun detachOverlay() {
        overlayView?.let { view ->
            runCatching { windowManager?.removeView(view) }
        }
        overlayView = null
        serviceLifecycleOwner?.let { owner ->
            owner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            owner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            owner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            owner.clearStore()
        }
        serviceLifecycleOwner = null
        windowManager = null
    }

    private fun stopOverlayAndSelf() {
        liveVisionJob?.cancel()
        screenScoutManager?.releaseProjection()
        projectionActive = false
        (application as? RoyalePulseApp)?.container?.elixirEngine?.setAutoVisionEnabled(false)
        detachOverlay()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        const val ACTION_START = "com.example.overlay.START"
        const val ACTION_STOP = "com.example.overlay.STOP"
        const val ACTION_ENABLE_PROJECTION_AND_SCAN = "com.example.overlay.ENABLE_PROJECTION_SCAN"
        const val ACTION_PROJECTION_DENIED = "com.example.overlay.PROJECTION_DENIED"
        private const val EXTRA_RESULT_CODE = "extra_result_code"
        private const val EXTRA_DATA_INTENT = "extra_data_intent"
        private const val NOTIFICATION_ID = 4201

        fun start(context: Context) {
            val intent = Intent(context, ElixirOverlayService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun startWithProjection(context: Context, resultCode: Int, data: Intent) {
            val intent = Intent(context, ElixirOverlayService::class.java).apply {
                action = ACTION_ENABLE_PROJECTION_AND_SCAN
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_DATA_INTENT, data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun notifyProjectionDenied(context: Context) {
            val intent = Intent(context, ElixirOverlayService::class.java).apply {
                action = ACTION_PROJECTION_DENIED
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, ElixirOverlayService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}

private class ServiceLifecycleOwner :
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    fun performRestore() {
        savedStateController.performRestore(null)
    }

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        lifecycleRegistry.handleLifecycleEvent(event)
    }

    fun clearStore() {
        store.clear()
    }
}
