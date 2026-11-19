package kr.sunsetcoast.domain.reservation.vo

/**
 * 고객이 실제로 예약한 채널. 떠나요는 네이버/야놀자/홈페이지 예약을 모아서 정산한다.
 * YEOGI_PARTNER: 여기어때 정산 파일의 판매채널 '여기어때 연동 예약 사이트' (여기어때와 연동된 다른 예약 사이트)
 */
enum class SalesChannel {
    NAVER, YANOLJA, YEOGI, YEOGI_PARTNER, DDNAYO, HOMEPAGE, PHONE, WALK_IN, OTHER,
    ;

    companion object {
        fun fromRaw(raw: String?): SalesChannel {
            val value = raw?.trim().orEmpty()
            return when {
                value.contains("네이버") -> NAVER
                value.contains("야놀자") || value.contains("NOL") -> YANOLJA
                value.contains("여기어때") && value.contains("연동") -> YEOGI_PARTNER
                value.contains("여기어때") -> YEOGI
                value.contains("떠나요") -> DDNAYO
                value.contains("홈페이지") -> HOMEPAGE
                else -> OTHER
            }
        }
    }
}
