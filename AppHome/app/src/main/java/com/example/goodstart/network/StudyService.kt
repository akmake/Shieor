package com.example.goodstart.network

import com.example.goodstart.model.StudyDay
import retrofit2.http.GET
import retrofit2.http.Query

interface StudyService {
    @GET("api/study/day")
    suspend fun getDailyStudy(@Query("date") date: String): StudyDay
}
