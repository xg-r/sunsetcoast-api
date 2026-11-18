package kr.sunsetcoast.domain.reservation.vo

import kr.sunsetcoast.domain.managementfee.entity.ManagementFeeEntity
import kr.sunsetcoast.domain.reservation.entity.ReservationEntity
import kr.sunsetcoast.domain.room.entity.RoomEntity
import kr.sunsetcoast.domain.settlement.entity.SettlementLineEntity
import java.math.BigDecimal
import java.time.LocalDate

/** 예약 목록 한 줄 */
data class ReservationSummary(
    val reservation: ReservationEntity,
    /** 예약의 객실 (다중 객실 예약은 여러 개) */
    val rooms: List<RoomEntity>,
    /** 입실일. 모르면(취소수수료만 있는 예약) 매출인식일 */
    val baseDate: LocalDate?,
    val managementFee: BigDecimal?,
    /** 현장 객실 변경 (관리비 화면에서 입력) */
    val roomChanges: List<RoomChange> = emptyList(),
)

/** 현장에서 방을 옮긴 기록. difference: 받은 차액 (매출에 더함) */
data class RoomChange(val from: RoomEntity, val to: RoomEntity, val difference: BigDecimal?, val memo: String?) {
    companion object {
        fun of(fees: Collection<ManagementFeeEntity>): List<RoomChange> =
            fees.mapNotNull { f -> f.movedRoom?.let { RoomChange(f.room, it, f.roomChangeDifference, f.roomChangeMemo) } }
                .sortedBy { it.from.id }
    }
}

/** 예약 상세: 정산 행과 관리비 */
data class ReservationDetail(
    val reservation: ReservationEntity,
    val rooms: List<RoomEntity>,
    val lines: List<SettlementLineWithFile>,
    val fees: List<ManagementFeeEntity>,
)

data class SettlementLineWithFile(
    val line: SettlementLineEntity,
    val fileName: String?,
)
