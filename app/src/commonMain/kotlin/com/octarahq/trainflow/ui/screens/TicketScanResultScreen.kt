package com.octarahq.trainflow.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.octarahq.trainflow.ui.utils.TicketParser
import com.octarahq.trainflow.ui.utils.SavedJourney
import com.octarahq.trainflow.ui.utils.saveJourney

private object ScanResultPalette {
    val background = Color(0xFF0F1115)
    val panel = Color(0xFF1D2026)
    val border = Color(0xFF2A303C)
    val textPrimary = Color(0xFFF1F5F9)
    val textSecondary = Color(0xFF94A3B8)
    val blue = Color(0xFF3B82F6)
    val surface = Color(0xFF262B35)
}

data class TicketScanResultScreen(
    val rawData: String,
    val onBack: (() -> Unit)? = null,
    val onRescan: () -> Unit = {}
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val handleBack: () -> Unit = { if (onBack != null) onBack.invoke() else navigator.pop() }
        val scrollState = rememberScrollState()
        val parsedTicket = TicketParser.parse(rawData)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScanResultPalette.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = handleBack, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour",
                        tint = ScanResultPalette.textPrimary
                    )
                }
                Text(
                    text = "Détails du voyage",
                    color = ScanResultPalette.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (parsedTicket != null && !parsedTicket.isSecureBinary) {
                    Surface(
                        color = ScanResultPalette.panel,
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ScanResultPalette.border)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Départ",
                                        color = ScanResultPalette.textSecondary,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = parsedTicket.departureStationCode,
                                        color = ScanResultPalette.textPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    tint = ScanResultPalette.blue,
                                    modifier = Modifier.size(18.dp).rotate(180f)
                                )

                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = "Arrivée",
                                        color = ScanResultPalette.textSecondary,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = parsedTicket.arrivalStationCode,
                                        color = ScanResultPalette.textPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(ScanResultPalette.border)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = "Voyageur", color = ScanResultPalette.textSecondary, fontSize = 12.sp)
                                    Text(text = parsedTicket.passengerName, color = ScanResultPalette.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "Dossier", color = ScanResultPalette.textSecondary, fontSize = 12.sp)
                                    Text(text = parsedTicket.pnr, color = ScanResultPalette.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = "Train", color = ScanResultPalette.textSecondary, fontSize = 12.sp)
                                    Text(text = "n°${parsedTicket.trainNumber}", color = ScanResultPalette.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "Date", color = ScanResultPalette.textSecondary, fontSize = 12.sp)
                                    Text(text = parsedTicket.travelDate, color = ScanResultPalette.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val savedJourney = SavedJourney(
                                pnr = parsedTicket.pnr,
                                passengerName = parsedTicket.passengerName,
                                departureStationCode = parsedTicket.departureStationCode,
                                arrivalStationCode = parsedTicket.arrivalStationCode,
                                trainNumber = parsedTicket.trainNumber,
                                travelDate = parsedTicket.travelDate,
                                departureTime = parsedTicket.departureTime,
                                arrivalTime = parsedTicket.arrivalTime
                            )
                            saveJourney(savedJourney)

                            handleBack()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ScanResultPalette.blue,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Ajouter dans mes voyages",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Surface(
                        color = ScanResultPalette.panel,
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ScanResultPalette.border)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Impossible de lire le billet",
                                color = ScanResultPalette.textSecondary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onRescan,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ScanResultPalette.surface,
                            contentColor = ScanResultPalette.textPrimary
                        )
                    ) {
                        Text(text = "Rescanner", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = handleBack,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color(0xFFEF4444)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
                    ) {
                        Text(text = "Annuler", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
}
