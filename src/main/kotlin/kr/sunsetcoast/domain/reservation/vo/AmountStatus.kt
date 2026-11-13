package kr.sunsetcoast.domain.reservation.vo

enum class AmountStatus {
    /** 예약 파일 기준 추정 금액 */
    ESTIMATED,

    /** 정산 파일 기준 확정 금액 */
    CONFIRMED,
}
