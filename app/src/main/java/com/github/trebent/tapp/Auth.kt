package com.github.trebent.tapp

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp


@Composable
fun SignupScreenRoute(authViewModel: AuthViewModel, onCancel: () -> Unit, onSignedUp: () -> Unit) {
    Log.i("Home", "signup route")

    SignupScreen({ t, e, p -> authViewModel.signup(t, e, p) }, onCancel, onSignedUp)
}

@Composable
fun SignupScreen(
    onSignup: (t: String, e: String, p: String) -> Boolean,
    onCancel: () -> Unit,
    onSignedUp: () -> Unit
) {
    Log.i("Home", "rending SignupScreen")

    var tag by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
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
                    value = tag,
                    label = { Text("Tag") },
                    placeholder = { Text("Enter Tag") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "",
                        )
                    },
                    onValueChange = { v: String ->
                        tag = v
                        Log.i("LoginScreen", "entered text in tag field: $tag")
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            item(span = { GridItemSpan(4) }) {
                Text(
                    text = "an optional user handle that your friends can find you by",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            item(span = { GridItemSpan(4) }) {
                TextField(
                    value = email,
                    label = { Text("* Email") },
                    placeholder = { Text("Enter email") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "",
                        )
                    },
                    onValueChange = { v: String ->
                        email = v
                        Log.i("LoginScreen", "entered text in email field: $email")
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
                    label = { Text("* Password") },
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
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            item(span = { GridItemSpan(4) }) {
                Text(
                    text = "fields marked with * are required",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            item(span = { GridItemSpan(4) }) {
                Button(shape = RoundedCornerShape(8.dp), onClick = {
                    Log.i("SignupScreen", "clicked signup button")
                    if (onSignup(tag, email, password)) {
                        onSignedUp()
                    } else {
                        Log.e("SignupScreen", "signup failed")
                    }
                }) {
                    Text("Sign up")
                }
            }
            item(span = { GridItemSpan(4) }) {
                OutlinedButton(shape = RoundedCornerShape(8.dp), onClick = {
                    Log.i("SignupScreen", "clicked cancel button")
                    onCancel()
                }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Preview
@Composable
fun SignupScreenPreview() {
    SignupScreen({ t, e, p -> true }, {}, {})
}

@Composable
fun LoginScreenRoute(authViewModel: AuthViewModel, onSignup: () -> Unit, onLogin: () -> Unit) {
    Log.i("Home", "login route")

    LoginScreen({ u, p -> authViewModel.login(u, p) }, onSignup, onLogin)
}

@Composable
fun LoginScreen(loginFun: (String, String) -> Boolean, onSignup: () -> Unit, onLogin: () -> Unit) {
    Log.i("Home", "rending LoginScreen")

    var email by remember { mutableStateOf("") }
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
                    value = email,
                    label = { Text("Email") },
                    placeholder = { Text("Enter email") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "",
                        )
                    },
                    onValueChange = { v: String ->
                        email = v
                        Log.i("LoginScreen", "entered text in email field: $email")
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
                Button(shape = RoundedCornerShape(8.dp), onClick = {
                    Log.i("LoginScreen", "clicked login button")
                    if (loginFun(email, password)) {
                        onLogin()
                    } else {
                        Log.e("LoginScreen", "login failed")
                    }
                }) {
                    Text("Log in")
                }
            }
            item(span = { GridItemSpan(4) }) {
                OutlinedButton(shape = RoundedCornerShape(8.dp), onClick = {
                    Log.i("LoginScreen", "clicked sign up button")
                    onSignup()
                }) {
                    Text("Sign up", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Preview
@Composable
fun LoginScreenPreview() {
    LoginScreen({ u, p -> true }, {}, {})
}