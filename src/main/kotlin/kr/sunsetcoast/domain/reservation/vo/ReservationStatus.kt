package kr.sunsetcoast.domain.reservation.vo

enum class ReservationStatus {
    /** 투숙(예정 포함) */
    CONFIRMED,

    /** 취소 (취소수수료만 남은 예약 포함) */
    CANCELLED,
}
