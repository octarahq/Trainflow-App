package com.octarahq.trainflow.ui.utils

import androidx.compose.runtime.Composable
import kotlinx.browser.window
import org.khronos.webgl.get



actual class PlatformContext

actual fun PlatformContext.copyToClipboard(text: String) {
    try {
        window.navigator.clipboard.writeText(text)
    } catch (e: Exception) {
        println("Clipboard not supported or failed")
    }
}

actual fun PlatformContext.shareText(text: String) {
    window.open("mailto:?subject=Trainflow&body=${text.replace(" ", "%20")}", "_blank")
}

actual fun PlatformContext.showToast(message: String) {
    window.alert(message)
}

actual val currentPlatformContext: PlatformContext
    @Composable get() = PlatformContext()

actual fun getAppVersion(): String = "Web Beta"

@JsFun("() => window.railSegments || null")
private external fun getRailSegmentsJs(): org.khronos.webgl.Float64Array?

actual fun snapToRailNetwork(lat: Double, lon: Double): Pair<Double, Double> {
    val arr = getRailSegmentsJs() ?: return Pair(lat, lon)
    val len = arr.length
    if (len == 0 || lat == 0.0 || lon == 0.0) return Pair(lat, lon)

    var bestLat = lat
    var bestLon = lon
    val maxLatDist = 0.015
    val maxLonDist = 0.022
    var minDistSqM = Double.MAX_VALUE

    var i = 0
    while (i < len) {
        val lat1 = arr[i]
        val lon1 = arr[i + 1]
        val lat2 = arr[i + 2]
        val lon2 = arr[i + 3]
        i += 4

        val segMinLat = if (lat1 < lat2) lat1 else lat2
        val segMaxLat = if (lat1 > lat2) lat1 else lat2
        val segMinLon = if (lon1 < lon2) lon1 else lon2
        val segMaxLon = if (lon1 > lon2) lon1 else lon2
        if (lat < segMinLat - maxLatDist || lat > segMaxLat + maxLatDist) continue
        if (lon < segMinLon - maxLonDist || lon > segMaxLon + maxLonDist) continue

        val dLat = lat2 - lat1
        val dLon = lon2 - lon1
        val ab2 = dLat * dLat + dLon * dLon
        if (ab2 == 0.0) continue

        val apLat = lat - lat1
        val apLon = lon - lon1
        val t = ((apLat * dLat + apLon * dLon) / ab2).coerceIn(0.0, 1.0)

        val projLat = lat1 + t * dLat
        val projLon = lon1 + t * dLon

        val dLatM = (lat - projLat) * 111000.0
        val dLonM = (lon - projLon) * 74000.0
        val distSqM = dLatM * dLatM + dLonM * dLonM
        if (distSqM < minDistSqM) {
            minDistSqM = distSqM
            bestLat = projLat
            bestLon = projLon
        }
    }

    return if (minDistSqM < 500.0 * 500.0) Pair(bestLat, bestLon) else Pair(lat, lon)
}

actual fun isPullToRefreshSupported(): Boolean {
    val ua = window.navigator.userAgent.lowercase()
    return ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")
}

@JsFun("() => window.requestNotificationPermission()")
private external fun requestNotificationPermissionJs()

@JsFun("(title, body) => window.sendLocalNotification(title, body)")
private external fun sendLocalNotificationJs(title: String, body: String)

actual fun requestNotificationPermission() {
    requestNotificationPermissionJs()
}

actual fun sendLocalNotification(title: String, body: String) {
    sendLocalNotificationJs(title, body)
}

@JsFun("(path) => { try { window.history.pushState(null, '', path); } catch(e) { console.error('pushState error:', e); } }")
private external fun pushStateJs(path: String)

@JsFun("() => window.location.pathname + window.location.search")
private external fun getLocationPathJs(): String

actual fun getInitialDeepLink(): String? {
    val path = getLocationPathJs()
    return if (path == "/" || path.isBlank()) null else path
}

actual fun updateBrowserUrl(path: String) {
    val current = getLocationPathJs()
    if (current != path) {
        pushStateJs(path)
    }
}
