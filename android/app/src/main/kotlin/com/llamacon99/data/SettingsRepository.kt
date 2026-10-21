package com.llamacon99.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import com.llamacon99.ui.theme.AppThemeCatalog
import com.llamacon99.ui.theme.AppThemeDefinition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * User-configurable behavior — equivalent of the toggles iOS keeps in `InstallGuideView` /
 * `AppStorage` (`anonymousSwipeEnabled`, the weekly welcome-reminder timestamp), plus the new
 * Android-only [DialMode] choice (see plan: the user rejected becoming the default dialer, so
 * tapping a contact either calls directly or hands the number to the user's own phone app).
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    enum class DialMode { DIRECT, PREFILL }

    private object Keys {
        val dialMode = stringPreferencesKey("dial_mode")
        val appTheme = stringPreferencesKey("app_theme")
        val anonymousSwipeEnabled = booleanPreferencesKey("anonymous_swipe_enabled")
        val callMonitorEnabled = booleanPreferencesKey("call_monitor_enabled")
        val monitorFromBootEnabled = booleanPreferencesKey("monitor_from_boot_enabled")
        val lastWelcomeShownAtMillis = longPreferencesKey("last_welcome_shown_at_millis")
        val lastUpdateCheckAtMillis = longPreferencesKey("last_update_check_at_millis")
    }

    /** Default is [DialMode.PREFILL]: no extra permission, and the user confirms the call themselves. */
    val dialMode: Flow<DialMode> = dataStore.data.map { prefs ->
        prefs[Keys.dialMode]?.let { runCatching { DialMode.valueOf(it) }.getOrNull() } ?: DialMode.PREFILL
    }

    suspend fun setDialMode(mode: DialMode) {
        dataStore.edit { it[Keys.dialMode] = mode.name }
    }

    val appTheme: Flow<AppThemeDefinition> = dataStore.data.map { prefs ->
        AppThemeCatalog.find(prefs[Keys.appTheme]?.let { if (it == "SAMSUNG") "llama" else it.lowercase() })
    }

    suspend fun setAppTheme(theme: AppThemeDefinition) {
        dataStore.edit { it[Keys.appTheme] = theme.id }
    }

    val anonymousSwipeEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.anonymousSwipeEnabled] ?: false }

    suspend fun setAnonymousSwipeEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.anonymousSwipeEnabled] = enabled }
    }

    val callMonitorEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.callMonitorEnabled] ?: false }

    suspend fun setCallMonitorEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.callMonitorEnabled] = enabled }
    }

    val monitorFromBootEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.monitorFromBootEnabled] ?: false }

    suspend fun setMonitorFromBootEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.monitorFromBootEnabled] = enabled }
    }

    val lastWelcomeShownAtMillis: Flow<Long?> = dataStore.data.map { it[Keys.lastWelcomeShownAtMillis] }

    suspend fun setLastWelcomeShownAtMillis(millis: Long) {
        dataStore.edit { it[Keys.lastWelcomeShownAtMillis] = millis }
    }

    val lastUpdateCheckAtMillis: Flow<Long?> = dataStore.data.map { it[Keys.lastUpdateCheckAtMillis] }

    suspend fun setLastUpdateCheckAtMillis(millis: Long) {
        dataStore.edit { it[Keys.lastUpdateCheckAtMillis] = millis }
    }
}
