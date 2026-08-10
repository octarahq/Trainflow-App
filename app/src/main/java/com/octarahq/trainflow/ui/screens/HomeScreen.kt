package com.octarahq.trainflow.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.octarahq.trainflow.ApiClient
import com.octarahq.trainflow.NetworkStatusStats
import com.octarahq.trainflow.ui.utils.getTrainCategoryDisplay
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
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.times

private object TrainflowPalette {
    val background = Color(0xFF0F1115)
    val panel = Color(0xFF1D2026)
    val surface = Color(0xFF262B35)
    val border = Color(0xFF2A303C)
    val textPrimary = Color(0xFFF1F5F9)
    val textSecondary = Color(0xFF94A3B8)
    val activeGreen = Color(0xFF4ADE80)
    val punctualGreen = Color(0xFF22C55E)
    val delayAmber = Color(0xFFFBBF24)
    val blue = Color(0xFF3B82F6)
    val teal = Color(0xFF0D9488)
    val pink = Color(0xFFDB2777)
    val purple = Color(0xFF7C3AED)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenMenu: () -> Unit = {},
    onOpenSearch: () -> Unit = {}
) {
    var networkStatus by remember { mutableStateOf<NetworkStatusStats?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            try {
                val response = ApiClient.apiService.getNetworkStatus(showDelaysTrains = 0)
                networkStatus = response.stats
            } catch (e: Exception) {
                e.printStackTrace()
            }
            kotlinx.coroutines.delay(120_000)
        }
    }

    BottomSheetScaffold(
        scaffoldState = androidx.compose.material3.rememberBottomSheetScaffoldState(
            bottomSheetState = androidx.compose.material3.rememberStandardBottomSheetState(
                initialValue = androidx.compose.material3.SheetValue.Expanded
            )
        ),
        sheetPeekHeight = 96.dp,
        sheetSwipeEnabled = true,
        sheetContainerColor = TrainflowPalette.panel,
        sheetShadowElevation = 12.dp,
        sheetContent = {
            HomeBottomSheet(onOpenSearch = onOpenSearch, stats = networkStatus)
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TrainflowPalette.background)
        ) {
            TrainflowMapBackground(modifier = Modifier.fillMaxSize())

            HomeTopControls(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                onOpenMenu = onOpenMenu
            )
        }
    }
}

@Composable
private fun TrainflowMapBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        listOf(0.0f, 0.1367f, 0.2735f, 0.4103f, 0.5471f, 0.6839f, 0.8206f).forEach { fraction ->
            drawLine(
                color = Color(0xFF1B2331).copy(alpha = 0.48f),
                start = Offset(0f, height * fraction),
                end = Offset(width, height * fraction),
                strokeWidth = 1.2f
            )
        }

        listOf(0.1990f, 0.3980f, 0.5970f, 0.7960f).forEach { fraction ->
            drawLine(
                color = Color(0xFF1B2331).copy(alpha = 0.48f),
                start = Offset(width * fraction, 0f),
                end = Offset(width * fraction, height),
                strokeWidth = 1.2f
            )
        }

        drawLine(
            color = TrainflowPalette.blue,
            start = Offset(width * 0.099f, height * 0.171f),
            end = Offset(width * 0.973f, height * 0.574f),
            strokeWidth = 2.6f
        )
        drawCircle(
            color = TrainflowPalette.blue,
            radius = width * 0.015f,
            center = Offset(width * 0.100f, height * 0.170f),
            style = Stroke(width = 2.4f)
        )
        drawCircle(
            color = TrainflowPalette.activeGreen,
            radius = width * 0.015f,
            center = Offset(width * 0.375f, height * 0.306f)
        )
        drawCircle(
            color = Color(0xFF10B981),
            radius = width * 0.010f,
            center = Offset(width * 0.627f, height * 0.414f),
            style = Stroke(width = 2.2f)
        )
        drawCircle(
            color = TrainflowPalette.blue,
            radius = width * 0.016f,
            center = Offset(width * 0.903f, height * 0.553f),
            style = Stroke(width = 2.4f)
        )

        drawLine(
            color = Color(0xFF0E7D57),
            start = Offset(width * 0.024f, height * 0.682f),
            end = Offset(width * 0.493f, height * 0.114f),
            strokeWidth = 2.6f
        )
        drawLine(
            color = Color(0xFFB7791F),
            start = Offset(width * 0.000f, height * 0.684f),
            end = Offset(width * 0.927f, height * 0.573f),
            strokeWidth = 2.6f
        )

        drawCircle(
            color = Color(0xFFFBBF24),
            radius = width * 0.018f,
            center = Offset(width * 0.586f, height * 0.512f),
            style = Stroke(width = 2.6f)
        )
        drawCircle(
            color = Color(0xFF0B8F6A),
            radius = width * 0.013f,
            center = Offset(width * 0.02f, height * 0.684f)
        )
        drawCircle(
            color = Color(0xFF34D399),
            radius = width * 0.022f,
            center = Offset(width * 0.375f, height * 0.304f)
        )
    }
}

