package com.llamacon99.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.llamacon99.telemetry.AppTelemetry

/** Sends explicitly labelled synthetic telemetry using the real app's Firebase configuration. */
class TelemetryTestReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = FirebaseApp.getInstance()
        val testId = intent.getStringExtra("test_id") ?: "manual"
        Log.i("TelemetryTest", "project=${app.options.projectId}, test_id=$testId")
        FirebaseCrashlytics.getInstance().setCustomKey("telemetry_test_id", testId)
        when (intent.getStringExtra("kind")) {
            "nonfatal" -> {
                AppTelemetry.recordNonFatal(
                    context,
                    IllegalStateException("LlamaCon99 synthetic nonfatal test: $testId"),
                    "Deliberate Firebase integration test",
                )
                Log.i("TelemetryTest", "Nonfatal recorded")
            }
            "fatal" -> {
                Handler(Looper.getMainLooper()).postDelayed({
                    throw IllegalStateException("LlamaCon99 synthetic fatal test: $testId")
                }, 500)
                Log.i("TelemetryTest", "Deliberate crash scheduled")
            }
            "event" -> {
                FirebaseAnalytics.getInstance(context).logEvent("telemetry_test", Bundle().apply {
                    putString("test_id", testId)
                })
                val trace = AppTelemetry.startTrace(context, "telemetry_test")
                trace?.putAttribute("test_id", testId)
                trace?.incrementMetric("test_runs", 1)
                trace?.stop()
                Log.i("TelemetryTest", "Analytics event and performance trace recorded")
            }
            else -> error("Specify kind: event, nonfatal or fatal")
        }
    }
}
