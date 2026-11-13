package kr.sunsetcoast.controller.managementfee.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.sunsetcoast.domain.managementfee.entity.ManagementFeeEntity
import java.math.BigDecimal

@Schema(description = "(예약, 객실)별 현장 관리비")
data class ManagementFeeResponseBody(
    @field:Schema(description = "관리비 ID", example = "12")
    val id: Long,
    @field:Schema(description = "예약 ID", example = "656")
    val reservationId: Long,
    @field:Schema(description = "플랫폼", example = "YEOGI")
    val source: String,
    @field:Schema(description = "플랫폼 예약번호", example = "20260801123456")
    val reservationNumber: String,
    @field:Schema(description = "예약자 이름", nullable = true, example = "홍길동")
    val bookerName: String?,
    @field:Schema(description = "객실 ID", example = "26")
    val roomId: Long,
    @field:Schema(description = "객실 이름", example = "RESORT_305")
    val roomName: String,
    @field:Schema(description = "입실일", nullable = true, example = "2026-08-03")
    val checkInDate: String?,
    @field:Schema(description = "박수", nullable = true, example = "1")
    val nights: Int?,
    @field:Schema(description = "예약 상태", example = "CONFIRMED")
    val status: String,
    @field:Schema(
        description = "CHARGED: 지급, CANCELLED: 취소, CONSECUTIVE: 이어진 예약(앞 예약에서 지급), NO_STAY: 투숙한 밤 없음, NO_RATE: 단가 없음",
        allowableValues = ["CHARGED", "CANCELLED", "CONSECUTIVE", "NO_STAY", "NO_RATE"],
        example = "CHARGED",
    )
    val basis: String,
    @field:Schema(description = "CONSECUTIVE일 때 앞 예약 ID", nullable = true, example = "655")
    val consecutiveOfReservationId: Long?,
    @field:Schema(description = "자동 계산 금액", example = "30000")
    val calculatedAmount: BigDecimal,
    @field:Schema(description = "관리자가 고친 금액", nullable = true, example = "null")
    val overrideAmount: BigDecimal?,
    @field:Schema(description = "수정 사유", nullable = true)
    val overrideReason: String?,
    @field:Schema(description = "실제 지급액 (수정 금액이 있으면 수정 금액)", example = "30000")
    val amount: BigDecimal,
) {
    companion object {
        fun from(entity: ManagementFeeEntity) = ManagementFeeResponseBody(
            id = entity.id!!,
            reservationId = entity.reservation.id!!,
            source = entity.reservation.source.name,
            reservationNumber = entity.reservation.reservationNumber,
            bookerName = entity.reservation.bookerName,
            roomId = entity.room.id!!,
            roomName = entity.room.name,
            checkInDate = entity.reservation.checkInDate?.toString(),
            nights = entity.reservation.nights,
            status = entity.reservation.status.name,
            basis = entity.basis.name,
            consecutiveOfReservationId = entity.consecutiveOfReservationId,
            calculatedAmount = entity.calculatedAmount,
            overrideAmount = entity.overrideAmount,
            overrideReason = entity.overrideReason,
            amount = entity.amount,
        )
    }
}
