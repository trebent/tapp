package com.github.trebent.tapp.screen

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.trebent.tapp.viewmodel.AuthViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@ExperimentalMaterial3Api
@Composable
fun AccountScreenRoute(authViewModel: AuthViewModel, goToLogin: () -> Unit, goBack: () -> Unit) {
    Log.i("AccountScreenRoute", "navigated")
    AccountScreen(
        authViewModel.tag,
        { tag -> authViewModel.updateTag(tag) },
        { password -> authViewModel.updatePassword(password) },
        { authViewModel.logout() },
        goToLogin,
        goBack
    )
}

@ExperimentalMaterial3Api
@Composable
fun AccountScreen(
    currentTag: StateFlow<String>,
    updateTag: (String) -> Unit,
    updatePassword: (String) -> Unit,
    logout: () -> Unit,
    goToLogin: () -> Unit,
    goBack: () -> Unit
) {
    Log.i("AccountScreen", "rendering")

    var tag by remember { mutableStateOf(currentTag.value) }
    var password by remember { mutableStateOf("") }
    var repeatPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                "Your user tag",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            TextField(
                value = tag,
                label = { Text("Tag") },
                placeholder = { Text("Enter a tag") },
                onValueChange = { v: String -> tag = v },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
            )
            Button(modifier = Modifier.fillMaxWidth(), onClick = {
                Log.i("AccountScreen", "clicked the update tag button")
                updateTag(tag)
            }, shape = RoundedCornerShape(8.dp)) {
                Text("Update")
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                "Change your password",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            TextField(
                value = password,
                label = { Text("Password") },
                placeholder = { Text("Password") },
                onValueChange = { v: String -> password = v },
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
            TextField(
                value = repeatPassword,
                label = { Text("Repeat password") },
                placeholder = { Text("Repeat password") },
                onValueChange = { v: String -> repeatPassword = v },
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
            Button(modifier = Modifier.fillMaxWidth(), onClick = {
                Log.i("AccountScreen", "clicked the update password button")

                if (password != repeatPassword) {
                    passwordError = true
                } else {
                    updatePassword(password)
                    password = ""
                    repeatPassword = ""
                    passwordError = false
                }

            }, shape = RoundedCornerShape(8.dp)) {
                Text("Update")
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Button(modifier = Modifier.fillMaxWidth(), onClick = {
                Log.i("AccountScreen", "clicked the logout button")
                logout()
                goToLogin()
            }, shape = RoundedCornerShape(8.dp)) {
                Text("Log out")
            }
        }
    }
}

@ExperimentalMaterial3Api
@Preview
@Composable
fun AccountScreenPreview() {
    AccountScreen(MutableStateFlow("tag").asStateFlow(), {}, {}, {}, {}, {})
}
