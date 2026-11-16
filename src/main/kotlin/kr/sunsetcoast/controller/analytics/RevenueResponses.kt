package kr.sunsetcoast.controller.analytics

import io.swagger.v3.oas.annotations.media.Schema
import kr.sunsetcoast.domain.analytics.service.RevenueAnalyticsService
import java.math.BigDecimal

@Schema(description = "예약 기준 월 매출 전년 동월 비교")
data class RevenueComparisonResponseBody(
    val year: Int,
    @field:Schema(nullable = true) val propertyId: Long?,
    val months: List<Month>,
    @field:Schema(description = "올해·작년 모두 데이터가 있는 달", example = "[\"2026-07\", \"2026-08\"]") val comparableMonths: List<String>,
    @field:Schema(description = "비교 가능한 달 합계 (올해)", nullable = true) val comparableCurrent: Totals?,
    @field:Schema(description = "비교 가능한 달 합계 (작년 같은 달)", nullable = true) val comparablePrevious: Totals?,
    @field:Schema(description = "비교 가능한 달 매출 증감률 (%)", nullable = true) val comparableGrossChange: BigDecimal?,
    @field:Schema(nullable = true) val comparableNetChange: BigDecimal?,
) {
    data class Totals(
        @field:Schema(description = "매출 (판매가, 취소수수료 포함)") val gross: BigDecimal,
        @field:Schema(description = "정산액 (수수료·할인 뺀 금액)") val net: BigDecimal,
        @field:Schema(description = "객실·박 수") val roomNights: Long,
        val cancellationFees: BigDecimal,
        @field:Schema(description = "객실 1박 평균 판매가 (취소수수료 제외)", nullable = true) val adr: BigDecimal?,
        @field:Schema(description = "플랫폼 → 매출") val byPlatform: Map<String, BigDecimal>,
    )

    data class Month(
        @field:Schema(example = "2026-08") val month: String,
        @field:Schema(description = "올해. 정산 행이 없으면 null", nullable = true) val current: Totals?,
        @field:Schema(description = "작년 같은 달. 정산 행이 없으면 null (데이터 없음)", nullable = true) val previous: Totals?,
        @field:Schema(description = "매출 증감률 (%)", nullable = true) val grossChange: BigDecimal?,
        @field:Schema(nullable = true) val netChange: BigDecimal?,
        @field:Schema(nullable = true) val roomNightsChange: BigDecimal?,
        @field:Schema(nullable = true) val adrChange: BigDecimal?,
    )

    companion object {
        private fun totals(t: RevenueAnalyticsService.MonthTotals?) = t?.let {
            Totals(it.gross, it.net, it.roomNights, it.cancellationFees, it.adr, it.byPlatform.mapKeys { (k, _) -> k.name })
        }

        fun from(c: RevenueAnalyticsService.YearComparison) = RevenueComparisonResponseBody(
            year = c.year,
            propertyId = c.propertyId,
            months = c.months.map {
                Month(it.month.toString(), totals(it.current), totals(it.previous), it.grossChange, it.netChange, it.roomNightsChange, it.adrChange)
            },
            comparableMonths = c.comparableMonths.map { it.toString() },
            comparableCurrent = totals(c.comparableCurrent),
            comparablePrevious = totals(c.comparablePrevious),
            comparableGrossChange = c.comparableGrossChange,
            comparableNetChange = c.comparableNetChange,
        )
    }
}
