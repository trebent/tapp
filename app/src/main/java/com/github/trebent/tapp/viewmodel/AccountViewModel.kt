package com.github.trebent.tapp.viewmodel


import android.app.Application
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.github.trebent.tapp.api.Account
import com.github.trebent.tapp.api.ChangePasswordRequest
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


val testAccount = Account("testemail@domain.se", "pw", "superman")
val testAccountTag2 = Account("testemail@domain.se", "pw", "taggy")
val testAccountWithNullTag = Account("someone@domain.se", "pw", null)
val testAccountWithEmptyTag = Account("else@domain.se", "pw", "")


class AccountViewModel(application: Application) : AndroidViewModel(application) {
    private val _loginState = MutableStateFlow(false)

    // Used for relaying to the MainActivity if the splash screen can be removed or not.
    private val _i = MutableStateFlow(false)
    private var _account = MutableStateFlow(Account("", "", null))
    val account = _account.asStateFlow()
    private val _token = MutableStateFlow<String?>(null)

    val isLoggedIn = _loginState.asStateFlow()
    val initialised = _i.asStateFlow()

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

            if (_loginState.value) {
                val response = accountService.getAccount(_token.value!!, _account.value.email)
                if (response.isSuccessful) {
                    Log.i("AccountViewModel", "successfully fetched account ${response.body()}")
                    _account.value = Account(_account.value.email, "", response.body()!!.tag)
                }
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
                _token.value = preferences[tokenkey]
                Log.i("AccountViewModel", "preferences updated token ${_token.value}")
                Log.i("AccountViewModel", "preferences updated account ${_account.value}")
            }
        }
    }

    fun fetchAccount(): StateFlow<Account> {
        viewModelScope.launch {
            val response = accountService.getAccount(_token.value!!, _account.value.email)
            if (response.isSuccessful) {
                _account.value = Account(_account.value.email, "", response.body()?.tag)
                Log.i("AccountViewModel", "fetched account successfully ${_account.value}")
            } else {
                Log.e("AccountViewModel", "failed to fetch account")
            }
        }

        return _account
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
                }
                onSuccess()
            } else {
                onFailure()
            }
        }
    }

    fun logout(onDone: () -> Unit) {
        Log.i("AccountViewModel", "logout")

        viewModelScope.launch {
            val response = accountService.logout(_token.value!!)

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

    fun updateTag(tag: String, onSuccess: () -> Unit, onFailure: () -> Unit) {
        Log.i("AccountViewModel", "updating tag to $tag")

        viewModelScope.launch {
            val response = accountService.updateAccount(
                _token.value!!,
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

    fun updatePassword(password: String, onSuccess: () -> Unit, onFailure: () -> Unit) {
        Log.i("AccountViewModel", "updating password")
        viewModelScope.launch {
            val response =
                accountService.updatePassword(_token.value!!, ChangePasswordRequest(password))
            if (response.isSuccessful) {
                Log.i("AccountViewModel", "successfully updated the password!")
                onSuccess()
            } else {
                Log.i("AccountViewModel", "failed to update the password")
                onFailure()
            }
        }
    }

    fun deleteAccount(onSuccess: () -> Unit, onFailure: () -> Unit) {
        Log.i("AccountViewModel", "deleting account ${_account.value.email}")
        viewModelScope.launch {
            val response =
                accountService.deleteAccount(_token.value!!, _account.value.email)
            if (response.isSuccessful) {
                Log.i("AccountViewModel", "successfully deleted the account!")
                logout { onSuccess() }
            } else {
                Log.e("AccountViewModel", "failed to delete the account")
                onFailure()
            }
        }
    }
}