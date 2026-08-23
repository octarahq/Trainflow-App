package com.octarahq.trainflow.ui.utils

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

actual class PlatformContext(val androidContext: Context)

actual fun PlatformContext.copyToClipboard(text: String) {
    val clipboard = androidContext.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
    if (clipboard != null) {
        val clip = android.content.ClipData.newPlainText("Trainflow", text)
        clipboard.setPrimaryClip(clip)
    }
}

actual fun PlatformContext.shareText(text: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Partager")
    shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    androidContext.startActivity(shareIntent)
}

actual fun PlatformContext.showToast(message: String) {
    Toast.makeText(androidContext, message, Toast.LENGTH_SHORT).show()
}

actual val currentPlatformContext: PlatformContext
    @Composable get() = PlatformContext(LocalContext.current)
actual fun getAppVersion(): String = "1.0.1"
actual fun isPullToRefreshSupported(): Boolean = true

actual fun snapToRailNetwork(lat: Double, lon: Double): Pair<Double, Double> = Pair(lat, lon)

actual fun requestNotificationPermission() {
}

actual fun sendLocalNotification(title: String, body: String) {
}

actual fun getInitialDeepLink(): String? {
    val intent = com.octarahq.trainflow.AppContextHolder.currentIntent
    val data = intent?.data
    return if (data != null) {
        val path = data.path
        if (path.isNullOrBlank() || path == "/") null else path
    } else null
}

actual fun updateBrowserUrl(path: String) {
}
