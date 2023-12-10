package com.esg.plogging.activity

import com.google.gson.annotations.SerializedName


data class LoginData(
    val logUserID: String?,
    val nickname: String?,
    val bio: String?,
    val totalLog: Int?,
    val totalDistance: Double?,
    val totalTime: Any? // 여기서 Any?는 어떤 형식의 데이터든 받을 수 있도록 하기 위함입니다.
)