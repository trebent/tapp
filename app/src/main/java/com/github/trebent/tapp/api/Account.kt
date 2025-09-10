/**
 * Account related declarations and Retrofit services, mapping objects to the Tapp backend..
 */

package com.github.trebent.tapp.api

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path


/**
 * Encompasses the background object used to keep track of a user's account information. For
 * security purposes, the password field will never be populated, EXCEPT for when the account is
 * created. It is only here to help align object usage across Tapp.
 */
@Parcelize
data class Account(val email: String, val password: String?, val tag: String?) : Parcelable {
    /**
     * User identifier utility, will utilise the tag if present, otherwise the email.
     *
     * @return tag or email
     */
    fun userIdentifier(): String {
        if (tag != null && tag != "") {
            return tag
        }
        return email
    }
}

/**
 * A Login request object, as expected by the Tapp backend.
 */
data class LoginRequest(val email: String, val password: String)

/**
 * A password change request object, as expected by the Tapp backend.
 */
data class ChangePasswordRequest(val password: String)

/**
 * The AccountService is used for everything related to the user's account, from sign up to
 * account deletion. It also includes the FCM update request, as it relates to the user account's
 * email.
 */
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
    suspend fun pushFCM(
        @Header("X-fcm-token") fcm: String,
        @Header("Authorization") token: String
    ): Response<Unit>
}