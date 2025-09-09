/**
 * This file contains all group-edit related logic that is needed to render the UI.
 */

package com.github.trebent.tapp.screen.tappgroup

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.github.trebent.tapp.api.TappGroup
import com.github.trebent.tapp.viewmodel.TappGroupViewModel
import com.github.trebent.tapp.viewmodel.newTappGroup
import com.github.trebent.tapp.viewmodel.testGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


/**
 * The route target when navigating to edit a Tapp group.
 */
@Composable
fun EditTappGroupScreenRoute(
    tappGroupViewModel: TappGroupViewModel,
    goBack: () -> Unit,
    goBackHome: () -> Unit
) {
    Log.i("EditTappGroupScreenRoute", "navigated")

    EditTappGroupScreen(
        tappGroupViewModel.selectedGroup,
        { tg, s, f -> tappGroupViewModel.save(tg, s, f) },
        { tg, s, f -> tappGroupViewModel.delete(tg, s, f) },
        goBack,
        goBackHome,
    )
}

/**
 * Main entrypoint screen for when editing a tapp group. This renders controls for a group owner
 * to edit the group. A non-owner won't be able to navigate here to edit, but will be able to
 * navigate here when creating a NEW group.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTappGroupScreen(
    tappGroupStateFlow: StateFlow<TappGroup>,
    saveGroup: (tappGroup: TappGroup, onSuccess: () -> Unit, onFailure: () -> Unit) -> Unit,
    deleteGroup: (tappGroup: TappGroup, onSuccess: () -> Unit, onFailure: () -> Unit) -> Unit,
    goBack: () -> Unit,
    goBackHome: () -> Unit
) {
    Log.i("EditTappGroupScreen", "rendering")

    val context = LocalContext.current

    val selectedGroup = tappGroupStateFlow.collectAsState()
    val new = selectedGroup.value.id == 0

    var name by rememberSaveable { mutableStateOf(selectedGroup.value.name) }
    var description by rememberSaveable { mutableStateOf(selectedGroup.value.description ?: "") }
    var emoji by rememberSaveable { mutableStateOf(selectedGroup.value.emoji) }

    var nameError by rememberSaveable { mutableStateOf(false) }
    var descriptionError by rememberSaveable { mutableStateOf(false) }

    var showSelectEmojiDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteGroupDialog by rememberSaveable { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (new) "Create a new group" else "Editing ${selectedGroup.value.name}",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        Log.i("EditTappGroupScreen", "clicked the back button")
                        goBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back"
                        )
                    }
                },
                actions = {
                    if (!new) {
                        IconButton(onClick = {
                            Log.i("EditTappGroupScreen", "clicked the delete button")
                            showDeleteGroupDialog = true
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete the tapp group"
                            )
                        }
                    }
                }
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp),
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
                        text = "the group name must be at least 3 characters long",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
            item(span = { GridItemSpan(4) }) {
                TextField(
                    value = description,
                    label = { Text("Description") },
                    placeholder = { Text("Enter a group description") },
                    isError = descriptionError,
                    onValueChange = { v: String ->
                        description = v
                        descriptionError = description.length > 250
                        Log.i(
                            "EditTappGroupScreen",
                            "entered text in description field: $description"
                        )
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            if (nameError) {
                item(span = { GridItemSpan(4) }) {
                    Text(
                        text = "description is too long, it must be maximum 250 characters",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
            item(span = { GridItemSpan(2) }) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (emoji.isEmpty()) {
                        Text("", fontSize = 32.sp, modifier = Modifier.padding(bottom = 8.dp))
                    } else {
                        Text(
                            emoji,
                            fontSize = 32.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    Text("Selected emoji", style = MaterialTheme.typography.bodySmall)
                }
            }
            item(span = { GridItemSpan(2) }) {
                OutlinedButton(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(bottom = 16.dp),
                    onClick = {
                        showSelectEmojiDialog = true
                    }) {
                    Text("Pick an emoji")
                }

            }
            item(span = { GridItemSpan(4) }) {
                Button(
                    shape = RoundedCornerShape(8.dp),
                    onClick = {
                        Log.i("EditTappGroupScreen", "clicked to save the group")
                        if (name.isEmpty()) {
                            Log.e("EditTappGroupScreen", "group name was invalid")
                            nameError = true
                        } else {
                            val updatedGroup = TappGroup(
                                selectedGroup.value.id,
                                name,
                                emoji,
                                selectedGroup.value.owner,
                                description,
                                selectedGroup.value.members,
                                selectedGroup.value.invites,
                            )
                            saveGroup(
                                updatedGroup, {
                                    Log.i("EditTappGroupScreen", "successfully saved the group")
                                    goBack()
                                }, {
                                    Log.e("EditTappGroupScreen", "failed to save the group")
                                    nameError = true
                                }
                            )

                        }
                    }
                ) {
                    Text("Save")
                }
            }
        }
    }

    if (showDeleteGroupDialog) {
        ConfirmTappGroupDeleteDialog({
            deleteGroup(selectedGroup.value, {
                Log.i("EditTappGroupScreen", "successfully deleted the group")
                goBackHome()
            }, {
                Log.e("EditTappGroupScreen", "failed to delete the group")
                Toast.makeText(
                    context,
                    "Group could not be accepted, please try again.",
                    Toast.LENGTH_SHORT
                ).show()
            })
            showDeleteGroupDialog = false
        }, { showDeleteGroupDialog = false })
    }

    if (showSelectEmojiDialog) {
        EmojiPickerDialog({ showSelectEmojiDialog = false }, { e ->
            Log.i(
                "EditTappGroupScreen",
                "selected emoji $e for group ${selectedGroup.value.id}: ${selectedGroup.value.name}"
            )
            emoji = e
        })
    }
}

/**
 * Dialog shown when the user has clicked to delete the group, confirmation will delete the group.
 */
