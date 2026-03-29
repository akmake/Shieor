package com.example.goodstart.network

import retrofit2.Call
import retrofit2.http.*

/** API response from the server for user data. */
data class UserDataResponse(
    val userId: String,
    val readingPositions: Map<String, Int>?,
    val preferences: Map<String, Any>?,
    val savedArticleIds: List<String>?,
    val lastSyncAt: String?
)

/** Request body for POST /register. */
data class RegisterResponse(val userId: String)

/** Request body for POST /login. */
data class LoginRequest(val userId: String)

/** Request body for PUT /sync. */
data class SyncRequest(
    val readingPositions: Map<String, Int>?,
    val preferences: Map<String, Any>?,
    val savedArticleIds: List<String>?
)

interface UserService {
    @POST("api/user/register")
    fun register(): Call<RegisterResponse>

    @POST("api/user/login")
    fun login(@Body body: LoginRequest): Call<UserDataResponse>

    @GET("api/user/{userId}")
    fun getUserData(@Path("userId") userId: String): Call<UserDataResponse>

    @PUT("api/user/{userId}/sync")
    fun sync(@Path("userId") userId: String, @Body body: SyncRequest): Call<UserDataResponse>
}
