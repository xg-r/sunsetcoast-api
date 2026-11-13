package kr.sunsetcoast.domain.settlement.vo

enum class SettlementLineType {
    /** 1박 투숙 매출 */
    ROOM_NIGHT,

    /** 취소수수료 (매출이지만 점유율/ADR에서는 제외) */
    CANCELLATION_FEE,

    /** 입실 후 취소로 앞서 정산된 1박을 차감 (금액 음수) */
    ROOM_NIGHT_REVERSAL,
}
