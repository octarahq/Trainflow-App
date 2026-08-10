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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.delay
import com.octarahq.trainflow.ApiClient
import com.octarahq.trainflow.GareSearchResult
import com.octarahq.trainflow.TrainSearchResult
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
    var searchQuery by remember { mutableStateOf("") }
    var gares by remember { mutableStateOf<List<GareSearchResult>>(emptyList()) }
    var trains by remember { mutableStateOf<List<TrainSearchResult>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(searchQuery, selectedTab) {
        if (searchQuery.isBlank()) {
            gares = emptyList()
            trains = emptyList()
            return@LaunchedEffect
        }
        delay(300)
        isLoading = true
        try {
            val resultParam = if (selectedTab == 0) "gare" else "train"
            val response = ApiClient.apiService.search(searchQuery, resultParam)
            gares = response.gares ?: emptyList()
            trains = response.trains ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    val uiStations = gares.map { 
        StationResult(it.obj.name, it.obj.uic ?: "")
    }

    val uiTrains = trains.map { 
        val status = if (it.obj.delayMinutes == 0) "À l'heure" else "+${it.obj.delayMinutes} min"
        val statusColor = if (it.obj.delayMinutes == 0) SearchPalette.green else SearchPalette.amber
        val badgeColor = when {
            it.obj.type.contains("suburban") -> SearchPalette.blue
            it.obj.type.contains("regional") -> SearchPalette.teal
            it.obj.type.contains("highspeedrail") -> SearchPalette.purple
            else -> SearchPalette.textSecondary
        }
        val typeLabel = when {
            it.obj.type.contains("suburban") -> "RER"
            it.obj.type.contains("regional") -> "TER"
            it.obj.type.contains("highspeedrail") -> "TGV"
            else -> it.obj.type
        }

        TrainResult(
            badge = typeLabel,
            number = it.obj.name,
            route = "${it.obj.origin} → ${it.obj.destination}",
            status = status,
            statusColor = statusColor,
            badgeColor = badgeColor
        )
    }

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
            SearchTopBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onClear = { searchQuery = "" },
                onBack = onBack
            )
            SearchTabs(
                garesCount = uiStations.size,
                trainsCount = uiTrains.size,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SearchPalette.blue)
                }
            } else if (searchQuery.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 64.dp), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = SearchPalette.textSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Rechercher une gare ou un train",
                            color = SearchPalette.textSecondary,
                            fontSize = 16.sp
                        )
                    }
                }
            } else if (uiStations.isEmpty() && uiTrains.isEmpty() && searchQuery.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Aucun résultat", color = SearchPalette.textSecondary)
                }
            } else {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (selectedTab == 0) {
                        uiStations.forEach { station ->
                            StationResultCard(
                                station = station,
                                onClick = onOpenTrainInfo
                            )
                        }

                        if (uiStations.isNotEmpty() && uiTrains.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        if (uiTrains.isNotEmpty()) {
                            val stationName = uiStations.firstOrNull()?.name
                            val headerText = if (stationName != null) {
                                "${uiTrains.size} Trains passant par $stationName"
                            } else {
                                "${uiTrains.size} Trains"
                            }
                            Text(
                                text = headerText,
                                color = SearchPalette.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )

                            uiTrains.forEach { train ->
                                TrainResultCard(
                                    train = train,
                                    onClick = onOpenTrainInfo
                                )
                            }
                        }
                    } else {
                        if (uiTrains.isNotEmpty()) {
                            Text(
                                text = "${uiTrains.size} Trains",
                                color = SearchPalette.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
    
                            uiTrains.forEach { train ->
                                TrainResultCard(
                                    train = train,
                                    onClick = onOpenTrainInfo
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit
) {
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
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        textStyle = TextStyle(
                            color = SearchPalette.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (query.isEmpty()) {
                                Text(
                                    text = "Rechercher...",
                                    color = SearchPalette.textSecondary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (query.isNotEmpty()) {
                        IconButton(onClick = onClear, modifier = Modifier.size(18.dp)) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Effacer",
                                tint = SearchPalette.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
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
private fun SearchTabs(
    garesCount: Int,
    trainsCount: Int,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SearchTab(
            selected = selectedTab == 0,
            label = "Gares",
            onClick = { onTabSelected(0) }
        ) { color ->
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        }
        SearchTab(
            selected = selectedTab == 1,
            label = "Trains",
            onClick = { onTabSelected(1) }
        ) { color ->
            Icon(Icons.Filled.List, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun SearchTab(selected: Boolean, label: String, onClick: () -> Unit, glyph: @Composable (Color) -> Unit) {
    Surface(
        onClick = onClick,
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
            glyph(if (selected) SearchPalette.blue else SearchPalette.textSecondary)
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
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = SearchPalette.blue,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    color = SearchPalette.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = station.details,
                    color = SearchPalette.textSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = train.route,
                    color = SearchPalette.textSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
