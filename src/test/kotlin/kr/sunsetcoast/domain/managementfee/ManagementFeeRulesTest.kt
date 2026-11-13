package kr.sunsetcoast.domain.managementfee

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kr.sunsetcoast.domain.managementfee.entity.ManagementFeeRateEntity
import kr.sunsetcoast.domain.managementfee.vo.FeeDecision
import kr.sunsetcoast.domain.managementfee.vo.FeeStay
import kr.sunsetcoast.domain.managementfee.vo.ManagementFeeBasis
import kr.sunsetcoast.domain.managementfee.vo.ManagementFeeRules
import java.math.BigDecimal
import java.time.LocalDate

class ManagementFeeRulesTest : FunSpec({
    fun date(value: String) = LocalDate.parse(value)
    val resort = 26L
    val villa2 = 31L
    val rates = mapOf(resort to BigDecimal(30_000), villa2 to BigDecimal.ZERO)
    fun decide(stay: FeeStay): FeeDecision = ManagementFeeRules.decide(stay) { roomId, _ -> rates[roomId] }

    fun stay(id: Long, checkIn: String?, nights: Int?, room: Long = resort, cancelled: Boolean = false) =
        FeeStay(id, room, cancelled, checkIn?.let { date(it) }, nights)

    test("박수와 무관하게 예약마다 1회") {
        decide(stay(1, "2026-08-03", 1)) shouldBe FeeDecision(ManagementFeeBasis.CHARGED, BigDecimal(30_000))
        decide(stay(2, "2026-08-10", 3)) shouldBe FeeDecision(ManagementFeeBasis.CHARGED, BigDecimal(30_000))
    }

    test("예약번호가 다르면 이어서 묵어도 따로 지급") {
        decide(stay(1, "2026-08-03", 1)).amount shouldBe BigDecimal(30_000)
        decide(stay(2, "2026-08-04", 1)).amount shouldBe BigDecimal(30_000)
    }

    test("취소 예약과 투숙한 밤이 없는 예약은 0원") {
        decide(stay(1, null, null, cancelled = true)).basis shouldBe ManagementFeeBasis.CANCELLED
        decide(stay(2, "2026-08-03", 1, cancelled = true)).basis shouldBe ManagementFeeBasis.CANCELLED
        decide(stay(3, "2026-08-03", 0)) shouldBe FeeDecision(ManagementFeeBasis.NO_STAY, BigDecimal.ZERO)
    }

    test("Villa II·III는 단가 0원, 단가가 없으면 NO_RATE") {
        decide(stay(1, "2026-08-03", 1, room = villa2)) shouldBe FeeDecision(ManagementFeeBasis.CHARGED, BigDecimal.ZERO)
        decide(stay(2, "2026-08-03", 1, room = 99)).basis shouldBe ManagementFeeBasis.NO_RATE
    }

    test("단가 적용 기간은 입실일 기준 [시작, 종료)") {
        val old = ManagementFeeRateEntity(roomId = resort, amount = BigDecimal(30_000), effectiveFrom = date("2020-01-01"), effectiveTo = date("2027-01-01"))
        old.appliesTo(date("2026-12-31")) shouldBe true
        old.appliesTo(date("2027-01-01")) shouldBe false
        old.appliesTo(date("2019-12-31")) shouldBe false
    }
})
