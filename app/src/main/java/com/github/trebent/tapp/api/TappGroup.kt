package com.github.trebent.tapp.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header


data class TappGroup(
    val id: Int,
    val name: String,
    val emoji: String,
    val description: String,
    val members: List<Account>,
    val invites: List<Account>,
)

interface GroupService {
    @GET("/groups")
    suspend fun listGroups(@Header("Authorization") token: String): Response<List<TappGroup>>
}
