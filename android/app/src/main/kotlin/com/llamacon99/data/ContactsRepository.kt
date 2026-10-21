package com.llamacon99.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.llamacon99.model.CallerIdEntry
import com.llamacon99.model.ContactPhoneNumber
import com.llamacon99.model.DeviceContact
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Reads the device's Contacts, keeping only contacts with at least one Cuban number
 * ([CubanPhoneNumber.normalize]). Equivalent of iOS's `ContactsService` (`ContactsApp/Services.swift`),
 * minus the UIKit/Combine bits — this is a plain repository backed by a [StateFlow].
 */
@Singleton
class ContactsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val callerIdStore: CallerIdStore,
) {
    private val _contacts = MutableStateFlow<List<DeviceContact>>(emptyList())
    val contacts: StateFlow<List<DeviceContact>> = _contacts.asStateFlow()

    /** Re-reads the Contacts provider and refreshes [contacts] plus the shared [CallerIdStore]. */
    suspend fun reload() = withContext(Dispatchers.IO) {
        val result = if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED
        ) {
            emptyList()
        } else {
            try {
                queryCubanContacts()
            } catch (_: SecurityException) {
                // Permission can be revoked between the check and the provider query.
                emptyList()
            }
        }
        _contacts.value = result
        callerIdStore.replaceAll(result.toCallerIdEntries())
    }

    private fun queryCubanContacts(): List<DeviceContact> {
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.LABEL,
            ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
        )

        val byContactId = LinkedHashMap<String, MutableList<ContactPhoneNumber>>()
        val nameByContactId = LinkedHashMap<String, String>()
        val photoUriByContactId = LinkedHashMap<String, String?>()
        val seenNumbersByContactId = HashMap<String, MutableSet<String>>()

        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC",
        )?.use { cursor ->
            val contactIdIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val typeIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE)
            val labelIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LABEL)
            val photoUriIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)

            while (cursor.moveToNext()) {
                val contactId = cursor.getString(contactIdIdx) ?: continue
                val rawNumber = cursor.getString(numberIdx) ?: continue
                val normalized = CubanPhoneNumber.normalize(rawNumber) ?: continue

                val seen = seenNumbersByContactId.getOrPut(contactId) { mutableSetOf() }
                if (!seen.add(normalized)) continue

                val type = cursor.getInt(typeIdx)
                val customLabel = cursor.getString(labelIdx)
                val label = phoneLabel(type, customLabel)

                nameByContactId.getOrPut(contactId) { cursor.getString(nameIdx) ?: "" }
                photoUriByContactId.getOrPut(contactId) { cursor.getString(photoUriIdx) }
                byContactId.getOrPut(contactId) { mutableListOf() }
                    .add(ContactPhoneNumber(label = label, number = normalized))
            }
        }

        return byContactId.map { (contactId, numbers) ->
            DeviceContact(
                id = contactId,
                name = nameByContactId[contactId].orEmpty(),
                numbers = numbers,
                photoUri = photoUriByContactId[contactId],
            )
        }.filter { it.name.isNotBlank() && it.numbers.isNotEmpty() }
    }

    private fun phoneLabel(type: Int, customLabel: String?): String =
        ContactsContract.CommonDataKinds.Phone.getTypeLabel(context.resources, type, customLabel).toString()

    private fun List<DeviceContact>.toCallerIdEntries(): List<CallerIdEntry> =
        flatMap { contact ->
            contact.numbers.mapNotNull { number ->
                CallerIdMatcher.wrappedNumber(number.number)?.let { wrapped ->
                    CallerIdEntry(wrappedNumber = wrapped, name = contact.name)
                }
            }
        }
}
