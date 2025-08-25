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
import com.github.trebent.tapp.viewmodel.TappGroup
import com.github.trebent.tapp.viewmodel.TappGroupViewModel
import com.github.trebent.tapp.viewmodel.newTappGroup
import com.github.trebent.tapp.viewmodel.testGroups
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


@ExperimentalMaterial3Api
@Composable
fun HomeScreenRoute(
    tappGroupViewModel: TappGroupViewModel,
    goToGroup: (TappGroup) -> Unit,
    goToAccount: () -> Unit,
) {
    Log.i("HomeScreenRoute", "navigated")

    HomeScreen(
        tappGroupViewModel.groups,
        goToGroup,
        goToAccount,
    )
}

@ExperimentalMaterial3Api
@Composable
fun HomeScreen(
    groups: StateFlow<List<TappGroup>>,
    goToGroup: (tappGroup: TappGroup) -> Unit,
    goToAccount: () -> Unit,
) {
    Log.i("HomeScreen", "rendering")
    val items by groups.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = {
                        Log.i("HomeScreen", "clicked account icon")
                        goToAccount()
                    }) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = "Go to account"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                Log.i("HomeScreen", "clicked main create group FAB")
                goToGroup(newTappGroup)
            }) {
                Icon(
                    imageVector = Icons.Filled.AddCircle,
                    contentDescription = "Go to account"
                )
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                verticalArrangement = Arrangement.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                items(items, span = { GridItemSpan(4) }) { tappGroup ->
                    TappGroupRow(tappGroup, goToGroup)
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
        MutableStateFlow(testGroups), {}, {},
    )
}
