package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.local.AppDatabase
import com.example.ui.ApiSettingsScreen
import com.example.ui.ChatScreen
import com.example.ui.ChatViewModel
import com.example.ui.ChatViewModelFactory
import com.example.ui.HistoryScreen
import com.example.ui.OnboardingScreen
import com.example.ui.Route
import com.example.ui.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.OnboardingViewModel
import com.example.viewmodel.OnboardingViewModelFactory

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    setContent {
      MyApplicationTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            // null while the local DB read is in flight, then true/false.
            val hasProfile by produceState<Boolean?>(initialValue = null) {
                value = AppDatabase.getInstance(applicationContext)
                    .userDao()
                    .getUserProfile() != null
            }
            val startDestination = when (hasProfile) {
                null -> return@Surface
                true -> Route.CHAT
                false -> Route.ONBOARDING
            }

            val navController = rememberNavController()
            val chatViewModel: ChatViewModel = viewModel(
                factory = ChatViewModelFactory(applicationContext)
            )

            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
                composable(Route.ONBOARDING) {
                    val onboardingViewModel: OnboardingViewModel = viewModel(
                        factory = OnboardingViewModelFactory(applicationContext)
                    )
                    OnboardingScreen(
                        viewModel = onboardingViewModel,
                        onOnboardingComplete = {
                            navController.navigate(Route.CHAT) {
                                popUpTo(Route.ONBOARDING) { inclusive = true }
                            }
                        },
                        onOpenSettings = { navController.navigate(Route.SETTINGS) { launchSingleTop = true } }
                    )
                }
                composable(Route.CHAT) {
                    ChatScreen(viewModel = chatViewModel, navController = navController)
                }
                composable(Route.HISTORY) {
                    HistoryScreen(viewModel = chatViewModel, navController = navController)
                }
                composable(Route.SETTINGS) {
                    SettingsScreen(navController = navController)
                }
                composable(Route.API_SETTINGS) {
                    ApiSettingsScreen(navController = navController)
                }
            }
        }
      }
    }
  }
}
