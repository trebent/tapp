package com.github.trebent.tapp


import androidx.lifecycle.ViewModel


const val testUsername = "u"
const val testPassword = "p"


class AuthViewModel(var loginState: Boolean = false) : ViewModel() {

    fun isLoggedIn(): Boolean {
        return loginState
    }

    fun signup(tag: String, email: String, password: String): Boolean {
        return true
    }

    fun login(email: String, password: String): Boolean {
        if (BuildConfig.TAPP_TEST_MODE && email == testUsername && password == testPassword) {
            loginState = true
        }

        return loginState
    }

    fun logout() {
        loginState = false
    }
}