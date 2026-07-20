package com.example.ui

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController

object Route {
    const val ONBOARDING = "onboarding"
    const val CHAT = "chat"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val API_SETTINGS = "api_settings"
}

/**
 * Pops the back stack only while the current destination is still resumed.
 * Duplicate taps on a back button during the exit transition would otherwise
 * pop the start destination too, leaving the NavHost empty (black screen).
 */
fun NavController.safePopBackStack() {
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
        popBackStack()
    }
}
