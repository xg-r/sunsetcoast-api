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
    val items: List<ManagementFeeResponseBody>,
) {
    companion object {
        fun from(propertyId: Long?, businessOwnerId: Long?, month: YearMonth, fees: List<ManagementFeeEntity>) = MonthlyManagementFeeResponseBody(
            propertyId = propertyId,
            businessOwnerId = businessOwnerId,
            month = month.toString(),
            chargedCount = fees.count { it.amount.signum() > 0 },
            totalAmount = fees.fold(BigDecimal.ZERO) { sum, fee -> sum + fee.amount },
            items = fees.map { ManagementFeeResponseBody.from(it) },
        )
    }
}
