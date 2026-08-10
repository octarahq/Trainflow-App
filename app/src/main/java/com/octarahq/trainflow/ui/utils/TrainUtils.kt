package com.octarahq.trainflow.ui.utils

import androidx.compose.ui.graphics.Color

data class TrainCategoryDisplay(val label: String, val color: Color)

fun getTrainCategoryDisplay(key: String): TrainCategoryDisplay {
    val activeGreen = Color(0xFF4ADE80)
    val delayAmber = Color(0xFFFBBF24)
    val blue = Color(0xFF3B82F6)
    val teal = Color(0xFF0D9488)
    val pink = Color(0xFFDB2777)
    val purple = Color(0xFF7C3AED)
    val textSecondary = Color(0xFF94A3B8)
    
    return when (key.lowercase()) {
        "fr:typeofproductcategory::highspeedrail::", "tgv" -> TrainCategoryDisplay("TGV", purple)
        "fr:typeofproductcategory::regionalrail::", "ter" -> TrainCategoryDisplay("TER", teal)
        "fr:typeofproductcategory::interregionalrail::", "intercités" -> TrainCategoryDisplay("Intercités", blue)
        "fr:typeofproductcategory::suburbanrailway::", "rer" -> TrainCategoryDisplay("RER", pink)
        "fr:typeofproductcategory::local::", "transilien" -> TrainCategoryDisplay("Transilien", activeGreen)
        "fr:typeofproductcategory::tramtrain::", "tram" -> TrainCategoryDisplay("Tram-Train", delayAmber)
        "fr:typeofproductcategory::longdistance::" -> TrainCategoryDisplay("Grandes Lignes", Color(0xFF6366F1))
        "fr:typeofproductcategory::crosscountryrail::" -> TrainCategoryDisplay("Transversal", Color(0xFFF59E0B))
        "fr:typeofproductcategory::railshuttle::", "navette" -> TrainCategoryDisplay("Navette", Color(0xFF14B8A6))
        else -> {
            val shortName = key.removePrefix("fr:typeofproductcategory::").removeSuffix("::").replaceFirstChar { it.uppercase() }
            TrainCategoryDisplay(shortName.ifEmpty { "Train" }, textSecondary)
        }
    }
}

data class NextStopInfo(
    val name: String,
    val minutes: Int,
    val timeFormatted: String,
    val platform: String?
)

fun getNextStopInfo(train: com.octarahq.trainflow.InterpolatedJourney): NextStopInfo? {
    val firstEstimated = train.journey.EstimatedCalls?.EstimatedCall?.firstOrNull()

    val name = firstEstimated?.StopPointName ?: train.journey.DestinationName
    val timeStr = firstEstimated?.ExpectedArrivalTime ?: firstEstimated?.AimedArrivalTime 
        ?: firstEstimated?.ExpectedDepartureTime ?: firstEstimated?.AimedDepartureTime
    val platform = firstEstimated?.ArrivalPlatformName

    if (timeStr.isNullOrEmpty()) return null

    try {
        val instant = java.time.Instant.parse(timeStr)
        
        val diffMs = instant.toEpochMilli() - System.currentTimeMillis()
        val minutes = (diffMs / 60000).toInt()
        
        val displayFormat = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
            .withZone(java.time.ZoneId.systemDefault())
        val timeFormatted = displayFormat.format(instant)
        
        return NextStopInfo(name, minutes, timeFormatted, platform)
    } catch (e: Exception) {
        return null
    }
}

fun getDestinationInfo(train: com.octarahq.trainflow.InterpolatedJourney): NextStopInfo? {
    val estimatedCalls = train.journey.EstimatedCalls?.EstimatedCall
    val recordedCalls = train.journey.RecordedCalls?.RecordedCall
    
    val lastEst = estimatedCalls?.lastOrNull()
    val lastRec = recordedCalls?.lastOrNull()

    val name = lastEst?.StopPointName ?: lastRec?.StopPointName ?: train.journey.DestinationName
    val timeStr = lastEst?.ExpectedArrivalTime ?: lastEst?.AimedArrivalTime 
        ?: lastEst?.ExpectedDepartureTime ?: lastEst?.AimedDepartureTime
        ?: lastRec?.ExpectedArrivalTime ?: lastRec?.AimedArrivalTime
        ?: lastRec?.ExpectedDepartureTime ?: lastRec?.AimedDepartureTime
    val platform = lastEst?.ArrivalPlatformName ?: lastRec?.ArrivalPlatformName

    if (timeStr.isNullOrEmpty()) return null

    try {
        val instant = java.time.Instant.parse(timeStr)
        
        val diffMs = instant.toEpochMilli() - System.currentTimeMillis()
        val minutes = (diffMs / 60000).toInt()
        
        val displayFormat = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
            .withZone(java.time.ZoneId.systemDefault())
        val timeFormatted = displayFormat.format(instant)
        
        return NextStopInfo(name, minutes, timeFormatted, platform)
    } catch (e: Exception) {
        return null
    }
}

