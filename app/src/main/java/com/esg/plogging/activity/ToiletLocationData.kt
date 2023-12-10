package com.esg.plogging.activity

data class ToiletLocationData(
    var toiletId : Long,
    var toiletName: String? = null, // 미션 이름
    var latitude: Double, // 위도
    var longitude: Double // 경도
) {
    // 다음과 같이 기본값을 직접 속성 초기화 블록에서 설정
    init {
        if (toiletName == null) {
            toiletName = "Default Mission Name"
        }
    }
}