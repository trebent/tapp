package com.github.trebent.tapp.api

import retrofit2.http.POST

data class Account(val email: String, val password: String, var tag: String)

interface AccountService {
    @POST("/accounts")
    suspend fun createAccount(account: Account)
}