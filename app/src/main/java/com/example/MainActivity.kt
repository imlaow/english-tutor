package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.ChatScreen
import com.example.ui.ChatViewModel
import com.example.ui.HistoryScreen
import com.example.ui.Route
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    setContent {
      MyApplicationTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val navController = rememberNavController()
            val chatViewModel: ChatViewModel = viewModel()

            NavHost(
                navController = navController,
                startDestination = Route.CHAT
            ) {
                composable(Route.CHAT) {
                    ChatScreen(viewModel = chatViewModel, navController = navController)
                }
                composable(Route.HISTORY) {
                    HistoryScreen(viewModel = chatViewModel, navController = navController)
                }
            }
        }
      }
    }
  }
}
