package kr.sunsetcoast.controller.managementfee.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.sunsetcoast.domain.managementfee.service.ManagementFeeService
import java.math.BigDecimal

@Schema(description = "입금 완료된 관리비, 사업자 × 월")
data class PaidManagementFeeSummaryResponseBody(
    val from: String,
    val to: String,
    @field:Schema(description = "FEE_MONTH: 입실 월 기준, PAID_MONTH: 입금 완료로 표시한 달 기준", example = "FEE_MONTH")
    val basis: String,
    @field:Schema(example = "[\"2026-08\", \"2026-09\"]") val months: List<String>,
    @field:Schema(description = "사업자별 월 금액과 합계 (금액이 큰 순)") val owners: List<Owner>,
    @field:Schema(description = "월별 합계 (months 순서)") val monthTotals: List<Amount>,
    val total: BigDecimal,
    val totalCount: Int,
) {
    data class Amount(val amount: BigDecimal, val count: Int)

    data class Owner(
        val businessOwnerId: Long,
        val name: String,
        @field:Schema(description = "months 순서의 월별 금액") val months: List<Amount>,
        val total: BigDecimal,
        val count: Int,
        @field:Schema(description = "총액 대비 비율 (%)", nullable = true) val share: BigDecimal?,
    )

    companion object {
        fun from(s: ManagementFeeService.PaidSummary): PaidManagementFeeSummaryResponseBody {
            val cells = s.cells.associateBy { it.ownerId to it.month }
            val zero = Amount(BigDecimal.ZERO, 0)
            val total = s.cells.sumOf { it.amount }
            val owners = s.owners.map { o ->
                val months = s.months.map { m -> cells[o.id to m]?.let { Amount(it.amount, it.count) } ?: zero }
                val ownerTotal = months.sumOf { it.amount }
                Owner(
                    businessOwnerId = o.id,
                    name = o.name,
                    months = months,
                    total = ownerTotal,
                    count = months.sumOf { it.count },
                    share = if (total.signum() == 0) null else ownerTotal.multiply(BigDecimal(100)).divide(total, 1, java.math.RoundingMode.HALF_UP),
                )
            }.sortedByDescending { it.total }
            return PaidManagementFeeSummaryResponseBody(
                from = s.from.toString(),
                to = s.to.toString(),
                basis = s.basis.name,
                months = s.months.map { it.toString() },
                owners = owners,
                monthTotals = s.months.map { m ->
                    val list = s.cells.filter { it.month == m }
                    Amount(list.sumOf { it.amount }, list.sumOf { it.count })
                },
                total = total,
                totalCount = s.cells.sumOf { it.count },
            )
        }
    }
}
