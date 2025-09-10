/**
 * The main and only activity.
 */
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
import com.github.trebent.tapp.screen.HomeScreenRoute
import com.github.trebent.tapp.screen.LoginScreenRoute
import com.github.trebent.tapp.screen.SignupScreenRoute
import com.github.trebent.tapp.screen.tappgroup.EditTappGroupScreenRoute
import com.github.trebent.tapp.screen.tappgroup.TappGroupScreenRoute
import com.github.trebent.tapp.ui.theme.TappTheme
import com.github.trebent.tapp.viewmodel.AccountViewModel
import com.github.trebent.tapp.viewmodel.TappGroupViewModel


// Set the data store in the global context to make it accessible.
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

// Two data store keys, one for the backend token, and one for the user email.
val tokenkey = stringPreferencesKey("auth_token")

// The email preference is used to link the device's FCM with the user account (if the user is signed in).
// It's also used to fetch account information on start.
val emailkey = stringPreferencesKey("email")


/**
 * Main activity
 *
 * @constructor Create empty Main activity
 */
class MainActivity : ComponentActivity() {
    // The Tapp view models.
    private val accountViewModel: AccountViewModel by viewModels()
    private val tappGroupViewModel: TappGroupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Keep the splash as long as the auth view model needs to do its cloud sync.
        val splash = installSplashScreen()
        splash.setKeepOnScreenCondition {
            !accountViewModel.initialised.value && !tappGroupViewModel.initialised.value
        }

        // Ensure the group view model always uses the most up to date token, rely on the account
        // view model for this. Preference handling is a bit tricky, this felt cleaner.
        tappGroupViewModel.setTokenGetter({ accountViewModel.getToken() })
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TappTheme {
                // Because the on screen condition for the splash runs in the background, prevent
                // rendering the login page (for a logged in user) and display an empty box instead.
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

/**
 * Main composable, providing the main navigation entrypoint.
 *
 * @param accountViewModel
 * @param tappGroupViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Main(accountViewModel: AccountViewModel, tappGroupViewModel: TappGroupViewModel) {
    Log.i("Main", "main compose entrypoint")
    val navController = rememberNavController()

    // navigation actions, to keep track of navController calls and keep a central way of controlling
    // the way the user is allowed to navigate. Popping the nav stack is done here too, to not allow
    // confusing backtracking to signup, login, home and such.
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

    // Used to set the start destination.
    val isLoggedIn = accountViewModel.isLoggedIn.collectAsState()
    val startDest = if (isLoggedIn.value) "home" else "login"

    Log.i("Main", "start destination set to '${startDest}'")

    /*
    An important note here is that the navigation map MUST ONLY CONTAIN "ROUTE" composables. Route
    composables are allowed to accept view model references, screen composables are not. This is to
    allow an easier time of rendering previews in each sub-screen section. View models are tricky
    to pass references to for previewable components. Route-composables are basically one level
    above the screen they're targetting, creating simpler lambdas wrapping viewmodel calls.
    */
    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDest,
            modifier = Modifier
                .padding(padding)
        ) {
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
                    { navController.navigate("login") },
                    { navController.navigate("login") }
                )
            }
        }
    }
}
