package com.dc.checkinbb

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dc.checkinbb.navigation.AppRoutes
import com.dc.checkinbb.ui.screens.FullHistoryScreen
import com.dc.checkinbb.ui.screens.MainFeedingScreen
import com.dc.checkinbb.ui.theme.CheckInBBTheme
import com.dc.checkinbb.viewmodel.FeedingViewModel
import com.dc.checkinbb.viewmodel.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted
        } else {
            // Explain to the user that the feature is unavailable because the features requires a permission that the user has denied.
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val currentTheme by themeViewModel.currentTheme.collectAsState()

            CheckInBBTheme(appTheme = currentTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val feedingViewModel: FeedingViewModel = hiltViewModel()

                    NavHost(
                        navController = navController,
                        startDestination = AppRoutes.MAIN
                    ) {
                        composable(AppRoutes.MAIN) {
                            MainFeedingScreen(
                                feedingViewModel = feedingViewModel,
                                themeViewModel = themeViewModel,
                                onOpenFullHistory = {
                                    navController.navigate(AppRoutes.FULL_HISTORY)
                                }
                            )
                        }
                        composable(AppRoutes.FULL_HISTORY) {
                            FullHistoryScreen(
                                feedingViewModel = feedingViewModel,
                                onClose = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
