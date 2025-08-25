package com.github.trebent.tapp

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@ExperimentalMaterial3Api
@Composable
fun AccountScreenRoute(authViewModel: AuthViewModel, onLogout: () -> Unit, goBack: () -> Unit) {
    AccountScreen({ authViewModel.logout() }, onLogout, goBack)
}

@ExperimentalMaterial3Api
@Composable
fun AccountScreen(logout: () -> Unit, onLogout: () -> Unit, goBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Account settings")
                },
                navigationIcon = {
                    IconButton(onClick = {
                        Log.i("AccountScreen", "clicked the back button")
                        goBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go to account"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Text("This is account settings")
            Button(onClick = {
                Log.i("AccountScreen", "clicked the logout button")
                logout()
                onLogout()
            }, shape = RoundedCornerShape(8.dp)) {
                Text("Log out")
            }
        }
    }
}

@ExperimentalMaterial3Api
@Preview
@Composable
fun AccountScreenPreview() {
    AccountScreen({}, {}, {})
}
