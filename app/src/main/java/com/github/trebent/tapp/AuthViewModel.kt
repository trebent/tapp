package com.github.trebent.tapp


import androidx.lifecycle.ViewModel
import com.github.trebent.tapp.BuildConfig.TEST_MODE


const val testUsername = "u"
const val testPassword = "p"

const val testMode = TEST_MODE

class AuthViewModel(var loginState: Boolean = false) : ViewModel() {

    fun isLoggedIn(): Boolean {
        return loginState
    }

    fun login(username: String, password: String) {
        if (testMode && username == testUsername && password == testPassword) {
            loginState = true
        }
    }

    fun logout() {
        loginState = false
    }
}