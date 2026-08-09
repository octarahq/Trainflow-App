package com.octarahq.trainflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BottomPanel = Color(0xFF1D2026)
private val BottomBorder = Color(0xFF2A303C)
private val BottomText = Color(0xFF94A3B8)
private val BottomTextActive = Color(0xFFF1F5F9)
private val BottomBlue = Color(0xFF3B82F6)

@Composable
fun TrainflowBottomChrome(
    currentRoute: String,
    onHome: () -> Unit,
    onTrips: () -> Unit,
    onAlerts: () -> Unit,
    onAddJourney: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (currentRoute == "trips") {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.BottomEnd
            ) {
                Surface(
                    onClick = onAddJourney,
                    modifier = Modifier.padding(end = 16.dp, bottom = 16.dp),
                    color = BottomBlue,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 6.dp
                ) {
                    Box(
                        modifier = Modifier.size(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Ajouter un trajet",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        Surface(
            color = BottomPanel,
            border = androidx.compose.foundation.BorderStroke(1.dp, BottomBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.height(80.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomTab(
                    label = "Accueil",
                    selected = currentRoute == "home",
                    onClick = onHome
                )
                BottomTab(
                    label = "Trajets",
                    selected = currentRoute == "trips",
                    onClick = onTrips,
                    highlightSelected = true,
                    customSelectedGlyph = true
                )
                BottomTab(
                    label = "Alertes",
                    selected = currentRoute == "alerts",
                    onClick = onAlerts
                )
            }
        }
    }
}

@Composable
private fun BottomTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    highlightSelected: Boolean = false,
    customSelectedGlyph: Boolean = false
) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(vertical = 10.dp)
            .size(width = 118.dp, height = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (selected && highlightSelected) {
            Surface(
                color = Color(0xFF1E3A8A).copy(alpha = 0.35f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(width = 64.dp, height = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (customSelectedGlyph) {
                        RouteGlyph()
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Home,
                            contentDescription = label,
                            tint = BottomBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        } else {
            Icon(
                imageVector = when (label) {
                    "Accueil" -> Icons.Filled.Home
                    "Alertes" -> Icons.Filled.Notifications
                    else -> Icons.Filled.Home
                },
                contentDescription = label,
                tint = if (selected) BottomTextActive else BottomText,
                modifier = Modifier.size(24.dp)
            )
        }

        Text(
            text = label,
            color = if (selected) BottomTextActive else BottomText,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun RouteGlyph() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(BottomBlue, CircleShape)
        )
        Box(
            modifier = Modifier
                .width(14.dp)
                .height(2.dp)
                .background(BottomBlue, RoundedCornerShape(1.dp))
        )
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(BottomBlue, CircleShape)
        )
    }
}
