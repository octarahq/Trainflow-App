package com.octarahq.trainflow.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.octarahq.trainflow.ui.components.TrainflowBottomChrome
import com.octarahq.trainflow.ui.components.TrainflowDrawer
import com.octarahq.trainflow.ui.navigation.Screen
import com.octarahq.trainflow.ui.screens.AlertsScreen
import com.octarahq.trainflow.ui.screens.HomeScreen
import com.octarahq.trainflow.ui.screens.SearchScreen
import com.octarahq.trainflow.ui.screens.TicketScanResultScreen
import com.octarahq.trainflow.ui.screens.TrainInfoScreen
import com.octarahq.trainflow.ui.screens.TripsScreen
import com.octarahq.trainflow.ui.utils.AztecScanner
import kotlinx.coroutines.launch
import java.net.URLDecoder

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

    val context = LocalContext.current
    var dropdownOpen by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val rawData = AztecScanner.decodeAztecFromUri(context, uri)
            if (rawData != null) {
                navController.navigate(Screen.TicketResult.createRoute(rawData)) {
                    launchSingleTop = true
                }
            } else {
                snackbarHostState.showSnackbar("Aucun QR code Aztec trouvé dans ce fichier")
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            TrainflowDrawer(
                currentRoute = currentRoute,
                onNavigateTo = { route ->
                    scope.launch { drawerState.close() }
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
            ) {
                composable(
                    route = Screen.Home.route,
                    arguments = listOf(
                        navArgument("select") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { backStackEntry ->
                    val selectTrainId = backStackEntry.arguments?.getString("select")
                    HomeScreen(
                        selectTrainId = selectTrainId,
                        onOpenMenu = {
                            scope.launch { drawerState.open() }
                        },
                        onOpenSearch = {
                            navController.navigate(Screen.Search.route) { launchSingleTop = true }
                        },
                        onOpenTrainInfo = { trainId, speedKmh ->
                            navController.navigate(Screen.TrainInfo.createRoute(trainId, speedKmh)) { launchSingleTop = true }
                        },
                        onOpenNetworkStatus = {
                            navController.navigate(Screen.Alerts.route) { launchSingleTop = true }
                        }
                    )
                }
                composable(Screen.Trips.route) {
                    TripsScreen(
                        onBack = {
                            navController.navigate(Screen.Home.route) {
                                launchSingleTop = true
                            }
                        },
                        onSearch = {
                            navController.navigate(Screen.Search.route) { launchSingleTop = true }
                        },
                        onOpenTrainInfo = { trainNumber, ref ->
                            val id = ref ?: trainNumber
                            navController.navigate(Screen.TrainInfo.createRoute(id, null)) { launchSingleTop = true }
                        }
                    )
                }
                composable(Screen.Search.route) {
                    SearchScreen(
                        onBack = { navController.popBackStack() },
                        onOpenTrainInfo = {}
                    )
                }
                composable(Screen.Alerts.route) { AlertsScreen() }
                composable(
                    route = Screen.TrainInfo.route,
                    arguments = listOf(
                        navArgument("trainId") { type = NavType.StringType },
                        navArgument("speed") {
                            type = NavType.IntType
                            defaultValue = -1
                        }
                    )
                ) { backStackEntry ->
                    val trainId = backStackEntry.arguments?.getString("trainId") ?: ""
                    val speedVal = backStackEntry.arguments?.getInt("speed") ?: -1
                    val speed = if (speedVal != -1) speedVal else null
                    TrainInfoScreen(
                        trainId = trainId,
                        speedKmh = speed,
                        onBack = { navController.popBackStack() },
                        onLocateOnMap = { selectTrainId ->
                            navController.navigate(Screen.Home.createRoute(selectTrainId)) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        }
                    )
                }
                composable(
                    route = Screen.TicketResult.route,
                    arguments = listOf(
                        navArgument("rawData") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val encoded = backStackEntry.arguments?.getString("rawData") ?: ""
                    val rawData = try {
                        URLDecoder.decode(encoded, "UTF-8")
                    } catch (e: Exception) {
                        encoded
                    }
                    TicketScanResultScreen(
                        rawData = rawData,
                        onBack = {
                            navController.navigate(Screen.Trips.route) {
                                popUpTo(Screen.Trips.route) { inclusive = true }
                            }
                        },
                        onRescan = {
                            filePicker.launch(arrayOf("image/*", "application/pdf"))
                        }
                    )
                }
            }

            if (dropdownOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { dropdownOpen = false }
                )
            }

            if (currentRoute in setOf(Screen.Trips.route, Screen.Search.route, Screen.Alerts.route, Screen.TrainInfo.route)) {
                TrainflowBottomChrome(
                    currentRoute = currentRoute,
                    dropdownOpen = dropdownOpen,
                    onToggleDropdown = { dropdownOpen = !dropdownOpen },
                    onHome = { navController.navigate(Screen.Home.route) { launchSingleTop = true } },
                    onTrips = { navController.navigate(Screen.Trips.route) { launchSingleTop = true } },
                    onAlerts = { navController.navigate(Screen.Alerts.route) { launchSingleTop = true } },
                    onAddJourney = {},
                    onImportTicket = {
                        filePicker.launch(arrayOf("image/*", "application/pdf"))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(androidx.compose.ui.Alignment.BottomCenter)
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
            )
        }
    }
}
