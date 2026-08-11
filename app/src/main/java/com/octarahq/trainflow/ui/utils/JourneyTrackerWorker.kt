package com.octarahq.trainflow.ui.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.octarahq.trainflow.ApiClient
import com.octarahq.trainflow.InterpolatedJourney
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class JourneyTrackerWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        private const val CHANNEL_ID = "trainflow_alerts_channel"
        private const val CHANNEL_NAME = "Alertes Voyages"
    }

    override suspend fun doWork(): Result {
        val trainNumber = inputData.getString("trainNumber") ?: return Result.failure()
        val pnr = inputData.getString("pnr") ?: return Result.failure()

        createNotificationChannel()

        val journeys = JourneyRepository.getJourneys(applicationContext)
        val savedJourney = journeys.firstOrNull { it.pnr == pnr && it.trainNumber == trainNumber }
            ?: return Result.failure()

        if (!savedJourney.isActive) {
            return Result.success()
        }

        try {
            var activeJourney: InterpolatedJourney? = null

            if (savedJourney.vehicleJourneyRef != null) {
                try {
                    val response = ApiClient.apiService.getSingleVehicle(savedJourney.vehicleJourneyRef!!)
                    activeJourney = response.vehicle
                } catch (e: Exception) {
                    savedJourney.vehicleJourneyRef = null
                }
            }

            if (activeJourney == null) {
                val liveResponse = ApiClient.apiService.getLiveVehicles()
                activeJourney = liveResponse.vehicles.firstOrNull {
                    it.journey.TrainNumbers?.TrainNumberRef == trainNumber
                }
                if (activeJourney != null) {
                    savedJourney.vehicleJourneyRef = activeJourney.journey.VehicleJourneyRef
                    JourneyRepository.updateJourney(applicationContext, savedJourney)
                }
            }

            if (activeJourney != null) {
                val delay = activeJourney.delayMinutes ?: 0
                val speed = activeJourney.speed ?: 0
                
                if (delay > 0) {
                    sendNotification(
                        id = (trainNumber + "_delay").hashCode(),
                        title = "Train n°$trainNumber retardé",
                        text = "Votre train a actuellement un retard de $delay minutes. Vitesse actuelle: $speed km/h."
                    )
                }

                var departurePlatform: String? = null
                var departureAimedTime: String? = null

                val estDepCall = activeJourney.journey.EstimatedCalls?.EstimatedCall?.firstOrNull {
                    it.StopPointName.contains(savedJourney.departureStationCode, ignoreCase = true)
                }
                if (estDepCall != null) {
                    departurePlatform = estDepCall.ArrivalPlatformName
                    departureAimedTime = estDepCall.AimedDepartureTime ?: estDepCall.AimedArrivalTime
                } else {
                    val recDepCall = activeJourney.journey.RecordedCalls?.RecordedCall?.firstOrNull {
                        it.StopPointName.contains(savedJourney.departureStationCode, ignoreCase = true)
                    }
                    if (recDepCall != null) {
                        departurePlatform = recDepCall.DeparturePlatformName ?: recDepCall.ArrivalPlatformName
                        departureAimedTime = recDepCall.AimedDepartureTime ?: recDepCall.AimedArrivalTime
                    }
                }

                if (departurePlatform != null || departureAimedTime != null) {
                    if (!departurePlatform.isNullOrEmpty()) {
                        sendNotification(
                            id = (trainNumber + "_platform").hashCode(),
                            title = "Voie de départ disponible",
                            text = "Le train n°$trainNumber partira depuis la Voie $departurePlatform."
                        )
                    }

                    if (departureAimedTime != null) {
                        val departureDate = parseIsoDate(departureAimedTime)
                        if (departureDate != null) {
                            val timeDiffMs = departureDate.time - System.currentTimeMillis()
                            val fifteenMinutesMs = 15 * 60 * 1000
                            if (timeDiffMs in 0..fifteenMinutesMs) {
                                sendNotification(
                                    id = (trainNumber + "_boarding").hashCode(),
                                    title = "Départ imminent !",
                                    text = "Votre train part dans moins de 10-15 minutes. Préparez-vous à monter."
                                )
                            }
                        }
                    }
                }

                var arrivalAimedTime: String? = null
                val estArrCall = activeJourney.journey.EstimatedCalls?.EstimatedCall?.firstOrNull {
                    it.StopPointName.contains(savedJourney.arrivalStationCode, ignoreCase = true)
                }
                if (estArrCall != null) {
                    arrivalAimedTime = estArrCall.AimedArrivalTime
                } else {
                    val recArrCall = activeJourney.journey.RecordedCalls?.RecordedCall?.firstOrNull {
                        it.StopPointName.contains(savedJourney.arrivalStationCode, ignoreCase = true)
                    }
                    if (recArrCall != null) {
                        arrivalAimedTime = recArrCall.AimedArrivalTime
                    }
                }

                if (arrivalAimedTime != null) {
                    val arrivalDate = parseIsoDate(arrivalAimedTime)
                    if (arrivalDate != null) {
                        val timeDiffMs = arrivalDate.time - System.currentTimeMillis()
                        val fiveMinutesMs = 5 * 60 * 1000
                        if (timeDiffMs in 0..fiveMinutesMs) {
                            sendNotification(
                                id = (trainNumber + "_arrival").hashCode(),
                                title = "Arrivée imminente",
                                text = "Nous arrivons bientôt en gare de ${savedJourney.arrivalStationCode}. N'oubliez pas vos bagages."
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return Result.success()
    }

    private fun sendNotification(id: Int, title: String, text: String) {
        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(id, builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance)
            notificationManager.createNotificationChannel(channel)
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
}
