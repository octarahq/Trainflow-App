package com.octarahq.trainflow.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.octarahq.trainflow.network.apiService
import com.octarahq.trainflow.network.NetworkStatusStats
import com.octarahq.trainflow.ui.utils.getTrainCategoryDisplay
import kotlin.random.Random

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
    val id: String = Random.nextLong().toString(),
    val type: String,
    val typeColor: Color,
    val typeTextColor: Color,
    val number: String,
    val delay: String,
    val delayMinutes: Int,
    val delayColor: Color,
    val delayBgColor: Color,
    val origin: String,
    val destination: String,
)

private enum class SortOption(val label: String) {
    DELAY_DESC("Plus gros retard"),
    DELAY_ASC("Moins de retard"),
    NUMBER_ASC("Numéro de train"),
    ORIGIN_ASC("Gare de départ")
}

class AlertsScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var isSearchExpanded by remember { mutableStateOf(false) }
        var searchQuery by remember { mutableStateOf("") }
        var showSortMenu by remember { mutableStateOf(false) }
        var sortOption by remember { mutableStateOf(SortOption.DELAY_DESC) }

        var allDelayedTrains by remember { mutableStateOf<List<DelayedTrain>>(emptyList()) }
        var currentStats by remember { mutableStateOf<NetworkStatusStats?>(null) }
        
        var page by remember { mutableStateOf(1) }
        var isLoading by remember { mutableStateOf(false) }
        var isRefreshing by remember { mutableStateOf(false) }
        var hasMore by remember { mutableStateOf(true) }
        


        LaunchedEffect(page) {
            isLoading = true
            try {
                val networkStatus = apiService.getNetworkStatus(page = page, pageSize = 10)
                currentStats = networkStatus.stats
                
                val newTrains = networkStatus.delayedTrains.map { train ->
                    val display = getTrainCategoryDisplay(train.type)
                    DelayedTrain(
                        type = display.label,
                        typeColor = display.color,
                        typeTextColor = Color.White,
                        number = train.number,
                        delay = "+${train.delayMinutes} min",
                        delayMinutes = train.delayMinutes,
                        delayColor = AlertsPalette.amber,
                        delayBgColor = Color(0xFF451A03),
                        origin = train.origin,
                        destination = train.destination,
                    )
                }
                
                if (newTrains.isEmpty()) {
                    hasMore = false
                }

                if (page == 1) {
                    allDelayedTrains = newTrains
                } else {
                    allDelayedTrains = allDelayedTrains.plus(newTrains)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
                if (isRefreshing) {
                    isRefreshing = false
                }
            }
        }

        val filteredTrains = allDelayedTrains.filter { 
            it.number.contains(searchQuery, ignoreCase = true) ||
            it.type.contains(searchQuery, ignoreCase = true) ||
            it.origin.contains(searchQuery, ignoreCase = true) ||
            it.destination.contains(searchQuery, ignoreCase = true)
        }

        val sortedTrains = when (sortOption) {
            SortOption.DELAY_DESC -> filteredTrains.sortedByDescending { it.delayMinutes }
            SortOption.DELAY_ASC -> filteredTrains.sortedBy { it.delayMinutes }
            SortOption.NUMBER_ASC -> filteredTrains.sortedBy { it.number }
            SortOption.ORIGIN_ASC -> filteredTrains.sortedBy { it.origin }
        }

        val listContent: @Composable () -> Unit = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 24.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { navigator.pop() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Retour",
                                tint = AlertsPalette.textPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Alertes & Etat du réseau",
                            color = AlertsPalette.textPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                item { AlertsStatsRow(currentStats) }
                
                item {
                    if (isSearchExpanded) {
                        Surface(
                            color = AlertsPalette.surface,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, AlertsPalette.border),
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
                                            Text("Rechercher (numéro, gare, type)...", color = AlertsPalette.textSecondary, fontSize = 16.sp)
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
                                text = "${currentStats?.delays ?: 0} Trains en retard",
                                color = AlertsPalette.textPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box {
                                    IconButton(onClick = { showSortMenu = true }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.List,
                                            contentDescription = "Trier",
                                            tint = AlertsPalette.textSecondary
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showSortMenu,
                                        onDismissRequest = { showSortMenu = false },
                                        modifier = Modifier.background(AlertsPalette.panel)
                                    ) {
                                        SortOption.values().forEach { option ->
                                            DropdownMenuItem(
                                                text = { Text(option.label, color = if (option == sortOption) AlertsPalette.blue else AlertsPalette.textPrimary) },
                                                onClick = {
                                                    sortOption = option
                                                    showSortMenu = false
                                                }
                                            )
                                        }
                                    }
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
                }

                items(
                    count = sortedTrains.size,
                    key = { sortedTrains[it].id }
                ) { index ->
                    DelayedTrainCard(train = sortedTrains[index])
                }
                
                if (isLoading && page > 1) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AlertsPalette.blue, modifier = Modifier.size(24.dp))
                        }
                    }
                } else if (!isLoading && hasMore && sortedTrains.isNotEmpty() && searchQuery.isEmpty()) {
                    item {
                        LaunchedEffect(Unit) {
                            page++
                        }
                    }
                }
            }
        }

        if (com.octarahq.trainflow.ui.utils.isPullToRefreshSupported()) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    page = 1
                    hasMore = true
                    isRefreshing = true
                },
                modifier = Modifier
                    .fillMaxSize()
                    .background(AlertsPalette.background)
            ) {
                listContent()
            }
        } else {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AlertsPalette.background)
            ) {
                listContent()
            }
        }
    }
}

@Composable
private fun AlertsStatsRow(stats: NetworkStatusStats?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatBox(
            title = "PONCTUALITÉ",
            value = "${stats?.punctuality ?: 0}%",
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
            value = "${stats?.incidents ?: 0}",
            valueColor = AlertsPalette.amber,
            modifier = Modifier.weight(1f)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val incidents = stats?.incidents ?: 0
                val delays = stats?.delays ?: 0
                
                val redBars = kotlin.math.ceil(incidents / 50.0).toInt()
                val yellowBars = kotlin.math.ceil(delays / 200.0).toInt()
                
                var currentBars = 0
                
                for (i in 0 until redBars) {
                    if (currentBars >= 5) break
                    Box(modifier = Modifier.height(4.dp).weight(1f).background(AlertsPalette.red, RoundedCornerShape(2.dp)))
                    currentBars++
                }
                
                for (i in 0 until yellowBars) {
                    if (currentBars >= 5) break
                    Box(modifier = Modifier.height(4.dp).weight(1f).background(AlertsPalette.amber, RoundedCornerShape(2.dp)))
                    currentBars++
                }
                
                while (currentBars < 5) {
                    Box(modifier = Modifier.height(4.dp).weight(1f).background(AlertsPalette.surface, RoundedCornerShape(2.dp)))
                    currentBars++
                }
            }
        }
        StatBox(
            title = "TRAINS ACTIFS",
            value = "${stats?.inTransit ?: 0}",
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
        border = BorderStroke(1.dp, AlertsPalette.border),
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
        border = BorderStroke(1.dp, AlertsPalette.border)
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
                    modifier = Modifier.weight(1f),
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
                        text = train.number.ifBlank { train.destination.ifBlank { "Inconnue" } },
                        color = AlertsPalette.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
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
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
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
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }
    }
}
