package com.octarahq.trainflow.ui.screens

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.octarahq.trainflow.ApiClient
import com.octarahq.trainflow.GareObj
import com.octarahq.trainflow.InterpolatedJourney
import com.octarahq.trainflow.ui.utils.getTrainCategoryDisplay

private object StationInfoPalette {
    val background = Color(0xFF0F1115)
    val panel = Color(0xFF1D2026)
    val surface = Color(0xFF262B35)
    val border = Color(0xFF2A303C)
    val textPrimary = Color(0xFFF1F5F9)
    val textSecondary = Color(0xFF94A3B8)
    val blue = Color(0xFF3B82F6)
    val purple = Color(0xFF7C3AED)
    val amber = Color(0xFFFBBF24)
    val green = Color(0xFF22C55E)
}

private data class StationBoardItem(
    val smartId: String,
    val trainName: String,
    val categoryLabel: String,
    val categoryColor: Color,
    val originName: String,
    val destinationName: String,
    val platform: String?,
    val formattedTime: String,
    val delayMinutes: Int,
    val timeMs: Long = Long.MAX_VALUE
)

@Composable
fun StationInfoScreen(
    stationName: String,
    uic: String? = null,
    lat: Double? = null,
    lon: Double? = null,
    onBack: () -> Unit = {},
    onLocateOnMap: (Double, Double) -> Unit = { _, _ -> },
    onOpenTrainInfo: (String) -> Unit = {}
) {
    var activeTab by remember { mutableStateOf(0) }
    var liveVehicles by remember { mutableStateOf<List<InterpolatedJourney>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val gareObj = remember(stationName, uic, lat, lon) {
        GareObj(name = stationName, uic = uic, lat = lat, lon = lon)
    }

    LaunchedEffect(gareObj) {
        isLoading = true
        try {
            val response = ApiClient.apiService.getLiveVehicles()
            liveVehicles = response.vehicles
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    val (departures, arrivals) = remember(liveVehicles, gareObj) {
        extractStationBoard(gareObj, liveVehicles)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StationInfoPalette.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(StationInfoPalette.panel)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour",
                        tint = StationInfoPalette.textPrimary
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stationName,
                        color = StationInfoPalette.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (!uic.isNullOrEmpty()) "Code Gare UIC : $uic" else "Gare SNCF",
                        color = StationInfoPalette.textSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            if (lat != null && lon != null) {
                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    onClick = { onLocateOnMap(lat, lon) },
                    color = StationInfoPalette.blue,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Voir sur la carte",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                onClick = { activeTab = 0 },
                color = if (activeTab == 0) StationInfoPalette.surface else StationInfoPalette.panel,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, if (activeTab == 0) StationInfoPalette.blue else StationInfoPalette.border),
                modifier = Modifier.weight(1f)
            ) {
                Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Départs (${departures.size})",
                        color = if (activeTab == 0) StationInfoPalette.blue else StationInfoPalette.textSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Surface(
                onClick = { activeTab = 1 },
                color = if (activeTab == 1) StationInfoPalette.surface else StationInfoPalette.panel,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, if (activeTab == 1) StationInfoPalette.blue else StationInfoPalette.border),
                modifier = Modifier.weight(1f)
            ) {
                Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Arrivées (${arrivals.size})",
                        color = if (activeTab == 1) StationInfoPalette.blue else StationInfoPalette.textSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        val currentList = if (activeTab == 0) departures else arrivals

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = StationInfoPalette.blue)
            }
        } else if (currentList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = if (activeTab == 0) "Aucun départ prévu prochainement" else "Aucune arrivée prévue prochainement",
                    color = StationInfoPalette.textSecondary,
                    fontSize = 15.sp
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                currentList.forEach { call ->
                    StationCallCard(call = call, onClick = { if (call.smartId.isNotBlank()) onOpenTrainInfo(call.smartId) })
                }
            }
        }
    }
}

