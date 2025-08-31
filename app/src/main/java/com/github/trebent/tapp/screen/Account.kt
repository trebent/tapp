package com.github.trebent.tapp.screen

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.github.trebent.tapp.api.Account
import com.github.trebent.tapp.viewmodel.AccountViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@ExperimentalMaterial3Api
@Composable
fun AccountScreenRoute(
    accountViewModel: AccountViewModel,
    goToLogin: () -> Unit,
    goBack: () -> Unit
) {
    Log.i("AccountScreenRoute", "navigated")
    AccountScreen(
        accountViewModel.account,
        { tag -> accountViewModel.updateTag(tag) },
        { password -> accountViewModel.updatePassword(password) },
        { accountViewModel.deleteAccount() },
        { o -> accountViewModel.logout(o) },
        goToLogin,
        goBack
    )
}

@ExperimentalMaterial3Api
@Composable
fun AccountScreen(
    account: StateFlow<Account>,
    updateTag: (String) -> Unit,
    updatePassword: (String) -> Unit,
    deleteAccount: () -> Unit,
    logout: (onDone: () -> Unit) -> Unit,
    goToLogin: () -> Unit,
    goBack: () -> Unit
) {
    Log.i("AccountScreen", "rendering")

    var tag by rememberSaveable { mutableStateOf(account.value.tag ?: "") }
    var tagError by rememberSaveable { mutableStateOf(false) }
    var tagSaved by rememberSaveable { mutableStateOf(false) }

    var password by rememberSaveable { mutableStateOf("") }
    var repeatPassword by rememberSaveable { mutableStateOf("") }
    var passwordError by rememberSaveable { mutableStateOf(false) }
    var passwordSaved by rememberSaveable { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    var showConfirmAccountDelete by rememberSaveable { mutableStateOf(false) }

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
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            item(span = { GridItemSpan(4) }) {
                Text(
                    "Your user tag",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
            item(span = { GridItemSpan(4) }) {
                TextField(
                    value = tag,
                    label = { Text("Tag") },
                    placeholder = { Text("Enter a tag") },
                    onValueChange = { v: String ->
                        tagError = false
                        tagSaved = false
                        tag = v
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    isError = tagError,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                )
            }
            if (tagError) {
                item(span = { GridItemSpan(4) }) {
                    Text(
                        text = "failed to update tag",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
            if (tagSaved) {
                item(span = { GridItemSpan(4) }) {
                    Log.i("AccountScreen", "showing tag saved message")
                    Text(
                        text = "tag updated successfully!",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
            item(span = { GridItemSpan(4) }) {
                Button(modifier = Modifier.fillMaxWidth(), onClick = {
                    Log.i("AccountScreen", "clicked the update tag button")
                    updateTag(tag)
                    tagSaved = true
                }, shape = RoundedCornerShape(8.dp)) {
                    Text("Update")
                }
            }
            item(span = { GridItemSpan(4) }) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            item(span = { GridItemSpan(4) }) {
                Text(
                    "Change your password",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
            item(span = { GridItemSpan(4) }) {
                TextField(
                    value = password,
                    label = { Text("Password") },
                    placeholder = { Text("Password") },
                    onValueChange = { v: String ->
                        passwordError = false
                        passwordSaved = false
                        password = v
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    isError = passwordError,
                    visualTransformation = PasswordVisualTransformation(),
                )
            }
            item(span = { GridItemSpan(4) }) {
                TextField(
                    value = repeatPassword,
                    label = { Text("Repeat password") },
                    placeholder = { Text("Repeat password") },
                    onValueChange = { v: String ->
                        passwordError = false
                        passwordSaved = false
                        repeatPassword = v
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    isError = passwordError,
                    visualTransformation = PasswordVisualTransformation(),
                )
            }
            if (passwordError) {
                item(span = { GridItemSpan(4) }) {
                    Text(
                        text = "passwords must match and be at least 6 and at most 25 characters",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
            if (passwordSaved) {
                item(span = { GridItemSpan(4) }) {
                    Log.i("AccountScreen", "showing password saved message")
                    Text(
                        text = "password updated successfully!",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
            item(span = { GridItemSpan(4) }) {
                Button(modifier = Modifier.fillMaxWidth(), onClick = {
                    Log.i("AccountScreen", "clicked the update password button")

                    if (password.isEmpty() || repeatPassword.isEmpty() || (password != repeatPassword) || (password.length < 6 || password.length > 25)) {
                        passwordError = true
                    } else {
                        updatePassword(password)
                        password = ""
                        repeatPassword = ""
                        passwordError = false
                        passwordSaved = true
                    }

                }, shape = RoundedCornerShape(8.dp)) {
                    Text("Update")
                }
            }
            item(span = { GridItemSpan(4) }) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            item(span = { GridItemSpan(4) }) {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp), onClick = {
                        Log.i("AccountScreen", "clicked the logout button")
                        logout({
                            Log.i("AccountScreen", "logout completed")
                            goToLogin()
                        })
                    }, shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Log out")
                }
            }
            item(span = { GridItemSpan(4) }) {
                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth(), onClick = {
                        Log.i("AccountScreen", "clicked the delete account button")
                        showConfirmAccountDelete = true
                    }, shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete account")
                }
            }
        }
    }

    if (showConfirmAccountDelete) {
        ConfirmAccountDeleteDialog({ showConfirmAccountDelete = false }, deleteAccount, goToLogin)
    }
}

@Composable
fun ConfirmAccountDeleteDialog(
    onDismiss: () -> Unit,
    deleteAccount: () -> Unit,
    goToLogin: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        ConfirmAccountDeleteContent(onDismiss, deleteAccount, goToLogin)
    }
}

@Composable
fun ConfirmAccountDeleteContent(
    onDismiss: () -> Unit,
    deleteAccount: () -> Unit,
    goToLogin: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Are you sure you want to delete your account? This action is not reversible.",
                modifier = Modifier.padding(8.dp)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(8.dp)
            ) {
                Text("Cancel")
            }
            Button(onClick = {
                deleteAccount()
                goToLogin()
                onDismiss()
            }, shape = RoundedCornerShape(8.dp), modifier = Modifier.padding(8.dp)) {
                Text("Confirm")
            }
        }
    }
}

@Preview
@Composable
fun ConfirmAccountDeleteDialogPreview() {
    ConfirmAccountDeleteContent({}, {}, {})
}

@ExperimentalMaterial3Api
@Preview
@Composable
fun AccountScreenPreview() {
    AccountScreen(
        MutableStateFlow(Account(2, "", "", "tag")).asStateFlow(),
        {},
        {},
        {},
        {},
        {},
        {})
}
