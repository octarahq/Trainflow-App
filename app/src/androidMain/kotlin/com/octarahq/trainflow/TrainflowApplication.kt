package com.octarahq.trainflow

import android.app.Application

class TrainflowApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContextHolder.context = this
    }
}
