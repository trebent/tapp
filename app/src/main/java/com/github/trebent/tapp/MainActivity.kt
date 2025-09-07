package com.github.trebent.tapp

import android.content.Context
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.github.trebent.tapp.api.TappGroup
import com.github.trebent.tapp.screen.AccountScreenRoute
import com.github.trebent.tapp.screen.EditTappGroupScreenRoute
import com.github.trebent.tapp.screen.HomeScreenRoute
import com.github.trebent.tapp.screen.LoginScreenRoute
import com.github.trebent.tapp.screen.SignupScreenRoute
import com.github.trebent.tapp.screen.TappGroupScreenRoute
import com.github.trebent.tapp.ui.theme.TappTheme
import com.github.trebent.tapp.viewmodel.AccountViewModel
import com.github.trebent.tapp.viewmodel.TappGroupViewModel


val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
val tokenkey = stringPreferencesKey("auth_token")
val emailkey = stringPreferencesKey("email")


class MainActivity : ComponentActivity() {

    private val accountViewModel: AccountViewModel by viewModels()
    private val tappGroupViewModel: TappGroupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Keep the splash as long as the auth view model needs to do its cloud sync.
        val splash = installSplashScreen()
        splash.setKeepOnScreenCondition {
            !accountViewModel.initialised.value && !tappGroupViewModel.initialised.value
        }

        tappGroupViewModel.setTokenGetter({ accountViewModel.getToken() })
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TappTheme {
                val authReady by accountViewModel.initialised.collectAsState()
                val groupsReady by tappGroupViewModel.initialised.collectAsState()

                if (!authReady || !groupsReady) {
                    // Show nothing (SplashScreen still visible)
                    Box(modifier = Modifier.fillMaxSize()) {}
                } else {
                    // Only show your main UI after both view models are ready
                    Main(accountViewModel, tappGroupViewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Main(accountViewModel: AccountViewModel, tappGroupViewModel: TappGroupViewModel) {
    Log.i("Main", "main compose entrypoint")
    val navController = rememberNavController()

    val goBack: () -> Unit = { navController.popBackStack() }
    val goBackHome: () -> Unit =
        { navController.navigate("home") { popUpTo("home") { inclusive = true } } }
    val goToLogin: () -> Unit =
        { navController.navigate("login") { popUpTo(0) { inclusive = true } } }
    val goToAccount: () -> Unit = { navController.navigate("account") }
    val goToViewGroup: (TappGroup) -> Unit = { tg ->
        tappGroupViewModel.selectGroup(tg)
        navController.navigate("viewGroup")
    }
    val goToEditGroup: (TappGroup) -> Unit = { tg ->
        tappGroupViewModel.selectGroup(tg)
        navController.navigate("editGroup")
    }

    val isLoggedIn = accountViewModel.isLoggedIn.collectAsState()
    val startDest = if (isLoggedIn.value) "home" else "login"

    Log.i("Main", "start destination set to '${startDest}'")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDest,
            modifier = Modifier
                .padding(padding)
        ) {
            composable("splash") { SplashScreen() }
            composable("account") {
                AccountScreenRoute(accountViewModel, goToLogin, goBack)
            }
            composable("home") {
                HomeScreenRoute(
                    tappGroupViewModel,
                    goToViewGroup,
                    goToEditGroup,
                    goToAccount,
                )
            }
            composable("editGroup") {
                EditTappGroupScreenRoute(
                    tappGroupViewModel,
                    goBack,
                    goBackHome,
                )
            }
            composable("viewGroup") {
                TappGroupScreenRoute(
                    accountViewModel,
                    tappGroupViewModel,
                    goToEditGroup,
                    goBack,
                )
            }
            composable("login") {
                LoginScreenRoute(
                    accountViewModel,
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
                    accountViewModel,
                    { navController.navigate("login") }
                ) { navController.navigate("login") }
            }
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
