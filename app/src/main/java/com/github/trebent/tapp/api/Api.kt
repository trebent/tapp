package com.github.trebent.tapp.api

import com.github.trebent.tapp.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(5, TimeUnit.SECONDS)
    .readTimeout(5, TimeUnit.SECONDS)
    .writeTimeout(5, TimeUnit.SECONDS)
    .retryOnConnectionFailure(true)
    .build()

var retrofit: Retrofit = Retrofit.Builder()
    .baseUrl(BuildConfig.TAPP_API_ENDPOINT)
    .client(okHttpClient)
    .addConverterFactory(GsonConverterFactory.create())
    .build()

val accountService = retrofit.create(AccountService::class.java)
val groupService = retrofit.create(GroupService::class.java)
