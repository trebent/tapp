package com.github.trebent.tapp.screen

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.github.trebent.tapp.R
import com.github.trebent.tapp.api.Account
import com.github.trebent.tapp.api.TappGroup
import com.github.trebent.tapp.viewmodel.AccountViewModel
import com.github.trebent.tapp.viewmodel.TappGroupViewModel
import com.github.trebent.tapp.viewmodel.newTappGroup
import com.github.trebent.tapp.viewmodel.testAccount
import com.github.trebent.tapp.viewmodel.testGroup
import com.github.trebent.tapp.viewmodel.testGroupOwner
import com.github.trebent.tapp.viewmodel.testGroupOwnerNoMembers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


@Composable
fun TappGroupScreenRoute(
    accountViewModel: AccountViewModel,
    tappGroupViewModel: TappGroupViewModel,
    goToEditGroup: (tappGroup: TappGroup) -> Unit,
    goBack: () -> Unit
) {
    Log.i("TappGroupScreenRoute", "navigated")
    TappGroupScreen(
        0,
        accountViewModel.account,
        tappGroupViewModel.selectedGroup,
        { tappGroupViewModel.refreshSelectedGroup() },
        goToEditGroup,
        { e, g, s, f -> tappGroupViewModel.inviteToGroup(e, g, s, f) },
        { g, s, f -> tappGroupViewModel.leaveGroup(g, s, f) },
        { g, e, s, f -> tappGroupViewModel.kickFromGroup(g, e, s, f) },
        goBack,
    )
}

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

@Composable
fun ConfirmTappGroupDeleteDialog(onConfirm: () -> Unit, onCancel: () -> Unit) {
    Dialog(onDismissRequest = onCancel) {
        ConfirmTappGroupDeleteContent(onConfirm, onCancel)
    }
}

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

@Composable
fun ConfirmRemoveGroupMemberDialog(email: String, onConfirm: () -> Unit, onCancel: () -> Unit) {
    Dialog(onDismissRequest = onCancel) {
        ConfirmRemoveGroupMemberContent(email, onConfirm, onCancel)
    }
}

