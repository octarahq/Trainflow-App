package com.octarahq.trainflow.ui.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.octarahq.trainflow.ApiClient
import com.octarahq.trainflow.InterpolatedJourney
import com.octarahq.trainflow.MainActivity
import com.octarahq.trainflow.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class JourneyTrackerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var notificationManager: NotificationManager
    private val notifiedTerminusAlerts = mutableSetOf<String>()
    private val notifiedPlatformAlerts = mutableSetOf<String>()

    companion object {
        private const val LIVE_CHANNEL_ID = "trainflow_live_tracking_v3"
        private const val LIVE_CHANNEL_NAME = "Suivi de Trajet en Direct"
        private const val ALERTS_CHANNEL_ID = "trainflow_alerts_channel_v3"
        private const val ALERTS_CHANNEL_NAME = "Alertes et Voies"
        private const val NOTIFICATION_ID = 9999
        private const val ACTION_STOP = "com.octarahq.trainflow.ACTION_STOP"
        
        fun startService(context: Context) {
            val intent = Intent(context, JourneyTrackerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopTrackingAndClear()
            return START_NOT_STICKY
        }

        val stopIntent = Intent(this, JourneyTrackerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 
            0, 
            stopIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this,
            1,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, LIVE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Trainflow")
            .setContentText("Recherche du suivi en direct...")
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setOngoing(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(false)
            .setProgress(100, 0, false)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openPendingIntent)
            .setGroup(null)
            .setSortKey(null)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Arrêter le trajet",
                stopPendingIntent
            )

        applyLiveUpdateExtras(notificationBuilder, "Suivi")
        val initialNotification = notificationBuilder.build()
        applyLiveUpdateExtras(initialNotification, "Suivi")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val primaryType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            }
            try {
                startForeground(NOTIFICATION_ID, initialNotification, primaryType)
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    startForeground(
                        NOTIFICATION_ID, 
                        initialNotification, 
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                } catch (e2: Exception) {
                    e2.printStackTrace()
                }
            }
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }

        startTrackingLoop()

        return START_STICKY
    }

    private fun stopTrackingAndClear() {
        try {
            val currentList = JourneyRepository.getJourneys(applicationContext)
            for (journey in currentList) {
                if (journey.isActive) {
                    JourneyRepository.removeJourneyByTrainNumber(applicationContext, journey.trainNumber)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        notificationManager.cancel(NOTIFICATION_ID)
        stopSelf()
    }

    private fun startTrackingLoop() {
        serviceScope.launch {
            while (true) {
                val journeys = JourneyRepository.getJourneys(applicationContext)
                val activeJourneys = journeys.filter { it.isActive }

                if (activeJourneys.isEmpty()) {
                    notificationManager.cancel(NOTIFICATION_ID)
                    stopSelf()
                    break
                }

                for (journey in activeJourneys) {
                    try {
                        var activeJourney: InterpolatedJourney? = null

                        if (journey.vehicleJourneyRef != null) {
                            try {
                                val response = ApiClient.apiService.getSingleVehicle(journey.vehicleJourneyRef!!)
                                activeJourney = response.vehicle
                            } catch (e: Exception) {
                                journey.vehicleJourneyRef = null
                            }
                        }

                        if (activeJourney == null) {
                            val liveResponse = ApiClient.apiService.getLiveVehicles()
                            activeJourney = liveResponse.vehicles.firstOrNull {
                                it.journey.TrainNumbers?.TrainNumberRef == journey.trainNumber
                            }
                            if (activeJourney != null) {
                                journey.vehicleJourneyRef = activeJourney.journey.VehicleJourneyRef
                                JourneyRepository.updateJourney(applicationContext, journey)
                            }
                        }

                        if (activeJourney != null) {
                            val nextStop = getNextStopInfo(activeJourney)?.name ?: ""
                            val delayMin = activeJourney.delayMinutes ?: 0

                            val recCount = activeJourney.journey.RecordedCalls?.RecordedCall?.size ?: 0
                            val estCount = activeJourney.journey.EstimatedCalls?.EstimatedCall?.size ?: 0
                            val totalCount = recCount + estCount
                            val progressPercent = if (totalCount > 0) {
                                ((recCount.toFloat() / totalCount.toFloat()) * 100).toInt().coerceIn(0, 100)
                            } else 0

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
                                sendPlatformAlert(journey.trainNumber, formatStationName(journey.departureStationCode), departurePlatform)
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

                            var timeUntilArrMin: Long? = null
                            if (arrivalAimedTime != null) {
                                val arrivalDate = parseIsoDate(arrivalAimedTime)
                                if (arrivalDate != null) {
                                    val timeDiffMs = arrivalDate.time - System.currentTimeMillis()
                                    if (timeDiffMs > 0) {
                                        timeUntilArrMin = timeDiffMs / (60 * 1000)
                                    }

                                    val arrMin = timeUntilArrMin
                                    if (journey.notifyTerminus && arrMin != null && arrMin in 0L..5L && !notifiedTerminusAlerts.contains(journey.trainNumber)) {
                                        notifiedTerminusAlerts.add(journey.trainNumber)
                                        sendTerminusArrivalAlert(journey.trainNumber, formatStationName(journey.arrivalStationCode))
                                    }

                                    if (timeDiffMs < -10 * 60 * 1000) {
                                        val currentList = JourneyRepository.getJourneys(applicationContext).toMutableList()
                                        val itemToFinish = currentList.firstOrNull { it.pnr == journey.pnr && it.trainNumber == journey.trainNumber }
                                        if (itemToFinish != null) {
                                            val finished = itemToFinish.copy(isActive = false)
                                            currentList.remove(itemToFinish)
                                            currentList.add(finished)
                                            val prefs = applicationContext.getSharedPreferences("trainflow_journeys_prefs", Context.MODE_PRIVATE)
                                            val json = com.google.gson.Gson().toJson(currentList)
                                            prefs.edit().putString("saved_journeys", json).apply()
                                        }
                                    }
                                }
                            }

                            updateForegroundNotification(
                                trainNumber = journey.trainNumber,
                                origin = journey.departureStationCode,
                                destination = journey.arrivalStationCode,
                                currentStop = nextStop,
                                delayMin = delayMin,
                                platform = departurePlatform,
                                progressPercent = progressPercent
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                delay(60000)
            }
        }
    }

    private fun updateForegroundNotification(
        trainNumber: String,
        origin: String,
        destination: String,
        currentStop: String,
        delayMin: Int,
        platform: String?,
        progressPercent: Int
    ) {
        val cleanOrigin = formatStationName(origin)
        val cleanDest = formatStationName(destination)
        val cleanCurrent = if (currentStop.isNotEmpty()) formatStationName(currentStop) else "En route"
        
        val titleText = "Train n°$trainNumber"
        val fullContentText = "$cleanOrigin ➔ $cleanDest"
        val subText = "Prochain arrêt : $cleanCurrent"

        val chipText = when {
            delayMin > 0 -> "+${delayMin}m"
            !platform.isNullOrEmpty() -> "V. $platform"
            else -> "n°$trainNumber"
        }

        val stopIntent = Intent(this, JourneyTrackerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 
            0, 
            stopIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this,
            1,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, LIVE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titleText)
            .setContentText(fullContentText)
            .setSubText(subText)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setOngoing(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(false)
            .setProgress(100, progressPercent, false)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openPendingIntent)
            .setGroup(null)
            .setSortKey(null)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Arrêter le trajet",
                stopPendingIntent
            )

        applyLiveUpdateExtras(notificationBuilder, chipText)
        val notification = notificationBuilder.build()
        applyLiveUpdateExtras(notification, chipText)

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun sendPlatformAlert(trainNumber: String, departureStationName: String, platform: String) {
        val builder = NotificationCompat.Builder(this, ALERTS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Voie de départ disponible !")
            .setContentText("Le train n°$trainNumber partira de la Voie $platform en gare de $departureStationName.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_ALL)
            .setAutoCancel(true)

        notificationManager.notify((trainNumber + "_platform").hashCode(), builder.build())
    }

    private fun sendTerminusArrivalAlert(trainNumber: String, destinationName: String) {
        val builder = NotificationCompat.Builder(this, ALERTS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Arrivée à votre terminus !")
            .setContentText("Le train n°$trainNumber arrive dans moins de 5 min en gare de $destinationName.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_ALL)
            .setAutoCancel(true)

        notificationManager.notify((trainNumber + "_terminus").hashCode(), builder.build())
    }

    private fun formatStationName(name: String): String {
        if (name.isBlank()) return ""
        return name
            .replace(" Gare de ", " ", ignoreCase = true)
            .replace(" Gare d'", " ", ignoreCase = true)
            .replace(" Gare", "", ignoreCase = true)
            .replace("Saint-", "St-", ignoreCase = true)
            .replace("Sainte-", "Ste-", ignoreCase = true)
            .trim()
    }

    private fun applyLiveUpdateExtras(builder: NotificationCompat.Builder, chipText: String) {
        builder.addExtras(android.os.Bundle().apply {
            putBoolean("android.requestPromotedOngoing", true)
            putCharSequence("android.shortCriticalText", chipText)
            putString("android.shortCriticalText", chipText)
            putString("android.substName", "Trainflow")
        })
    }

    private fun applyLiveUpdateExtras(notification: Notification, chipText: String) {
        notification.extras.putBoolean("android.requestPromotedOngoing", true)
        notification.extras.putCharSequence("android.shortCriticalText", chipText)
        notification.extras.putString("android.shortCriticalText", chipText)
        notification.extras.putString("android.substName", "Trainflow")

        if (Build.VERSION.SDK_INT >= 36) {
            try {
                val method = notification.javaClass.getMethod("setShortCriticalText", CharSequence::class.java)
                method.invoke(notification, chipText)
            } catch (e: Throwable) {
                
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val liveChannel = NotificationChannel(
                LIVE_CHANNEL_ID,
                LIVE_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notification dynamique de suivi du trajet"
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val alertsChannel = NotificationChannel(
                ALERTS_CHANNEL_ID,
                ALERTS_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertes de voie et sonnerie au terminus"
                setShowBadge(true)
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(liveChannel)
            notificationManager.createNotificationChannel(alertsChannel)
        }
    }

    private fun parseIsoDate(isoDate: String): Date? {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            format.parse(isoDate)
        } catch (e: Exception) {
            try {
                val alternativeFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.getDefault())
                alternativeFormat.parse(isoDate)
            } catch (e2: Exception) {
                null
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
