package com.octarahq.trainflow.ui.components

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.MotionEvent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.octarahq.trainflow.network.InterpolatedJourney
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.sources.VectorSource
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.TileSet
import com.octarahq.trainflow.network.apiService
import com.octarahq.trainflow.ui.utils.TrainTrackingData
import androidx.compose.ui.unit.dp

@SuppressLint("ClickableViewAccessibility")
@Composable
actual fun TrainflowMap(
    trains: List<InterpolatedJourney>,
    trackingData: Map<String, TrainTrackingData>,
    selectedTrain: InterpolatedJourney?,
    isCameraLocked: Boolean,
    onCameraLockChange: (Boolean) -> Unit,
    onMapInteract: () -> Unit,
    onTrainClick: (InterpolatedJourney) -> Unit,
    targetLat: Double? = null,
    targetLon: Double? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    
    val touchRadiusPx = 30.0 * density.density
    val touchRadiusSq = Math.pow(touchRadiusPx, 2.0)

    val currentTrains by rememberUpdatedState(trains)
    val currentTrackingData by rememberUpdatedState(trackingData)
    val currentOnTrainClick by rememberUpdatedState(onTrainClick)
    val currentOnCameraLockChange by rememberUpdatedState(onCameraLockChange)
    val clickTempLatLng = remember { LatLng(0.0, 0.0) }

    val mapView = remember {
        MapView(context).apply {
            getMapAsync { map ->
                mapLibreMap = map
                map.uiSettings.isAttributionEnabled = false
                map.uiSettings.isLogoEnabled = false
                map.uiSettings.isRotateGesturesEnabled = false
                
                map.setStyle(Style.Builder().fromJson("{\"version\": 8, \"sources\": {}, \"layers\": []}")) { style ->
                    
                    val ignUrl = "https://data.geopf.fr/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=GEOGRAPHICALGRIDSYSTEMS.PLANIGNV2&STYLE=normal&FORMAT=image/png&TILEMATRIXSET=PM&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}"
                    val ignTileSet = TileSet("2.2.0", ignUrl)
                    val ignSource = RasterSource("ign-source", ignTileSet, 256)
                    style.addSource(ignSource)
                    val ignLayer = org.maplibre.android.style.layers.RasterLayer("ign-layer", "ign-source")
                    style.addLayer(ignLayer)

                    val baseUrl = apiService.baseUrl.removeSuffix("/")
                    val railsSource = org.maplibre.android.style.sources.GeoJsonSource("rails", java.net.URI.create("$baseUrl/shapes/lignes_sncf.geojson"))
                    style.addSource(railsSource)
                    
                    val railsLayer = LineLayer("rails-layer", "rails").apply {
                        setProperties(
                            lineColor(Color.parseColor("#4B5563")),
                            lineWidth(2f)
                        )
                    }
                    style.addLayer(railsLayer)
                }

                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(46.603354, 1.888334))
                    .zoom(5.0)
                    .build()
                    
                map.addOnMapClickListener { point ->
                    val pointF = map.projection.toScreenLocation(point)
                    
                    var clickedTrain: InterpolatedJourney? = null
                    val isZoomedIn = map.cameraPosition.zoom >= 10.0
                    
                    val loopTrains = currentTrains
                    val loopTrackingData = currentTrackingData
                    
                    if (isZoomedIn) {
                        val currentTime = System.currentTimeMillis()
                        for (train in loopTrains) {
                            if (train.lat != 0.0 && train.lon != 0.0) {
                                val smartId = train.journey.FramedVehicleJourneyRef?.DatedVehicleJourneyRef 
                                    ?: train.journey.TrainNumbers?.TrainNumberRef 
                                    ?: train.journey.VehicleJourneyRef
                                
                                var cLat = train.lat
                                var cLon = train.lon
                                val data = loopTrackingData[smartId]
                                if (data != null && data.lastUpdate != data.currentUpdate) {
                                    val timeDiff = data.currentUpdate - data.lastUpdate
                                    val elapsed = currentTime - data.currentUpdate
                                    val ratio = (elapsed.toDouble() / timeDiff).coerceAtMost(1.5)
                                    cLat = data.currentLat + (data.currentLat - data.lastLat) * ratio
                                    cLon = data.currentLon + (data.currentLon - data.lastLon) * ratio
                                }
                                
                                clickTempLatLng.latitude = cLat
                                clickTempLatLng.longitude = cLon
                                val trainScreenPoint = map.projection.toScreenLocation(clickTempLatLng)
                                val distSq = Math.pow((trainScreenPoint.x - pointF.x).toDouble(), 2.0) + Math.pow((trainScreenPoint.y - pointF.y).toDouble(), 2.0)
                                if (distSq < touchRadiusSq) {
                                    clickedTrain = train
                                    break
                                }
                            }
                        }
                    } else {
                        for (train in loopTrains) {
                            if (train.lat != 0.0 && train.lon != 0.0) {
                                clickTempLatLng.latitude = train.lat
                                clickTempLatLng.longitude = train.lon
                                val trainScreenPoint = map.projection.toScreenLocation(clickTempLatLng)
                                val distSq = Math.pow((trainScreenPoint.x - pointF.x).toDouble(), 2.0) + Math.pow((trainScreenPoint.y - pointF.y).toDouble(), 2.0)
                                if (distSq < touchRadiusSq) {
                                    clickedTrain = train
                                    break
                                }
                            }
                        }
                    }

                    if (clickedTrain != null) {
                        currentOnTrainClick(clickedTrain)
                        return@addOnMapClickListener true
                    } else {
                        currentOnCameraLockChange(false)
                        return@addOnMapClickListener false
                    }
                }
                
                map.addOnMoveListener(object : org.maplibre.android.maps.MapLibreMap.OnMoveListener {
                    override fun onMoveBegin(detector: org.maplibre.android.gestures.MoveGestureDetector) {
                        onCameraLockChange(false)
                        onMapInteract()
                    }
                    override fun onMove(detector: org.maplibre.android.gestures.MoveGestureDetector) {}
                    override fun onMoveEnd(detector: org.maplibre.android.gestures.MoveGestureDetector) {}
                })
            }

            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
                    onMapInteract()
                }
                false
            }
        }
    }

    var lastAnimatedTrainId by remember { mutableStateOf<String?>(null) }
    var frameTrigger by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            frameTrigger = System.currentTimeMillis()
            kotlinx.coroutines.delay(16)
        }
    }

    LaunchedEffect(targetLat, targetLon, mapLibreMap) {
        if (targetLat != null && targetLon != null && mapLibreMap != null) {
            mapLibreMap?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(targetLat, targetLon), 13.5),
                1000
            )
        }
    }

    LaunchedEffect(trains, trackingData, isCameraLocked, selectedTrain) {
        if (selectedTrain == null) {
            onCameraLockChange(false)
            mapLibreMap?.setPadding(0, 0, 0, 0)
            lastAnimatedTrainId = null
            return@LaunchedEffect
        }
        
        val targetSmartId = selectedTrain.journey.FramedVehicleJourneyRef?.DatedVehicleJourneyRef 
            ?: selectedTrain.journey.TrainNumbers?.TrainNumberRef 
            ?: selectedTrain.journey.VehicleJourneyRef
            
        onCameraLockChange(true)
        val bottomPaddingPx = with(density) { 280.dp.toPx() }.toInt()
        mapLibreMap?.setPadding(0, 0, 0, bottomPaddingPx)
        
        if (lastAnimatedTrainId != targetSmartId) {
            mapLibreMap?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(selectedTrain.lat, selectedTrain.lon),
                    14.0
                ),
                1000
            )
            lastAnimatedTrainId = targetSmartId
            kotlinx.coroutines.delay(1000)
        }
        
        while (true) {
            if (isCameraLocked && (mapLibreMap?.cameraPosition?.zoom ?: 0.0) >= 10.0) {
                val currentTime = System.currentTimeMillis()
                val train = trains.find { 
                    val tid = it.journey.FramedVehicleJourneyRef?.DatedVehicleJourneyRef 
                        ?: it.journey.TrainNumbers?.TrainNumberRef 
                        ?: it.journey.VehicleJourneyRef
                    tid == targetSmartId 
                }
                
                if (train != null && train.lat != 0.0 && train.lon != 0.0) {
                    val data = trackingData[targetSmartId]
                    val zoom = mapLibreMap?.cameraPosition?.zoom ?: 10.0
                    val interp = com.octarahq.trainflow.ui.utils.getSmoothInterpolatedPosition(data, train.lat, train.lon, currentTime, zoom)
                    mapLibreMap?.moveCamera(CameraUpdateFactory.newLatLng(LatLng(interp.lat, interp.lon)))
                }
            }
            
            kotlinx.coroutines.delay(16)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )
        
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val _t = frameTrigger 
            val map = mapLibreMap ?: return@Canvas
            
            val isZoomedIn = map.cameraPosition.zoom >= 10.0
            
            val targetSmartId = selectedTrain?.let {
                it.journey.FramedVehicleJourneyRef?.DatedVehicleJourneyRef 
                    ?: it.journey.TrainNumbers?.TrainNumberRef 
                    ?: it.journey.VehicleJourneyRef
            }
            
            for (train in trains) {
                if (train.lat == 0.0 || train.lon == 0.0) continue
                
                val smartId = train.journey.FramedVehicleJourneyRef?.DatedVehicleJourneyRef 
                    ?: train.journey.TrainNumbers?.TrainNumberRef 
                    ?: train.journey.VehicleJourneyRef 
                    
                var cLat = train.lat
                var cLon = train.lon
                
                if (isZoomedIn) {
                    val data = trackingData[smartId]
                    if (data != null && data.lastUpdate != data.currentUpdate) {
                        val timeDiff = data.currentUpdate - data.lastUpdate
                        val elapsed = frameTrigger - data.currentUpdate
                        val ratio = (elapsed.toDouble() / timeDiff).coerceAtMost(1.5)
                        cLat = data.currentLat + (data.currentLat - data.lastLat) * ratio
                        cLon = data.currentLon + (data.currentLon - data.lastLon) * ratio
                    }
                }
                
                clickTempLatLng.latitude = cLat
                clickTempLatLng.longitude = cLon
                val pt = map.projection.toScreenLocation(clickTempLatLng)
                val isSelected = (smartId == targetSmartId)
                val color = if (isSelected) androidx.compose.ui.graphics.Color.Red else androidx.compose.ui.graphics.Color(0xFF3B82F6)
                val radius = if (isSelected) 10f else 6f
                
                drawCircle(
                    color = color,
                    radius = radius * density.density,
                    center = androidx.compose.ui.geometry.Offset(pt.x, pt.y)
                )
                drawCircle(
                    color = androidx.compose.ui.graphics.Color.White,
                    radius = radius * density.density,
                    center = androidx.compose.ui.geometry.Offset(pt.x, pt.y),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f * density.density)
                )
            }
        }
    }
}
