package com.llamacon99.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import com.llamacon99.model.ContactPhoneNumber
import com.llamacon99.model.DeviceContact
import com.llamacon99.ui.contacts.*
import com.llamacon99.ui.theme.AppThemeCatalog
import com.llamacon99.ui.theme.LlamaCon99Theme
import com.llamacon99.ui.settings.SettingsActions
import com.llamacon99.ui.settings.SettingsContent
import com.llamacon99.ui.settings.SettingsUiState
import com.llamacon99.ui.settings.ThemePickerSheet

/** Debug-only visual fixture. Does not read contacts, persist settings, or place calls. */
class ThemePreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val theme = AppThemeCatalog.find(intent.getStringExtra("theme") ?: "manzana")
        val dark = intent.getBooleanExtra("dark", false)
        val settings = intent.getStringExtra("screen") == "settings"
        val barStyle = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
        val names = listOf("Ana García", "Andrés Pérez", "Antonio Ruiz", "Beatriz López", "Bruno Díaz",
            "Camila Torres", "Carlos Martínez", "Claudia Pérez", "Daniel Suárez", "Diego Castro",
            "Elena González", "Fernando Ramos", "Gabriela León", "Héctor Vidal", "Isabel Molina",
            "Javier Romero", "Karla Medina", "Laura Santos", "Luis Rodríguez", "María Fernández",
            "Natalia Cruz", "Olivia Silva", "Pablo Reyes", "Raquel Navarro", "Sofía Alonso",
            "Teresa Vega", "Ulises Duarte", "Valeria Rojas", "Walter Gómez", "Ximena Mora", "Yolanda Gil", "Zoe Blanco")
        val contacts = names.mapIndexed { index, name ->
            val phone = ContactPhoneNumber("móvil", "5123${index.toString().padStart(4, '0')}")
            ContactNumberEntry(DeviceContact(index.toString(), name, listOf(phone)), phone)
        }
        setContent {
            var query by remember { mutableStateOf("") }
            var selectedTheme by remember { mutableStateOf(theme) }
            var showThemePicker by remember { mutableStateOf(false) }
            var preferenceState by remember { mutableStateOf(SettingsUiState(
                hasCallMonitorPermissions = true, callMonitorEnabled = true,
                canShowCallerIdOverlay = true, monitorFromBootEnabled = true,
            )) }
            val groups = contacts.filter { it.contact.name.contains(query, ignoreCase = true) }
                .groupBy { it.contact.name.take(1) }.map { AlphabetGroup(it.key, it.value) }
            LlamaCon99Theme(darkTheme = dark, appTheme = selectedTheme) {
                Surface {
                    if (settings) {
                        SettingsContent(preferenceState, selectedTheme, onBack = {},
                            actions = SettingsActions(
                                onThemePicker = { showThemePicker = true },
                                onSelectDialMode = { preferenceState = preferenceState.copy(dialMode = it) },
                                onAnonymousSwipe = { preferenceState = preferenceState.copy(anonymousSwipeEnabled = it) },
                                onCallMonitor = { preferenceState = preferenceState.copy(callMonitorEnabled = it) },
                                onMonitorFromBoot = { preferenceState = preferenceState.copy(monitorFromBootEnabled = it) },
                                onMonitorPermissions = {}, onOverlayPermissions = {}, onCheckUpdate = {}, onLegalLinks = {},
                            ),
                            overlays = {
                                if (showThemePicker) ThemePickerSheet(selectedTheme,
                                    onSelect = { selectedTheme = it; showThemePicker = false }, onDismiss = { showThemePicker = false })
                            },
                        )
                    } else PlatformContactsScreen(ContactsListUiState(groups = groups), query, { query = it }, {}, {}, {}, {})
                }
            }
        }
    }
}
