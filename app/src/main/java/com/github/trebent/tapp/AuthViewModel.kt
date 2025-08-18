package com.github.trebent.tapp


import androidx.lifecycle.ViewModel


const val testUsername = "u"
const val testPassword = "p"


class AuthViewModel(var loginState: Boolean = false) : ViewModel() {

    fun isLoggedIn(): Boolean {
        return loginState
    }

    fun login(username: String, password: String) {
        if (BuildConfig.TAPP_TEST_MODE && username == testUsername && password == testPassword) {
            loginState = true
        }
    }

    fun logout() {
        loginState = false
    }
}