package com.github.trebent.tapp

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview


@Composable
fun EditTappGroupScreenRoute(groupViewModel: GroupViewModel, lookupId: Int, goBack: () -> Unit) {
    val new = lookupId == 0
    var actualTappGroup: TappGroup = newTappGroup
    if (!new) {
        actualTappGroup = groupViewModel.get(lookupId)
        actualTappGroup.edit = true
    }
    EditTappGroupScreen(new, actualTappGroup, goBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTappGroupScreen(new: Boolean, tappGroup: TappGroup, goBack: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(
            title = {
                Text(text = if (new) "Create a new group" else "Editing ${tappGroup.name}", style = MaterialTheme.typography.titleLarge)
            },
            navigationIcon = {
                IconButton(onClick = {
                    Log.i("TappGroupScreen", "clicked the back button")
                    tappGroup.edit = false
                    goBack()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Go to account"
                    )
                }
            },
        )},
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Text("this is the tapp group creating/editing screen")
        }
    }
}

@Composable
fun TappGroupScreenRoute(groupViewModel: GroupViewModel,
                         lookupTappGroup: TappGroup,
                         editGroup: (tappGroup: TappGroup) -> Unit,
                         goBack: () -> Unit) {
    val actualTappGroup = groupViewModel.get(lookupTappGroup.id)
    TappGroupScreen(actualTappGroup, editGroup, goBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TappGroupScreen(tappGroup: TappGroup,
                    editGroup: (tappGroup: TappGroup) -> Unit,
                    goBack: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(
            title = {
                Text(text = tappGroup.name, style = MaterialTheme.typography.titleLarge)
            },
            navigationIcon = {
                IconButton(onClick = {
                    Log.i("TappGroupScreen", "clicked the back button")
                    goBack()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Go to account"
                    )
                }
            },
            actions = {
                IconButton(onClick = {
                    Log.i("HomeScreen", "clicked edit button")
                    tappGroup.edit = true
                    editGroup(tappGroup)
                }) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit tapp group"
                    )
                }
            }
        )},
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Text("this is the tapp group viewing screen")
        }
    }
}

@Preview
@Composable
fun TappGroupScreenPreview() {
    TappGroupScreen(testGroup, {}, {})
}

@Preview
@Composable
fun NewGroupScreenPreview() {
    EditTappGroupScreen(true, newTappGroup, {})
}

@Preview
@Composable
fun EditGroupScreenPreview() {
    EditTappGroupScreen(false, testGroup, {})
}