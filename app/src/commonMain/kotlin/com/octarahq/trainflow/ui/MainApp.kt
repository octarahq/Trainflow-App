package com.octarahq.trainflow.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.octarahq.trainflow.ui.screens.HomeScreen
import kotlinx.coroutines.launch
import com.octarahq.trainflow.network.apiService
import com.octarahq.trainflow.ui.utils.getNextStopInfo

@Composable
fun MainApp() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        com.octarahq.trainflow.ui.utils.requestNotificationPermission()

        val notifiedPlatformAlerts = mutableSetOf<String>()
        val notifiedTerminusAlerts = mutableSetOf<String>()

        while (true) {
            try {
                val journeys = com.octarahq.trainflow.ui.utils.getSavedJourneys()
                val activeJourneys = journeys.filter { it.isActive }

                if (activeJourneys.isNotEmpty()) {
                    for (journey in activeJourneys) {
                        var activeJourney: com.octarahq.trainflow.network.InterpolatedJourney? = null

                        if (!journey.vehicleJourneyRef.isNullOrBlank()) {
                            try {
                                val response = apiService.getSingleVehicle(journey.vehicleJourneyRef!!)
                                activeJourney = response.vehicle
                            } catch (e: Exception) {
                                journey.vehicleJourneyRef = null
                            }
                        }

                        if (activeJourney == null) {
                            val liveResponse = apiService.getLiveVehicles()
                            activeJourney = liveResponse.vehicles.firstOrNull {
                                it.journey.TrainNumbers?.TrainNumberRef == journey.trainNumber
                            }
                            if (activeJourney != null) {
                                journey.vehicleJourneyRef = activeJourney.journey.VehicleJourneyRef
                                com.octarahq.trainflow.ui.utils.saveJourney(journey)
                            }
                        }

                        if (activeJourney != null) {
                            var departurePlatform: String? = null
                            var hasDepartedFromDepartureStation = false

                            val estDepCall = activeJourney.journey.EstimatedCalls?.EstimatedCall?.firstOrNull {
                                it.StopPointName.contains(journey.departureStationCode, ignoreCase = true)
                            }
                            val recDepCall = activeJourney.journey.RecordedCalls?.RecordedCall?.firstOrNull {
                                it.StopPointName.contains(journey.departureStationCode, ignoreCase = true)
                            }

                            if (recDepCall != null) {
                                departurePlatform = recDepCall.DeparturePlatformName ?: recDepCall.ArrivalPlatformName
                                if (estDepCall == null || activeJourney.status == "RUNNING") {
                                    hasDepartedFromDepartureStation = true
                                }
                            } else if (estDepCall != null) {
                                departurePlatform = estDepCall.ArrivalPlatformName
                            }

                            if (journey.notifyPlatform && !hasDepartedFromDepartureStation && !departurePlatform.isNullOrEmpty() && !notifiedPlatformAlerts.contains(journey.trainNumber)) {
                                notifiedPlatformAlerts.add(journey.trainNumber)
                                val title = "Voie disponible - Train n°${journey.trainNumber}"
                                val body = "Le train au départ de ${journey.departureStationCode} partira voie $departurePlatform."
                                com.octarahq.trainflow.ui.utils.sendLocalNotification(title, body)
                            }

                            var arrivalAimedTime: String? = null
                            val estArrCall = activeJourney.journey.EstimatedCalls?.EstimatedCall?.firstOrNull {
                                it.StopPointName.contains(journey.arrivalStationCode, ignoreCase = true)
                            }
                            if (estArrCall != null) {
                                arrivalAimedTime = estArrCall.AimedArrivalTime
                            } else {
                                val recArrCall = activeJourney.journey.RecordedCalls?.RecordedCall?.firstOrNull {
                                    it.StopPointName.contains(journey.arrivalStationCode, ignoreCase = true)
                                }
                                if (recArrCall != null) {
                                    arrivalAimedTime = recArrCall.AimedArrivalTime
                                }
                            }

                            if (arrivalAimedTime != null) {
                                try {
                                    val inst = kotlinx.datetime.Instant.parse(arrivalAimedTime)
                                    val nowInst = kotlinx.datetime.Clock.System.now()
                                    val diffMs = inst.toEpochMilliseconds() - nowInst.toEpochMilliseconds()
                                    val diffMin = diffMs / 60000

                                    if (journey.notifyTerminus && diffMin in 0..5 && !notifiedTerminusAlerts.contains(journey.trainNumber)) {
                                        notifiedTerminusAlerts.add(journey.trainNumber)
                                        val title = "Arrivée imminente - Train n°${journey.trainNumber}"
                                        val body = "Votre train arrive à destination de ${journey.arrivalStationCode} dans ${diffMin} min."
                                        com.octarahq.trainflow.ui.utils.sendLocalNotification(title, body)
                                    }

                                    if (diffMs < -10 * 60 * 1000) {
                                        val finished = journey.copy(isActive = false)
                                        com.octarahq.trainflow.ui.utils.saveJourney(finished)
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
            }
            kotlinx.coroutines.delay(30_000)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Navigator(
            HomeScreen(
                onOpenMenu = { scope.launch { drawerState.open() } }
            )
        ) { navigator ->
            LaunchedEffect(Unit) {
                val initialPath = com.octarahq.trainflow.ui.utils.getInitialDeepLink()
                val deepLinkScreen = com.octarahq.trainflow.ui.utils.RouteMapper.getScreen(initialPath)
                if (deepLinkScreen != null && deepLinkScreen !is com.octarahq.trainflow.ui.screens.HomeScreen) {
                    navigator.push(deepLinkScreen)
                }
            }

            LaunchedEffect(navigator.lastItem) {
                val path = com.octarahq.trainflow.ui.utils.RouteMapper.getPath(navigator.lastItem)
                com.octarahq.trainflow.ui.utils.updateBrowserUrl(path)
            }

            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = false,
                drawerContent = {
                    com.octarahq.trainflow.components.TrainflowDrawer(
                        navigator = navigator,
                        currentScreen = navigator.lastItem,
                        drawerState = drawerState,
                        scope = scope
                    )
                }
            ) {
                SlideTransition(navigator)
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.BottomCenter)
                .padding(bottom = 100.dp)
        )
    }
}
