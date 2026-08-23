package com.octarahq.trainflow.ui.utils

import com.octarahq.trainflow.AppContextHolder

actual fun getSavedJourneys(): List<SavedJourney> {
    return JourneyRepository.getJourneys(AppContextHolder.context)
}

actual fun deleteSavedJourney(pnr: String, trainNumber: String) {
    JourneyRepository.deleteJourney(AppContextHolder.context, pnr, trainNumber)
}

actual fun getJourneyForTrain(trainNumber: String): SavedJourney? {
    return JourneyRepository.getJourneyForTrain(AppContextHolder.context, trainNumber)
}

actual fun updateNotificationSettings(trainNumber: String, notifyPlatform: Boolean, notifyTerminus: Boolean, notifyDelay: Boolean) {
    JourneyRepository.updateNotificationSettings(AppContextHolder.context, trainNumber, notifyPlatform, notifyTerminus, notifyDelay)
}

actual fun isJourneyFollowed(trainNumber: String?, vehicleRef: String?): Boolean {
    return JourneyRepository.isJourneyFollowed(AppContextHolder.context, trainNumber, vehicleRef)
}

actual fun toggleFollowJourney(train: com.octarahq.trainflow.network.InterpolatedJourney): Boolean {
    return JourneyRepository.toggleFollowJourney(AppContextHolder.context, train)
}

actual fun saveJourney(journey: SavedJourney) {
    JourneyRepository.saveJourney(AppContextHolder.context, journey)
    
    val data = androidx.work.workDataOf(
        "trainNumber" to journey.trainNumber,
        "pnr" to journey.pnr
    )
    val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.octarahq.trainflow.ui.utils.JourneyTrackerWorker>(15, java.util.concurrent.TimeUnit.MINUTES)
        .setInputData(data)
        .build()
    androidx.work.WorkManager.getInstance(AppContextHolder.context).enqueueUniquePeriodicWork(
        "${journey.pnr}_${journey.trainNumber}",
        androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
        workRequest
    )
}
