package com.octarahq.trainflow.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun PlatformMapView(
    modifier: Modifier = Modifier,
    latitude: Double,
    longitude: Double,
    onMapReady: () -> Unit
)
