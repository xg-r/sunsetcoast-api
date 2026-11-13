package kr.sunsetcoast.domain.reservation.vo

enum class PaymentReceiver {
    /** 우리 계좌/플랫폼 정산으로 입금 */
    US,

    /** 외부 소유주 계좌로 직접 입금 (매출, 소유주 정산 제외) */
    OWNER,
}
