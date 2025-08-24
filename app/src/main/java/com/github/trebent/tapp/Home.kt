package com.github.trebent.tapp

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview


@Composable
fun HomeScreenRoute(authViewModel: AuthViewModel, onLogout: () -> Unit) {
    Log.i("Home", "home route")

    HomeScreen({ authViewModel.logout() }, onLogout)
}

@Composable
fun HomeScreen(logout: () -> Unit, onLogout: () -> Unit) {
    Log.i("Home", "rending HomeScreen")

    Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
        LazyVerticalGrid(
            modifier = Modifier.padding(paddingValues),
            columns = GridCells.Fixed(4),
            verticalArrangement = Arrangement.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
        }
    }
}

@Preview
@Composable
fun HomeScreenPreview() {
    HomeScreen({}, {})
}
