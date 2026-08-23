package com.octarahq.trainflow.ui.utils

import androidx.compose.runtime.Composable

expect class PlatformContext

expect fun PlatformContext.copyToClipboard(text: String)
expect fun PlatformContext.shareText(text: String)
expect fun PlatformContext.showToast(message: String)

expect val currentPlatformContext: PlatformContext
    @Composable get
expect fun getAppVersion(): String
expect fun isPullToRefreshSupported(): Boolean
expect fun snapToRailNetwork(lat: Double, lon: Double): Pair<Double, Double>
expect fun requestNotificationPermission()
expect fun sendLocalNotification(title: String, body: String)
expect fun getInitialDeepLink(): String?
expect fun updateBrowserUrl(path: String)
