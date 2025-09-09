/**
 * This file handles everything screen-related to authentication handling. Including sign up and
 * login/logout UI componennts.
 */
package com.github.trebent.tapp.screen

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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.trebent.tapp.viewmodel.AccountViewModel


val emailRegex = Regex("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$")


/**
 * Signup screen route
 *
 * @param accountViewModel
 * @param onCancel
 * @param onSignedUp
 */
@Composable
fun SignupScreenRoute(
    accountViewModel: AccountViewModel,
    onCancel: () -> Unit,
    onSignedUp: () -> Unit
) {
    Log.i("SignupScreenRoute", "navigated")

    SignupScreen(
        { t, e, p, success, failure ->
            accountViewModel.signup(t, e, p, success, failure)
        },
        onCancel,
        onSignedUp,
    )
}

/**
 * Signup screen
 *
 * @param onSignup
 * @param onCancel
 * @param onSignedUp
 */
@Composable
fun SignupScreen(
    onSignup: (t: String, e: String, p: String, onSuccess: () -> Unit, onFailure: () -> Unit) -> Unit,
    onCancel: () -> Unit,
    onSignedUp: () -> Unit
) {
    Log.i("SignupScreen", "rendering")

    var tag by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    var tagError by rememberSaveable { mutableStateOf(false) }
    var emailError by rememberSaveable { mutableStateOf(false) }
    var passwordError by rememberSaveable { mutableStateOf(false) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            verticalArrangement = Arrangement.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            item(span = { GridItemSpan(4) }) {
                TextField(
                    value = tag,
                    label = { Text("Tag") },
                    placeholder = { Text("Enter tag") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "",
                        )
                    },
                    isError = tagError,
                    onValueChange = { v: String ->
                        tagError = false
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
            if (tagError) {
                item(span = { GridItemSpan(4) }) {
                    Text(
                        text = "tag is already taken",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
            item(span = { GridItemSpan(4) }) {
                Text(
                    text = "an optional user handle that your friends can find you by",
                    style = MaterialTheme.typography.bodySmall,
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
                    isError = emailError,
                    onValueChange = { v: String ->
                        emailError = false
                        email = v
                        Log.i("LoginScreen", "entered text in email field: $email")
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Next,
                        keyboardType = KeyboardType.Email,
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            if (emailError) {
                item(span = { GridItemSpan(4) }) {
                    Text(
                        text = "email address is required, example: person@domain.se",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
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
                    isError = passwordError,
                    onValueChange = { v: String ->
                        passwordError = false
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
            if (passwordError) {
                item(span = { GridItemSpan(4) }) {
                    Text(
                        text = "password must be at least 6 and at most 25 characters",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
            item(span = { GridItemSpan(4) }) {
                Text(
                    text = "fields marked with * are required",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            item(span = { GridItemSpan(4) }) {
                Button(shape = RoundedCornerShape(8.dp), onClick = {
                    Log.i("SignupScreen", "clicked signup button")

                    emailError = !email.matches(emailRegex)
                    Log.i("SignupScreen", "emailError $emailError")
                    passwordError = password.length < 6 || password.length > 25
                    Log.i("SignupScreen", "passwordError $passwordError")

                    if (!(emailError || passwordError)) {
                        onSignup(tag, email, password, {
                            onSignedUp()
                            Log.i("SignupScreen", "signup succeeded")
                        }, {
                            Log.e("SignupScreen", "signup call failed")
                        })
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
                    Text("Cancel")
                }
            }
        }
    }
}

/**
 * Login screen route
 *
 * @param accountViewModel
 * @param onSignup
 * @param onLogin
 */
@Composable
fun LoginScreenRoute(
    accountViewModel: AccountViewModel,
    onSignup: () -> Unit,
    onLogin: () -> Unit
) {
    Log.i("LoginScreenRoute", "navigated")

    LoginScreen(
        { u, p, s, f ->
            accountViewModel.login(u, p, s, f)
        },
        onSignup,
        onLogin,
    )
}

@Composable
fun LoginScreen(
    loginFun: (String, String, () -> Unit, () -> Unit) -> Unit,
    onSignup: () -> Unit,
    onLogin: () -> Unit
) {
    Log.i("LoginScreen", "rendering")

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    var emailError by rememberSaveable { mutableStateOf(false) }
    var passwordError by rememberSaveable { mutableStateOf(false) }
    var loginError by rememberSaveable { mutableStateOf(false) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            verticalArrangement = Arrangement.Bottom,
            horizontalArrangement = Arrangement.Center,
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
                    isError = emailError,
                    onValueChange = { v: String ->
                        email = v
                        emailError = false
                        loginError = false
                        Log.i("LoginScreen", "entered text in email field: $email")
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Next,
                        keyboardType = KeyboardType.Email,
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            if (emailError) {
                item(span = { GridItemSpan(4) }) {
                    Text(
                        text = "email is required to log in",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
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
                    isError = passwordError,
                    onValueChange = { v: String ->
                        password = v
                        passwordError = false
                        loginError = false
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
            if (passwordError) {
                item(span = { GridItemSpan(4) }) {
                    Text(
                        text = "password is required to log in",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
            if (loginError) {
                item(span = { GridItemSpan(4) }) {
                    Text(
                        text = "incorrect email and password combination",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
            item(span = { GridItemSpan(4) }) {
                Button(shape = RoundedCornerShape(8.dp), onClick = {
                    Log.i("LoginScreen", "clicked login button")

                    emailError = email.isEmpty()
                    passwordError = password.isEmpty()

                    if (!(emailError || passwordError)) {
                        loginFun(email, password, {
                            Log.i("LoginScreen", "login succeeded")
                            onLogin()
                        }, {
                            Log.e("LoginScreen", "login failed")
                            loginError = true
                        })
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
                    Text("Sign up")
                }
            }
        }
    }
}

/**
 * Signup screen preview
 *
 */
@Preview
@Composable
fun SignupScreenPreview() {
    SignupScreen({ t, e, p, s, f -> }, {}, {})
}

/**
 * Login screen preview
 *
 */
@Preview
@Composable
fun LoginScreenPreview() {
    LoginScreen({ u, p, s, f -> }, {}, {})
}