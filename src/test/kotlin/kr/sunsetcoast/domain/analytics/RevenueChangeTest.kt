package kr.sunsetcoast.domain.analytics

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kr.sunsetcoast.domain.analytics.service.RevenueAnalyticsService
import java.math.BigDecimal

class RevenueChangeTest : FunSpec({
    test("증감률") {
        RevenueAnalyticsService.change(BigDecimal(1000), BigDecimal(1250)) shouldBe BigDecimal("25.0")
        RevenueAnalyticsService.change(BigDecimal(1000), BigDecimal(800)) shouldBe BigDecimal("-20.0")
    }

    test("작년 데이터가 없거나 0이면 비교하지 않는다") {
        RevenueAnalyticsService.change(null, BigDecimal(1000)) shouldBe null
        RevenueAnalyticsService.change(BigDecimal.ZERO, BigDecimal(1000)) shouldBe null
    }
})
