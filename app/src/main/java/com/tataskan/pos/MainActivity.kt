package com.tataskan.pos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tataskan.pos.ui.auth.AuthViewModel
import com.tataskan.pos.ui.navigation.TataskanNavKey
import com.tataskan.pos.ui.navigation.TataskanNavigation
import com.tataskan.pos.ui.theme.TataskanTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val authViewModel: AuthViewModel = viewModel()
            val isAuthLoading by authViewModel.isAuthLoading.collectAsState()

            // Keep the splash screen on screen until the auth state is resolved
            splashScreen.setKeepOnScreenCondition { isAuthLoading }

            TataskanTheme {
                val permissionsState = rememberMultiplePermissionsState(
                    permissions = mutableListOf(
                        android.Manifest.permission.CAMERA,
                    ).apply {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            add(android.Manifest.permission.READ_MEDIA_IMAGES)
                        } else {
                            add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                            add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                    }
                )

                LaunchedEffect(Unit) {
                    if (!permissionsState.allPermissionsGranted) {
                        permissionsState.launchMultiplePermissionRequest()
                    }
                }

                val backStack = remember { mutableStateListOf<TataskanNavKey>(TataskanNavKey.Splash) }
                
                TataskanNavigation(
                    backStack = backStack,
                    authViewModel = authViewModel,
                    windowSizeClass = windowSizeClass
                )
            }
        }
    }
}

