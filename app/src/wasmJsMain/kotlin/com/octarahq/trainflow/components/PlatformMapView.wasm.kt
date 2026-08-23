package com.octarahq.trainflow.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier

@JsFun("function(lat, lon) { if (window.trainflowMap) { window.trainflowMap.init(lat, lon, 10); window.trainflowMap.setView(lat, lon, 13); } }")
private external fun initTrainflowMapJs(lat: Double, lon: Double)

@JsFun("function(lat, lon) { if (window.trainflowMap) { window.trainflowMap.setView(lat, lon, 13); } }")
private external fun updateTrainflowMapCenterJs(lat: Double, lon: Double)

@Composable
actual fun PlatformMapView(
    modifier: Modifier,
    latitude: Double,
    longitude: Double,
    onMapReady: () -> Unit
) {
    LaunchedEffect(Unit) {
        initTrainflowMapJs(latitude, longitude)
        onMapReady()
    }

    LaunchedEffect(latitude, longitude) {
        updateTrainflowMapCenterJs(latitude, longitude)
    }
    Box(modifier = modifier)
}
