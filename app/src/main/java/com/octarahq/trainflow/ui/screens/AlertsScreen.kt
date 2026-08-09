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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
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

private data class AlertItem(
    val title: String,
    val description: String,
    val time: String,
    val levelLabel: String,
    val levelColor: Color,
    val badgeColor: Color,
    val iconTint: Color
)

@Composable
fun AlertsScreen() {
    val alerts = listOf(
        AlertItem(
            title = "Retard sur TGV 6231",
            description = "Le départ de Paris Gare de Lyon est décalé de 8 minutes.",
            time = "Il y a 4 min",
            levelLabel = "Impact moyen",
            levelColor = AlertsPalette.amber,
            badgeColor = Color(0xFF451A03),
            iconTint = AlertsPalette.amber
        ),
        AlertItem(
            title = "Voie H confirmée à Lyon Part-Dieu",
            description = "Les contrôles ont validé la voie prévue pour le prochain arrêt.",
            time = "Il y a 12 min",
            levelLabel = "Info réseau",
            levelColor = AlertsPalette.blue,
            badgeColor = Color(0xFF172554),
            iconTint = AlertsPalette.blue
        ),
        AlertItem(
            title = "TER 86509 à l'heure",
            description = "Aucun incident détecté entre Bordeaux Saint-Jean et Toulouse.",
            time = "Il y a 22 min",
            levelLabel = "Stable",
            levelColor = AlertsPalette.green,
            badgeColor = Color(0xFF064E3B),
            iconTint = AlertsPalette.green
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
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(bottom = 170.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AlertsHeader()
            CriticalAlertCard()
            AlertsSummaryRow()
            Text(
                text = "Dernières alertes",
                color = AlertsPalette.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            alerts.forEach { alert ->
                AlertCard(alert = alert)
            }
        }
    }
}

@Composable
private fun AlertsHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Alertes",
                color = AlertsPalette.textPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Surveille le réseau en temps réel",
                color = AlertsPalette.textSecondary,
                fontSize = 13.sp
            )
        }

        Surface(
            color = AlertsPalette.surface,
            shape = CircleShape,
            border = androidx.compose.foundation.BorderStroke(1.dp, AlertsPalette.border)
        ) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = null,
                    tint = AlertsPalette.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun CriticalAlertCard() {
    Surface(
        color = AlertsPalette.panel,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AlertsPalette.border)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(color = Color(0xFF7F1D1D), shape = RoundedCornerShape(999.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFCA5A5),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Critique",
                            color = Color(0xFFFCA5A5),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = "Incident réseau majeur",
                    color = AlertsPalette.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Signalement actif sur la ligne Paris-Lyon. Les horaires peuvent évoluer dans les prochains minutes.",
                color = AlertsPalette.textSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AlertsPalette.border)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TGV 6231",
                    color = AlertsPalette.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Voir le train",
                    color = AlertsPalette.blue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AlertsSummaryRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        SummaryPill(title = "Actives", value = "3", color = AlertsPalette.red, modifier = Modifier.weight(1f))
        SummaryPill(title = "Info", value = "5", color = AlertsPalette.blue, modifier = Modifier.weight(1f))
        SummaryPill(title = "Stables", value = "12", color = AlertsPalette.green, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SummaryPill(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        color = AlertsPalette.surface,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AlertsPalette.border),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = title, color = AlertsPalette.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(text = value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AlertCard(alert: AlertItem) {
    Surface(
        color = AlertsPalette.panel,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AlertsPalette.border)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(color = AlertsPalette.surface, shape = RoundedCornerShape(12.dp)) {
                Box(
                    modifier = Modifier.size(42.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = null,
                        tint = alert.iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = alert.title,
                    color = AlertsPalette.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = alert.description,
                    color = AlertsPalette.textSecondary,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = alert.time,
                    color = AlertsPalette.textSecondary,
                    fontSize = 11.sp
                )
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(color = alert.badgeColor, shape = RoundedCornerShape(20.dp)) {
                    Text(
                        text = alert.levelLabel,
                        color = alert.levelColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = AlertsPalette.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
