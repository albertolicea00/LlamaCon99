package com.llamacon99.telemetry

import android.content.Context
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import com.llamacon99.BuildConfig

/** Analytics, Crashlytics and Performance stay dormant until this app's Firebase config is present. */
object AppTelemetry {
    private fun isConfigured(context: Context): Boolean = FirebaseApp.getApps(context).isNotEmpty()

    fun initialize(context: Context) {
        if (!isConfigured(context)) return
        val buildType = if (BuildConfig.DEBUG) "debug" else "release"
        FirebaseAnalytics.getInstance(context).setUserProperty("build_type", buildType)
        FirebaseCrashlytics.getInstance().setCustomKey("build_type", buildType)
    }

    fun screen(context: Context, name: String) {
        if (!isConfigured(context)) return
        val parameters = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, name)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, "MainActivity")
        }
        FirebaseAnalytics.getInstance(context).logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, parameters)
    }

    fun recordNonFatal(context: Context, error: Throwable, message: String? = null) {
        if (!isConfigured(context)) return
        message?.let { FirebaseCrashlytics.getInstance().log(it) }
        FirebaseCrashlytics.getInstance().recordException(error)
    }

    fun startTrace(context: Context, name: String): Trace? {
        if (!isConfigured(context)) return null
        return FirebasePerformance.getInstance().newTrace(name).apply { start() }
    }
}
