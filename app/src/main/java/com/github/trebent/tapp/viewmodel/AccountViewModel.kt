/**
 * This file contains the implementation of the Tapplication's account view model. All UI components
 * that show any element related to the user's login and account status derives its information from
 * here.
 */
package com.github.trebent.tapp.viewmodel


import android.app.Application
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.trebent.tapp.api.Account
import com.github.trebent.tapp.api.ChangePasswordRequest
import com.github.trebent.tapp.api.LoginRequest
import com.github.trebent.tapp.api.accountService
import com.github.trebent.tapp.dataStore
import com.github.trebent.tapp.emailkey
import com.github.trebent.tapp.tokenkey
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


/*
Test declarations, used for preview composables.
 */
val testAccount = Account("testemail@domain.se", "pw", "superman")
val testAccountTag2 = Account("testemail@domain.se", "pw", "taggy")
val testAccountWithNullTag = Account("someone@domain.se", "pw", null)
val testAccountWithEmptyTag = Account("else@domain.se", "pw", "")

/**
 * Account view model
 *
 * @property application
 * @constructor Create empty Account view model
 */
class AccountViewModel(private val application: Application) : AndroidViewModel(application) {
    // Used for relaying to the MainActivity if the splash screen can be removed or not.
    private val _i = MutableStateFlow(false)
    val initialised = _i.asStateFlow()

    // The user's currently logged in account info.
    private var _account = MutableStateFlow(Account("", "", null))
    val account = _account.asStateFlow()

    // If the user is logged in or not.
    private val _loginState = MutableStateFlow(false)
    val isLoggedIn = _loginState.asStateFlow()

    // The sign in token, used to authenticate any outgoing request that requires the used to be
    // logged in.
    private val _token = MutableStateFlow("")

    /**
     * The view model's init:
     * 1. Checks if the user is logged in. If true, fetches the user's account information.
     * 2. Subscribes to preferences related to tapp. Preferences are used to store email and the
     *    auth token for knowing on app boot if the user is already signed in.
     * 3. If the user is signed in, the user's device FCM is reported to the Tapp backend to continue
     *    to be able to receive accurate notifications.
     */
    init {
        Log.i("AccountViewModel", "initialising the auth view model")
        viewModelScope.launch {
            Log.i("AccountViewModel", "checking datastore for stored tokens")

            val preferences = application.dataStore.data.first()
            val prefToken = preferences[tokenkey]
            if (prefToken != null) {
                Log.i("AccountViewModel", "found stored token, using it...")
                _token.value = prefToken
            }

            val email = preferences[emailkey]
            if (email != null) {
                _account.value = Account(email, "", null)
            }

            if (_token.value != "") {
                _loginState.value = true
            }

            if (_loginState.value) {
                val response = accountService.getAccount(_token.value, _account.value.email)
                if (response.isSuccessful) {
                    Log.i("AccountViewModel", "successfully fetched account ${response.body()}")
                    _account.value =
                        Account(_account.value.email, "", response.body()!!.tag)
                }

                reportFCM()
            }

            Log.i("AccountViewModel", "initialised account view model")
            _i.value = true

            application.dataStore.data.collect { preferences ->
                Log.i("AccountViewModel", "preferences $preferences")

                val email = preferences[emailkey]
                if (email != null) {
                    _account.value = Account(email, "", _account.value.tag)
                } else {
                    _account.value = Account("", "", null)
                }

                val prefToken = preferences[tokenkey]
                if (prefToken != null) {
                    _token.value = prefToken
                } else {
                    _token.value = ""
                }

                Log.i("AccountViewModel", "preferences updated token ${_token.value}")
                Log.i("AccountViewModel", "preferences updated account ${_account.value}")
            }
        }
    }

    /**
     * Get token for outgoing HTTP requests.
     *
     * @return the auth token for outgoing requests
     */
    fun getToken(): String {
        return _token.value
    }

    /**
     * Fetch account that's currently signed in.
     *
     * @return a stateful of the user account
     */
    fun fetchAccount(): StateFlow<Account> {
        viewModelScope.launch {
            val response = accountService.getAccount(_token.value, _account.value.email)
            if (response.isSuccessful) {
                _account.value =
                    Account(_account.value.email, "", response.body()?.tag)
                Log.i("AccountViewModel", "fetched account successfully ${_account.value}")
            } else {
                Log.e("AccountViewModel", "failed to fetch account")
            }
        }

        return _account
    }

    /**
     * Signup a new user
     *
     * @param tag
     * @param email
     * @param password
     * @param onSuccess callback
     * @param onFailure callback
     */
    fun signup(
        tag: String,
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        Log.i(
            "AccountViewModel",
            "signup tag: $tag, email: $email, password length: ${password.length}"
        )

        val t = if (tag == "") {
            null
        } else {
            tag
        }

        viewModelScope.launch {
            val response = accountService.createAccount(Account(email, password, t))
            if (response.isSuccessful) {
                // no need to set the account body, credentials are provided on login, in fact it
                // would be confusing to set it here.
                onSuccess()
            } else {
                onFailure()
            }
        }
    }

