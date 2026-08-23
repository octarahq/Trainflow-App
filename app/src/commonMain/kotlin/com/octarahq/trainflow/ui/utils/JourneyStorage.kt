package com.octarahq.trainflow.ui.utils

expect fun getSavedJourneys(): List<SavedJourney>
expect fun deleteSavedJourney(pnr: String, trainNumber: String)
expect fun getJourneyForTrain(trainNumber: String): SavedJourney?
expect fun updateNotificationSettings(trainNumber: String, notifyPlatform: Boolean, notifyTerminus: Boolean, notifyDelay: Boolean)
expect fun isJourneyFollowed(trainNumber: String?, vehicleRef: String? = null): Boolean
expect fun toggleFollowJourney(train: com.octarahq.trainflow.network.InterpolatedJourney): Boolean
expect fun saveJourney(journey: SavedJourney)
