package com.octarahq.trainflow.ui.utils

import kotlinx.browser.window
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

private const val JOURNEYS_KEY = "trainflow_saved_journeys"
private const val FOLLOWED_KEY = "trainflow_followed_journeys"

private fun getRawJourneys(): List<SavedJourney> {
    val jsonStr = window.localStorage.getItem(JOURNEYS_KEY) ?: return emptyList()
    return try {
        Json.decodeFromString<List<SavedJourney>>(jsonStr)
    } catch (e: Exception) {
        emptyList()
    }
}

private fun saveRawJourneys(list: List<SavedJourney>) {
    try {
        val jsonStr = Json.encodeToString(list)
        window.localStorage.setItem(JOURNEYS_KEY, jsonStr)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun getFollowedTrainNumbers(): Set<String> {
    val jsonStr = window.localStorage.getItem(FOLLOWED_KEY) ?: return emptySet()
    return try {
        Json.decodeFromString<Set<String>>(jsonStr)
    } catch (e: Exception) {
        emptySet()
    }
}

private fun saveFollowedTrainNumbers(set: Set<String>) {
    try {
        val jsonStr = Json.encodeToString(set)
        window.localStorage.setItem(FOLLOWED_KEY, jsonStr)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

actual fun getSavedJourneys(): List<SavedJourney> {
    return getRawJourneys().sortedByDescending { it.addedAt }
}

actual fun deleteSavedJourney(pnr: String, trainNumber: String) {
    val current = getRawJourneys().toMutableList()
    current.removeAll { 
        (pnr.isNotEmpty() && it.pnr == pnr) || (pnr.isEmpty() && it.trainNumber == trainNumber)
    }
    saveRawJourneys(current)
}

actual fun getJourneyForTrain(trainNumber: String): SavedJourney? {
    return getRawJourneys().firstOrNull { it.trainNumber == trainNumber }
}

actual fun updateNotificationSettings(trainNumber: String, notifyPlatform: Boolean, notifyTerminus: Boolean, notifyDelay: Boolean) {
    val current = getRawJourneys().map {
        if (it.trainNumber == trainNumber) {
            it.copy(notifyPlatform = notifyPlatform, notifyTerminus = notifyTerminus, notifyDelay = notifyDelay)
        } else {
            it
        }
    }
    saveRawJourneys(current)
}

actual fun isJourneyFollowed(trainNumber: String?, vehicleRef: String?): Boolean {
    if (trainNumber == null) return false
    return getFollowedTrainNumbers().contains(trainNumber)
}

actual fun toggleFollowJourney(train: com.octarahq.trainflow.network.InterpolatedJourney): Boolean {
    val trainNum = train.journey.TrainNumbers?.TrainNumberRef ?: train.journey.PublishedLineName
    val current = getFollowedTrainNumbers().toMutableSet()
    val isFollowing = if (current.contains(trainNum)) {
        current.remove(trainNum)
        false
    } else {
        current.add(trainNum)
        true
    }
    saveFollowedTrainNumbers(current)
    return isFollowing
}

actual fun saveJourney(journey: SavedJourney) {
    val current = getRawJourneys().toMutableList()
    current.removeAll { it.trainNumber == journey.trainNumber || it.pnr == journey.pnr }
    current.add(journey)
    saveRawJourneys(current)
}
