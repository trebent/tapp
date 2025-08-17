package com.github.trebent.tapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
            modifier = Modifier.padding(padding)
        ) {
            composable("home") { HomeScreen(authViewModel) { navController.navigate("login") } }
            composable("login") { LoginScreen(authViewModel) { navController.navigate("home") } }
        }
    }
}

@Composable
fun LoginScreen(authViewModel: AuthViewModel, onLogin: () -> Unit) {
    Column {
        Text(text = "this is the login view")
        Button(onClick = {
            Log.i("LoginScreen", "clicked login button")
            authViewModel.login("u", "p")
            onLogin()
        }) { Text(text = "Log in") }
    }

}

@Composable
fun HomeScreen(authViewModel: AuthViewModel, onLogout: () -> Unit) {
    Column {
        Text(text = "this is the home view")
        Button(onClick = {
            Log.i("HomeScreen", "clicked logout button")
            authViewModel.logout()
            onLogout()
        }) { Text(text = "Log out") }
    }
}
