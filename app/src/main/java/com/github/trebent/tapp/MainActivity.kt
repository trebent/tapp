package com.github.trebent.tapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.github.trebent.tapp.ui.theme.TappTheme


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        var i: Int = 0
        splash.setKeepOnScreenCondition {
            i++
            i != 20
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TappTheme { Main() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Main(authViewModel: AuthViewModel = viewModel()) {
    val startDestination = if (authViewModel.isLoggedIn()) "home" else "login"
    val navController = rememberNavController()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = {
                Text(text = stringResource(id = R.string.app_name))
            })
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            composable("home") { HomeScreenRoute(authViewModel) { navController.navigate("login") } }
            composable("login") { LoginScreenRoute(authViewModel) { navController.navigate("home") } }
        }
    }
}

