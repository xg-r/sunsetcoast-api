package kr.sunsetcoast.controller.managementfee.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.sunsetcoast.domain.managementfee.entity.ManagementFeeEntity
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
    @field:Schema(description = "지급 건수 (금액이 0보다 큰 행)", example = "120")
    val chargedCount: Int,
    @field:Schema(description = "지급 합계", example = "5160000")
    val totalAmount: BigDecimal,
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
        fun from(propertyId: Long?, businessOwnerId: Long?, month: YearMonth, fees: List<ManagementFeeEntity>) = MonthlyManagementFeeResponseBody(
            propertyId = propertyId,
            businessOwnerId = businessOwnerId,
            month = month.toString(),
            chargedCount = fees.count { it.amount.signum() > 0 },
            totalAmount = fees.fold(BigDecimal.ZERO) { sum, fee -> sum + fee.amount },
            paidAmount = fees.filter { it.paid }.fold(BigDecimal.ZERO) { sum, fee -> sum + fee.paidAmount!! },
            unpaidCount = fees.count { !it.paid && it.amount.signum() > 0 },
            unpaidAmount = fees.filter { !it.paid }.fold(BigDecimal.ZERO) { sum, fee -> sum + fee.amount },
            verifiedCount = fees.count { it.verified && !it.amountChangedAfterVerification },
            reverifyCount = fees.count { it.verified && it.amountChangedAfterVerification },
            items = fees.map { ManagementFeeResponseBody.from(it) },
        )
    }
}
