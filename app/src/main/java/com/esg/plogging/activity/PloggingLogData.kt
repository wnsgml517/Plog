package com.esg.plogging.activity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

data class PloggingLogData (
    val UserID: String,
    val PloggingDate : String,
    val locationName : String,
    val PloggingDistance : Double,
    val TrashStroagePhotos: String,
    val OneLineReview : String,
    val PloggingTime : Int,
    val trailID : Int,
    val PloggingSticker: Int
) : java.io.Serializable