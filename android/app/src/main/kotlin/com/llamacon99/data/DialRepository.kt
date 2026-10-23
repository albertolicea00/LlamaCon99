package com.llamacon99.data

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** The two ways a dial code can reach the phone app — see [SettingsRepository.DialMode]. */
@Singleton
class DialRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val simRepository: SimRepository,
) {
    /** Places the call with permission; otherwise lets the user confirm it in the dialer. */
    fun callDirect(code: String) {
        if (simRepository.refresh() != 1 || ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            openDialerPrefilled(code)
            return
        }
        val intent = Intent(Intent.ACTION_CALL, Uri.fromParts("tel", code, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (_: SecurityException) {
            // Android can revoke the permission between the check and the activity launch.
            openDialerPrefilled(code)
        }
    }

    /** Opens the user's default phone app with [code] already entered — no permission needed. */
    fun openDialerPrefilled(code: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", code, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
