package com.github.trebent.tapp.api

import com.github.trebent.tapp.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


var retrofit = Retrofit.Builder()
    .baseUrl(BuildConfig.TAPP_API_ENDPOINT)
    .addConverterFactory(GsonConverterFactory.create())
    .build()

val accountService = retrofit.create(AccountService::class.java)
val groupService = retrofit.create(GroupService::class.java)
