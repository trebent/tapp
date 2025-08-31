package com.github.trebent.tapp.viewmodel


import android.app.Application
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.github.trebent.tapp.BuildConfig
import com.github.trebent.tapp.api.Account
import com.github.trebent.tapp.api.LoginRequest
import com.github.trebent.tapp.api.accountService
import com.github.trebent.tapp.dataStore
import com.github.trebent.tapp.emailkey
import com.github.trebent.tapp.tokenkey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


const val testUsername = "u"
var testPassword = "p"

class AccountViewModel(application: Application) : AndroidViewModel(application) {
    private val _loginState = MutableStateFlow(false)

    // Used for relaying to the MainActivity if the splash screen can be removed or not.
    private val _i = MutableStateFlow(false)
    private var _account = MutableStateFlow(Account("", "", null))
    private val _token = MutableStateFlow<String?>(null)

    val isLoggedIn = _loginState.asStateFlow()
    val initialised = _i.asStateFlow()
    val account: StateFlow<Account> = _account.asStateFlow()

    init {
        Log.i("AccountViewModel", "initialising the auth view model")
        viewModelScope.launch {
            Log.i("AccountViewModel", "checking datastore for stored tokens")

            val preferences = application.dataStore.data.first()
            _token.value = preferences[tokenkey]

            val email = preferences[emailkey]
            if (email != null) {
                _account.value = Account(email, "", null)
            }

            if (_token.value != null) {
                _loginState.value = true
            }

            Log.i("AccountViewModel", "initialised account view model")
            _i.value = true
        }
    }

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

    fun login(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        if (BuildConfig.TAPP_TEST_MODE && email == testUsername && password == testPassword) {
            Log.i("AccountViewModel", "login succeeded")
            _loginState.value = true
        } else {
            viewModelScope.launch {
                val response = accountService.login(LoginRequest(email, password))
                if (response.isSuccessful) {
                    _loginState.value = true
                    val newToken = response.headers()["Authorization"]
                    if (newToken == null) {
                        Log.i("AccountViewModel", "unable to find authorization token")
                        onFailure()
                    } else {

                        _token.value = newToken
                        _account.value = Account(email, "", "")

                        application.dataStore.edit { preferences ->
                            preferences[tokenkey] = newToken
                            preferences[emailkey] = email
                        }

                        onSuccess()
                    }
                    onSuccess()
                } else {
                    onFailure()
                }
            }
        }
    }

    fun logout(onDone: () -> Unit) {
        Log.i("AccountViewModel", "logout")
        if (BuildConfig.TAPP_TEST_MODE) {
            Log.i("AccountViewModel", "test mode, skip API call")
            _loginState.value = false
        } else {
            viewModelScope.launch {
                val response = accountService.logout()

                _loginState.value = false
                _token.value = null
                _account.value = Account("", "", null)

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
    }

    fun updateTag(tag: String, onSuccess: () -> Unit, onFailure: () -> Unit) {
        Log.i("AccountViewModel", "updating tag to $tag")

        viewModelScope.launch {
            accountService.updateAccount()
        }
        _account.value =
            Account(_account.value.email, _account.value.password, tag)
    }

    fun updatePassword(password: String) {
        Log.i("AccountViewModel", "updating password")
        if (BuildConfig.TAPP_TEST_MODE) {
            Log.d("AccountViewModel", "set test password")
            testPassword = password
        }
    }

    fun deleteAccount() {
        Log.i("AccountViewModel", "deleting account")
        _loginState.value = false
    }
}