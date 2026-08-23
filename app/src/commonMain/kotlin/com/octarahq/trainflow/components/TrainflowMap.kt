package com.octarahq.trainflow.components

import androidx.compose.runtime.Composable
import com.octarahq.trainflow.network.InterpolatedJourney
import com.octarahq.trainflow.ui.utils.TrainTrackingData

@Composable
expect fun TrainflowMap(
    trains: List<InterpolatedJourney>,
    trackingData: Map<String, TrainTrackingData>,
    selectedTrain: InterpolatedJourney?,
    isCameraLocked: Boolean,
    onCameraLockChange: (Boolean) -> Unit,
    onMapInteract: () -> Unit,
    onTrainClick: (InterpolatedJourney) -> Unit,
    targetLat: Double? = null,
    targetLon: Double? = null
)
