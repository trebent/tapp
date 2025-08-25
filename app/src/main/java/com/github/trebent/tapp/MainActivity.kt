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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.github.trebent.tapp.screen.AccountScreenRoute
import com.github.trebent.tapp.screen.EditTappGroupScreenRoute
import com.github.trebent.tapp.screen.HomeScreenRoute
import com.github.trebent.tapp.screen.LoginScreenRoute
import com.github.trebent.tapp.screen.SignupScreenRoute
import com.github.trebent.tapp.screen.TappGroupScreenRoute
import com.github.trebent.tapp.ui.theme.TappTheme
import com.github.trebent.tapp.viewmodel.AuthViewModel
import com.github.trebent.tapp.viewmodel.TappGroup
import com.github.trebent.tapp.viewmodel.TappGroupViewModel


class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val tappGroupViewModel: TappGroupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Keep the splash as long as the auth view model needs to do its cloud sync.
        val splash = installSplashScreen()
        splash.setKeepOnScreenCondition {
            !authViewModel.initialised.value && !tappGroupViewModel.initialised.value
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TappTheme { Main(authViewModel, tappGroupViewModel) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Main(authViewModel: AuthViewModel, tappGroupViewModel: TappGroupViewModel) {
    Log.i("Main", "main compose entrypoint")
    val navController = rememberNavController()

    val isLoggedIn = authViewModel.isLoggedIn()

    val goBack: () -> Unit = { navController.popBackStack() }
    val goBackHome: () -> Unit =
        { navController.navigate("home") { popUpTo("home") { inclusive = true } } }
    val goToLogin: () -> Unit =
        { navController.navigate("login") { popUpTo(0) { inclusive = true } } }
    val goToAccount: () -> Unit = { navController.navigate("account") }
    val goToGroup: (TappGroup) -> Unit = { tg -> navController.navigate(tg) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = if (isLoggedIn) "home" else "login",
            modifier = Modifier
                .padding(padding)
        ) {
            composable("splash") { SplashScreen() }
            composable("account") {
                AccountScreenRoute(authViewModel, goToLogin, goBack)
            }
            composable("home") {
                HomeScreenRoute(
                    authViewModel,
                    tappGroupViewModel,
                    goToGroup,
                    goToAccount,
                    goToLogin,
                )
            }
            composable<TappGroup> { nbse ->
                val lookupTappGroup: TappGroup = nbse.toRoute()

                if (lookupTappGroup.edit) {
                    EditTappGroupScreenRoute(
                        tappGroupViewModel,
                        lookupTappGroup.id,
                        goBack,
                        goBackHome,
                    )
                } else {
                    TappGroupScreenRoute(
                        tappGroupViewModel,
                        lookupTappGroup,
                        goToGroup,
                        goBack,
                    )
                }
            }
            composable("login") {
                LoginScreenRoute(
                    authViewModel,
                    { navController.navigate("signup") },
                    {
                        navController.navigate("home") {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
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
