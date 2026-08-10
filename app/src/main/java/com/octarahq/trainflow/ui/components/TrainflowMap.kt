package com.octarahq.trainflow.ui.components

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.MotionEvent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.octarahq.trainflow.InterpolatedJourney
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.sources.VectorSource
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.TileSet
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import com.octarahq.trainflow.ApiClient

@SuppressLint("ClickableViewAccessibility")
@Composable
fun TrainflowMap(
    trains: List<InterpolatedJourney>,
    onMapInteract: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var geoJsonSource by remember { mutableStateOf<GeoJsonSource?>(null) }

    val mapView = remember {
        MapView(context).apply {
            getMapAsync { map ->
                mapLibreMap = map
                map.uiSettings.isAttributionEnabled = false
                map.uiSettings.isLogoEnabled = false
                
                map.setStyle(Style.Builder().fromJson("{\"version\": 8, \"sources\": {}, \"layers\": []}")) { style ->
                    
                    val ignUrl = "https://data.geopf.fr/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=GEOGRAPHICALGRIDSYSTEMS.PLANIGNV2&STYLE=normal&FORMAT=image/png&TILEMATRIXSET=PM&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}"
                    val ignTileSet = TileSet("2.2.0", ignUrl)
                    val ignSource = RasterSource("ign-source", ignTileSet, 256)
                    style.addSource(ignSource)
                    val ignLayer = org.maplibre.android.style.layers.RasterLayer("ign-layer", "ign-source")
                    style.addLayer(ignLayer)

                    val baseUrl = ApiClient.BASE_URL.removeSuffix("/")
                    val railsSource = VectorSource("rails", "$baseUrl/data/rails/{z}/{x}/{y}.pbf")
                    style.addSource(railsSource)
                    
                    val railsLayer = LineLayer("rails-layer", "rails").apply {
                        setSourceLayer("rails")
                        setProperties(
                            lineColor(Color.parseColor("#4B5563")),
                            lineWidth(2f)
                        )
                    }
                    style.addLayer(railsLayer)

                    val source = GeoJsonSource("trains-source")
                    style.addSource(source)
                    geoJsonSource = source

                    val trainLayer = CircleLayer("trains-layer", "trains-source").apply {
                        setProperties(
                            circleRadius(6f),
                            circleColor(Color.parseColor("#3B82F6")),
                            circleStrokeWidth(2f),
                            circleStrokeColor(Color.WHITE)
                        )
                    }
                    style.addLayer(trainLayer)
                }

                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(46.603354, 1.888334))
                    .zoom(5.0)
                    .build()
            }

            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
                    onMapInteract()
                }
                false
            }
        }
    }

    LaunchedEffect(trains, geoJsonSource) {
        if (geoJsonSource != null && trains.isNotEmpty()) {
            val features = trains.filter { it.lat != 0.0 && it.lon != 0.0 }.map {
                Feature.fromGeometry(Point.fromLngLat(it.lon, it.lat))
            }
            geoJsonSource?.setGeoJson(FeatureCollection.fromFeatures(features))
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

    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize()
    )
}
