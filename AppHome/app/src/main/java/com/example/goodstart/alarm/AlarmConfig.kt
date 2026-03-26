package com.example.goodstart.alarm

data class AlarmConfig(
    val zmanLabel: String = "",
    val offsetMinutes: Int = 0,
    val isBefore: Boolean = true,
    val ringCount: Int = 3,
    val ringtoneUri: String = ""
)
