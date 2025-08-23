package com.github.trebent.tapp

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp


@Composable
fun LoginScreenRoute(authViewModel: AuthViewModel, onLogin: () -> Unit) {
    LoginScreen({ u, p -> authViewModel.login(u, p) }, onLogin)
}

@Composable
fun LoginScreen(loginFun: (String, String) -> Boolean, onLogin: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            verticalArrangement = Arrangement.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            item(span = { GridItemSpan(4) }) {
                TextField(
                    value = username,
                    label = { Text("Username") },
                    placeholder = { Text("Enter username") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "",
                        )
                    },
                    onValueChange = { v: String ->
                        username = v
                        Log.i("LoginScreen", "entered text in username field: $username")
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            item(span = { GridItemSpan(4) }) {
                TextField(
                    value = password,
                    label = { Text("Password") },
                    placeholder = { Text("Enter password") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "",
                        )
                    },
                    onValueChange = { v: String ->
                        password = v
                        // Don't emit the actual password, but make sure to report the string size
                        // for easy debugging.
                        Log.i(
                            "LoginScreen",
                            "entered text in password field ${password.length} characters long"
                        )
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            item(span = { GridItemSpan(4) }) {
                Button(onClick = {
                    Log.i("LoginScreen", "clicked login button")
                    if (loginFun(username, password)) {
                        onLogin()
                    } else {
                        Log.e("LoginScreen", "login failed")
                    }
                }) {
                    Text("Log in")
                }
            }
        }
    }
}

@Preview
@Composable
fun LoginScreenPreview() {
    LoginScreen({ u, p -> true }, {})
}