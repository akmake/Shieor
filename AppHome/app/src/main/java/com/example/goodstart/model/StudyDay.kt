package com.example.goodstart.model

data class StudyDay(
    val date: String? = null,
    val hebrewDate: String? = null,
    val studies: Map<String, Study>? = null
)
