package kr.sunsetcoast.controller.analytics

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kr.sunsetcoast.domain.analytics.service.RevenueAnalyticsService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Analytics", description = "매출 분석 (슈퍼어드민)")
interface RevenueAnalyticsControllerDocs {
    @Operation(
        summary = "월 매출 전년 동월 비교",
        description = "예약(정산 행) 기준. 월은 숙박일 기준, 외부 소유주 객실 제외. 정산 행이 없는 달은 null(데이터 없음)이라 0원과 구분한다. " +
            "작년 비교를 하려면 작년 정산 파일(예: 떠나요 AdjustCalendar 2025)을 올려야 한다.",
    )
    fun getRevenueComparison(
        @Parameter(description = "연도", example = "2026") year: Int,
        @Parameter(description = "업소 ID (생략하면 전체)", example = "1") propertyId: Long?,
    ): RevenueComparisonResponseBody
}

@RestController
@RequestMapping("/analytics")
class RevenueAnalyticsController(
    private val revenueAnalyticsService: RevenueAnalyticsService,
) : RevenueAnalyticsControllerDocs {
    @GetMapping("/revenue", version = "1.0")
    override fun getRevenueComparison(
        @RequestParam year: Int,
        @RequestParam(required = false) propertyId: Long?,
    ): RevenueComparisonResponseBody =
        RevenueComparisonResponseBody.from(revenueAnalyticsService.compareYear(year, propertyId))
}
