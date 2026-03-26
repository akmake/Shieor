package com.example.goodstart.model

data class Mamaar(
    val id: String,
    val title: String,
    val fileName: String,
    val sections: List<MamaarSection>,
    val createdAt: Long
)

data class MamaarSection(
    /** Null for the opening/intro block before any lettered section marker */
    val heading: String?,
    val body: String
)
