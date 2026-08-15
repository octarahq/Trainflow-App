package com.octarahq.trainflow.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import com.octarahq.trainflow.InterpolatedJourney
import com.octarahq.trainflow.ui.components.TrainflowMap
import com.octarahq.trainflow.ui.utils.JourneyRepository
import com.octarahq.trainflow.ui.utils.getTrainCategoryDisplay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.input.pointer.pointerInput
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
    selectTrainId: String? = null,
    targetLat: Double? = null,
    targetLon: Double? = null,
    onOpenMenu: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenTrainInfo: (String, Int?) -> Unit = { _, _ -> },
    onOpenNetworkStatus: () -> Unit = {}
) {
    var networkStatus by remember { mutableStateOf<NetworkStatusStats?>(null) }
    var liveVehicles by remember { mutableStateOf<List<InterpolatedJourney>>(emptyList()) }
    var selectedTrain by remember { mutableStateOf<InterpolatedJourney?>(null) }
    val trainTrackingData = remember { androidx.compose.runtime.mutableStateMapOf<String, com.octarahq.trainflow.ui.utils.TrainTrackingData>() }
    val scope = rememberCoroutineScope()
    var interactionJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var selectedCategoryLabels by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(liveVehicles, selectTrainId) {
        if (selectTrainId != null && liveVehicles.isNotEmpty()) {
            val matched = liveVehicles.firstOrNull { train ->
                val smartId = train.journey.FramedVehicleJourneyRef?.DatedVehicleJourneyRef 
                    ?: train.journey.TrainNumbers?.TrainNumberRef 
                    ?: train.journey.VehicleJourneyRef
                smartId == selectTrainId || train.journey.TrainNumbers?.TrainNumberRef == selectTrainId
            }
            if (matched != null) {
                selectedTrain = matched
            }
        }
    }

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

    LaunchedEffect(Unit) {
        while (true) {
            try {
                val response = ApiClient.apiService.getLiveVehicles()
                val currentTime = System.currentTimeMillis()
                response.vehicles.forEach { train ->
                    val smartId = train.journey.FramedVehicleJourneyRef?.DatedVehicleJourneyRef 
                        ?: train.journey.TrainNumbers?.TrainNumberRef 
                        ?: train.journey.VehicleJourneyRef
                        ?: return@forEach
                    
                    val oldData = trainTrackingData[smartId]
                    val newData = com.octarahq.trainflow.ui.utils.calculateTrackingData(oldData, train.lat, train.lon, currentTime)
                    if (newData != null) {
                        trainTrackingData[smartId] = newData
                    }
                }
                liveVehicles = response.vehicles
            } catch (e: Exception) {
                e.printStackTrace()
            }
            kotlinx.coroutines.delay(10_000)
        }
    }

    val scaffoldState = androidx.compose.material3.rememberBottomSheetScaffoldState(
        bottomSheetState = androidx.compose.material3.rememberStandardBottomSheetState(
            initialValue = androidx.compose.material3.SheetValue.Expanded,
            skipHiddenState = false
        )
    )

    var isCameraLocked by remember { mutableStateOf(false) }
    var showOnlySelected by remember { mutableStateOf(false) }

    LaunchedEffect(selectedTrain) {
        if (selectedTrain != null) {
            isCameraLocked = true
        } else {
            isCameraLocked = false
            showOnlySelected = false
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 96.dp,
        sheetSwipeEnabled = true,
        sheetContainerColor = TrainflowPalette.panel,
        sheetShadowElevation = 12.dp,
        sheetContent = {
            HomeBottomSheet(
                onOpenSearch = onOpenSearch,
                stats = networkStatus,
                selectedCategoryLabels = selectedCategoryLabels,
                onCategoryToggle = { label ->
                    selectedCategoryLabels = when {
                        selectedCategoryLabels.size == 1 && selectedCategoryLabels.contains(label) -> emptySet()
                        selectedCategoryLabels.contains(label) -> selectedCategoryLabels - label
                        else -> if (selectedCategoryLabels.isEmpty()) setOf(label) else selectedCategoryLabels + label
                    }
                }
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TrainflowPalette.background)
        ) {
            val filteredVehicles = when {
                showOnlySelected && selectedTrain != null ->
                    liveVehicles.filter {
                        it.journey.VehicleJourneyRef == selectedTrain?.journey?.VehicleJourneyRef ||
                        it.journey.FramedVehicleJourneyRef?.DatedVehicleJourneyRef == selectedTrain?.journey?.FramedVehicleJourneyRef?.DatedVehicleJourneyRef
                    }
                selectedCategoryLabels.isNotEmpty() ->
                    liveVehicles.filter {
                        val ref = it.journey.ProductCategoryRef.ifBlank { it.journey.VehicleMode }
                        val label = getTrainCategoryDisplay(ref.lowercase()).label
                        selectedCategoryLabels.contains(label)
                    }
                else -> liveVehicles
            }
            TrainflowMap(
                trains = filteredVehicles,
                trackingData = trainTrackingData,
                selectedTrain = selectedTrain,
                isCameraLocked = isCameraLocked,
                onCameraLockChange = { isCameraLocked = it },
                onMapInteract = {
                    if (selectedTrain == null) {
                        interactionJob?.cancel()
                        scope.launch {
                            if (scaffoldState.bottomSheetState.currentValue != androidx.compose.material3.SheetValue.Hidden) {
                                scaffoldState.bottomSheetState.hide()
                            }
                        }
                        interactionJob = scope.launch {
                            kotlinx.coroutines.delay(1000)
                            scaffoldState.bottomSheetState.partialExpand()
                        }
                    }
                },
                onTrainClick = { train ->
                    selectedTrain = train
                    interactionJob?.cancel()
                    scope.launch {
                        scaffoldState.bottomSheetState.hide()
                    }
                },
                targetLat = targetLat,
                targetLon = targetLon
            )

            HomeTopControls(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 12.dp),
                isBack = selectedTrain != null,
                onOpenMenu = {
                    if (selectedTrain != null) {
                        selectedTrain = null
                        scope.launch { scaffoldState.bottomSheetState.partialExpand() }
                    } else {
                        onOpenMenu()
                    }
                },
                onOpenNetworkStatus = onOpenNetworkStatus
            )

            if (selectedTrain != null) {
                androidx.activity.compose.BackHandler {
                    selectedTrain = null
                    scope.launch { scaffoldState.bottomSheetState.partialExpand() }
                }
            }
            
            var lastTrain by remember { mutableStateOf<InterpolatedJourney?>(null) }
            LaunchedEffect(selectedTrain) {
                if (selectedTrain != null) lastTrain = selectedTrain
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = selectedTrain != null,
                    enter = androidx.compose.animation.slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = androidx.compose.animation.core.tween(300)
                    ) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300)),
                    exit = androidx.compose.animation.slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = androidx.compose.animation.core.tween(250)
                    ) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(250))
                ) {
                    lastTrain?.let { train ->
                        val smartId = train.journey.FramedVehicleJourneyRef?.DatedVehicleJourneyRef 
                            ?: train.journey.TrainNumbers?.TrainNumberRef 
                            ?: train.journey.VehicleJourneyRef
                        val speedKmh = train.speed ?: smartId?.let { trainTrackingData[it]?.speedKmh }
                        TrainDetailOverlay(
                            train = train,
                            speedKmh = speedKmh,
                            isCameraLocked = isCameraLocked,
                            showOnlySelected = showOnlySelected,
                            onCameraLockChange = { isCameraLocked = it },
                            onShowOnlySelectedChange = { showOnlySelected = it },
                            onOpenTrainInfo = {
                                if (smartId != null) {
                                    onOpenTrainInfo(smartId, speedKmh)
                                }
                            },
                            onClose = { selectedTrain = null },
                            modifier = Modifier
                                .padding(12.dp)
                                .navigationBarsPadding()
                                .padding(bottom = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TrainDetailOverlay(
    train: InterpolatedJourney, 
    speedKmh: Int? = null, 
    isCameraLocked: Boolean = false,
    showOnlySelected: Boolean = false,
    onCameraLockChange: (Boolean) -> Unit = {},
    onShowOnlySelectedChange: (Boolean) -> Unit = {},
    onOpenTrainInfo: () -> Unit = {},
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = TrainflowPalette.surface),
        border = BorderStroke(1.dp, TrainflowPalette.border),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                var dragY = 0f
                detectVerticalDragGestures(
                    onDragEnd = { dragY = 0f },
                    onDragCancel = { dragY = 0f },
                    onVerticalDrag = { change, dragAmount ->
                        dragY += dragAmount
                        if (dragY > 150f) {
                            onClose()
                            dragY = 0f
                        }
                    }
                )
            }
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color(0xFF4B5563), CircleShape)
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val category = getTrainCategoryDisplay(train.journey.ProductCategoryRef)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Box(
                        modifier = Modifier
                            .background(category.color, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = category.label.uppercase(),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = train.journey.PublishedLineName,
                        color = TrainflowPalette.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                val delayText = if ((train.delayMinutes ?: 0) > 0) "+${train.delayMinutes} min" else "À l'heure"
                val statusColor = if ((train.delayMinutes ?: 0) > 0) TrainflowPalette.delayAmber else TrainflowPalette.punctualGreen
                
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = delayText,
                        color = statusColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val nextStop = com.octarahq.trainflow.ui.utils.getNextStopInfo(train)
            val destInfo = com.octarahq.trainflow.ui.utils.getDestinationInfo(train)
            val stopName = nextStop?.name ?: train.journey.DestinationName
            val arrivalText = if (destInfo != null) {
                val voie = destInfo.platform?.let { " (voie $it)" } ?: ""
                "Arrivée prévue à ${destInfo.timeFormatted}$voie"
            } else {
                "Destination : ${train.journey.DestinationName}"
            }
            val stopTitle = if (nextStop != null) {
                "PROCHAIN ARRÊT (${maxOf(0, nextStop.minutes)} min)"
            } else {
                "PROCHAIN ARRÊT"
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("VITESSE", color = TrainflowPalette.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    val speedText = speedKmh?.let { "$it km/h" } ?: "En route"
                    Text(speedText, color = TrainflowPalette.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(stopTitle, color = TrainflowPalette.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(stopName, color = TrainflowPalette.blue, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = arrivalText,
                color = TrainflowPalette.textSecondary,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            val context = androidx.compose.ui.platform.LocalContext.current
            var isFollowed by remember(train) {
                mutableStateOf(
                    JourneyRepository.isJourneyFollowed(
                        context,
                        train.journey.TrainNumbers?.TrainNumberRef ?: train.journey.PublishedLineName,
                        train.journey.VehicleJourneyRef
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OverlayActionButton(
                    icon = Icons.Filled.LocationOn,
                    label = if (isCameraLocked) "Libérer" else "Centrer",
                    modifier = Modifier.weight(1f).clickable { onCameraLockChange(!isCameraLocked) }
                )
                OverlayActionButton(
                    icon = Icons.Filled.Info,
                    label = if (showOnlySelected) "Tout aff." else "Isoler",
                    modifier = Modifier.weight(1f).clickable { onShowOnlySelectedChange(!showOnlySelected) }
                )
                OverlayActionButton(
                    icon = Icons.Filled.Notifications,
                    label = if (isFollowed) "Ne plus suivre" else "Suivre",
                    modifier = Modifier.weight(1f).clickable {
                        val nowFollowed = JourneyRepository.toggleFollowJourney(context, train)
                        isFollowed = nowFollowed
                        val trainNum = train.journey.TrainNumbers?.TrainNumberRef ?: train.journey.PublishedLineName
                        val msg = if (nowFollowed) "Suivi activé pour le train n°$trainNum" else "Suivi arrêté pour le train n°$trainNum"
                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TrainflowPalette.border, RoundedCornerShape(16.dp))
                    .clickable { onOpenTrainInfo() }
                    .padding(16.dp)
            ) {
                Text(
                    text = "Voir détails",
                    color = TrainflowPalette.blue,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = TrainflowPalette.blue,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }
    }
}

@Composable
fun OverlayActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(TrainflowPalette.panel, RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = TrainflowPalette.textPrimary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, color = TrainflowPalette.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun HomeTopControls(
    modifier: Modifier = Modifier,
    isBack: Boolean = false,
    onOpenMenu: () -> Unit,
    onOpenNetworkStatus: () -> Unit = {}
) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FloatingControlButton(onClick = onOpenMenu) {
            Icon(
                imageVector = if (isBack) Icons.Filled.ArrowBack else Icons.Filled.Menu,
                contentDescription = if (isBack) "Back" else "Menu",
                tint = TrainflowPalette.textPrimary,
                modifier = Modifier.size(24.dp)
            )
        }

        FloatingControlButton(onClick = onOpenNetworkStatus) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = "Notifications",
                tint = TrainflowPalette.textPrimary,
                modifier = Modifier.size(24.dp)
            )
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
    stats: NetworkStatusStats?,
    selectedCategoryLabels: Set<String> = emptySet(),
    onCategoryToggle: (String) -> Unit = {}
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
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
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
                    val mergedCounts = linkedMapOf<String, Pair<Int, androidx.compose.ui.graphics.Color>>()
                    stats?.trainCounts?.forEach { (key, count) ->
                        val display = getTrainCategoryDisplay(key.lowercase())
                        val existing = mergedCounts[display.label]
                        mergedCounts[display.label] = Pair(
                            (existing?.first ?: 0) + count,
                            display.color
                        )
                    }
                    val counts = mergedCounts.entries.sortedByDescending { it.value.first }
                    val chunks = counts.chunked(3)

                    chunks.forEach { chunk ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            chunk.forEach { (label, countAndColor) ->
                                val isActive = selectedCategoryLabels.isEmpty() || selectedCategoryLabels.contains(label)
                                BreakdownChip(
                                    label = label,
                                    value = countAndColor.first.toString(),
                                    badgeColor = countAndColor.second,
                                    isActive = isActive,
                                    onClick = { onCategoryToggle(label) },
                                    modifier = Modifier.weight(1f)
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
    badgeColor: Color,
    isActive: Boolean = true,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val chipAlpha = if (isActive) 1f else 0.35f
    val borderColor = if (isActive) TrainflowPalette.border else TrainflowPalette.border.copy(alpha = 0.3f)

    Surface(
        color = TrainflowPalette.surface.copy(alpha = chipAlpha),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = badgeColor.copy(alpha = chipAlpha),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.wrapContentHeight()
            ) {
                Text(
                    text = label,
                    color = Color.White.copy(alpha = if (isActive) 0.93f else 0.4f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Text(
                text = value,
                color = TrainflowPalette.textPrimary.copy(alpha = chipAlpha),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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


