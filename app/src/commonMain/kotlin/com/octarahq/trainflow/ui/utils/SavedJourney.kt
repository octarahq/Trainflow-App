package com.octarahq.trainflow.ui.utils

import kotlinx.serialization.Serializable

@Serializable
data class SavedJourney(
    val pnr: String,
    val passengerName: String,
    val departureStationCode: String,
    val arrivalStationCode: String,
    val trainNumber: String,
    val travelDate: String,
    val departureTime: String = "--:--",
    val arrivalTime: String = "--:--",
    val addedAt: Long = 0L,
    val isActive: Boolean = true,
    var vehicleJourneyRef: String? = null,
    val notifyPlatform: Boolean = true,
    val notifyTerminus: Boolean = true,
    val notifyDelay: Boolean = true,
    val categoryRef: String = ""
)
