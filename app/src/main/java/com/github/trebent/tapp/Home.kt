package com.github.trebent.tapp

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview


@Composable
fun HomeScreenRoute(authViewModel: AuthViewModel, onLogout: () -> Unit) {
    HomeScreen({ authViewModel.logout() }, onLogout)
}

@Composable
fun HomeScreen(logout: () -> Unit, onLogout: () -> Unit) {
    Column {
        Text(text = "this is the home view")
        Button(onClick = {
            Log.i("HomeScreen", "clicked logout button")
            logout()
            onLogout()
        }) { Text(text = "Log out") }
    }
}

@Preview
@Composable
fun HomeScreenPreview() {
    HomeScreen({}, {})
}
