package com.octarahq.trainflow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.octarahq.trainflow.ui.utils.JourneyRepository
import com.octarahq.trainflow.ui.utils.SavedJourney

private object TripsPalette {
    val background = Color(0xFF0F1115)
    val panel = Color(0xFF1D2026)
    val surface = Color(0xFF262B35)
    val border = Color(0xFF2A303C)
    val textPrimary = Color(0xFFF1F5F9)
    val textSecondary = Color(0xFF94A3B8)
    val activeGreen = Color(0xFF4ADE80)
    val delayAmber = Color(0xFFFBBF24)
    val blue = Color(0xFF3B82F6)
    val teal = Color(0xFF0D9488)
    val purple = Color(0xFF7C3AED)
}

private data class TripItem(
    val date: String,
    val trainType: String,
    val trainNumber: String,
    val departureTime: String,
    val arrivalTime: String,
    val departureStation: String,
    val arrivalStation: String,
    val duration: String,
    val status: String,
    val statusColor: Color,
    val badgeColor: Color
)

@Composable
fun TripsScreen(
    onBack: () -> Unit = {},
    onSearch: () -> Unit = {},
    onOpenTrainInfo: (String, String?) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var journeysList by remember { mutableStateOf<List<SavedJourney>>(JourneyRepository.getJourneys(context)) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TripsPalette.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TripsTopBar(onBack = onBack, onSearch = onSearch)

            if (journeysList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp)
                        .padding(bottom = 120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucun voyage actif.\nAppuyez sur le bouton + pour en ajouter un.",
                        color = TripsPalette.textSecondary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .padding(bottom = 170.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (item in journeysList) {
                        TripCard(
                            journey = item,
                            onDelete = {
                                JourneyRepository.deleteJourney(context, item.pnr, item.trainNumber)
                                journeysList = JourneyRepository.getJourneys(context)
                            },
                            onClick = {
                                onOpenTrainInfo(item.trainNumber, item.vehicleJourneyRef)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TripsTopBar(onBack: () -> Unit, onSearch: () -> Unit) {
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
                tint = TripsPalette.textPrimary
            )
        }
        Text(
            text = "Mes Trajets",
            color = TripsPalette.textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f).padding(start = 16.dp)
        )
        IconButton(onClick = onSearch, modifier = Modifier.size(24.dp)) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Rechercher",
                tint = TripsPalette.textPrimary
            )
        }
    }
}

@Composable
private fun TripCard(
    journey: SavedJourney,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = TripsPalette.panel,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, TripsPalette.border)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = journey.travelDate,
                        color = TripsPalette.textSecondary,
                        fontSize = 13.sp
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    val catRef = journey.categoryRef ?: ""
                    val categoryDisplay = com.octarahq.trainflow.ui.utils.getTrainCategoryDisplay(catRef.ifEmpty { "train" })
                    Surface(color = categoryDisplay.color, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = categoryDisplay.label.uppercase(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "n°${journey.trainNumber}",
                        color = TripsPalette.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Supprimer",
                            tint = Color(0xFFEF4444).copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.width(50.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(text = journey.departureTime, color = TripsPalette.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(text = journey.arrivalTime, color = TripsPalette.textSecondary, fontSize = 14.sp)
                }

                TimelineRail(active = true)

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = journey.departureStationCode,
                        color = TripsPalette.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = journey.arrivalStationCode,
                        color = TripsPalette.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineRail(active: Boolean) {
    Box(
        modifier = Modifier
            .width(16.dp)
            .height(56.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(TripsPalette.blue, CircleShape)
                    .border(2.dp, Color(0xFF0F1115), CircleShape)
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(28.dp)
                    .background(if (active) TripsPalette.blue else TripsPalette.border)
            )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .border(2.dp, Color(0xFF94A3B8), CircleShape)
            )
        }
    }
}
