package com.octarahq.trainflow.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private object TrainInfoPalette {
    val background = Color(0xFF0F1115)
    val panel = Color(0xFF1D2026)
    val surface = Color(0xFF262B35)
    val border = Color(0xFF2A303C)
    val textPrimary = Color(0xFFF1F5F9)
    val textSecondary = Color(0xFF94A3B8)
    val activeGreen = Color(0xFF4ADE80)
    val blue = Color(0xFF3B82F6)
    val purple = Color(0xFF7C3AED)
    val amber = Color(0xFFFBBF24)
}

@Composable
fun TrainInfoScreen(trainId: String = "", speedKmh: Int? = null, onBack: () -> Unit = {}) {
    var train by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<com.octarahq.trainflow.InterpolatedJourney?>(null) }
    var loading by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }

    androidx.compose.runtime.LaunchedEffect(trainId) {
        if (trainId.isNotEmpty()) {
            try {
                val response = com.octarahq.trainflow.ApiClient.apiService.getSingleVehicle(trainId)
                train = response.vehicle
            } catch (e: Exception) {
            } finally {
                loading = false
            }
        } else {
            loading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TrainInfoPalette.background)
    ) {
        if (loading) {
            androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = TrainInfoPalette.blue)
        } else if (train == null) {
            Text("Train non trouvé", color = TrainInfoPalette.textPrimary, modifier = Modifier.align(Alignment.Center))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 170.dp)
            ) {
                TrainInfoTopBar(train = train!!, onBack = onBack)
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    HeroCard(train = train!!, speedKmh = speedKmh)
                    SectionTitle(text = "Parcours du Train")
                    TimelineCard(train = train!!)
                    DetailsCard(train = train!!)
                }
            }
        }
    }
}

