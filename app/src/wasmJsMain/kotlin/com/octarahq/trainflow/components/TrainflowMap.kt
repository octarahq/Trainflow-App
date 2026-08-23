package com.octarahq.trainflow.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.octarahq.trainflow.network.InterpolatedJourney
import com.octarahq.trainflow.ui.utils.TrainTrackingData
import com.octarahq.trainflow.ui.utils.getTrainCategoryDisplay
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import kotlin.math.*

@JsFun("(z, x, y) => window.getTileData ? window.getTileData(z, x, y) : null")
private external fun getTileDataJs(z: Int, x: Int, y: Int): Uint8Array?

@JsFun("(lat, lon, zoom) => { if (window.trainflowMap) window.trainflowMap.init(lat, lon, zoom); }")
private external fun initMapJs(lat: Double, lon: Double, zoom: Double)

@JsFun("(lat, lon, zoom) => { if (window.trainflowMap) window.trainflowMap.setView(lat, lon, zoom); }")
private external fun setMapViewJs(lat: Double, lon: Double, zoom: Double)

@JsFun("() => window.innerWidth")
private external fun getWindowWidthJs(): Int

@Composable
actual fun TrainflowMap(
    trains: List<InterpolatedJourney>,
    trackingData: Map<String, TrainTrackingData>,
    selectedTrain: InterpolatedJourney?,
    isCameraLocked: Boolean,
    onCameraLockChange: (Boolean) -> Unit,
    onMapInteract: () -> Unit,
    onTrainClick: (InterpolatedJourney) -> Unit,
    targetLat: Double?,
    targetLon: Double?
) {
    var centerLat by remember { mutableStateOf(46.603354) }
    var centerLon by remember { mutableStateOf(1.888334) }
    var zoom by remember { mutableStateOf(6.0) }
    var redrawTrigger by remember { mutableStateOf(0) }

    val skiaCache = remember { mutableMapOf<String, org.jetbrains.skia.Image>() }

    LaunchedEffect(Unit) {
        initMapJs(centerLat, centerLon, zoom)
    }

    LaunchedEffect(targetLat, targetLon) {
        if (targetLat != null && targetLon != null && targetLat != 0.0 && targetLon != 0.0) {
            centerLat = targetLat
            centerLon = targetLon
            zoom = 12.0
            setMapViewJs(centerLat, centerLon, zoom)
        }
    }

    LaunchedEffect(selectedTrain, isCameraLocked, zoom, redrawTrigger) {
        if (isCameraLocked && selectedTrain != null) {
            val smartId = selectedTrain.journey.FramedVehicleJourneyRef?.DatedVehicleJourneyRef 
                ?: selectedTrain.journey.TrainNumbers?.TrainNumberRef 
                ?: selectedTrain.journey.VehicleJourneyRef
            val track = trackingData[smartId]
            val nowMs = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
            val interp = com.octarahq.trainflow.ui.utils.getSmoothInterpolatedPosition(
                trackingData = track,
                baseLat = selectedTrain.lat,
                baseLon = selectedTrain.lon,
                currentTime = nowMs,
                zoom = zoom,
                smartId = smartId
            )

            if (interp.lat != 0.0 && interp.lon != 0.0) {
                val scale = 256.0 * 2.0.pow(zoom)
                val winWidth = getWindowWidthJs()
                val isWide = winWidth > 700

                if (isWide) {
                    val shiftPx = 180.0
                    val deltaLon = shiftPx / scale * 360.0
                    centerLon = interp.lon - deltaLon
                    centerLat = interp.lat
                } else {
                    val shiftPx = 120.0
                    val centerLatRad = interp.lat * PI / 180.0
                    val deltaLat = shiftPx / scale * 360.0 * cos(centerLatRad)
                    centerLat = interp.lat - deltaLat
                    centerLon = interp.lon
                }
                setMapViewJs(centerLat, centerLon, zoom)
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(16)
            redrawTrigger++
        }
    }

    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(trains, centerLat, centerLon, zoom, trackingData) {
                awaitPointerEventScope {
                    var pressPos: Offset? = null
                    var isMultiTouch = false

                    while (true) {
                        val event = awaitPointerEvent()

                        when (event.type) {
                            PointerEventType.Scroll -> {
                                val scrollDelta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                                if (scrollDelta != 0f) {
                                    val zoomFactor = if (scrollDelta > 0) 0.88f else 1.14f
                                    val newZoom = (zoom * zoomFactor).coerceIn(4.0, 18.0)
                                    zoom = newZoom
                                    setMapViewJs(centerLat, centerLon, zoom)
                                }
                            }
                            PointerEventType.Press -> {
                                val change = event.changes.firstOrNull()
                                pressPos = change?.position
                                isMultiTouch = event.changes.size > 1
                            }
                            PointerEventType.Move -> {
                                val changes = event.changes
                                if (changes.size == 1) {
                                    val change = changes.first()
                                    if (change.pressed) {
                                        val pan = change.position - change.previousPosition
                                        if (pan.getDistance() > 0.5f) {
                                            onMapInteract()
                                            if (isCameraLocked) onCameraLockChange(false)

                                            val scale = 256.0 * 2.0.pow(zoom)
                                            val deltaLon = -pan.x / scale * 360.0
                                            val centerLatRad = centerLat * PI / 180.0
                                            val deltaLat = pan.y / scale * 360.0 * cos(centerLatRad)

                                            centerLon = (centerLon + deltaLon).coerceIn(-180.0, 180.0)
                                            centerLat = (centerLat + deltaLat).coerceIn(-85.0, 85.0)

                                            setMapViewJs(centerLat, centerLon, zoom)
                                        }
                                    }
                                } else if (changes.size >= 2) {
                                    isMultiTouch = true
                                    val p1 = changes[0].position
                                    val p2 = changes[1].position
                                    val prevP1 = changes[0].previousPosition
                                    val prevP2 = changes[1].previousPosition

                                    val currentDist = (p1 - p2).getDistance()
                                    val prevDist = (prevP1 - prevP2).getDistance()

                                    if (prevDist > 0) {
                                        val zoomFactor = (currentDist / prevDist).toDouble()
                                        val newZoom = (zoom * zoomFactor).coerceIn(4.0, 18.0)
                                        zoom = newZoom
                                        setMapViewJs(centerLat, centerLon, zoom)
                                    }
                                }
                            }
                            PointerEventType.Release -> {
                                val change = event.changes.firstOrNull()
                                val start = pressPos
                                if (change != null && start != null && !isMultiTouch) {
                                    val moveDist = (change.position - start).getDistance()
                                    if (moveDist < 16.0f) {
                                        val tapOffset = change.position
                                        onMapInteract()
                                        val width = size.width.toFloat()
                                        val height = size.height.toFloat()
                                        val nowMs = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()

                                        var clickedTrain: InterpolatedJourney? = null
                                        var minDistanceSq = 50.0 * 50.0

                                        for (train in trains) {
                                            val smartId = train.journey.FramedVehicleJourneyRef?.DatedVehicleJourneyRef 
                                                ?: train.journey.TrainNumbers?.TrainNumberRef 
                                                ?: train.journey.VehicleJourneyRef
                                            val track = trackingData[smartId]
                                            val interp = com.octarahq.trainflow.ui.utils.getSmoothInterpolatedPosition(
                                                trackingData = track,
                                                baseLat = train.lat,
                                                baseLon = train.lon,
                                                currentTime = nowMs,
                                                zoom = zoom,
                                                smartId = smartId
                                            )

                                            val lat = if (interp.lat != 0.0) interp.lat else continue
                                            val lon = if (interp.lon != 0.0) interp.lon else continue
                                            val pos = projectWebMercator(lat, lon, centerLat, centerLon, zoom, width, height)
                                            val distSq = (pos.x - tapOffset.x).pow(2) + (pos.y - tapOffset.y).pow(2)
                                            if (distSq < minDistanceSq) {
                                                minDistanceSq = distSq.toDouble()
                                                clickedTrain = train
                                            }
                                        }

                                        if (clickedTrain != null) {
                                            onTrainClick(clickedTrain)
                                        }
                                    }
                                }
                                pressPos = null
                            }
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            @Suppress("UNUSED_VARIABLE")
            val dummy = redrawTrigger
            val nowMs = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()

            val width = size.width.toFloat()
            val height = size.height.toFloat()

            val intZoom = floor(zoom).toInt().coerceIn(2, 18)
            val numTiles = 1 shl intZoom
            val tileSize = 256.0 * 2.0.pow(zoom - intZoom)

            val centerTileX = (centerLon + 180.0) / 360.0 * numTiles
            val centerTileY = (1.0 - ln(tan(centerLat * PI / 180.0) + 1.0 / cos(centerLat * PI / 180.0)) / PI) / 2.0 * numTiles

            val minTileX = floor(centerTileX - width / (2.0 * tileSize)).toInt().coerceIn(0, numTiles - 1)
            val maxTileX = floor(centerTileX + width / (2.0 * tileSize)).toInt().coerceIn(0, numTiles - 1)
            val minTileY = floor(centerTileY - height / (2.0 * tileSize)).toInt().coerceIn(0, numTiles - 1)
            val maxTileY = floor(centerTileY + height / (2.0 * tileSize)).toInt().coerceIn(0, numTiles - 1)

            drawRect(color = Color(0xFF1D2026))

            for (tx in minTileX..maxTileX) {
                for (ty in minTileY..maxTileY) {
                    val tileKey = "$intZoom/$tx/$ty"
                    var skiaImage = skiaCache[tileKey]

                    if (skiaImage == null) {
                        val arr = getTileDataJs(intZoom, tx, ty)
                        if (arr != null) {
                            val len = arr.length
                            val bytes = ByteArray(len)
                            for (i in 0 until len) {
                                bytes[i] = arr[i]
                            }
                            try {
                                skiaImage = org.jetbrains.skia.Image.makeFromEncoded(bytes)
                                skiaCache[tileKey] = skiaImage
                            } catch (_: Exception) {}
                        }
                    }

                    if (skiaImage != null) {
                        val left = (width / 2.0 + (tx - centerTileX) * tileSize).toFloat()
                        val top = (height / 2.0 + (ty - centerTileY) * tileSize).toFloat()
                        val right = (left + tileSize).toFloat()
                        val bottom = (top + tileSize).toFloat()

                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawImageRect(
                                image = skiaImage,
                                dst = org.jetbrains.skia.Rect.makeLTRB(left, top, right, bottom)
                            )
                        }
                    }
                }
            }

            for (train in trains) {
                val smartId = train.journey.FramedVehicleJourneyRef?.DatedVehicleJourneyRef 
                    ?: train.journey.TrainNumbers?.TrainNumberRef 
                    ?: train.journey.VehicleJourneyRef
                val track = trackingData[smartId]
                val interp = com.octarahq.trainflow.ui.utils.getSmoothInterpolatedPosition(
                    trackingData = track,
                    baseLat = train.lat,
                    baseLon = train.lon,
                    currentTime = nowMs,
                    zoom = zoom,
                    smartId = smartId
                )

                val lat = interp.lat
                val lon = interp.lon
                if (lat == 0.0 && lon == 0.0) continue

                val pos = projectWebMercator(lat, lon, centerLat, centerLon, zoom, width, height)

                if (pos.x < -50 || pos.x > width + 50 || pos.y < -50 || pos.y > height + 50) continue

                val isSelected = selectedTrain?.journey?.VehicleJourneyRef == train.journey.VehicleJourneyRef &&
                        selectedTrain?.journey?.VehicleJourneyRef?.isNotEmpty() == true

                val ref = train.journey.ProductCategoryRef.ifBlank { train.journey.VehicleMode }
                val display = getTrainCategoryDisplay(ref.lowercase())
                val trainColor = display.color

                if (isSelected) {
                    drawCircle(
                        color = trainColor.copy(alpha = 0.3f),
                        radius = 24f,
                        center = pos
                    )
                    drawCircle(
                        color = trainColor,
                        radius = 16f,
                        center = pos,
                        style = Stroke(width = 3f)
                    )
                }

                drawCircle(
                    color = Color(0xFF1D2026),
                    radius = if (isSelected) 14f else 11f,
                    center = pos
                )

                drawCircle(
                    color = trainColor,
                    radius = if (isSelected) 10f else 7f,
                    center = pos
                )

                if (zoom >= 8.0 || isSelected) {
                    val trainNum = train.journey.TrainNumbers?.TrainNumberRef 
                        ?: train.journey.PublishedLineName.ifBlank { train.journey.VehicleJourneyRef }
                    
                    if (trainNum.isNotBlank()) {
                        val textResult = textMeasurer.measure(
                            text = trainNum,
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        val bgWidth = textResult.size.width + 12f
                        val bgHeight = textResult.size.height + 4f
                        val bgLeft = pos.x - bgWidth / 2f
                        val bgTop = pos.y - 28f - bgHeight

                        drawRoundRect(
                            color = Color(0xFF1E293B).copy(alpha = 0.9f),
                            topLeft = Offset(bgLeft, bgTop),
                            size = Size(bgWidth, bgHeight),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                        drawRoundRect(
                            color = trainColor,
                            topLeft = Offset(bgLeft, bgTop),
                            size = Size(bgWidth, bgHeight),
                            cornerRadius = CornerRadius(6f, 6f),
                            style = Stroke(width = 1.5f)
                        )
                        drawText(
                            textLayoutResult = textResult,
                            topLeft = Offset(bgLeft + 6f, bgTop + 2f)
                        )
                    }
                }
            }
        }
    }
}

private fun projectWebMercator(
    lat: Double,
    lon: Double,
    centerLat: Double,
    centerLon: Double,
    zoom: Double,
    width: Float,
    height: Float
): Offset {
    val scale = 256.0 * 2.0.pow(zoom)
    val worldX = scale * (lon + 180.0) / 360.0
    val worldY = scale * (1.0 - ln(tan(lat * PI / 180.0) + 1.0 / cos(lat * PI / 180.0)) / PI) / 2.0

    val centerWorldX = scale * (centerLon + 180.0) / 360.0
    val centerWorldY = scale * (1.0 - ln(tan(centerLat * PI / 180.0) + 1.0 / cos(centerLat * PI / 180.0)) / PI) / 2.0

    val screenX = (worldX - centerWorldX + width / 2).toFloat()
    val screenY = (worldY - centerWorldY + height / 2).toFloat()
    return Offset(screenX, screenY)
}