enum class StopStatus { PASSED, CURRENT, UPCOMING }

data class TimelineStop(
    val time: String,
    val subTime: String,
    val station: String,
    val status: StopStatus,
    val last: Boolean,
    val segmentFill: Float = 0f
)

fun buildTimeline(train: com.octarahq.trainflow.InterpolatedJourney): List<TimelineStop> {
    val stops = mutableListOf<TimelineStop>()

    val displayFormat = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
        .withZone(java.time.ZoneId.systemDefault())

    fun formatTime(timeStr: String?): String {
        if (timeStr.isNullOrEmpty()) return ""
        return try {
            val instant = java.time.Instant.parse(timeStr)
            displayFormat.format(instant)
        } catch (e: Exception) { "" }
    }

    val nextStopId = train.nextStopId
    val lastStopId = train.lastStopId
    val ratio = train.ratio.toFloat()

    data class RawStop(val ref: String, val name: String, val aimedTime: String?, val realTime: String?)

    val rawStops = mutableListOf<RawStop>()

    train.journey.RecordedCalls?.RecordedCall?.forEach { call ->
        rawStops.add(RawStop(
            ref = call.StopPointRef,
            name = call.StopPointName,
            aimedTime = call.AimedArrivalTime ?: call.AimedDepartureTime,
            realTime = call.ExpectedArrivalTime ?: call.ExpectedDepartureTime
        ))
    }
    train.journey.EstimatedCalls?.EstimatedCall?.forEach { call ->
        rawStops.add(RawStop(
            ref = call.StopPointRef,
            name = call.StopPointName,
            aimedTime = call.AimedArrivalTime ?: call.AimedDepartureTime,
            realTime = call.ExpectedArrivalTime ?: call.ExpectedDepartureTime
        ))
    }

    val currentIndex = rawStops.indexOfFirst { it.ref == nextStopId }
    val lastIndex = rawStops.indexOfFirst { it.ref == lastStopId }

    val absoluteProgress = if (lastIndex >= 0) lastIndex + ratio else 0f

    rawStops.forEachIndexed { index, raw ->
        val status = when {
            index < currentIndex -> StopStatus.PASSED
            index == currentIndex -> StopStatus.CURRENT
            else -> StopStatus.UPCOMING
        }

        val segmentFill = when {
            index == 0 -> 1f
            index <= Math.floor(absoluteProgress.toDouble()).toInt() -> 1f
            index == Math.floor(absoluteProgress.toDouble()).toInt() + 1 -> absoluteProgress % 1f
            else -> 0f
        }

        stops.add(TimelineStop(
            time = formatTime(raw.aimedTime).ifEmpty { formatTime(raw.realTime) },
            subTime = formatTime(raw.realTime),
            station = raw.name,
            status = status,
            last = index == rawStops.size - 1,
            segmentFill = segmentFill
        ))
    }

    return stops
}

data class TrainTrackingData(
    val lastUpdate: Long,
    val currentUpdate: Long,
    val lastLat: Double,
    val lastLon: Double,
    val currentLat: Double,
    val currentLon: Double,
    val speedKmh: Int
)

fun calculateTrackingData(
    oldData: TrainTrackingData?,
    newLat: Double,
    newLon: Double,
    currentTime: Long
): TrainTrackingData? {
    if (oldData == null) {
        return TrainTrackingData(currentTime, currentTime, newLat, newLon, newLat, newLon, 0)
    }
    if (oldData.currentLat == newLat && oldData.currentLon == newLon) {
        return oldData
    }
    
    val results = FloatArray(1)
    android.location.Location.distanceBetween(
        oldData.currentLat, oldData.currentLon,
        newLat, newLon,
        results
    )
    val distanceMeters = results[0]
    val timeDiffHours = (currentTime - oldData.currentUpdate) / 3600000.0
    val speed = if (timeDiffHours > 0) (distanceMeters / 1000.0 / timeDiffHours).toInt() else oldData.speedKmh
    
    return TrainTrackingData(
        lastUpdate = oldData.currentUpdate,
        currentUpdate = currentTime,
        lastLat = oldData.currentLat,
        lastLon = oldData.currentLon,
        currentLat = newLat,
        currentLon = newLon,
        speedKmh = speed
    )
}
