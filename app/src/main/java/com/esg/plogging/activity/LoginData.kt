package com.esg.plogging.activity

import com.google.gson.annotations.SerializedName


data class LoginData(
    val logUserID: String?,
    var nickname: String?,
    var bio: String?,
    val totalLog: Int?,
    val totalDistance: Double?,
    val totalTime: Any?,
    var profilePhoto : String
)