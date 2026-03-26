package com.example.goodstart.model

import com.google.gson.annotations.SerializedName

data class ArticleDto(
    @SerializedName("_id")
    val id: String,
    val title: String,
    val rawText: String?,
    val createdAt: Long?
)
