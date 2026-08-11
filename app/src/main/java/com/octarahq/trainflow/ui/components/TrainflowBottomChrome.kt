package com.octarahq.trainflow.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    dropdownOpen: Boolean,
    onToggleDropdown: () -> Unit,
    onHome: () -> Unit,
    onTrips: () -> Unit,
    onAlerts: () -> Unit,
    onAddJourney: () -> Unit,
    onImportTicket: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (currentRoute == "trips") {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.BottomEnd
            ) {
                Column(
                    modifier = Modifier.padding(end = 16.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    AnimatedVisibility(
                        visible = dropdownOpen,
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = tween(220)
                        ) + fadeIn(animationSpec = tween(180)),
                        exit = slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = tween(180)
                        ) + fadeOut(animationSpec = tween(140))
                    ) {
                        Surface(
                            color = Color(0xFF1D2026),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BottomBorder),
                            shadowElevation = 12.dp
                        ) {
                            Column(
                                modifier = Modifier.width(220.dp),
                                verticalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                DropdownItem(
                                    icon = Icons.Filled.Notifications,
                                    label = "Importer un billet",
                                    sublabel = "Image ou PDF",
                                    onClick = {
                                        onToggleDropdown()
                                        onImportTicket()
                                    }
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = onToggleDropdown,
                        color = BottomBlue,
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 6.dp
                    ) {
                        Box(
                            modifier = Modifier.size(56.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (dropdownOpen) Icons.Filled.Close else Icons.Filled.Add,
                                contentDescription = if (dropdownOpen) "Fermer" else "Ajouter un trajet",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(24.dp)
                                    .rotate(if (dropdownOpen) 90f else 0f)
                            )
                        }
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
private fun DropdownItem(
    icon: ImageVector,
    label: String,
    sublabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            color = BottomBlue.copy(alpha = 0.15f),
            shape = RoundedCornerShape(10.dp)
        ) {
            Box(
                modifier = Modifier.size(38.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BottomBlue,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                color = BottomTextActive,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = sublabel,
                color = BottomText,
                fontSize = 11.sp
            )
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
