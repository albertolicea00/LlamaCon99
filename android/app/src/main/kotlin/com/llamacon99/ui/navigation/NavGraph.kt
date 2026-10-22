package com.llamacon99.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.llamacon99.ui.contacts.ContactsListScreen
import com.llamacon99.ui.settings.SettingsScreen
import com.llamacon99.ui.welcome.WelcomeSheet
import com.llamacon99.telemetry.AppTelemetry

private object Routes {
    const val CONTACTS = "contacts"
    const val SETTINGS = "settings"
}

@Composable
fun LlamaCon99NavGraph(
    navController: NavHostController = rememberNavController(),
    quickDialMode: String? = null,
) {
    val context = LocalContext.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    LaunchedEffect(currentRoute) {
        currentRoute?.let { AppTelemetry.screen(context, it) }
    }

    NavHost(navController = navController, startDestination = Routes.CONTACTS) {
        composable(Routes.CONTACTS) {
            Box {
                ContactsListScreen(
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    quickDialMode = quickDialMode,
                )
                WelcomeSheet(onOpenSettings = { navController.navigate(Routes.SETTINGS) })
            }
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = navController::popBackStack)
        }
    }
}