@Composable
private fun StationCallCard(call: StationBoardItem, onClick: () -> Unit) {
    val isRelative = call.formattedTime.startsWith("dans ") || call.formattedTime == "À l'instant"
    val timeBgColor = if (isRelative) StationInfoPalette.blue.copy(alpha = 0.2f) else StationInfoPalette.surface
    val timeTextColor = if (isRelative) StationInfoPalette.blue else StationInfoPalette.textPrimary

    val delayText = if (call.delayMinutes > 0) "+${call.delayMinutes} min" else "À l'heure"
    val delayColor = if (call.delayMinutes > 0) StationInfoPalette.amber else StationInfoPalette.green

    Surface(
        onClick = onClick,
        color = StationInfoPalette.panel,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, StationInfoPalette.border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(color = call.categoryColor, shape = RoundedCornerShape(6.dp)) {
                    Text(
                        text = call.categoryLabel,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = call.trainName,
                    color = StationInfoPalette.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (!call.platform.isNullOrEmpty()) {
                    Surface(
                        color = StationInfoPalette.purple.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Voie ${call.platform}",
                            color = StationInfoPalette.purple,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Surface(
                    color = timeBgColor,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = call.formattedTime,
                        color = timeTextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    color = if (call.delayMinutes > 0) Color(0xFF451A03) else Color(0xFF064E3B),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = delayText,
                        color = delayColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = call.originName,
                    color = StationInfoPalette.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = StationInfoPalette.textSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = call.destinationName,
                    color = StationInfoPalette.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }
    }
}

private fun extractStationBoard(
    gare: GareObj,
    vehicles: List<InterpolatedJourney>
): Pair<List<StationBoardItem>, List<StationBoardItem>> {
    val departures = mutableListOf<StationBoardItem>()
    val arrivals = mutableListOf<StationBoardItem>()

    val normGareName = normalizeStationString(gare.name)
    val gareUic = gare.uic?.lowercase()?.trim()
    val nowMs = System.currentTimeMillis()

    for (vehicle in vehicles) {
        val category = getTrainCategoryDisplay(vehicle.journey.ProductCategoryRef.ifBlank { vehicle.journey.VehicleMode })
        val smartId = vehicle.journey.FramedVehicleJourneyRef?.DatedVehicleJourneyRef
            ?: vehicle.journey.TrainNumbers?.TrainNumberRef
            ?: vehicle.journey.VehicleJourneyRef

        val rawLineName = vehicle.journey.PublishedLineName.ifBlank {
            vehicle.journey.TrainNumbers?.TrainNumberRef ?: vehicle.journey.VehicleJourneyRef
        }

        val trainName = if (rawLineName.lowercase().startsWith(category.label.lowercase())) {
            rawLineName
        } else {
            "${category.label} $rawLineName"
        }

        val originName = vehicle.journey.OriginName.ifBlank { "Inconnue" }
        val destinationName = vehicle.journey.DestinationName.ifBlank { "Inconnue" }
        val delayMin = vehicle.delayMinutes ?: 0

        val processedDepIds = mutableSetOf<String>()
        val processedArrIds = mutableSetOf<String>()

        fun isStationMatch(stopRef: String, stopName: String): Boolean {
            if (!gareUic.isNullOrEmpty()) {
                val refLower = stopRef.lowercase()
                if (refLower.contains(gareUic) || refLower.endsWith(gareUic)) return true
            }
            val normStop = normalizeStationString(stopName)
            if (normStop.isBlank() || normGareName.isBlank()) return false
            return normStop.contains(normGareName) || normGareName.contains(normStop)
        }

        vehicle.journey.EstimatedCalls?.EstimatedCall?.forEach { call ->
            if (isStationMatch(call.StopPointRef, call.StopPointName)) {
                val depIso = call.ExpectedDepartureTime ?: call.AimedDepartureTime
                val arrIso = call.ExpectedArrivalTime ?: call.AimedArrivalTime
                val plat = call.ArrivalPlatformName

                if (depIso != null && processedDepIds.add(smartId)) {
                    val formatted = formatRelativeOrClockTime(depIso, nowMs)
                    departures.add(
                        StationBoardItem(
                            smartId = smartId,
                            trainName = trainName,
                            categoryLabel = category.label,
                            categoryColor = category.color,
                            originName = originName,
                            destinationName = destinationName,
                            platform = plat,
                            formattedTime = formatted,
                            delayMinutes = delayMin,
                            timeMs = parseIsoToEpochMs(depIso) ?: Long.MAX_VALUE
                        )
                    )
                }
                if (arrIso != null && processedArrIds.add(smartId)) {
                    val formatted = formatRelativeOrClockTime(arrIso, nowMs)
                    arrivals.add(
                        StationBoardItem(
                            smartId = smartId,
                            trainName = trainName,
                            categoryLabel = category.label,
                            categoryColor = category.color,
                            originName = originName,
                            destinationName = destinationName,
                            platform = plat,
                            formattedTime = formatted,
                            delayMinutes = delayMin,
                            timeMs = parseIsoToEpochMs(arrIso) ?: Long.MAX_VALUE
                        )
                    )
                }
            }
        }

        vehicle.journey.RecordedCalls?.RecordedCall?.forEach { call ->
            if (isStationMatch(call.StopPointRef, call.StopPointName)) {
                val depIso = call.ExpectedDepartureTime ?: call.AimedDepartureTime
                val arrIso = call.ExpectedArrivalTime ?: call.AimedArrivalTime
                val plat = call.ArrivalPlatformName ?: call.DeparturePlatformName

                if (depIso != null && processedDepIds.add(smartId)) {
                    val formatted = formatRelativeOrClockTime(depIso, nowMs)
                    departures.add(
                        StationBoardItem(
                            smartId = smartId,
                            trainName = trainName,
                            categoryLabel = category.label,
                            categoryColor = category.color,
                            originName = originName,
                            destinationName = destinationName,
                            platform = plat,
                            formattedTime = formatted,
                            delayMinutes = delayMin,
                            timeMs = parseIsoToEpochMs(depIso) ?: Long.MAX_VALUE
                        )
                    )
                }
                if (arrIso != null && processedArrIds.add(smartId)) {
                    val formatted = formatRelativeOrClockTime(arrIso, nowMs)
                    arrivals.add(
                        StationBoardItem(
                            smartId = smartId,
                            trainName = trainName,
                            categoryLabel = category.label,
                            categoryColor = category.color,
                            originName = originName,
                            destinationName = destinationName,
                            platform = plat,
                            formattedTime = formatted,
                            delayMinutes = delayMin,
                            timeMs = parseIsoToEpochMs(arrIso) ?: Long.MAX_VALUE
                        )
                    )
                }
            }
        }

        val normOrigin = normalizeStationString(vehicle.journey.OriginName)
        val normDest = normalizeStationString(vehicle.journey.DestinationName)

        val isOriginMatch = (normOrigin.isNotBlank() && (normOrigin.contains(normGareName) || normGareName.contains(normOrigin)))
        val isDestMatch = (normDest.isNotBlank() && (normDest.contains(normGareName) || normGareName.contains(normDest)))

        if (isOriginMatch && processedDepIds.add(smartId)) {
            departures.add(
                StationBoardItem(
                    smartId = smartId,
                    trainName = trainName,
                    categoryLabel = category.label,
                    categoryColor = category.color,
                    originName = originName,
                    destinationName = destinationName,
                    platform = null,
                    formattedTime = "En transit",
                    delayMinutes = delayMin,
                    timeMs = Long.MAX_VALUE
                )
            )
        }

        if (isDestMatch && processedArrIds.add(smartId)) {
            arrivals.add(
                StationBoardItem(
                    smartId = smartId,
                    trainName = trainName,
                    categoryLabel = category.label,
                    categoryColor = category.color,
                    originName = originName,
                    destinationName = destinationName,
                    platform = null,
                    formattedTime = "En transit",
                    delayMinutes = delayMin,
                    timeMs = Long.MAX_VALUE
                )
            )
        }
    }

    val sortedDepartures = departures.sortedBy { it.timeMs }
    val sortedArrivals = arrivals.sortedBy { it.timeMs }

    return Pair(sortedDepartures, sortedArrivals)
}

private fun normalizeStationString(str: String): String {
    if (str.isBlank()) return ""
    val unaccented = java.text.Normalizer.normalize(str, java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    return unaccented.lowercase()
        .replace("-", " ")
        .replace("gare de ", " ")
        .replace("gare d'", " ")
        .replace("gare du ", " ")
        .replace("gare des ", " ")
        .replace("gare", " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun formatRelativeOrClockTime(isoStr: String?, nowMs: Long = System.currentTimeMillis()): String {
    if (isoStr.isNullOrEmpty()) return "--:--"
    val targetMs = parseIsoToEpochMs(isoStr) ?: return isoStr.takeLast(8).take(5)

    val diffMs = targetMs - nowMs
    val diffMinutes = (diffMs / 60_000).toInt()

    return when {
        diffMinutes in 0..59 -> "dans ${maxOf(1, diffMinutes)} min"
        diffMinutes in -5..-1 -> "dans <1 min"
        else -> {
            val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            sdf.format(java.util.Date(targetMs))
        }
    }
}

private fun parseIsoToEpochMs(isoStr: String): Long? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            java.time.Instant.parse(isoStr).toEpochMilli()
        } else {
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).parse(isoStr)?.time
        }
    } catch (e: Exception) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                java.time.ZonedDateTime.parse(isoStr).toInstant().toEpochMilli()
            } else {
                null
            }
        } catch (e2: Exception) {
            null
        }
    }
}
