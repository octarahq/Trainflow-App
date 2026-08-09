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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private object SearchPalette {
    val background = Color(0xFF0F1115)
    val panel = Color(0xFF1D2026)
    val surface = Color(0xFF262B35)
    val border = Color(0xFF2A303C)
    val textPrimary = Color(0xFFF1F5F9)
    val textSecondary = Color(0xFF94A3B8)
    val blue = Color(0xFF3B82F6)
    val purple = Color(0xFF7C3AED)
    val teal = Color(0xFF0D9488)
    val amber = Color(0xFFFBBF24)
    val green = Color(0xFF22C55E)
}

private data class StationResult(
    val name: String,
    val details: String
)

private data class TrainResult(
    val badge: String,
    val number: String,
    val route: String,
    val status: String,
    val statusColor: Color,
    val badgeColor: Color,
    val onClick: Boolean = true
)

@Composable
fun SearchScreen(
    onBack: () -> Unit = {},
    onOpenTrainInfo: () -> Unit = {}
) {
    val stations = listOf(
        StationResult("Lyon Part-Dieu", "12 lignes (TGV, TER, Intercités)"),
        StationResult("Lyon Perrache", "8 lignes (TGV, TER)"),
        StationResult("Lyon Saint-Exupéry TGV", "6 lignes (TGV, Rer)"),
        StationResult("Lyon Jean Macé", "4 lignes (TER, Métro B)"),
        StationResult("Lyon Vaise", "3 lignes (TER, Métro D)")
    )

    val trains = listOf(
        TrainResult("TGV", "6231", "Paris Gare de Lyon → Marseille", "À l'heure", SearchPalette.green, SearchPalette.purple),
        TrainResult("TER", "86509", "Bordeaux St-Jean → Lyon Part-Dieu", "À l'heure", SearchPalette.green, SearchPalette.teal),
        TrainResult("TGV", "6604", "Lyon Part-Dieu → Paris GDL", "+12 min", SearchPalette.amber, SearchPalette.purple)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SearchPalette.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 170.dp)
        ) {
            SearchTopBar(onBack = onBack)
            SearchTabs()

            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                stations.forEach { station ->
                    StationResultCard(
                        station = station,
                        onClick = onOpenTrainInfo
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Trains passant par Lyon",
                    color = SearchPalette.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )

                trains.forEach { train ->
                    TrainResultCard(
                        train = train,
                        onClick = onOpenTrainInfo
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchTopBar(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Retour",
                    tint = SearchPalette.textPrimary
                )
            }

            Surface(
                color = SearchPalette.surface,
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SearchPalette.border),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Lyon",
                        color = SearchPalette.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Effacer",
                        tint = SearchPalette.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(SearchPalette.border)
        )
    }
}

@Composable
private fun SearchTabs() {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SearchTab(selected = true, label = "Gares (5)") { StationTabGlyph() }
        SearchTab(selected = false, label = "Trains") { TrainTabGlyph() }
    }
}

@Composable
private fun SearchTab(selected: Boolean, label: String, glyph: @Composable () -> Unit) {
    Surface(
        color = if (selected) SearchPalette.panel else SearchPalette.surface,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) SearchPalette.blue else SearchPalette.border
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            glyph()
            Text(
                text = label,
                color = if (selected) SearchPalette.textPrimary else SearchPalette.textSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StationTabGlyph() {
    Box(
        modifier = Modifier
            .size(16.dp)
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(14.dp)) {
            drawLine(Color(0xFF3B82F6), Offset(size.width * 0.2f, size.height * 0.15f), Offset(size.width * 0.2f, size.height * 0.9f), strokeWidth = 1.6f)
            drawLine(Color(0xFF3B82F6), Offset(size.width * 0.5f, size.height * 0.15f), Offset(size.width * 0.5f, size.height * 0.9f), strokeWidth = 1.6f)
            drawLine(Color(0xFF3B82F6), Offset(size.width * 0.8f, size.height * 0.3f), Offset(size.width * 0.8f, size.height * 0.9f), strokeWidth = 1.6f)
        }
    }
}

@Composable
private fun TrainTabGlyph() {
    Box(
        modifier = Modifier
            .size(16.dp)
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(14.dp)) {
            drawCircle(Color(0xFF94A3B8), radius = size.minDimension * 0.18f, center = Offset(size.width * 0.5f, size.height * 0.42f), style = Stroke(width = 1.4f))
            drawLine(Color(0xFF94A3B8), Offset(size.width * 0.32f, size.height * 0.76f), Offset(size.width * 0.68f, size.height * 0.76f), strokeWidth = 1.4f)
            drawLine(Color(0xFF94A3B8), Offset(size.width * 0.42f, size.height * 0.76f), Offset(size.width * 0.38f, size.height * 0.94f), strokeWidth = 1.4f)
            drawLine(Color(0xFF94A3B8), Offset(size.width * 0.58f, size.height * 0.76f), Offset(size.width * 0.62f, size.height * 0.94f), strokeWidth = 1.4f)
        }
    }
}

@Composable
private fun StationResultCard(station: StationResult, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = SearchPalette.panel,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SearchPalette.border)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = SearchPalette.surface,
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    StationGlyph()
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    color = SearchPalette.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = station.details,
                    color = SearchPalette.textSecondary,
                    fontSize = 13.sp
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = SearchPalette.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun StationGlyph() {
    Canvas(modifier = Modifier.size(20.dp)) {
        drawRoundRect(
            color = SearchPalette.blue,
            topLeft = Offset(size.width * 0.32f, size.height * 0.14f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.36f, size.height * 0.72f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f),
            style = Stroke(width = 1.8f)
        )
        drawLine(SearchPalette.blue, Offset(size.width * 0.22f, size.height * 0.15f), Offset(size.width * 0.22f, size.height * 0.85f), strokeWidth = 1.8f)
        drawLine(SearchPalette.blue, Offset(size.width * 0.78f, size.height * 0.15f), Offset(size.width * 0.78f, size.height * 0.85f), strokeWidth = 1.8f)
        drawCircle(SearchPalette.blue, radius = 1.7f, center = Offset(size.width * 0.5f, size.height * 0.5f))
    }
}

@Composable
private fun TrainResultCard(train: TrainResult, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = SearchPalette.panel,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SearchPalette.border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(color = train.badgeColor, shape = RoundedCornerShape(6.dp)) {
                Text(
                    text = train.badge,
                    color = Color.White.copy(alpha = 0.93f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${train.badge} ${train.number}",
                    color = SearchPalette.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = train.route,
                    color = SearchPalette.textSecondary,
                    fontSize = 13.sp
                )
            }

            Surface(
                color = if (train.statusColor == SearchPalette.green) Color(0xFF064E3B) else Color(0xFF451A03),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = train.status,
                    color = train.statusColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}