@Composable
private fun TrainInfoTopBar(train: com.octarahq.trainflow.InterpolatedJourney, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(24.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Retour",
                tint = TrainInfoPalette.textPrimary
            )
        }
        Text(
            text = "Train Info",
            color = TrainInfoPalette.textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f).padding(start = 16.dp)
        )
        Surface(
            color = TrainInfoPalette.surface,
            shape = RoundedCornerShape(6.dp)
        ) {
            val name = train.journey.TrainNumbers?.TrainNumberRef ?: train.journey.PublishedLineName
            Text(
                text = name,
                color = TrainInfoPalette.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun HeroCard(train: com.octarahq.trainflow.InterpolatedJourney, speedKmh: Int? = null) {
    Surface(
        color = TrainInfoPalette.panel,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, TrainInfoPalette.border)
    ) {
        val category = com.octarahq.trainflow.ui.utils.getTrainCategoryDisplay(train.journey.ProductCategoryRef)
        val name = train.journey.TrainNumbers?.TrainNumberRef ?: train.journey.PublishedLineName
        val delayText = if ((train.delayMinutes ?: 0) > 0) "+${train.delayMinutes} min" else "À l'heure"
        val statusColor = if ((train.delayMinutes ?: 0) > 0) TrainInfoPalette.amber else TrainInfoPalette.activeGreen

        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Surface(color = category.color, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = category.label.uppercase(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = name,
                        color = TrainInfoPalette.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(color = statusColor.copy(alpha = 0.2f), shape = RoundedCornerShape(20.dp)) {
                    Text(
                        text = delayText,
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(TrainInfoPalette.border))

            Row(modifier = Modifier.fillMaxWidth()) {
                val nextStop = com.octarahq.trainflow.ui.utils.getNextStopInfo(train)
                val stopName = nextStop?.name ?: train.journey.DestinationName
                val speedText = speedKmh?.let { "$it km/h" } ?: "En route"
                StatColumn(title = "VITESSE", value = speedText, valueColor = TrainInfoPalette.textPrimary, modifier = Modifier.weight(1f))
                StatColumn(title = "RETARD CUMULÉ", value = "${train.delayMinutes ?: 0} min", valueColor = if ((train.delayMinutes ?: 0) > 0) TrainInfoPalette.amber else TrainInfoPalette.activeGreen, modifier = Modifier.weight(1f))
                StatColumn(title = "SUIVANT", value = stopName, valueColor = TrainInfoPalette.blue, modifier = Modifier.weight(1f), truncate = true)
            }
        }
    }
}

@Composable
private fun StatColumn(
    title: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    truncate: Boolean = false
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = title, color = TrainInfoPalette.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Text(
            text = value,
            color = valueColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = if (truncate) 1 else Int.MAX_VALUE,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = TrainInfoPalette.textPrimary,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun TimelineCard(train: com.octarahq.trainflow.InterpolatedJourney) {
    Surface(
        color = TrainInfoPalette.panel,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, TrainInfoPalette.border)
    ) {
        val timeline = com.octarahq.trainflow.ui.utils.buildTimeline(train)
        if (timeline.isEmpty()) {
            Text(
                "Aucun arrêt prévu",
                color = TrainInfoPalette.textSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp)) {
                timeline.forEachIndexed { index, stop ->
                    StopRow(
                        stop = stop,
                        isLast = index == timeline.size - 1
                    )
                }
            }
        }
    }
}

@Composable
private fun StopRow(
    stop: com.octarahq.trainflow.ui.utils.TimelineStop,
    isLast: Boolean
) {
    val isPassed = stop.status == com.octarahq.trainflow.ui.utils.StopStatus.PASSED
    val isCurrent = stop.status == com.octarahq.trainflow.ui.utils.StopStatus.CURRENT

    val stationColor = when {
        isPassed -> TrainInfoPalette.textPrimary
        isCurrent -> TrainInfoPalette.blue
        else -> TrainInfoPalette.textSecondary
    }
    val timeColor = when {
        isPassed -> TrainInfoPalette.textSecondary
        isCurrent -> TrainInfoPalette.textPrimary
        else -> TrainInfoPalette.textSecondary
    }

    val trackWidthDp = 6.dp
    val dotSizeDp = if (isCurrent) 14.dp else 10.dp
    val railColWidthDp = 28.dp
    val connectorHeightDp = if (isLast) 0.dp else 36.dp

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier.width(railColWidthDp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .size(dotSizeDp + 10.dp)
                            .background(TrainInfoPalette.blue.copy(alpha = 0.20f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(dotSizeDp)
                                .background(TrainInfoPalette.blue, CircleShape)
                        )
                    }
                } else if (isPassed) {
                    Box(
                        modifier = Modifier
                            .size(dotSizeDp)
                            .background(TrainInfoPalette.blue, CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(dotSizeDp)
                            .background(Color(0xFF1E293B), CircleShape)
                            .border(2.dp, Color(0xFF475569), CircleShape)
                    )
                }

                if (!isLast) {
                    Box(
                        modifier = Modifier
                            .width(trackWidthDp)
                            .height(connectorHeightDp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF1E293B))
                        )
                        
                        if (isPassed) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(TrainInfoPalette.blue)
                            )
                        } else if (isCurrent && stop.segmentFill > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(connectorHeightDp * stop.segmentFill)
                                    .background(TrainInfoPalette.blue)
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .padding(
                    start = 8.dp,
                    bottom = if (isLast) 0.dp else connectorHeightDp + dotSizeDp - 12.dp
                ),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stop.station,
                color = stationColor,
                fontSize = if (isCurrent) 15.sp else 13.sp,
                fontWeight = if (isCurrent || isPassed) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            if (stop.time.isNotBlank()) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = stop.time,
                        color = timeColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (stop.subTime.isNotBlank() && stop.subTime != stop.time) {
                        Text(
                            text = stop.subTime,
                            color = TrainInfoPalette.activeGreen,
                            fontSize = 10.sp,
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun DetailsCard(train: com.octarahq.trainflow.InterpolatedJourney) {
    Surface(
        color = TrainInfoPalette.panel,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, TrainInfoPalette.border)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Détails du Matériel",
                color = TrainInfoPalette.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                SpecItem(label = "Opérateur", value = "SNCF", modifier = Modifier.weight(1f))
                val category = com.octarahq.trainflow.ui.utils.getTrainCategoryDisplay(train.journey.ProductCategoryRef)
                SpecItem(label = "Composition", value = category.label, modifier = Modifier.weight(1f))
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(TrainInfoPalette.border))

            Row(modifier = Modifier.fillMaxWidth()) {
                SpecItem(label = "Destination", value = train.journey.DestinationName, modifier = Modifier.weight(1f))
                val nextStop = com.octarahq.trainflow.ui.utils.getNextStopInfo(train)
                val platformStr = nextStop?.platform?.let { "Voie $it" } ?: "Voie non annoncée"
                SpecItem(label = "Voie Prévue", value = platformStr, valueColor = TrainInfoPalette.amber, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SpecItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = TrainInfoPalette.textPrimary
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, color = TrainInfoPalette.textSecondary, fontSize = 11.sp)
        Text(text = value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}