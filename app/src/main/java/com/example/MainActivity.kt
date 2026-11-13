package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.MainViewModel
import com.example.ui.screens.ScoutScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TrackerScreen
import com.example.ui.theme.GraphiteActive
import com.example.ui.theme.GraphiteBg
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.RoyalePulseTheme

private const val ROUTE_SCOUT = "scout"
private const val ROUTE_TRACKER = "tracker"
private const val ROUTE_SETTINGS = "settings"

private data class TopNavItem(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
    val testTag: String
)

private val TopDestinations = listOf(
    TopNavItem(
        route = ROUTE_SCOUT,
        labelRes = R.string.nav_scout,
        icon = Icons.Outlined.PersonSearch,
        testTag = "nav_scout"
    ),
    TopNavItem(
        route = ROUTE_TRACKER,
        labelRes = R.string.nav_tracker,
        icon = Icons.Outlined.Bolt,
        testTag = "nav_tracker"
    ),
    TopNavItem(
        route = ROUTE_SETTINGS,
        labelRes = R.string.nav_settings,
        icon = Icons.Outlined.Tune,
        testTag = "nav_settings"
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appContainer = (application as RoyalePulseApp).container
        setContent {
            RoyalePulseTheme {
                val viewModel: MainViewModel = viewModel(
                    factory = MainViewModel.Factory(
                        repository = appContainer.repository,
                        settingsStore = appContainer.settingsStore,
                        elixirEngine = appContainer.elixirEngine
                    )
                )
                RoyalePulseRoot(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun RoyalePulseRoot(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val scoutState by viewModel.scoutState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val trackerState by viewModel.trackerState.collectAsStateWithLifecycle()
    val recentOpponents by viewModel.recentOpponents.collectAsStateWithLifecycle()
    val cardCatalog by viewModel.cardCatalog.collectAsStateWithLifecycle()
    val apiTestResult by viewModel.apiTestResult.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(scoutState.statusMessage) {
        val msg = scoutState.statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    val navigateTo: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = GraphiteBg,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        containerColor = GraphiteSurface,
                        tonalElevation = 0.dp
                    ) {
                        TopDestinations.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = stringResource(item.labelRes),
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                label = { Text(stringResource(item.labelRes)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = InkPrimary,
                                    selectedTextColor = InkPrimary,
                                    indicatorColor = GraphiteActive,
                                    unselectedIconColor = InkMuted,
                                    unselectedTextColor = InkSecondary
                                ),
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = GraphiteSurface,
                        contentColor = Color.White
                    ) {
                        TopDestinations.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = stringResource(item.labelRes),
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                label = { Text(stringResource(item.labelRes)) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = InkPrimary,
                                    selectedTextColor = InkPrimary,
                                    indicatorColor = GraphiteActive,
                                    unselectedIconColor = InkMuted,
                                    unselectedTextColor = InkSecondary
                                ),
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_SCOUT,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(ROUTE_SCOUT) {
                        ScoutScreen(
                            scoutState = scoutState,
                            settings = settings,
                            isOverlayActive = trackerState.isOverlayActive,
                            recentOpponents = recentOpponents,
                            onSelectSearchMode = viewModel::setSearchMode,
                            onTagQueryChange = viewModel::updateTagQuery,
                            onClanQueryChange = viewModel::updateClanQuery,
                            onNicknameQueryChange = viewModel::updateNicknameQuery,
                            onScoutByTag = viewModel::scoutByTag,
                            onSearchClanNick = viewModel::searchByClanAndNick,
                            onSelectClan = viewModel::loadClanMembers,
                            onLoadMyBattles = viewModel::loadMyBattleOpponents,
                            onLoadProfileToTracker = { profile, reset ->
                                viewModel.loadDeckIntoTracker(
                                    opponentName = profile.name,
                                    opponentTag = profile.tag,
                                    cards = profile.currentDeck,
                                    resetTimer = reset
                                )
                            },
                            onLoadBattleOpponentToTracker = { opp, reset ->
                                viewModel.loadDeckIntoTracker(
                                    opponentName = opp.name,
                                    opponentTag = opp.tag,
                                    cards = opp.cards,
                                    resetTimer = reset
                                )
                            },
                            onLoadSavedOpponent = { saved, reset ->
                                viewModel.loadDeckIntoTracker(
                                    opponentName = saved.name,
                                    opponentTag = saved.tag,
                                    cards = saved.cards,
                                    resetTimer = reset
                                )
                            },
                            onDeleteSavedOpponent = viewModel::deleteSavedOpponent,
                            onOpenSettings = { navigateTo(ROUTE_SETTINGS) },
                            onOpenTracker = { navigateTo(ROUTE_TRACKER) }
                        )
                    }

                    composable(ROUTE_TRACKER) {
                        TrackerScreen(
                            state = trackerState,
                            cardCatalog = cardCatalog,
                            effectiveCostProvider = viewModel::effectiveCardCost,
                            onTogglePlayPause = viewModel::toggleMatchRunning,
                            onReset = viewModel::resetMatch,
                            onSelectPhase = viewModel::setPhase,
                            onAdjustElixir = viewModel::adjustElixir,
                            onPlayCard = viewModel::playCard,
                            onPlayGenericCost = viewModel::playGenericCost,
                            onUndo = viewModel::undoLastAction,
                            onClearDeck = viewModel::clearTrackerDeck,
                            onLoadManualDeck = { cards ->
                                viewModel.loadDeckIntoTracker(
                                    opponentName = "Своя сборка",
                                    opponentTag = "",
                                    cards = cards,
                                    resetTimer = true
                                )
                            },
                            onSyncCatalog = viewModel::syncCardsInBackground
                        )
                    }

                    composable(ROUTE_SETTINGS) {
                        SettingsScreen(
                            settings = settings,
                            apiTestResult = apiTestResult,
                            onSaveApiConfig = viewModel::saveApiConfig,
                            onTestApiAndSyncCards = viewModel::testApiAndSyncCards,
                            onSaveOverlayPrefs = viewModel::saveOverlayPreferences
                        )
                    }
                }
            }
        }
    }
}
