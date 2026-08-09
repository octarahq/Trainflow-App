package com.octarahq.trainflow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas

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
fun TrainInfoScreen(onBack: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TrainInfoPalette.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 170.dp)
        ) {
            TrainInfoTopBar(onBack = onBack)
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                HeroCard()
                SectionTitle(text = "Parcours du Train")
                TimelineCard()
                DetailsCard()
            }
        }
    }
}

@Composable
private fun TrainInfoTopBar(onBack: () -> Unit) {
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
            Text(
                text = "TGV 6231",
                color = TrainInfoPalette.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun HeroCard() {
    Surface(
        color = TrainInfoPalette.panel,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, TrainInfoPalette.border)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(color = TrainInfoPalette.purple, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = "TGV",
                            color = Color(0xFFF3E8FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "InOui 6231",
                        color = TrainInfoPalette.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(color = Color(0xFF064E3B), shape = RoundedCornerShape(20.dp)) {
                    Text(
                        text = "À l'heure",
                        color = TrainInfoPalette.activeGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(TrainInfoPalette.border))

            Row(modifier = Modifier.fillMaxWidth()) {
                StatColumn(title = "VITESSE", value = "246 km/h", valueColor = TrainInfoPalette.textPrimary, modifier = Modifier.weight(1f))
                StatColumn(title = "RETARD CUMULÉ", value = "0 min", valueColor = TrainInfoPalette.activeGreen, modifier = Modifier.weight(1f))
                StatColumn(title = "SUIVANT", value = "Lyon Part-D...", valueColor = TrainInfoPalette.blue, modifier = Modifier.weight(1f), truncate = true)
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
private fun TimelineCard() {
    Surface(
        color = TrainInfoPalette.panel,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, TrainInfoPalette.border)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            listOf(
                TimelineStop("14:15", "14:15", "Paris Gare de Lyon", active = true, last = false),
                TimelineStop("15:28", "15:28", "Creusot TGV", active = true, last = false),
                TimelineStop("15:52", "15:52", "Macon Loché TGV", active = true, last = false),
                TimelineStop("16:18", "16:18", "Lyon Part-Dieu", active = false, last = false),
                TimelineStop("16:54", "16:54", "Valence TGV", active = false, last = false),
                TimelineStop("17:19", "17:19", "Marseille Saint-Charles", active = false, last = true)
            ).forEachIndexed { index, stop ->
                StopRow(stop = stop, showConnector = index != 5)
            }
        }
    }
}

private data class TimelineStop(
    val time: String,
    val subTime: String,
    val station: String,
    val active: Boolean,
    val last: Boolean
)

@Composable
private fun StopRow(stop: TimelineStop, showConnector: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.width(48.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = stop.time,
                color = if (stop.active) TrainInfoPalette.textPrimary else TrainInfoPalette.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stop.subTime,
                color = TrainInfoPalette.activeGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal
            )
        }

        Box(
            modifier = Modifier
                .width(20.dp)
                .height(72.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            StopRail(active = stop.active, showConnector = showConnector)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 2.dp, top = 2.dp)
        ) {
            Text(
                text = stop.station,
                color = if (stop.active && stop.station == "Lyon Part-Dieu") TrainInfoPalette.textPrimary else TrainInfoPalette.textSecondary,
                fontSize = 14.sp,
                fontWeight = if (stop.active && stop.station == "Lyon Part-Dieu") FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun StopRail(active: Boolean, showConnector: Boolean) {
    Canvas(modifier = Modifier.size(width = 16.dp, height = 72.dp)) {
        val centerX = size.width / 2f
        val top = 6f
        val bottom = size.height - 6f
        val lineColor = if (active) TrainInfoPalette.blue else TrainInfoPalette.border
        val dotColor = if (active) TrainInfoPalette.blue else Color(0xFF94A3B8)

        drawCircle(
            color = dotColor,
            radius = 5.5f,
            center = Offset(centerX, top)
        )
        drawCircle(
            color = dotColor,
            radius = 5.5f,
            center = Offset(centerX, top),
            style = Stroke(width = 2.4f)
        )

        if (showConnector) {
            drawLine(
                color = lineColor,
                start = Offset(centerX, top + 8f),
                end = Offset(centerX, bottom),
                strokeWidth = 2.4f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun DetailsCard() {
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
                SpecItem(label = "Composition", value = "Double Rame (8 voitures)", modifier = Modifier.weight(1f))
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(TrainInfoPalette.border))

            Row(modifier = Modifier.fillMaxWidth()) {
                SpecItem(label = "Type de rame", value = "TGV Euroduplex", modifier = Modifier.weight(1f))
                SpecItem(label = "Voie Prévue", value = "Voie H (Gare de Lyon)", valueColor = TrainInfoPalette.amber, modifier = Modifier.weight(1f))
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