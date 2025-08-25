package com.github.trebent.tapp.screen

import android.util.Log
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.trebent.tapp.viewmodel.TappGroup
import com.github.trebent.tapp.viewmodel.TappGroupViewModel
import com.github.trebent.tapp.viewmodel.newTappGroup
import com.github.trebent.tapp.viewmodel.testGroup


@Composable
fun EditTappGroupScreenRoute(tappGroupViewModel: TappGroupViewModel, lookupId: Int, goBack: () -> Unit) {
    val new = lookupId == 0
    var actualTappGroup: TappGroup = newTappGroup
    if (!new) {
        actualTappGroup = tappGroupViewModel.get(lookupId)
        actualTappGroup.edit = true
    }
    EditTappGroupScreen(
        new,
        actualTappGroup,
        { tg -> tappGroupViewModel.save(tg) },
        { tg -> tappGroupViewModel.delete(tg) },
        goBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTappGroupScreen(
    new: Boolean,
    tappGroup: TappGroup,
    saveGroup: (tappGroup: TappGroup) -> Unit,
    deleteGroup: (tappGroup: TappGroup) -> Unit,
    goBack: () -> Unit
) {
    var name by remember { mutableStateOf(tappGroup.name) }
    var description by remember { mutableStateOf(tappGroup.description) }
    var emoji by remember { mutableStateOf(tappGroup.emoji) }

    var nameError by remember { mutableStateOf(false) }
    var descriptionError by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (new) "Create a new group" else "Editing ${tappGroup.name}",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        Log.i("EditTappGroupScreen", "clicked the back button")
                        tappGroup.edit = false
                        goBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go to account"
                        )
                    }
                },
                actions = {
                    if (!new) {
                        IconButton(onClick = {
                            Log.i("EditTappGroupScreen", "clicked the delete button")
                            deleteGroup(tappGroup)
                            goBack()
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Go to account"
                            )
                        }
                    }
                }
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.padding(padding),
        ) {
            item(span = { GridItemSpan(4) }) {
                TextField(
                    value = name,
                    label = { Text("* Name") },
                    placeholder = { Text("Enter a group name") },
                    isError = nameError,
                    onValueChange = { v: String ->
                        name = v
                        nameError = false
                        Log.i("EditTappGroupScreen", "entered text in name field: $name")
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            if (nameError) {
                item(span = { GridItemSpan(4) }) {
                    Text(
                        text = "name is required to create the group",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
            item(span = { GridItemSpan(4) }) {
                TextField(
                    value = name,
                    label = { Text("Description") },
                    placeholder = { Text("Enter a group description") },
                    isError = descriptionError,
                    onValueChange = { v: String ->
                        description = v
                        Log.i("EditTappGroupScreen", "entered text in description field: $name")
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            item(span = { GridItemSpan(4) }) {
                Row {
                    Text(emoji.ifEmpty { "Pick an emoji" })

                    OutlinedTextField(
                        value = emoji,
                        singleLine = true,
                        onValueChange = { emoji = it },
                        placeholder = { Text("Select an emoji") }
                    )
                }
            }
            item(span = { GridItemSpan(4) }) {
                Button(
                    shape = RoundedCornerShape(8.dp),
                    onClick = {
                        Log.i("EditTappGroupScreen", "clicked to save the group")
                        if (name.length == 0) {
                            Log.e("EditTappGroupScreen", "group name was invalid")
                            nameError = true
                        } else {
                            saveGroup(TappGroup(tappGroup.id, name, emoji, description, false))
                            goBack()
                        }
                    }
                ) {
                    Text("Save")
                }
            }
        }
    }
}

@Composable
fun TappGroupScreenRoute(
    tappGroupViewModel: TappGroupViewModel,
    lookupTappGroup: TappGroup,
    editGroup: (tappGroup: TappGroup) -> Unit,
    goBack: () -> Unit
) {
    val actualTappGroup = tappGroupViewModel.get(lookupTappGroup.id)
    TappGroupScreen(actualTappGroup, editGroup, goBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TappGroupScreen(
    tappGroup: TappGroup,
    editGroup: (tappGroup: TappGroup) -> Unit,
    goBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
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
                        Log.i("TappGroupScreen", "clicked edit button")
                        tappGroup.edit = true
                        editGroup(tappGroup)
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit tapp group"
                        )
                    }
                }
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.padding(padding),
        ) {
            item(span = { GridItemSpan(4) }) {
                Text("Tapp it!", style = MaterialTheme.typography.titleMedium)
            }
            item(span = { GridItemSpan(4) }) {
                Button(onClick = {
                    Log.i("TappGroupScreen", "tapping group ${tappGroup.id}: ${tappGroup.name}")
                }, shape = RoundedCornerShape(8.dp)) {
                    Text(tappGroup.emoji)
                }
            }
            if (!tappGroup.description.isEmpty()) {
                item(span = { GridItemSpan(4) }) {
                    Text(
                        tappGroup.description,
                    )
                }
            }
            item(span = { GridItemSpan(4) }) {
                Text("Tapps", style = MaterialTheme.typography.titleMedium)
            }
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
    EditTappGroupScreen(true, newTappGroup, {}, {}, {})
}

@Preview
@Composable
fun EditGroupScreenPreview() {
    EditTappGroupScreen(false, testGroup, {}, {}, {})
}