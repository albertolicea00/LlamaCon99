package com.llamacon99.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.llamacon99.ui.contacts.ContactsListScreen
import com.llamacon99.ui.settings.SettingsScreen
import com.llamacon99.ui.welcome.WelcomeSheet

private object Routes {
    const val CONTACTS = "contacts"
    const val SETTINGS = "settings"
}

@Composable
fun LlamaCon99NavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.CONTACTS) {
        composable(Routes.CONTACTS) {
            Box {
                ContactsListScreen(onOpenSettings = { navController.navigate(Routes.SETTINGS) })
                WelcomeSheet(onOpenSettings = { navController.navigate(Routes.SETTINGS) })
            }
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = navController::popBackStack)
        }
    }
}
