package kr.sunsetcoast.controller.managementfee.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.sunsetcoast.domain.managementfee.entity.ManagementFeeEntity
import kr.sunsetcoast.domain.managementfee.service.ManagementFeeService
import java.math.BigDecimal
import java.time.YearMonth

@Schema(description = "월별 현장 관리비 (입실일 기준)")
data class MonthlyManagementFeeResponseBody(
    @field:Schema(description = "업소 ID (지정한 경우)", nullable = true, example = "1")
    val propertyId: Long?,
    @field:Schema(description = "사업자 ID (지정한 경우)", nullable = true, example = "1")
    val businessOwnerId: Long?,
    @field:Schema(description = "입실 월", example = "2026-08")
    val month: String,
    @field:Schema(description = "지급 건수 (관리비 + 추가요금이 0보다 큰 행)", example = "120")
    val chargedCount: Int,
    @field:Schema(description = "관리비 합계 (추가요금 제외)", example = "5160000")
    val feeAmount: BigDecimal,
    @field:Schema(description = "추가요금 합계", example = "140000")
    val extraAmount: BigDecimal,
    @field:Schema(description = "지급 합계 = 관리비 + 추가요금", example = "5300000")
    val totalAmount: BigDecimal,
    @field:Schema(description = "매출 합계 (판매가, 매출을 아는 행만)", example = "48000000")
    val grossAmount: BigDecimal,
    @field:Schema(description = "정산금액 합계 (수수료 뺀 금액, 매출을 아는 행만)", example = "43000000")
    val netAmount: BigDecimal,
    @field:Schema(description = "입금 완료 합계 (입금 완료로 표시할 때의 금액)", example = "3000000")
    val paidAmount: BigDecimal,
    @field:Schema(description = "미입금 건수 (지급액이 있고 입금 완료가 아닌 행)", example = "40")
    val unpaidCount: Int,
    @field:Schema(description = "미입금 합계", example = "2160000")
    val unpaidAmount: BigDecimal,
    @field:Schema(description = "확인한 건수 (금액이 바뀐 건 제외)", example = "32")
    val verifiedCount: Int,
    @field:Schema(description = "확인한 뒤 금액이 바뀌어 다시 확인해야 하는 건수", example = "1")
    val reverifyCount: Int,
    val items: List<ManagementFeeResponseBody>,
) {
    companion object {
        fun from(
            propertyId: Long?,
            businessOwnerId: Long?,
            month: YearMonth,
            fees: List<ManagementFeeEntity>,
            revenue: Map<Long, ManagementFeeService.FeeRevenue>,
        ) = MonthlyManagementFeeResponseBody(
            propertyId = propertyId,
            businessOwnerId = businessOwnerId,
            month = month.toString(),
            chargedCount = fees.count { it.payableAmount.signum() > 0 },
            feeAmount = fees.fold(BigDecimal.ZERO) { sum, fee -> sum + fee.amount },
            extraAmount = fees.fold(BigDecimal.ZERO) { sum, fee -> sum + fee.extraAmount },
            totalAmount = fees.fold(BigDecimal.ZERO) { sum, fee -> sum + fee.payableAmount },
            grossAmount = revenue.values.fold(BigDecimal.ZERO) { sum, r -> sum + r.gross },
            netAmount = revenue.values.fold(BigDecimal.ZERO) { sum, r -> sum + r.net },
            paidAmount = fees.filter { it.paid }.fold(BigDecimal.ZERO) { sum, fee -> sum + fee.paidAmount!! },
            unpaidCount = fees.count { !it.paid && it.payableAmount.signum() > 0 },
            unpaidAmount = fees.filter { !it.paid }.fold(BigDecimal.ZERO) { sum, fee -> sum + fee.payableAmount },
            verifiedCount = fees.count { it.verified && !it.amountChangedAfterVerification },
            reverifyCount = fees.count { it.verified && it.amountChangedAfterVerification },
            items = fees.map { ManagementFeeResponseBody.from(it, revenue[it.id]) },
        )
    }
}
