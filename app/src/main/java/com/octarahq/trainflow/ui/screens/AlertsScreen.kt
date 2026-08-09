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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicTextField

private object AlertsPalette {
    val background = Color(0xFF0F1115)
    val panel = Color(0xFF1D2026)
    val surface = Color(0xFF262B35)
    val border = Color(0xFF2A303C)
    val textPrimary = Color(0xFFF1F5F9)
    val textSecondary = Color(0xFF94A3B8)
    val blue = Color(0xFF3B82F6)
    val green = Color(0xFF4ADE80)
    val amber = Color(0xFFFBBF24)
    val red = Color(0xFFEF4444)
    val purple = Color(0xFF7C3AED)
    val teal = Color(0xFF0D9488)
}

private data class DelayedTrain(
    val type: String,
    val typeColor: Color,
    val typeTextColor: Color,
    val number: String,
    val delay: String,
    val delayColor: Color,
    val delayBgColor: Color,
    val origin: String,
    val destination: String,
    val reason: String
)

@Composable
fun AlertsScreen() {
    var isSearchExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val allDelayedTrains = listOf(
        DelayedTrain(
            type = "TGV",
            typeColor = AlertsPalette.purple,
            typeTextColor = Color.White,
            number = "6231",
            delay = "+12 min",
            delayColor = AlertsPalette.amber,
            delayBgColor = Color(0xFF451A03),
            origin = "Paris Gare de Lyon",
            destination = "Marseille St-Charles",
            reason = "Panne de signalisation"
        ),
        DelayedTrain(
            type = "TER",
            typeColor = AlertsPalette.teal,
            typeTextColor = Color.White,
            number = "86509",
            delay = "+25 min",
            delayColor = AlertsPalette.amber,
            delayBgColor = Color(0xFF451A03),
            origin = "Bordeaux Saint-Jean",
            destination = "Toulouse Matabiau",
            reason = "Difficultés de raccordement"
        ),
        DelayedTrain(
            type = "Intercités",
            typeColor = AlertsPalette.blue,
            typeTextColor = Color.White,
            number = "4402",
            delay = "+45 min",
            delayColor = AlertsPalette.red,
            delayBgColor = Color(0xFF7F1D1D),
            origin = "Paris Austerlitz",
            destination = "Limoges Bénédictins",
            reason = "Incident technique sur la voie"
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AlertsPalette.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .padding(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            AlertsStatsRow()
            
            if (isSearchExpanded) {
                Surface(
                    color = AlertsPalette.surface,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AlertsPalette.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Filled.Search, contentDescription = null, tint = AlertsPalette.textSecondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle = TextStyle(color = AlertsPalette.textPrimary, fontSize = 16.sp),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text("Chercher un numéro de train...", color = AlertsPalette.textSecondary, fontSize = 16.sp)
                                }
                                innerTextField()
                            }
                        )
                        IconButton(
                            onClick = { 
                                isSearchExpanded = false
                                searchQuery = ""
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.Close, contentDescription = "Fermer", tint = AlertsPalette.textSecondary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "23 Trains en retard",
                        color = AlertsPalette.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { /* TODO: Trier */ }) {
                            Icon(
                                imageVector = Icons.Filled.List,
                                contentDescription = "Trier",
                                tint = AlertsPalette.textSecondary
                            )
                        }
                        IconButton(onClick = { isSearchExpanded = true }) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Rechercher",
                                tint = AlertsPalette.textSecondary
                            )
                        }
                    }
                }
            }

            val filteredTrains = allDelayedTrains.filter { 
                it.number.contains(searchQuery, ignoreCase = true) 
            }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                filteredTrains.forEach { train ->
                    DelayedTrainCard(train = train)
                }
            }
        }
    }
}

@Composable
private fun AlertsStatsRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatBox(
            title = "PONCTUALITÉ",
            value = "87%",
            valueColor = AlertsPalette.green,
            modifier = Modifier.weight(1f)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.height(4.dp).fillMaxWidth(0.8f).background(AlertsPalette.green, RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.width(4.dp))
                Box(modifier = Modifier.height(4.dp).fillMaxWidth().background(AlertsPalette.surface, RoundedCornerShape(2.dp)))
            }
        }
        StatBox(
            title = "INCIDENTS",
            value = "5",
            valueColor = AlertsPalette.amber,
            modifier = Modifier.weight(1f)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.height(4.dp).weight(1f).background(AlertsPalette.red, RoundedCornerShape(2.dp)))
                Box(modifier = Modifier.height(4.dp).weight(1f).background(AlertsPalette.red, RoundedCornerShape(2.dp)))
                Box(modifier = Modifier.height(4.dp).weight(1f).background(AlertsPalette.amber, RoundedCornerShape(2.dp)))
                Box(modifier = Modifier.height(4.dp).weight(1f).background(AlertsPalette.surface, RoundedCornerShape(2.dp)))
                Box(modifier = Modifier.height(4.dp).weight(1f).background(AlertsPalette.surface, RoundedCornerShape(2.dp)))
            }
        }
        StatBox(
            title = "TRAINS ACTIFS",
            value = "1 247",
            valueColor = AlertsPalette.textPrimary,
            modifier = Modifier.weight(1f)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.height(4.dp).fillMaxWidth(0.6f).background(AlertsPalette.blue, RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.width(4.dp))
                Box(modifier = Modifier.height(4.dp).fillMaxWidth().background(AlertsPalette.surface, RoundedCornerShape(2.dp)))
            }
        }
    }
}

@Composable
private fun StatBox(
    title: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    bars: @Composable () -> Unit
) {
    Surface(
        color = AlertsPalette.panel,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AlertsPalette.border),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                color = AlertsPalette.textSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value,
                color = valueColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            bars()
        }
    }
}

@Composable
private fun DelayedTrainCard(train: DelayedTrain) {
    Surface(
        color = AlertsPalette.panel,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AlertsPalette.border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = train.typeColor,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = train.type,
                            color = train.typeTextColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = train.number,
                        color = AlertsPalette.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    color = train.delayBgColor,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = train.delay,
                        color = train.delayColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = train.origin,
                    color = AlertsPalette.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = AlertsPalette.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = train.destination,
                    color = AlertsPalette.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = AlertsPalette.textSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = train.reason,
                    color = AlertsPalette.textSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}
