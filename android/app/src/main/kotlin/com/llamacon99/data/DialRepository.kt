package com.llamacon99.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** The two ways a dial code can reach the phone app — see [SettingsRepository.DialMode]. */
@Singleton
class DialRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** Places the call immediately, bypassing the user's phone app. Requires `CALL_PHONE`. */
    fun callDirect(code: String) {
        val intent = Intent(Intent.ACTION_CALL, Uri.fromParts("tel", code, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /** Opens the user's default phone app with [code] already entered — no permission needed. */
    fun openDialerPrefilled(code: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", code, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