    /**
     * Login, a successful login call (to the Tapp backend) will lead to the user's device FCM to be
     * reported to the Tapp backend, allowing them to start receiving notifications. Logout will NOT
     * clear the FCM, to ensure that even in a logged out state the device will continue to receive
     * notifications post a first successful login call. See this view model's init procedure for
     * more FCM handling, in addition to the notification package.
     *
     * @param email
     * @param password
     * @param onSuccess callback
     * @param onFailure callback
     */
    fun login(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        viewModelScope.launch {
            val response = accountService.login(LoginRequest(email, password))
            if (response.isSuccessful) {
                _loginState.value = true
                val newToken = response.headers()["Authorization"]
                if (newToken == null) {
                    Log.i("AccountViewModel", "unable to find authorization token")
                    onFailure()
                } else {
                    application.dataStore.edit { preferences ->
                        preferences[tokenkey] = newToken
                        preferences[emailkey] = email
                    }

                    onSuccess()

                    val response = accountService.getAccount(_token.value, _account.value.email)
                    if (response.isSuccessful) {
                        Log.i("AccountViewModel", "successfully fetched account ${response.body()}")
                        _account.value =
                            Account(_account.value.email, "", response.body()!!.tag)
                    }

                    reportFCM()
                }
            } else {
                onFailure()
            }
        }
    }

    /**
     * Logout, even on a backend failure, the auth token is cleared. It's not seen as important to
     * notify of failure if the credentials can be cleared locally. A logout call MUST be considered
     * final and a new login procedure to be started to perform any additional actions related to the
     * user account.
     *
     * @param onDone callback
     */
    fun logout(onDone: () -> Unit) {
        Log.i("AccountViewModel", "logout")

        viewModelScope.launch {
            val response = accountService.logout(_token.value)

            _loginState.value = false

            application.dataStore.edit { preferences ->
                preferences.remove(tokenkey)
                preferences.remove(emailkey)
            }

            if (!response.isSuccessful) {
                Log.i("AccountViewModel", "logout call failed!")
            }
            onDone()
        }
    }

    /**
     * Update tag of the user account
     *
     * @param tag
     * @param onSuccess callback
     * @param onFailure callback
     */
    fun updateTag(tag: String, onSuccess: () -> Unit, onFailure: () -> Unit) {
        Log.i("AccountViewModel", "updating tag to $tag")

        viewModelScope.launch {
            val response = accountService.updateAccount(
                _token.value,
                _account.value.email,
                Account(_account.value.email, "", tag)
            )
            if (response.isSuccessful) {
                Log.i("AccountViewModel", "account tag update succeeded")
                _account.value =
                    Account(_account.value.email, "", tag)
                onSuccess()
            } else {
                Log.e("AccountViewModel", "account update call failed")
                onFailure()
            }
        }
    }

    /**
     * Update password
     *
     * @param password
     * @param onSuccess callback
     * @param onFailure callback
     */
    fun updatePassword(password: String, onSuccess: () -> Unit, onFailure: () -> Unit) {
        Log.i("AccountViewModel", "updating password")
        viewModelScope.launch {
            val response =
                accountService.updatePassword(_token.value, ChangePasswordRequest(password))
            if (response.isSuccessful) {
                Log.i("AccountViewModel", "successfully updated the password!")
                onSuccess()
            } else {
                Log.i("AccountViewModel", "failed to update the password")
                onFailure()
            }
        }
    }

    /**
     * Delete account
     *
     * @param onSuccess callback
     * @param onFailure callback
     */
    fun deleteAccount(onSuccess: () -> Unit, onFailure: () -> Unit) {
        Log.i("AccountViewModel", "deleting account ${_account.value.email}")
        viewModelScope.launch {
            val response =
                accountService.deleteAccount(_token.value, _account.value.email)
            if (response.isSuccessful) {
                Log.i("AccountViewModel", "successfully deleted the account!")
                logout { onSuccess() }
            } else {
                Log.e("AccountViewModel", "failed to delete the account")
                onFailure()
            }
        }
    }

    /**
     * Report the device FCM. The FCM is 1:1 with the user account email.
     *
     */
    private fun reportFCM() {
        Log.i("AccountViewModel", "Fetching FCM token and notifying backend...")
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM token failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            Log.d("FCM", "FCM Token: $token")

            // Send token to backend
            viewModelScope.launch {
                val response = accountService.pushFCM(token, _token.value)
                if (response.isSuccessful) {
                    Log.i("AccountViewModel", "sent FCM successfully")
                } else {
                    Log.e("AccountViewModel", "failed to report FCM to cloud backend")
                }
            }
        }
    }
}