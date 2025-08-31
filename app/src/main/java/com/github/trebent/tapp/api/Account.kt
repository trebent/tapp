package com.github.trebent.tapp.api

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

@Serializable
data class Account(val email: String, val password: String, val tag: String?)
data class LoginRequest(val email: String, val password: String)

interface AccountService {
    @POST("/accounts")
    suspend fun createAccount(@Body() account: Account): Response<Account>

    @POST("/login")
    suspend fun login(@Body() loginRequest: LoginRequest): Response<Unit>

    @POST("/logout")
    suspend fun logout(): Response<Unit>
}