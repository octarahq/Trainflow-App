package com.octarahq.trainflow

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import com.octarahq.trainflow.ui.MainApp

import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    document.getElementById("loading-splash")?.remove()
    CanvasBasedWindow(canvasElementId = "ComposeTarget") {
        MainApp()
    }
}
