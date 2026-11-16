package kr.sunsetcoast.domain.managementfee

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kr.sunsetcoast.controller.managementfee.dto.response.PaidManagementFeeSummaryResponseBody
import kr.sunsetcoast.domain.managementfee.service.ManagementFeeService
import java.math.BigDecimal
import java.time.YearMonth

class PaidSummaryResponseTest : FunSpec({
    test("사업자 × 월 금액, 사업자 합계·비율, 월 합계, 총액. 입금이 없는 달은 0") {
        val aug = YearMonth.of(2026, 8)
        val sep = YearMonth.of(2026, 9)
        val oct = YearMonth.of(2026, 10)
        val summary = ManagementFeeService.PaidSummary(
            from = aug, to = oct, basis = ManagementFeeService.PaidBasis.FEE_MONTH,
            months = listOf(aug, sep, oct),
            owners = listOf(ManagementFeeService.PaidOwner(1, "선셋코스트"), ManagementFeeService.PaidOwner(2, "대흥씨엔에스(주)")),
            cells = listOf(
                ManagementFeeService.PaidCell(1, aug, BigDecimal(300_000), 7),
                ManagementFeeService.PaidCell(1, oct, BigDecimal(450_000), 10),
                ManagementFeeService.PaidCell(2, aug, BigDecimal(250_000), 5),
            ),
        )
        val body = PaidManagementFeeSummaryResponseBody.from(summary)

        body.total shouldBe BigDecimal(1_000_000)
        body.totalCount shouldBe 22
        body.monthTotals.map { it.amount } shouldBe listOf(BigDecimal(550_000), BigDecimal.ZERO, BigDecimal(450_000))
        body.owners.map { it.name } shouldBe listOf("선셋코스트", "대흥씨엔에스(주)")
        body.owners[0].months.map { it.amount } shouldBe listOf(BigDecimal(300_000), BigDecimal.ZERO, BigDecimal(450_000))
        body.owners[0].total shouldBe BigDecimal(750_000)
        body.owners[0].share shouldBe BigDecimal("75.0")
        body.owners[1].share shouldBe BigDecimal("25.0")
    }
})
