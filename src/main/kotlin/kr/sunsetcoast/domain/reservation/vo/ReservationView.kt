package kr.sunsetcoast.domain.reservation.vo

import kr.sunsetcoast.domain.managementfee.entity.ManagementFeeEntity
import kr.sunsetcoast.domain.reservation.entity.ReservationEntity
import kr.sunsetcoast.domain.settlement.entity.SettlementLineEntity
import java.math.BigDecimal
import java.time.LocalDate

/** 예약 목록 한 줄 */
data class ReservationSummary(
    val reservation: ReservationEntity,
    /** 입실일. 모르면(취소수수료만 있는 예약) 매출인식일 */
    val baseDate: LocalDate?,
    val managementFee: BigDecimal?,
)

/** 예약 상세: 정산 행과 관리비 */
data class ReservationDetail(
    val reservation: ReservationEntity,
    val lines: List<SettlementLineWithFile>,
    val fees: List<ManagementFeeEntity>,
)

data class SettlementLineWithFile(
    val line: SettlementLineEntity,
    val fileName: String?,
)
