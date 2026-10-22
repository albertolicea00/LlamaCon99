package com.llamacon99

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.llamacon99.telemetry.AppTelemetry

@HiltAndroidApp
class LlamaCon99Application : Application() {
    override fun onCreate() {
        super.onCreate()
        AppTelemetry.initialize(this)
    }
}
