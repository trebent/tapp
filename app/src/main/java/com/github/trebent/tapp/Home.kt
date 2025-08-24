package com.github.trebent.tapp

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.Group
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


@Composable
fun HomeScreenRoute(authViewModel: AuthViewModel,
                    groupViewModel: GroupViewModel,
                    onLogout: () -> Unit) {
    Log.i("Home", "home route")

    HomeScreen(groupViewModel.groups, { groupViewModel.createGroup(TappGroup("new")) },{ authViewModel.logout() }, onLogout)
}

@Composable
fun HomeScreen(groups: StateFlow<List<TappGroup>>, addGroup: () -> Unit, logout: () -> Unit, onLogout: () -> Unit) {
    Log.i("Home", "rendering HomeScreen")
    val items by groups.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Button(onClick = { addGroup() }) {
            Text(text = "Test add")
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            verticalArrangement = Arrangement.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            items(items, span = { GridItemSpan(4) }) { tappGroup ->
                TappGroupRow(tappGroup)
            }
        }
    }
}

@Composable
fun TappGroupRow(tappGroup: TappGroup) {
    Text(text = tappGroup.name, modifier = Modifier.padding(bottom = 8.dp))
}

@Preview
@Composable
fun HomeScreenPreview() {
    HomeScreen(
        MutableStateFlow(
            listOf(
                TappGroup("group1"),
                TappGroup("group2"),
                TappGroup("group3"),
                TappGroup("group4"),
                TappGroup("group5"),
                TappGroup("group6"),
                TappGroup("group7"),
                TappGroup("group8"),
                TappGroup("group9"),
                TappGroup("group10"),
            )
        ), {}, {}, {})
}
