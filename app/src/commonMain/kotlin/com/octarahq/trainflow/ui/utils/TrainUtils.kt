package com.octarahq.trainflow.ui.utils

import androidx.compose.ui.graphics.Color
import kotlinx.datetime.Instant
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class TrainCategoryDisplay(val label: String, val color: Color)

fun getTrainCategoryDisplay(key: String?): TrainCategoryDisplay {
    val activeGreen = Color(0xFF4ADE80)
    val delayAmber = Color(0xFFFBBF24)
    val blue = Color(0xFF3B82F6)
    val teal = Color(0xFF0D9488)
    val pink = Color(0xFFDB2777)
    val purple = Color(0xFF7C3AED)
    val textSecondary = Color(0xFF94A3B8)
    
    val safeKey = (key ?: "").lowercase()
    return when (safeKey) {
        "fr:typeofproductcategory::highspeedrail::", "tgv", "highspeedrail" -> TrainCategoryDisplay("TGV", purple)
        "fr:typeofproductcategory::regionalrail::", "ter", "regionalrail" -> TrainCategoryDisplay("TER", teal)
        "fr:typeofproductcategory::interregionalrail::", "intercités", "interregionalrail" -> TrainCategoryDisplay("Intercités", blue)
        "fr:typeofproductcategory::suburbanrailway::", "rer", "suburbanrailway",
        "fr:typeofproductcategory::local::", "transilien", "local" -> TrainCategoryDisplay("RER", pink)
        "fr:typeofproductcategory::tramtrain::", "tram", "tramtrain" -> TrainCategoryDisplay("Tram-Train", delayAmber)
        "fr:typeofproductcategory::longdistance::", "longdistance" -> TrainCategoryDisplay("Grandes Lignes", Color(0xFF6366F1))
        "fr:typeofproductcategory::crosscountryrail::", "crosscountryrail" -> TrainCategoryDisplay("Transversal", Color(0xFFF59E0B))
        "fr:typeofproductcategory::railshuttle::", "navette", "railshuttle" -> TrainCategoryDisplay("Navette", Color(0xFF14B8A6))
        "fr:typeofproductcategory::international::", "international" -> TrainCategoryDisplay("International", Color(0xFF8B5CF6))
        "fr:typeofproductcategory::regionalcoach::", "regionalcoach" -> TrainCategoryDisplay("Car Régional", Color(0xFF059669))
        "fr:typeofproductcategory::localbus::", "localbus" -> TrainCategoryDisplay("Bus Local", Color(0xFF0284C7))
        "fr:typeofproductcategory::railreplacementcoach::", "railreplacementcoach" -> TrainCategoryDisplay("Car de Remplacement", Color(0xFFDC2626))
        "rail", "train" -> TrainCategoryDisplay("Train", textSecondary)
        else -> {
            val shortName = safeKey.removePrefix("fr:typeofproductcategory::").removeSuffix("::").replaceFirstChar { it.uppercaseChar() }
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

private fun formatTime(timeStr: String?): String {
    if (timeStr.isNullOrEmpty()) return ""
    return try {
        val instant = Instant.parse(timeStr)
        val ldt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val hour = ldt.hour.toString().padStart(2, '0')
        val minute = ldt.minute.toString().padStart(2, '0')
        "$hour:$minute"
    } catch (e: Exception) { "" }
}

fun getNextStopInfo(train: com.octarahq.trainflow.network.InterpolatedJourney): NextStopInfo? {
    val firstEstimated = train.journey.EstimatedCalls?.EstimatedCall?.firstOrNull()

    val name = firstEstimated?.StopPointName ?: train.journey.DestinationName
    val timeStr = firstEstimated?.ExpectedArrivalTime ?: firstEstimated?.AimedArrivalTime 
        ?: firstEstimated?.ExpectedDepartureTime ?: firstEstimated?.AimedDepartureTime
    val platform = firstEstimated?.ArrivalPlatformName

    if (timeStr.isNullOrEmpty()) return null

    try {
        val instant = Instant.parse(timeStr)
        val diffMs = instant.toEpochMilliseconds() - Clock.System.now().toEpochMilliseconds()
        val minutes = (diffMs / 60000).toInt()
        val timeFormatted = formatTime(timeStr)
        
        return NextStopInfo(name, minutes, timeFormatted, platform)
    } catch (e: Exception) {
        return null
    }
}

fun getDestinationInfo(train: com.octarahq.trainflow.network.InterpolatedJourney): NextStopInfo? {
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
        val instant = Instant.parse(timeStr)
        val diffMs = instant.toEpochMilliseconds() - Clock.System.now().toEpochMilliseconds()
        val minutes = (diffMs / 60000).toInt()
        val timeFormatted = formatTime(timeStr)
        
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

fun buildTimeline(train: com.octarahq.trainflow.network.InterpolatedJourney): List<TimelineStop> {
    val stops = mutableListOf<TimelineStop>()
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
    val lastIndex = rawStops.indexOfFirst { it.ref == lastStopId  }
    val absoluteProgress = if (lastIndex >= 0) lastIndex + ratio else 0f

    rawStops.forEachIndexed { index, raw ->
        val status = when {
            index < currentIndex -> StopStatus.PASSED
            index == currentIndex -> StopStatus.CURRENT
            else -> StopStatus.UPCOMING
        }

        val segmentFill = when {
            index == 0 -> 1f
            index <= kotlin.math.floor(absoluteProgress.toDouble()).toInt() -> 1f
            index == kotlin.math.floor(absoluteProgress.toDouble()).toInt() + 1 -> absoluteProgress % 1f
            else -> 0f
        }

        val aimedTimeFormatted = formatTime(raw.aimedTime)
        val realTimeFormatted = formatTime(raw.realTime)

        stops.add(TimelineStop(
            time = aimedTimeFormatted.ifEmpty { realTimeFormatted },
            subTime = realTimeFormatted,
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
    
    val R = 6371e3
    val lat1 = oldData.currentLat * kotlin.math.PI / 180.0
    val lat2 = newLat * kotlin.math.PI / 180.0
    val dLat = (newLat - oldData.currentLat) * kotlin.math.PI / 180.0
    val dLon = (newLon - oldData.currentLon) * kotlin.math.PI / 180.0

    val a = sin(dLat/2) * sin(dLat/2) + cos(lat1) * cos(lat2) * sin(dLon/2) * sin(dLon/2)
    val c = 2 * atan2(sqrt(a), sqrt(1-a))
    val distanceMeters = R * c

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

data class InterpolatedPoint(val lat: Double, val lon: Double)

data class RailSegment(
    val lat1: Double, val lon1: Double,
    val lat2: Double, val lon2: Double
)

object RailNetwork {
    val segments = mutableListOf<RailSegment>()

    fun addLine(coords: List<Pair<Double, Double>>) {
        for (i in 0 until coords.size - 1) {
            val p1 = coords[i]
            val p2 = coords[i + 1]
            segments.add(RailSegment(p1.first, p1.second, p2.first, p2.second))
        }
    }

    fun snap(lat: Double, lon: Double, maxDistDegrees: Double = 0.015): Pair<Double, Double> {
        if (segments.isEmpty() || lat == 0.0 || lon == 0.0) return Pair(lat, lon)

        var bestLat = lat
        var bestLon = lon
        var minDistSq = Double.MAX_VALUE
        val maxLatDist = maxDistDegrees
        val maxLonDist = maxDistDegrees * 1.5

        for (seg in segments) {
            val segMinLat = minOf(seg.lat1, seg.lat2)
            val segMaxLat = maxOf(seg.lat1, seg.lat2)
            val segMinLon = minOf(seg.lon1, seg.lon2)
            val segMaxLon = maxOf(seg.lon1, seg.lon2)
            if (lat < segMinLat - maxLatDist || lat > segMaxLat + maxLatDist) continue
            if (lon < segMinLon - maxLonDist || lon > segMaxLon + maxLonDist) continue

            val dLat = seg.lat2 - seg.lat1
            val dLon = seg.lon2 - seg.lon1
            val ab2 = dLat * dLat + dLon * dLon
            if (ab2 == 0.0) continue

            val apLat = lat - seg.lat1
            val apLon = lon - seg.lon1
            val t = ((apLat * dLat + apLon * dLon) / ab2).coerceIn(0.0, 1.0)

            val projLat = seg.lat1 + t * dLat
            val projLon = seg.lon1 + t * dLon

            val dLatM = (lat - projLat) * 111000.0
            val dLonM = (lon - projLon) * 74000.0
            val distSq = dLatM * dLatM + dLonM * dLonM
            if (distSq < minDistSq) {
                minDistSq = distSq
                bestLat = projLat
                bestLon = projLon
            }
        }

        val maxSnap = 500.0 * 500.0
        return if (minDistSq < maxSnap) Pair(bestLat, bestLon) else Pair(lat, lon)
    }
}

object TrainPositionCache {
    fun getSmoothPosition(
        smartId: String,
        targetLat: Double,
        targetLon: Double,
        trackingData: TrainTrackingData?,
        currentTime: Long,
        zoom: Double = 10.0
    ): InterpolatedPoint {
        if (targetLat == 0.0 && targetLon == 0.0) return InterpolatedPoint(0.0, 0.0)

        if (trackingData == null || trackingData.lastUpdate == trackingData.currentUpdate) {
            return InterpolatedPoint(targetLat, targetLon)
        }

        val intervalMs = (trackingData.currentUpdate - trackingData.lastUpdate).toDouble()
        if (intervalMs < 500.0) return InterpolatedPoint(targetLat, targetLon)

        val intervalSec = intervalMs / 1000.0
        val elapsedSec = ((currentTime - trackingData.currentUpdate) / 1000.0).coerceAtLeast(0.0)

        val vLat = (trackingData.currentLat - trackingData.lastLat) / intervalSec
        val vLon = (trackingData.currentLon - trackingData.lastLon) / intervalSec

        var calcLat = trackingData.currentLat + vLat * elapsedSec
        var calcLon = trackingData.currentLon + vLon * elapsedSec

        if (zoom >= 8.5) {
            val snapped = snapToRailNetwork(calcLat, calcLon)
            calcLat = snapped.first
            calcLon = snapped.second
        }

        return InterpolatedPoint(calcLat, calcLon)
    }
}

fun getSmoothInterpolatedPosition(
    trackingData: TrainTrackingData?,
    baseLat: Double,
    baseLon: Double,
    currentTime: Long,
    zoom: Double = 10.0,
    smartId: String = ""
): InterpolatedPoint {
    return TrainPositionCache.getSmoothPosition(
        smartId = smartId.ifEmpty { "$baseLat,$baseLon" },
        targetLat = baseLat,
        targetLon = baseLon,
        trackingData = trackingData,
        currentTime = currentTime,
        zoom = zoom
    )
}
