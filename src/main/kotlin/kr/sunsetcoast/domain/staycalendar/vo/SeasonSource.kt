package kr.sunsetcoast.domain.staycalendar.vo

enum class SeasonSource {
    /** 예약 금액과 요금표로 자동 판단 */
    INFERRED,

    /** 관리자가 직접 지정 (자동 재계산이 덮어쓰지 않음) */
    MANUAL,
}
