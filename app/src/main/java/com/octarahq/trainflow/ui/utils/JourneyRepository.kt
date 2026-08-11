package com.octarahq.trainflow.ui.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class SavedJourney(
    val pnr: String,
    val passengerName: String,
    val departureStationCode: String,
    val arrivalStationCode: String,
    val trainNumber: String,
    val travelDate: String,
    val departureTime: String = "--:--",
    val arrivalTime: String = "--:--",
    val addedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    var vehicleJourneyRef: String? = null,
    val notifyPlatform: Boolean = true,
    val notifyTerminus: Boolean = true,
    val notifyDelay: Boolean = true,
    val categoryRef: String = ""
)

object JourneyRepository {

    private const val PREFS_NAME = "trainflow_journeys_prefs"
    private const val KEY_JOURNEYS = "saved_journeys_list"
    private val gson = Gson()

    fun getJourneys(context: Context): List<SavedJourney> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_JOURNEYS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<SavedJourney>>() {}.type
            val rawList: List<SavedJourney>? = gson.fromJson(json, type)
            rawList?.map { journey ->
                journey.copy(
                    pnr = journey.pnr ?: "SUIVI",
                    passengerName = journey.passengerName ?: "Suivi en direct",
                    departureStationCode = journey.departureStationCode ?: "",
                    arrivalStationCode = journey.arrivalStationCode ?: "",
                    trainNumber = journey.trainNumber ?: "",
                    travelDate = journey.travelDate ?: "Aujourd'hui",
                    departureTime = journey.departureTime ?: "--:--",
                    arrivalTime = journey.arrivalTime ?: "--:--",
                    categoryRef = journey.categoryRef ?: ""
                )
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveJourney(context: Context, journey: SavedJourney) {
        val currentList = getJourneys(context).toMutableList()
        currentList.removeAll { it.pnr == journey.pnr && it.trainNumber == journey.trainNumber }
        currentList.add(0, journey)
        saveList(context, currentList)
        try {
            JourneyTrackerService.startService(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addJourneyFromTrain(context: Context, train: com.octarahq.trainflow.InterpolatedJourney) {
        val trainNumber = train.journey.TrainNumbers?.TrainNumberRef ?: train.journey.PublishedLineName
        val pnr = "SUIVI-${trainNumber.take(4)}"

        val calls = train.journey.EstimatedCalls?.EstimatedCall
        val recorded = train.journey.RecordedCalls?.RecordedCall

        var depTime = "--:--"
        var arrTime = "--:--"

        if (!calls.isNullOrEmpty()) {
            val first = calls.first()
            depTime = first.AimedDepartureTime ?: first.AimedArrivalTime ?: "--:--"
            val last = calls.last()
            arrTime = last.AimedArrivalTime ?: "--:--"
        } else if (!recorded.isNullOrEmpty()) {
            val first = recorded.first()
            depTime = first.AimedDepartureTime ?: first.AimedArrivalTime ?: "--:--"
            val last = recorded.last()
            arrTime = last.AimedArrivalTime ?: "--:--"
        }

        if (depTime.contains("T")) depTime = depTime.substringAfter("T").take(5)
        if (arrTime.contains("T")) arrTime = arrTime.substringAfter("T").take(5)

        val savedJourney = SavedJourney(
            pnr = pnr,
            passengerName = "Suivi en direct",
            departureStationCode = train.journey.OriginName,
            arrivalStationCode = train.journey.DestinationName,
            trainNumber = trainNumber,
            travelDate = "Aujourd'hui",
            departureTime = depTime,
            arrivalTime = arrTime,
            vehicleJourneyRef = train.journey.VehicleJourneyRef,
            categoryRef = train.journey.ProductCategoryRef
        )

        saveJourney(context, savedJourney)

        val data = androidx.work.workDataOf(
            "trainNumber" to trainNumber,
            "pnr" to pnr
        )
        val workRequest = androidx.work.PeriodicWorkRequestBuilder<JourneyTrackerWorker>(15, java.util.concurrent.TimeUnit.MINUTES)
            .setInputData(data)
            .build()
        androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "${pnr}_${trainNumber}",
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    fun updateJourney(context: Context, updated: SavedJourney) {
        val currentList = getJourneys(context).map {
            if (it.pnr == updated.pnr && it.trainNumber == updated.trainNumber) updated else it
        }
        saveList(context, currentList)
    }

    fun isJourneyFollowed(context: Context, trainNumber: String?, vehicleRef: String? = null): Boolean {
        if (trainNumber.isNullOrEmpty() && vehicleRef.isNullOrEmpty()) return false
        val list = getJourneys(context)
        return list.any { journey ->
            journey.isActive && (
                (trainNumber != null && (journey.trainNumber == trainNumber || journey.pnr.endsWith(trainNumber))) ||
                (vehicleRef != null && journey.vehicleJourneyRef == vehicleRef)
            )
        }
    }

    fun removeJourneyByTrainNumber(context: Context, trainNumber: String?, vehicleRef: String? = null) {
        val currentList = getJourneys(context).filterNot { journey ->
            (trainNumber != null && (journey.trainNumber == trainNumber || journey.pnr.endsWith(trainNumber))) ||
            (vehicleRef != null && journey.vehicleJourneyRef == vehicleRef)
        }
        saveList(context, currentList)
    }

    fun toggleFollowJourney(context: Context, train: com.octarahq.trainflow.InterpolatedJourney): Boolean {
        val trainNumber = train.journey.TrainNumbers?.TrainNumberRef ?: train.journey.PublishedLineName
        val vehicleRef = train.journey.VehicleJourneyRef
        return if (isJourneyFollowed(context, trainNumber, vehicleRef)) {
            removeJourneyByTrainNumber(context, trainNumber, vehicleRef)
            false
        } else {
            addJourneyFromTrain(context, train)
            true
        }
    }

    fun deleteJourney(context: Context, pnr: String, trainNumber: String) {
        val currentList = getJourneys(context).filterNot { it.pnr == pnr && it.trainNumber == trainNumber }
        saveList(context, currentList)
    }

    fun updateNotificationSettings(
        context: Context,
        trainNumber: String,
        notifyPlatform: Boolean,
        notifyTerminus: Boolean,
        notifyDelay: Boolean
    ) {
        val currentList = getJourneys(context).map { journey ->
            if (journey.trainNumber == trainNumber || journey.pnr.endsWith(trainNumber)) {
                journey.copy(
                    notifyPlatform = notifyPlatform,
                    notifyTerminus = notifyTerminus,
                    notifyDelay = notifyDelay
                )
            } else {
                journey
            }
        }
        saveList(context, currentList)
    }

    fun getJourneyForTrain(context: Context, trainNumber: String): SavedJourney? {
        return getJourneys(context).firstOrNull { it.trainNumber == trainNumber || it.pnr.endsWith(trainNumber) }
    }

    private fun sendConfirmationNotification(
        context: Context,
        trainNumber: String,
        origin: String,
        destination: String,
        currentStop: String = ""
    ) {
        val channelId = "trainflow_alerts_channel"
        val channelName = "Alertes Voyages"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(channelId, channelName, android.app.NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val cleanOrigin = origin.substringBefore(" Gare").take(15)
        val cleanDest = destination.substringBefore(" Gare").take(15)
        val cleanCurrent = if (currentStop.isNotEmpty()) currentStop.substringBefore(" Gare").take(15) else "En route"
        val progressText = "$cleanOrigin ➔ [$cleanCurrent] ➔ $cleanDest"

        val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Suivi Train n°$trainNumber")
            .setContentText(progressText)
            .setSubText("Suivi de trajet actif")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify((trainNumber + "_confirm").hashCode(), builder.build())
    }

    private fun saveList(context: Context, list: List<SavedJourney>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(list)
        prefs.edit().putString(KEY_JOURNEYS, json).apply()
    }
}
