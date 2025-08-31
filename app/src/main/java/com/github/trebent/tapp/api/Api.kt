package com.github.trebent.tapp.api

import com.github.trebent.tapp.BuildConfig
import retrofit2.Retrofit

var retrofit = Retrofit.Builder()
    .baseUrl(BuildConfig.TAPP_API_ENDPOINT)
    .build()

val accountService = retrofit.create(AccountService::class.java)
val groupService = retrofit.create(GroupService::class.java)
