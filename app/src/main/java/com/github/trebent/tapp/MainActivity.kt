package com.github.trebent.tapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.github.trebent.tapp.ui.theme.TappTheme


class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Keep the splash as long as the auth view model needs to do its cloud sync.
        val splash = installSplashScreen()
        splash.setKeepOnScreenCondition {
            !authViewModel.initialised.value
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TappTheme { Main(authViewModel) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Main(authViewModel: AuthViewModel) {
    Log.i("Main", "main compose entrypoint")
    val navController = rememberNavController()

    val isLoggedIn = authViewModel.isLoggedIn()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = if (isLoggedIn) "home" else "login",
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            composable("splash") { SplashScreen() }
            composable("home") { HomeScreenRoute(authViewModel) { navController.navigate("login") } }
            composable("login") {
                LoginScreenRoute(
                    authViewModel,
                    { navController.navigate("signup") }
                ) { navController.navigate("home") }
            }
            composable("signup") {
                SignupScreenRoute(
                    authViewModel,
                    { navController.navigate("login") }
                ) { navController.navigate("login") }
            }
        }
    }

    // One-off navigation post NavHost graph-setting and view model initialisation.
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            navController.navigate("home")
        } else {
            navController.navigate("login")
        }
    }
}

@Composable
fun SplashScreen() {
    Box(modifier = Modifier.fillMaxSize()) {}
}

@Preview
@Composable
fun SplashScreenPreview() {
    SplashScreen()
}
