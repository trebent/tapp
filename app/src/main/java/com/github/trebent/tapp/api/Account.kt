package com.github.trebent.tapp.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path


data class Account(val email: String, val password: String, val tag: String?) {
    fun userIdentifier(): String {
        if (tag != null && tag != "") {
            return tag
        }
        return email
    }
}

data class LoginRequest(val email: String, val password: String)
data class ChangePasswordRequest(val password: String)

interface AccountService {
    @POST("/accounts")
    suspend fun createAccount(@Body() account: Account): Response<Account>

    @PUT("/accounts/{email}")
    suspend fun updateAccount(
        @Header("Authorization") token: String,
        @Path("email") email: String,
        @Body() account: Account
    ): Response<Account>

    @GET("/accounts/{email}")
    suspend fun getAccount(
        @Header("Authorization") token: String,
        @Path("email") email: String,
    ): Response<Account>

    @DELETE("/accounts/{email}")
    suspend fun deleteAccount(
        @Header("Authorization") token: String,
        @Path("email") email: String,
    ): Response<Unit>

    @POST("/password")
    suspend fun updatePassword(
        @Header("Authorization") token: String,
        @Body() changePasswordRequest: ChangePasswordRequest
    ): Response<Unit>

    @POST("/login")
    suspend fun login(@Body() loginRequest: LoginRequest): Response<Unit>

    @POST("/logout")
    suspend fun logout(@Header("Authorization") token: String): Response<Unit>

    @PUT("/fcm")
    suspend fun pushFCM(@Header("X-fcm-token") fcm: String): Response<Unit>
}