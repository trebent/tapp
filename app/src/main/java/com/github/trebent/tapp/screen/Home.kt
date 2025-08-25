package com.github.trebent.tapp.screen

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.trebent.tapp.R
import com.github.trebent.tapp.viewmodel.AuthViewModel
import com.github.trebent.tapp.viewmodel.TappGroup
import com.github.trebent.tapp.viewmodel.TappGroupViewModel
import com.github.trebent.tapp.viewmodel.newTappGroup
import com.github.trebent.tapp.viewmodel.testGroups
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


@ExperimentalMaterial3Api
@Composable
fun HomeScreenRoute(authViewModel: AuthViewModel,
                    tappGroupViewModel: TappGroupViewModel,
                    openGroup: (tappGroup: TappGroup) -> Unit,
                    onLogout: () -> Unit) {
    Log.i("Home", "home route")

    HomeScreen(
        tappGroupViewModel.groups,
        openGroup,
        { authViewModel.logout() },
        onLogout,
    )
}

@ExperimentalMaterial3Api
@Composable
fun HomeScreen(groups: StateFlow<List<TappGroup>>,
               openGroup: (tappGroup: TappGroup) -> Unit,
               logout: () -> Unit,
               onLogout: () -> Unit) {
    Log.i("Home", "rendering HomeScreen")
    val items by groups.collectAsState()
    
    Scaffold(
        topBar = { TopAppBar(
            title = {
                Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
            },
            actions = {
                IconButton(onClick = {
                    Log.i("HomeScreen", "clicked account icon")
                }) {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = "Go to account"
                    )
                }
            }
        )},
        floatingActionButton = {
            FloatingActionButton(onClick = {
                Log.i("HomeScreen", "clicked main create group FAB")
                openGroup(newTappGroup)
            }) {
                Icon(
                    imageVector = Icons.Filled.AddCircle,
                    contentDescription = "Go to account"
                )
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(horizontal = 16.dp)) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                verticalArrangement = Arrangement.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                items(items, span = { GridItemSpan(4) }) { tappGroup ->
                    TappGroupRow(tappGroup, openGroup)
                }
            }

        }
    }
}

@Composable
fun TappGroupRow(tappGroup: TappGroup, openGroup: (tappGroup: TappGroup) -> Unit) {
    OutlinedButton(
        shape = RoundedCornerShape(8.dp),
        onClick = {
            Log.i("HomeScreen", "opening group ${tappGroup.id}: ${tappGroup.name}")
            openGroup(tappGroup)
        },
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Text(tappGroup.name)
    }
}

@ExperimentalMaterial3Api
@Preview
@Composable
fun HomeScreenPreview() {
    HomeScreen(
        MutableStateFlow(testGroups), {}, {}, {}
    )
}
