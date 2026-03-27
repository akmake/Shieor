package com.example.goodstart.network

import com.example.goodstart.model.ArticleDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

data class ExtractResponse(val rawText: String, val pageCount: Int)

interface ArticleUploadService {

    @Multipart
    @POST("api/articles/extract")
    suspend fun extractText(
        @Part pdf: MultipartBody.Part
    ): ExtractResponse

    @Multipart
    @POST("api/articles/upload")
    suspend fun uploadArticle(
        @Part pdf:                MultipartBody.Part,
        @Part("rawText")  rawText:   RequestBody,
        @Part("pageCount") pageCount: RequestBody,
        @Part("title")    title:     RequestBody
    ): ArticleDto
}
