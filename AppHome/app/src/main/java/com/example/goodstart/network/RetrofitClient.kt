package com.example.goodstart.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "https://shieor.onrender.com/"

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val studyService:         StudyService         by lazy { retrofit.create(StudyService::class.java)         }
    val zmanimService:        ZmanimService        by lazy { retrofit.create(ZmanimService::class.java)        }
    val articleUploadService: ArticleUploadService by lazy { retrofit.create(ArticleUploadService::class.java) }
}
