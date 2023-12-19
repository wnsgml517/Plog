package com.esg.plogging.activity

data class PrivateTrailData(
    val locationName: String,
    val RegionID: Int,
    val latitude: Double,
    val longitude: Double,
    val UserID: String,
    val TrashStroagePhotos : String,
    val totalDistance : Double,
    val totalVisit : Int
)