package com.llamacon99.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.llamacon99.model.CallerIdEntry
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/**
 * Table of [CallerIdEntry] built from the device's contacts, read by
 * [com.llamacon99.monitor.CallMonitorService] to resolve an incoming wrapped number to a real
 * name. Unlike iOS's `CallerIDStore` (which has to cross an App Group because the Call Directory
 * Extension is a separate sandboxed process), this lives in the same process as the rest of the
 * app, so a flat DataStore-backed JSON blob is enough — no IPC needed.
 */
@Singleton
class CallerIdStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private val entriesKey = stringPreferencesKey("caller_id_entries")

    suspend fun replaceAll(entries: List<CallerIdEntry>) {
        val json = JSONArray().apply {
            entries.forEach { entry ->
                put(
                    JSONObject()
                        .put("wrappedNumber", entry.wrappedNumber)
                        .put("name", entry.name),
                )
            }
        }
        dataStore.edit { prefs -> prefs[entriesKey] = json.toString() }
    }

    suspend fun lookup(wrappedNumber: Long): String? =
        readAll().firstOrNull { it.wrappedNumber == wrappedNumber }?.name

    private suspend fun readAll(): List<CallerIdEntry> {
        val raw = dataStore.data.map { it[entriesKey] }.first() ?: return emptyList()
        val array = JSONArray(raw)
        return (0 until array.length()).map { index ->
            val obj = array.getJSONObject(index)
            CallerIdEntry(wrappedNumber = obj.getLong("wrappedNumber"), name = obj.getString("name"))
        }
    }
}
