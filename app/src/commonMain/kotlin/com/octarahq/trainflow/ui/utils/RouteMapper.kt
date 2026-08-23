package com.octarahq.trainflow.ui.utils

import cafe.adriel.voyager.core.screen.Screen
import com.octarahq.trainflow.ui.screens.*

object RouteMapper {
    const val BASE_PATH = ""

    fun getPath(screen: Screen): String {
        return when (screen) {
            is HomeScreen -> "/"
            is AlertsScreen -> "/alerts"
            is TripsScreen -> "/trips"
            is SearchScreen -> "/search"
            is TrainInfoScreen -> "/train/${screen.trainId}"
            is StationInfoScreen -> "/station/${screen.stationName}"
            is TicketScanResultScreen -> "/ticket/${screen.rawData}"
            else -> "/"
        }
    }

    fun getScreen(path: String?): Screen? {
        if (path == null) return null
        
        val cleanPath = path.substringBefore("?").removeSuffix("/")
        
        val route = cleanPath.removePrefix("/")
        
        val segments = route.split("/").filter { it.isNotEmpty() }

        
        if (segments.isEmpty()) {
            return HomeScreen()
        }

        return when (segments[0]) {
            "alerts" -> AlertsScreen()
            "trips" -> TripsScreen()
            "search" -> SearchScreen()
            "train" -> {
                if (segments.size > 1) TrainInfoScreen(segments[1])
                else HomeScreen()
            }
            "station" -> {
                if (segments.size > 1) StationInfoScreen(segments[1], null)
                else HomeScreen()
            }
            "ticket" -> {
                if (segments.size > 1) TicketScanResultScreen(segments[1])
                else HomeScreen()
            }
            else -> HomeScreen()
        }
    }
}
