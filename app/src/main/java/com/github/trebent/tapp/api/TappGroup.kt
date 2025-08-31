package com.github.trebent.tapp.api

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Header
import retrofit2.http.POST

@Serializable
data class RemoteTappGroup(
    val id: Int,
    val name: String,
    val emoji: String,
    val description: String,
    val members: List<Account>,
    val invites: List<Account>,
)

interface GroupService {
    @POST("/groups")
    suspend fun listGroups(@Header("Authorization") token: String): Response<List<RemoteTappGroup>>
}
