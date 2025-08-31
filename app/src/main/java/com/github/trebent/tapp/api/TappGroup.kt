package com.github.trebent.tapp.api

import kotlinx.serialization.Serializable
import retrofit2.http.POST

@Serializable
data class TappGroup(
    val id: Int,
    val name: String,
    val emoji: String,
    val description: String,
    var edit: Boolean
)

interface GroupService {
    @POST("/groups")
    suspend fun createGroup(group: TappGroup)
}