@Composable
private fun HomeTopControls(
    modifier: Modifier = Modifier,
    onOpenMenu: () -> Unit
) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FloatingControlButton(onClick = onOpenMenu) {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = "Menu",
                tint = TrainflowPalette.textPrimary,
                modifier = Modifier.size(24.dp)
            )
        }

        FloatingControlButton(onClick = {}) {
            Box(modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = "Notifications",
                    tint = TrainflowPalette.textPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(8.dp)
                        .background(Color(0xFFFBBF24), CircleShape)
                )
            }
        }
    }
}

@Composable
private fun FloatingControlButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = TrainflowPalette.panel,
        border = BorderStroke(1.dp, TrainflowPalette.border),
        shadowElevation = 0.dp,
        modifier = Modifier.size(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Composable
private fun HomeBottomSheet(
    onOpenSearch: () -> Unit,
    stats: NetworkStatusStats?
) {
    val actifs = stats?.inTransit?.toString() ?: "--"
    val ponctuels = stats?.punctuality?.let { "$it%" } ?: "--%"
    val retards = stats?.punctuality?.let { "${100 - it}%" } ?: "--%"

    Surface(
        color = TrainflowPalette.panel,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                onClick = onOpenSearch,
                modifier = Modifier.fillMaxWidth(),
                color = TrainflowPalette.surface,
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, TrainflowPalette.border)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = TrainflowPalette.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Rechercher une gare, un train...",
                        color = TrainflowPalette.textSecondary,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Réseau en direct",
                    color = TrainflowPalette.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Actifs",
                        value = actifs,
                        valueColor = TrainflowPalette.textPrimary,
                        leading = {
                            TrainFrontIcon(
                                tint = TrainflowPalette.blue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Ponctuels",
                        value = ponctuels,
                        valueColor = TrainflowPalette.activeGreen,
                        leading = {
                            StatusDot(
                                color = TrainflowPalette.punctualGreen,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Retards",
                        value = retards,
                        valueColor = TrainflowPalette.delayAmber,
                        leading = {
                            StatusDot(
                                color = TrainflowPalette.delayAmber,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val counts = stats?.trainCounts?.toList()?.sortedByDescending { it.second } ?: emptyList()
                    val chunks = counts.chunked(3)
                    
                    chunks.forEach { chunk ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            chunk.forEach { (key, count) ->
                                val display = getTrainCategoryDisplay(key)
                                BreakdownChip(label = display.label, value = count.toString(), badgeColor = display.color)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    valueColor: Color,
    leading: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        color = TrainflowPalette.surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, TrainflowPalette.border)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title.uppercase(),
                color = TrainflowPalette.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                leading()
                Text(
                    text = value,
                    color = valueColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun BreakdownChip(
    label: String,
    value: String,
    badgeColor: Color
) {
    Surface(
        color = TrainflowPalette.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, TrainflowPalette.border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = badgeColor,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = label,
                    color = Color.White.copy(alpha = 0.93f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Text(
                text = value,
                color = TrainflowPalette.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun StatusDot(
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(color, CircleShape)
    )
}

@Composable
private fun TrainFrontIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val stroke = 1.8f
        val bodyWidth = size.width * 0.68f
        val bodyHeight = size.height * 0.72f
        val left = (size.width - bodyWidth) / 2f
        val top = (size.height - bodyHeight) / 2f

        drawRoundRect(
            color = tint,
            topLeft = Offset(left, top),
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = CornerRadius(size.width * 0.18f, size.width * 0.18f),
            style = Stroke(width = stroke)
        )
        drawLine(
            color = tint,
            start = Offset(size.width * 0.30f, size.height * 0.42f),
            end = Offset(size.width * 0.70f, size.height * 0.42f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = tint,
            radius = size.minDimension * 0.08f,
            center = Offset(size.width * 0.34f, size.height * 0.56f)
        )
        drawCircle(
            color = tint,
            radius = size.minDimension * 0.08f,
            center = Offset(size.width * 0.66f, size.height * 0.56f)
        )
    }
}


