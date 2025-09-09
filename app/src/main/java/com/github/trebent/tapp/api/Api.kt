/**
 * API glue, declaring the retrofit object and registering services.
 */

package com.github.trebent.tapp.api

import com.github.trebent.tapp.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


// Used to override defaults, sets more sane defaults for timeouts and retry policy.
val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(5, TimeUnit.SECONDS)
    .readTimeout(5, TimeUnit.SECONDS)
    .writeTimeout(5, TimeUnit.SECONDS)
    .retryOnConnectionFailure(true)
    .build()

// GSON is used for JSON conversion for all Tapp APIs.
var retrofit: Retrofit = Retrofit.Builder()
    .baseUrl(BuildConfig.TAPP_API_ENDPOINT)
    .client(okHttpClient)
    .addConverterFactory(GsonConverterFactory.create())
    .build()

// Services to register with retrofit.
val accountService = retrofit.create(AccountService::class.java)
val groupService = retrofit.create(GroupService::class.java)