@Composable
fun ConfirmRemoveGroupMemberContent(email: String, onConfirm: () -> Unit, onCancel: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Are you sure you want to remove $email from the group?")
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

@Composable
@Preview
fun ConfirmRemoveGroupMemberDialogPreview() {
    ConfirmRemoveGroupMemberContent("email@test.se", {}, {})
}

@Composable
fun ConfirmLeaveGroupDialog(onConfirm: () -> Unit, onCancel: () -> Unit) {
    Dialog(onDismissRequest = onCancel) {
        ConfirmLeaveGroupContent(onConfirm, onCancel)
    }
}

@Composable
fun ConfirmLeaveGroupContent(onConfirm: () -> Unit, onCancel: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Are you sure you want to leave the group?")
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

@Composable
fun InviteToGroupDialog(onConfirm: (user: String) -> Unit, onCancel: () -> Unit) {
    Dialog(onDismissRequest = onCancel) {
        InviteToGroupContent(onConfirm, onCancel)
    }
}

@Composable
fun InviteToGroupContent(onConfirm: (user: String) -> Unit, onCancel: () -> Unit) {
    val focusManager = LocalFocusManager.current

    var user by rememberSaveable { mutableStateOf("") }
    var userError by rememberSaveable { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Enter the tag or email of the user to invite:")
        }
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            TextField(
                value = user,
                label = { Text("User tag or email") },
                placeholder = { Text("Enter tag or email") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "",
                    )
                },
                isError = userError,
                onValueChange = { v: String ->
                    userError = false
                    user = v
                    Log.i("InviteToGroupDialog", "entered text in user field: $user")
                },
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            if (userError) {
                Text(
                    text = "user must be filled in",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
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
                onClick = {
                    if (user.isEmpty()) {
                        userError = true
                    } else {
                        onConfirm(user)
                    }
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(8.dp)
            ) {
                Text("Confirm")
            }
        }
    }
}

@Composable
fun EmojiPickerDialog(onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        EmojiPickerContent(onDismiss, onSelect)
    }
}

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TappGroupScreen(
    currentTabIndex: Int,
    accountStateFlow: StateFlow<Account>,
    tappGroupStateFlow: StateFlow<TappGroup>,
    refreshSelectedGroup: () -> Unit,
    goToEditGroup: (tappGroup: TappGroup) -> Unit,
    inviteToGroup: (email: String, tappGroup: TappGroup, onSuccess: () -> Unit, onFailure: () -> Unit) -> Unit,
    leaveGroup: (tappGroup: TappGroup, onSuccess: () -> Unit, onFailure: () -> Unit) -> Unit,
    kickFromGroup: (tappGroup: TappGroup, email: String, onSuccess: () -> Unit, onFailure: () -> Unit) -> Unit,
    goBack: () -> Unit
) {
    Log.i("TappGroupScreen", "rendering")

    val context = LocalContext.current

    var showLeaveGroupDialog by rememberSaveable { mutableStateOf(false) }
    var showRemoveGroupMemberDialog by rememberSaveable { mutableStateOf(false) }
    var showInviteUserDialog by rememberSaveable { mutableStateOf(false) }

    val selectedGroup = tappGroupStateFlow.collectAsState()
    val account = accountStateFlow.collectAsState()

    var currentTabIndex by rememberSaveable { mutableIntStateOf(currentTabIndex) }

    var groupMemberEmailToRemove by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = selectedGroup.value.name,
                        style = MaterialTheme.typography.titleLarge
                    )
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
                    if (selectedGroup.value.owner == account.value.email) {
                        IconButton(onClick = {
                            Log.i(
                                "TappGroupScreen",
                                "clicked to add user to group ${selectedGroup.value.id}"
                            )
                            showInviteUserDialog = true
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.group_add),
                                contentDescription = "invite user to group"
                            )
                        }
                        IconButton(onClick = {
                            Log.i(
                                "TappGroupScreen",
                                "clicked edit button, opening ${selectedGroup.value.id}: ${selectedGroup.value.name}"
                            )
                            goToEditGroup(selectedGroup.value)
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit tapp group"
                            )
                        }
                    } else {
                        IconButton(onClick = {
                            Log.i(
                                "TappGroupScreen",
                                "clicked leave group button"
                            )
                            showLeaveGroupDialog = true
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.group_remove),
                                contentDescription = "Leave tapp group"
                            )
                        }
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            item(span = { GridItemSpan(4) }) {
                Row(horizontalArrangement = Arrangement.Center) {
                    Text(
                        "Tapp it!",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            item(span = { GridItemSpan(4) }) {
                Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center) {
                    TappButton(
                        selectedGroup.value.emoji,
                        {
                            Log.i(
                                "TappGroupScreen",
                                "tapped!!! ${selectedGroup.value.id}: ${selectedGroup.value.name}"
                            )
                        },
                        128.dp,
                        56.sp,
                    )
                }
            }
            if (selectedGroup.value.description?.isEmpty() != true) {
                item(span = { GridItemSpan(4) }) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Text(
                            "Group description",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                        Text(
                            selectedGroup.value.description ?: "",
                        )
                    }
                }
            }
            item(span = { GridItemSpan(4) }) {
                SecondaryTabRow(selectedTabIndex = currentTabIndex) {
                    Tab(
                        selected = currentTabIndex == 0,
                        onClick = {
                            currentTabIndex = 0
                        },
                        text = { Text("Tapp history") }
                    )
                    Tab(
                        selected = currentTabIndex == 1,
                        onClick = {
                            currentTabIndex = 1
                            refreshSelectedGroup()
                        },
                        text = { Text("Members") }
                    )
                }
            }
            item(span = { GridItemSpan(4) }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                ) {
                    // Content below the tabs
                    when (currentTabIndex) {
                        0 ->
                            Text(
                                "Tapp history",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                        1 ->
                            if (selectedGroup.value.memberCount() == 0) {
                                Text(
                                    text = "There are no members yet",
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                )
                            } else {
                                for (member in selectedGroup.value.members!!) {
                                    GroupMemberRow(
                                        selectedGroup.value.owner == account.value.email,
                                        member,
                                        { e ->
                                            groupMemberEmailToRemove = e
                                            showRemoveGroupMemberDialog = true
                                        })
                                }
                            }
                    }
                }
            }
        }
    }

    if (showInviteUserDialog) {
        InviteToGroupDialog({ user ->
            inviteToGroup(user, selectedGroup.value, {
                Log.i(
                    "TappGroupScreen",
                    "successfully invited $user to group ${selectedGroup.value.id}"
                )
                showInviteUserDialog = false
                Toast.makeText(
                    context,
                    "Invitation sent!",
                    Toast.LENGTH_SHORT
                ).show()
            }, {
                Log.e(
                    "TappGroupScreen",
                    "failed to invite $user to group ${selectedGroup.value.id}"
                )
                Toast.makeText(
                    context,
                    "Failed to invite user to group, please try again.",
                    Toast.LENGTH_SHORT
                ).show()
                showInviteUserDialog = false
            })
        }, {
            Log.i(
                "TappGroupScreen",
                "cancelled invite user dialog"
            )
            showInviteUserDialog = false
        })
    }

    if (showLeaveGroupDialog) {
        ConfirmLeaveGroupDialog(
            {
                Log.i(
                    "TappGroupScreen",
                    "confirmed to leave group ${selectedGroup.value.id}"
                )
                showLeaveGroupDialog = false

                leaveGroup(selectedGroup.value, {
                    Log.i("TappGroupScreen", "left group ${selectedGroup.value.id} successfully")
                    goBack()
                }, {
                    Log.e("TappGroupScreen", "failed to leave group ${selectedGroup.value.id}")
                    Toast.makeText(
                        context,
                        "Failed to leave group, please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                })
            }, {
                Log.e("TappGroupScreen", "cancelled leaving the group")
                showLeaveGroupDialog = false
            }
        )
    }

    if (showRemoveGroupMemberDialog) {
        ConfirmRemoveGroupMemberDialog(
            groupMemberEmailToRemove,
            {
                Log.i(
                    "TappGroupScreen",
                    "confirmed removal of user $groupMemberEmailToRemove from group ${selectedGroup.value.id}"
                )
                showRemoveGroupMemberDialog = false

                kickFromGroup(selectedGroup.value, groupMemberEmailToRemove, {
                    Log.i(
                        "TappGroupScreen",
                        "removed user $groupMemberEmailToRemove from group ${selectedGroup.value.id} successfully"
                    )
                    Toast.makeText(
                        context,
                        "Removed user $groupMemberEmailToRemove.",
                        Toast.LENGTH_SHORT
                    ).show()
                }, {
                    Log.e(
                        "TappGroupScreen",
                        "failed to remove user $groupMemberEmailToRemove from group ${selectedGroup.value.id}"
                    )
                    Toast.makeText(
                        context,
                        "Failed to remove user $groupMemberEmailToRemove, please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                })
            }, {
                Log.i("TappGroupScreen", "cancelled removing the user")
                showRemoveGroupMemberDialog = false
            }
        )
    }
}

@Composable
fun GroupMemberRow(isOwner: Boolean, account: Account, removeMember: (email: String) -> Unit) {
    LocalContext.current

    Card(
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .padding(bottom = 8.dp)
            .heightIn(min = 56.dp)
            .fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(start = 24.dp, end = 16.dp)
        ) {
            Text(
                account.email,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            )
            if (isOwner) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    IconButton(
                        onClick = {
                            removeMember(account.email)
                        },
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.group_remove),
                            contentDescription = "remove member from group",
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TappButton(emoji: String, onClick: () -> Unit, size: Dp, fontSize: TextUnit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.size(size),
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = emoji,
            fontSize = fontSize,
            textAlign = TextAlign.Center
        )
    }
}

@Preview
@Composable
fun TappGroupScreenPreview() {
    TappGroupScreen(
        0,
        MutableStateFlow(testAccount).asStateFlow(),
        MutableStateFlow(testGroup).asStateFlow(),
        {},
        {},
        { g, e, s, f -> },
        { g, s, f -> },
        { g, e, s, f -> },
        {}
    )
}

@Preview
@Composable
fun TappGroupOwnerScreenPreview() {
    TappGroupScreen(
        0,
        MutableStateFlow(testAccount).asStateFlow(),
        MutableStateFlow(testGroupOwner).asStateFlow(),
        {},
        {},
        { g, e, s, f -> },
        { g, s, f -> },
        { g, e, s, f -> },
        {}
    )
}

@Preview
@Composable
fun TappGroupMemberScreenPreview() {
    TappGroupScreen(
        1,
        MutableStateFlow(testAccount).asStateFlow(),
        MutableStateFlow(testGroup).asStateFlow(),
        {},
        {},
        { g, e, s, f -> },
        { g, s, f -> },
        { g, e, s, f -> },
        {}
    )
}

@Preview
@Composable
fun TappGroupNoMemberOwnerScreenPreview() {
    TappGroupScreen(
        1,
        MutableStateFlow(testAccount).asStateFlow(),
        MutableStateFlow(testGroupOwnerNoMembers).asStateFlow(),
        {},
        {},
        { g, e, s, f -> },
        { g, s, f -> },
        { g, e, s, f -> },
        {}
    )
}

@Preview
@Composable
fun TappGroupMemberOwnerScreenPreview() {
    TappGroupScreen(
        1,
        MutableStateFlow(testAccount).asStateFlow(),
        MutableStateFlow(testGroupOwner).asStateFlow(),
        {},
        {},
        { g, e, s, f -> },
        { g, s, f -> },
        { g, e, s, f -> },
        {}
    )
}

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

@Preview
@Composable
fun InviteToGroupDialogPreview() {
    InviteToGroupContent({ s -> }, {})
}


@Preview(showBackground = true)
@Composable
fun ConfirmTappGroupDeleteDialogPreview() {
    ConfirmTappGroupDeleteContent({}, {})
}


@Preview(showBackground = true)
@Composable
fun EmojiPickerPreview() {
    EmojiPickerContent({}, {})
}

@Composable
@Preview
fun ConfirmLeaveGroupDialogPreview() {
    ConfirmLeaveGroupContent({}, {})
}
