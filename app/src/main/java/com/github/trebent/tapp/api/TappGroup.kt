package com.github.trebent.tapp.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


data class TappGroup(
    val id: Int,
    val name: String,
    val emoji: String,
    val owner: String,
    val description: String?,
    val members: List<Account>?,
    val invites: List<Account>?,
) {
    fun memberCount(): Int = members?.size ?: 0
}

data class TappGroupInvitation(
    @SerializedName("group_id") val groupId: Int,
    @SerializedName("group_name") val groupName: String,
    val email: String,
)

val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

data class Tapp(
    @SerializedName("group_id") val groupId: Int,
    val time: Long,
    val user: Account
) {
    fun timeString(): String {
        return sdf.format(Date(time))
    }
}

interface GroupService {
    @GET("/groups")
    suspend fun listGroups(@Header("Authorization") token: String): Response<List<TappGroup>>

    @POST("/groups")
    suspend fun createGroup(
        @Header("Authorization") token: String,
        @Body() group: TappGroup
    ): Response<TappGroup>

    @GET("/groups/{groupID}")
    suspend fun getGroup(
        @Header("Authorization") token: String,
        @Path("groupID") groupID: Int,
    ): Response<TappGroup>

    @PUT("/groups/{groupID}")
    suspend fun updateGroup(
        @Header("Authorization") token: String,
        @Path("groupID") groupID: Int,
        @Body() group: TappGroup
    ): Response<TappGroup>

    @DELETE("/groups/{groupID}")
    suspend fun deleteGroup(
        @Header("Authorization") token: String,
        @Path("groupID") groupID: Int,
    ): Response<Unit>

    @POST("/groups/{groupID}/invite")
    suspend fun inviteToGroup(
        @Header("Authorization") token: String,
        @Path("groupID") groupID: Int,
        @Query("email") email: String,
    ): Response<Unit>

    @POST("/groups/{groupID}/join")
    suspend fun joinGroup(
        @Header("Authorization") token: String,
        @Path("groupID") groupID: Int,
    ): Response<Unit>

    @POST("/groups/{groupID}/decline")
    suspend fun declineGroup(
        @Header("Authorization") token: String,
        @Path("groupID") groupID: Int,
    ): Response<Unit>

    @POST("/groups/{groupID}/kick")
    suspend fun kickFromGroup(
        @Header("Authorization") token: String,
        @Path("groupID") groupID: Int,
        @Query("email") email: String,
    ): Response<Unit>

    @POST("/groups/{groupID}/leave")
    suspend fun leaveGroup(
        @Header("Authorization") token: String,
        @Path("groupID") groupID: Int,
    ): Response<Unit>

    @GET("groups/invitations")
    suspend fun listInvitations(
        @Header("Authorization") token: String,
    ): Response<List<TappGroupInvitation>>

    @POST("groups/{groupID}/tapp")
    suspend fun createTapp(
        @Header("Authorization") token: String,
        @Path("groupID") groupID: Int,
    ): Response<Unit>

    @GET("groups/{groupID}/tapp")
    suspend fun listTapps(
        @Header("Authorization") token: String,
        @Path("groupID") groupID: Int,
    ): Response<List<Tapp>>
}
