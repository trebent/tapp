package com.github.trebent.tapp.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path


data class Account(val email: String, val password: String, val tag: String?)
data class LoginRequest(val email: String, val password: String)

interface AccountService {
    @POST("/accounts")
    suspend fun createAccount(@Body() account: Account): Response<Account>

    @PUT("/accounts/{email}")
    suspend fun updateAccount(
        @Path("email") email: String,
        @Body() account: Account
    ): Response<Account>

    @GET("/accounts/{email}")
    suspend fun getAccount(
        @Path("email") email: String,
        @Body() account: Account
    ): Response<Account>

    @DELETE("/accounts/{email}")
    suspend fun deleteAccount(
        @Path("email") email: String,
        @Body() account: Account
    ): Response<Account>

    @POST("/password")
    suspend fun updatePassword(
        @Body() account: Account
    ): Response<Unit>

    @POST("/login")
    suspend fun login(@Body() loginRequest: LoginRequest): Response<Unit>

    @POST("/logout")
    suspend fun logout(): Response<Unit>
}