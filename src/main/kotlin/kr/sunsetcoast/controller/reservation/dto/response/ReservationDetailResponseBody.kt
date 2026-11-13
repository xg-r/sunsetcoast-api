package kr.sunsetcoast.controller.reservation.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.sunsetcoast.controller.managementfee.dto.response.ManagementFeeResponseBody
import kr.sunsetcoast.domain.reservation.vo.ReservationDetail
import kr.sunsetcoast.domain.reservation.vo.SettlementLineWithFile
import java.math.BigDecimal

@Schema(description = "예약 상세: 정산 행과 관리비")
data class ReservationDetailResponseBody(
    val reservation: ReservationResponseBody,
    @field:Schema(description = "정산 행 (1박, 취소수수료, 입실 후 취소)")
    val lines: List<Line>,
    val managementFees: List<ManagementFeeResponseBody>,
) {
    @Schema(name = "ReservationSettlementLine", description = "정산 행")
    data class Line(
        val id: Long,
        @field:Schema(
            description = "ROOM_NIGHT: 1박, CANCELLATION_FEE: 취소수수료, ROOM_NIGHT_REVERSAL: 입실 후 취소(차감)",
            allowableValues = ["ROOM_NIGHT", "CANCELLATION_FEE", "ROOM_NIGHT_REVERSAL"],
        )
        val lineType: String,
        @field:Schema(description = "숙박일 (취소수수료는 매출인식일)", example = "2026-08-03")
        val stayDate: String,
        @field:Schema(description = "정산(지급) 예정일", nullable = true, example = "2026-08-12")
        val settledOn: String?,
        val grossAmount: BigDecimal,
        @field:Schema(description = "우리 부담 할인")
        val partnerDiscount: BigDecimal,
        @field:Schema(description = "플랫폼 부담 할인")
        val platformDiscount: BigDecimal,
        @field:Schema(description = "수수료율 (%)", nullable = true, example = "15.00")
        val commissionRate: BigDecimal?,
        val commissionAmount: BigDecimal,
        @field:Schema(description = "기타 비용 (NOL 부스팅 이용료 등)")
        val otherFee: BigDecimal,
        val netAmount: BigDecimal,
        @field:Schema(description = "떠나요 연박 금액을 박수로 나눈 추정값인지")
        val splitEstimated: Boolean,
        @field:Schema(description = "이 행을 마지막으로 반영한 업로드 파일", nullable = true)
        val fileName: String?,
    ) {
        companion object {
            fun from(value: SettlementLineWithFile) = value.line.let {
                Line(
                    id = it.id!!,
                    lineType = it.lineType.name,
                    stayDate = it.stayDate.toString(),
                    settledOn = it.settledOn?.toString(),
                    grossAmount = it.grossAmount,
                    partnerDiscount = it.partnerDiscount,
                    platformDiscount = it.platformDiscount,
                    commissionRate = it.commissionRate,
                    commissionAmount = it.commissionAmount,
                    otherFee = it.otherFee,
                    netAmount = it.netAmount,
                    splitEstimated = it.splitEstimated,
                    fileName = value.fileName,
                )
            }
        }
    }

    companion object {
        fun from(detail: ReservationDetail): ReservationDetailResponseBody {
            val fee = detail.fees.takeIf { it.isNotEmpty() }?.fold(BigDecimal.ZERO) { sum, f -> sum + f.amount }
            val baseDate = detail.reservation.checkInDate ?: detail.lines.minOfOrNull { it.line.stayDate }
            return ReservationDetailResponseBody(
                reservation = ReservationResponseBody.of(detail.reservation, baseDate, fee),
                lines = detail.lines.map { Line.from(it) },
                managementFees = detail.fees.map { ManagementFeeResponseBody.from(it) },
            )
        }
    }
}
