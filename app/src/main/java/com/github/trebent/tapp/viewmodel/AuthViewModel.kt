package com.github.trebent.tapp.viewmodel


import android.util.Log
import androidx.lifecycle.ViewModel
import com.github.trebent.tapp.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow


const val testUsername = "u"
var testPassword = "p"


class AuthViewModel() : ViewModel() {
    private val _loginState = MutableStateFlow(false)

    // Used for relaying to the MainActivity if the splash screen can be removed or not.
    private val _i = MutableStateFlow(false)
    val initialised = _i.asStateFlow()

    private val _tag = MutableStateFlow("")
    val tag = _tag.asStateFlow()

    init {
        Log.i("AuthViewModel", "initialising the auth view model")
        // TODO: remove hardcoded sleep
        Thread.sleep(200)
        // TODO: add cloud login check
        _i.value = true
    }

    fun isLoggedIn(): Boolean {
        Log.i("AuthViewModel", "isLoggedIn ${_loginState.value}")
        return _loginState.value
    }

    fun signup(tag: String, email: String, password: String): Boolean {
        Log.i("AuthViewModel", "signup")
        return true
    }

    fun login(email: String, password: String): Boolean {
        if (BuildConfig.TAPP_TEST_MODE && email == testUsername && password == testPassword) {
            Log.i("AuthViewModel", "login succeeded")
            _loginState.value = true
        }

        return _loginState.value
    }

    fun logout() {
        Log.i("AuthViewModel", "logout")
        _loginState.value = false
    }

    fun updateTag(tag: String) {
        Log.i("AuthViewModel", "updating tag to $tag")
    }

    fun updatePassword(password: String) {
        Log.i("AuthViewModel", "updating password")
        if (BuildConfig.TAPP_TEST_MODE) {
            Log.d("AuthViewModel", "set test password")
            testPassword = password
        }
    }
}