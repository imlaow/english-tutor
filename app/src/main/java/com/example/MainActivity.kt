package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.AppDatabase
import com.example.manager.TtsManager
import com.example.ui.ApiProfileEditScreen
import com.example.ui.ApiProfileListScreen
import com.example.ui.ChatScreen
import com.example.ui.ChatViewModel
import com.example.ui.ChatViewModelFactory
import com.example.ui.HistoryScreen
import com.example.ui.OnboardingScreen
import com.example.ui.Route
import com.example.ui.SettingsScreen
import com.example.ui.TopicProviderScreen
import com.example.ui.TtsProfileEditScreen
import com.example.ui.TtsProfileListScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ApiProfileViewModel
import com.example.viewmodel.ApiProfileViewModelFactory
import com.example.viewmodel.OnboardingViewModel
import com.example.viewmodel.OnboardingViewModelFactory
import com.example.viewmodel.TopicsViewModel
import com.example.viewmodel.TopicsViewModelFactory
import com.example.viewmodel.TtsProfileViewModel
import com.example.viewmodel.TtsProfileViewModelFactory

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // Both bars sit on cream: the top bar surface runs up under the status bar and
    // the mic dock runs down under the gesture handle, so both need dark icons.
    // Pinned rather than left on auto() — the theme has no dark variant to follow.
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
    )

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
            // Shared by the screens that only observe the provider list; the edit
            // form keeps its own so its draft starts empty on each navigation.
            val apiProfileViewModel: ApiProfileViewModel = viewModel(
                factory = ApiProfileViewModelFactory(applicationContext)
            )
            val ttsProfileViewModel: TtsProfileViewModel = viewModel(
                factory = TtsProfileViewModelFactory(applicationContext)
            )

            // TtsManager holds no configuration of its own, so the composition
            // root keeps it pointed at the profile in effect. Done here rather
            // than in ChatScreen because playback outlives that destination:
            // leaving the chat stops an utterance, it does not unconfigure it.
            val speechProfile by ttsProfileViewModel.activeProfile.collectAsState()
            LaunchedEffect(speechProfile) {
                TtsManager.configure(speechProfile?.toConfig())
            }

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
                    // Scoped to the chat back-stack entry so topics regenerate once
                    // per launch, not when returning from Settings/History.
                    val topicsViewModel: TopicsViewModel = viewModel(
                        factory = TopicsViewModelFactory(applicationContext)
                    )
                    ChatScreen(
                        viewModel = chatViewModel,
                        apiProfileViewModel = apiProfileViewModel,
                        topicsViewModel = topicsViewModel,
                        navController = navController
                    )
                }
                composable(Route.HISTORY) {
                    HistoryScreen(viewModel = chatViewModel, navController = navController)
                }
                composable(Route.SETTINGS) {
                    SettingsScreen(navController = navController)
                }
                composable(Route.API_PROFILES) {
                    ApiProfileListScreen(
                        viewModel = apiProfileViewModel,
                        navController = navController
                    )
                }
                composable(Route.TOPIC_PROVIDER) {
                    TopicProviderScreen(
                        viewModel = apiProfileViewModel,
                        navController = navController
                    )
                }
                composable(Route.TTS_PROFILES) {
                    TtsProfileListScreen(
                        viewModel = ttsProfileViewModel,
                        navController = navController
                    )
                }
                composable(
                    route = Route.API_PROFILE_EDIT,
                    arguments = listOf(
                        navArgument(Route.PROFILE_ID_ARG) {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { backStackEntry ->
                    val editViewModel: ApiProfileViewModel = viewModel(
                        factory = ApiProfileViewModelFactory(applicationContext)
                    )
                    ApiProfileEditScreen(
                        viewModel = editViewModel,
                        navController = navController,
                        profileId = backStackEntry.arguments?.getString(Route.PROFILE_ID_ARG)
                    )
                }
                composable(
                    route = Route.TTS_PROFILE_EDIT,
                    arguments = listOf(
                        navArgument(Route.PROFILE_ID_ARG) {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { backStackEntry ->
                    // Its own instance, for the same reason the API edit form has
                    // one: the draft must start empty on each navigation.
                    val editViewModel: TtsProfileViewModel = viewModel(
                        factory = TtsProfileViewModelFactory(applicationContext)
                    )
                    TtsProfileEditScreen(
                        viewModel = editViewModel,
                        navController = navController,
                        profileId = backStackEntry.arguments?.getString(Route.PROFILE_ID_ARG)
                    )
                }
            }
        }
      }
    }
  }
}
