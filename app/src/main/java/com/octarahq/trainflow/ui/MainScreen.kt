package com.octarahq.trainflow.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import com.octarahq.trainflow.ui.components.TrainflowDrawer
import com.octarahq.trainflow.ui.components.TrainflowBottomChrome
import com.octarahq.trainflow.ui.navigation.Screen
import com.octarahq.trainflow.ui.screens.AlertsScreen
import com.octarahq.trainflow.ui.screens.HomeScreen
import com.octarahq.trainflow.ui.screens.TrainInfoScreen
import com.octarahq.trainflow.ui.screens.SearchScreen
import com.octarahq.trainflow.ui.screens.TripsScreen
import kotlinx.coroutines.launch

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

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
                composable(Screen.Home.route) {
                    HomeScreen(
                        onOpenMenu = {
                            scope.launch { drawerState.open() }
                        },
                        onOpenSearch = {
                            navController.navigate(Screen.Search.route) { launchSingleTop = true }
                        },
                        onOpenTrainInfo = { trainId, speedKmh ->
                            navController.navigate(Screen.TrainInfo.createRoute(trainId, speedKmh)) { launchSingleTop = true }
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
                        onOpenTrainInfo = {
                        }
                    )
                }
                composable(Screen.Search.route) {
                    SearchScreen(
                        onBack = { navController.popBackStack() },
                        onOpenTrainInfo = {
                        }
                    )
                }
                composable(Screen.Alerts.route) { AlertsScreen() }
                composable(
                    route = Screen.TrainInfo.route,
                    arguments = listOf(
                        androidx.navigation.navArgument("trainId") { type = androidx.navigation.NavType.StringType },
                        androidx.navigation.navArgument("speed") {
                            type = androidx.navigation.NavType.IntType
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
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }

            if (currentRoute in setOf(Screen.Trips.route, Screen.Search.route, Screen.Alerts.route, Screen.TrainInfo.route)) {
                TrainflowBottomChrome(
                    currentRoute = currentRoute,
                    onHome = { navController.navigate(Screen.Home.route) { launchSingleTop = true } },
                    onTrips = { navController.navigate(Screen.Trips.route) { launchSingleTop = true } },
                    onAlerts = { navController.navigate(Screen.Alerts.route) { launchSingleTop = true } },
                    onAddJourney = {
                        if (currentRoute == Screen.Trips.route) {
                            navController.navigate(Screen.TrainInfo.route) { launchSingleTop = true }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(androidx.compose.ui.Alignment.BottomCenter)
                )
            }
        }
    }
}
