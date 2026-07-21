package com.example.ui

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController

object Route {
    const val ONBOARDING = "onboarding"
    const val CHAT = "chat"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val API_PROFILES = "api_profiles"

    const val PROFILE_ID_ARG = "profileId"

    /** Doubles as the new-profile and edit-profile destination; no arg means "new". */
    const val API_PROFILE_EDIT = "api_profile_edit?$PROFILE_ID_ARG={$PROFILE_ID_ARG}"

    fun apiProfileEdit(profileId: String? = null): String =
        if (profileId == null) "api_profile_edit" else "api_profile_edit?$PROFILE_ID_ARG=$profileId"
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
