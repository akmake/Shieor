package com.example.goodstart.network

import com.example.goodstart.model.Study
import com.example.goodstart.model.StudyDay
import retrofit2.http.GET
import retrofit2.http.Query

interface StudyService {
    @GET("api/study/day")
    suspend fun getDailyStudy(@Query("date") date: String): StudyDay

    @GET("api/study/tehillim-chapters")
    suspend fun getTehillimChapters(@Query("chapters") chapters: String): Study
}
