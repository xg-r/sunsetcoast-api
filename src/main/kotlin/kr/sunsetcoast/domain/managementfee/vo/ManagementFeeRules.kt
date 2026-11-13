package kr.sunsetcoast.domain.managementfee.vo

import java.math.BigDecimal
import java.time.LocalDate

/** 관리비 계산에 필요한 (예약, 객실) 정보 */
data class FeeStay(
    val reservationId: Long,
    val roomId: Long,
    val bookerName: String?,
    val cancelled: Boolean,
    val checkInDate: LocalDate?,
    val nights: Int?,
) {
    val stayed: Boolean get() = !cancelled && checkInDate != null && (nights ?: 0) > 0
    val checkOutDate: LocalDate? get() = if (stayed) checkInDate!!.plusDays(nights!!.toLong()) else null
}

data class FeeDecision(
    val basis: ManagementFeeBasis,
    val amount: BigDecimal,
    val consecutiveOfReservationId: Long? = null,
)

/**
 * 현장 관리비 규칙 (SETTLE-02).
 *
 * - (예약, 객실)마다 1회, 박수와 무관
 * - 취소 예약은 0원
 * - 같은 예약자(이름 기준)가 같은 객실을 이어서 따로 예약하면(앞 예약 퇴실일 = 뒤 예약 입실일) 뒤 예약은 0원.
 *   플랫폼이 달라도 이름이 같으면 이어진 것으로 본다. 3건이 이어지면 첫 예약만 지급한다.
 */
object ManagementFeeRules {
    fun decide(stays: Collection<FeeStay>, rate: (roomId: Long, checkIn: LocalDate) -> BigDecimal?): Map<FeeStay, FeeDecision> {
        val byCheckOut = stays
            .filter { it.stayed && normalizeName(it.bookerName) != null }
            .groupBy { Triple(it.roomId, normalizeName(it.bookerName), it.checkOutDate) }

        return stays.associateWith { stay ->
            when {
                stay.cancelled -> FeeDecision(ManagementFeeBasis.CANCELLED, BigDecimal.ZERO)
                !stay.stayed -> FeeDecision(ManagementFeeBasis.NO_STAY, BigDecimal.ZERO)
                else -> {
                    val previous = normalizeName(stay.bookerName)
                        ?.let { byCheckOut[Triple(stay.roomId, it, stay.checkInDate)] }
                        ?.filter { it.reservationId != stay.reservationId }
                        ?.minByOrNull { it.reservationId }
                    val amount = rate(stay.roomId, stay.checkInDate!!)
                    when {
                        previous != null -> FeeDecision(ManagementFeeBasis.CONSECUTIVE, BigDecimal.ZERO, previous.reservationId)
                        amount == null -> FeeDecision(ManagementFeeBasis.NO_RATE, BigDecimal.ZERO)
                        else -> FeeDecision(ManagementFeeBasis.CHARGED, amount)
                    }
                }
            }
        }
    }

    /** 공백을 지우고 비교한다. ("홍 길동" = "홍길동") */
    fun normalizeName(name: String?): String? =
        name?.filterNot { it.isWhitespace() }?.lowercase()?.ifEmpty { null }
}
