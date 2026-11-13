package kr.sunsetcoast.domain.managementfee.vo

import java.math.BigDecimal
import java.time.LocalDate

/** 관리비 계산에 필요한 (예약, 객실) 정보 */
data class FeeStay(
    val reservationId: Long,
    val roomId: Long,
    val cancelled: Boolean,
    val checkInDate: LocalDate?,
    val nights: Int?,
) {
    val stayed: Boolean get() = !cancelled && checkInDate != null && (nights ?: 0) > 0
}

data class FeeDecision(
    val basis: ManagementFeeBasis,
    val amount: BigDecimal,
)

/**
 * 현장 관리비 규칙 (SETTLE-02).
 *
 * - (예약, 객실)마다 1회, 박수와 무관. 연박은 예약번호로만 판단한다
 *   (예약번호가 다르면 같은 손님이 이어서 묵어도 따로 지급. 이름 비교는 마스킹·동명이인 위험이 있어 하지 않는다)
 * - 취소 예약은 0원
 */
object ManagementFeeRules {
    fun decide(stay: FeeStay, rate: (roomId: Long, checkIn: LocalDate) -> BigDecimal?): FeeDecision =
        when {
            stay.cancelled -> FeeDecision(ManagementFeeBasis.CANCELLED, BigDecimal.ZERO)
            !stay.stayed -> FeeDecision(ManagementFeeBasis.NO_STAY, BigDecimal.ZERO)
            else -> rate(stay.roomId, stay.checkInDate!!)
                ?.let { FeeDecision(ManagementFeeBasis.CHARGED, it) }
                ?: FeeDecision(ManagementFeeBasis.NO_RATE, BigDecimal.ZERO)
        }
}
