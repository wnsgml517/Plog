package com.esg.plogging.activity

import com.google.gson.annotations.SerializedName


data class LoginData(
    val logUserID: String?,
    val nickname: String?,
    val bio: String?,
    val totalLog: Int?,
    val totalDistance: Double?,
    val totalTime: Any?,
    val profilePhoto : String
)