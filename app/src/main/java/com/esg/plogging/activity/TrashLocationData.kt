package com.esg.plogging.activity

data class TrashLocationData(
    var trashId : String? = null,
    var trashName: String? = null, // 미션 이름
    var latitude: Double, // 위도
    var longitude: Double // 경도
) {
    // 다음과 같이 기본값을 직접 속성 초기화 블록에서 설정
    init {
        if (trashName == null) {
            trashName = "Default Mission Name"
        }
    }
}