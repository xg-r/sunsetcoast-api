package kr.sunsetcoast.controller.managementfee.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.sunsetcoast.domain.managementfee.entity.ManagementFeeEntity
import kr.sunsetcoast.domain.managementfee.entity.ManagementFeeExtraEntity
import kr.sunsetcoast.domain.managementfee.service.ManagementFeeService
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
        description = "계산 근거. CHARGED: 지급 대상, CANCELLED: 취소, NO_STAY: 투숙한 밤 없음, NO_RATE: 단가 없음. 입금 여부는 paymentStatus",
        allowableValues = ["CHARGED", "CANCELLED", "NO_STAY", "NO_RATE"],
        example = "CHARGED",
    )
    val basis: String,
    @field:Schema(description = "자동 계산 금액", example = "30000")
    val calculatedAmount: BigDecimal,
    @field:Schema(description = "관리자가 고친 금액", nullable = true, example = "null")
    val overrideAmount: BigDecimal?,
    @field:Schema(description = "수정 사유", nullable = true)
    val overrideReason: String?,
    @field:Schema(description = "관리비 (수정 금액이 있으면 수정 금액). 추가요금 제외", example = "30000")
    val amount: BigDecimal,
    @field:Schema(description = "관리비와 함께 현장에 넘겨 줄 추가요금")
    val extras: List<Extra>,
    @field:Schema(description = "추가요금 합계", example = "70000")
    val extraAmount: BigDecimal,
    @field:Schema(description = "현장에 이체할 금액 = 관리비 + 추가요금. 입금 완료·확인은 이 금액 기준", example = "100000")
    val payableAmount: BigDecimal,
    @field:Schema(description = "이 객실의 매출 (판매가 합계, 취소수수료 포함). 관리비 화면에서만, 모르면 null", nullable = true, example = "180000")
    val grossAmount: BigDecimal?,
    @field:Schema(description = "이 객실의 정산금액 (수수료를 뺀 금액). 관리비 화면에서만, 모르면 null", nullable = true, example = "162000")
    val netAmount: BigDecimal?,
    @field:Schema(
        description = "입금 상태. NONE: 지급할 금액 없음(0원), UNPAID: 미입금, PAID: 입금 완료, " +
            "PAID_CHANGED: 입금 완료 뒤 지급액이 바뀜 (paidAmount와 payableAmount의 차액 확인)",
        allowableValues = ["NONE", "UNPAID", "PAID", "PAID_CHANGED"],
        example = "UNPAID",
    )
    val paymentStatus: String,
    @field:Schema(description = "입금 완료로 표시한 시각", nullable = true, example = "2026-11-16T14:05:00")
    val paidAt: String?,
    @field:Schema(description = "입금 완료로 표시할 때의 지급액 (관리비 + 추가요금)", nullable = true, example = "100000")
    val paidAmount: BigDecimal?,
    @field:Schema(
        description = "관리자 확인. NONE: 확인 안 함, VERIFIED: 확인함, CHANGED: 확인한 뒤 지급액이 바뀜 (다시 확인 필요)",
        allowableValues = ["NONE", "VERIFIED", "CHANGED"],
        example = "VERIFIED",
    )
    val verificationStatus: String,
    @field:Schema(description = "확인한 시각", nullable = true) val verifiedAt: String?,
    @field:Schema(description = "확인할 때의 지급액", nullable = true) val verifiedAmount: BigDecimal?,
) {
    @Schema(description = "추가요금 한 건. 금액 = 단가 × 수량 × 박수")
    data class Extra(
        @field:Schema(description = "추가요금 ID", example = "3") val id: Long,
        @field:Schema(description = "BBQ: 바베큐, EXTRA_PERSON: 추가인원(1박 1인당), OTHER: 기타", example = "EXTRA_PERSON") val item: String,
        @field:Schema(example = "20000") val unitPrice: BigDecimal,
        @field:Schema(description = "수량 (추가인원은 인원)", example = "2") val quantity: Int,
        @field:Schema(description = "박수 (1박 단위 요금만)", nullable = true, example = "1") val nights: Int?,
        @field:Schema(example = "40000") val amount: BigDecimal,
        @field:Schema(nullable = true) val memo: String?,
    ) {
        companion object {
            fun from(e: ManagementFeeExtraEntity) = Extra(e.id!!, e.item.name, e.unitPrice, e.quantity, e.nights, e.amount, e.memo)
        }
    }

    companion object {
        fun from(entity: ManagementFeeEntity, revenue: ManagementFeeService.FeeRevenue? = null) = ManagementFeeResponseBody(
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
            calculatedAmount = entity.calculatedAmount,
            overrideAmount = entity.overrideAmount,
            overrideReason = entity.overrideReason,
            amount = entity.amount,
            extras = entity.extras.sortedBy { it.id }.map { Extra.from(it) },
            extraAmount = entity.extraAmount,
            payableAmount = entity.payableAmount,
            grossAmount = revenue?.gross,
            netAmount = revenue?.net,
            paymentStatus = paymentStatus(entity),
            paidAt = entity.paidAt?.withNano(0)?.toString(),
            paidAmount = entity.paidAmount,
            verificationStatus = when {
                !entity.verified -> "NONE"
                entity.amountChangedAfterVerification -> "CHANGED"
                else -> "VERIFIED"
            },
            verifiedAt = entity.verifiedAt?.withNano(0)?.toString(),
            verifiedAmount = entity.verifiedAmount,
        )

        private fun paymentStatus(entity: ManagementFeeEntity): String = when {
            entity.paid && entity.amountChangedAfterPayment -> "PAID_CHANGED"
            entity.paid -> "PAID"
            entity.payableAmount.signum() > 0 -> "UNPAID"
            else -> "NONE"
        }
    }
}
