package com.llamacon99.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CallerIdLookup @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: CallerIdStore,
) {
    /** Query current contacts so identification also works before the next cached list refresh. */
    suspend fun nameFor(localNumber: String): String? = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return@withContext null
        }
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode("+53$localNumber"))
        try {
            context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val name = cursor.getString(0)
                    if (!name.isNullOrBlank()) return@withContext name
                }
            }
            CallerIdMatcher.wrappedNumber(localNumber)?.let { store.lookup(it) }?.takeIf { it.isNotBlank() }
        } catch (_: SecurityException) {
            // Contact access can be revoked between the check and the query.
            null
        }
    }
}