@Composable
fun ConfirmTappGroupDeleteDialog(onConfirm: () -> Unit, onCancel: () -> Unit) {
    Dialog(onDismissRequest = onCancel) {
        ConfirmTappGroupDeleteContent(onConfirm, onCancel)
    }
}

/**
 * Content for the group delete confirmation dialog, user solely for preview rendering.
 */
@Composable
fun ConfirmTappGroupDeleteContent(onConfirm: () -> Unit, onCancel: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Are you sure you want to delete the group?")
        }
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            OutlinedButton(
                onClick = onCancel,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(8.dp)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(8.dp)
            ) {
                Text("Confirm")
            }
        }
    }
}

/**
 * Emoji picker dialog, lets the group owner select the emoji for the group.
 */
@Composable
fun EmojiPickerDialog(onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        EmojiPickerContent(onDismiss, onSelect)
    }
}

/**
 * Content for the emoji picker dialog, used solely for preview rendering.
 */
@Composable
fun EmojiPickerContent(onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    Card {
        LazyVerticalGrid(
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            columns = GridCells.Adaptive(minSize = 48.dp),
        ) {
            items(listOf("❤️", "🍺", "😭", "🎉", "👍", "🤌", "🔥", "🐶", "🐱")) { e ->
                EmojiButton(e) {
                    onSelect(e)
                    onDismiss()
                }
            }
        }
    }
}

/**
 * An emoji button, making the emojis clickable.
 */
@Composable
fun EmojiButton(emoji: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            fontSize = 32.sp,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Preview how it looks when a new group is created.
 */
@Preview
@Composable
fun NewGroupScreenPreview() {
    EditTappGroupScreen(
        MutableStateFlow(newTappGroup).asStateFlow(),
        { g, s, f -> },
        { g, s, f -> },
        {},
        {}
    )
}

/**
 * Preview for the editing screen, when a group already exists.
 */
@Preview
@Composable
fun EditGroupScreenPreview() {
    EditTappGroupScreen(
        MutableStateFlow(testGroup).asStateFlow(),
        { g, s, f -> },
        { g, s, f -> },
        {},
        {}
    )
}

/**
 * Emoji picker preview.
 */
@Preview(showBackground = true)
@Composable
fun EmojiPickerPreview() {
    EmojiPickerContent({}, {})
}
