/**
 * Group related services and objects, mapping to objects in the Tapp backend.
 */

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


/**
 * A Tapp ground is a collection of users that can "Tapp" each other. The group owner is the only
 * person that is allowed to change the group properties and invite new members. Members may only
 * "Tapp" the group, which means to notify them.
 */
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

/**
 * This is a utility class to simplify invitation handling. This object is only ever used to have a
 * singular endpoint to list all invitations related to a given user. The invitations per group is
 * also maintained inside each Tapp group object, but is not used for UI elements.
 */
data class TappGroupInvitation(
    @SerializedName("group_id") val groupId: Int,
    @SerializedName("group_name") val groupName: String,
    val email: String,
)

// datetime visalisation conversion formatter.
val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

/**
 * The whole point of this Tapplication, a Tapp! A Tapp is an object capturing the moment a user
 * pressed the "Tapp" button in a given group. It has a timestamp, a group ID and a user link. A
 * Tapp captures a user's tag (if there is one) at the time of the Tapp. If the user updates their
 * tag and tapps again, the old Tapps will still show the old user tag.
 */
data class Tapp(
    @SerializedName("group_id") val groupId: Int,
    // Time in UNIX millis format for sorting and serialization simplicity.
    val time: Long,
    val user: Account
) {
    fun timeString(): String {
        return sdf.format(Date(time))
    }
}

/**
 * Sort a list of Tapps in descending order, with the latest date first.
 */
fun sortedTapps(inputTapps: List<Tapp>): List<Tapp> {
    return inputTapps.sortedByDescending { it.time }
}

/**
 * The group service handles the administrative aspect of the Tapplictation. It lets users create,
 * update, list, get, and delete (etc.) groups. Group user management is also here.
 */
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
