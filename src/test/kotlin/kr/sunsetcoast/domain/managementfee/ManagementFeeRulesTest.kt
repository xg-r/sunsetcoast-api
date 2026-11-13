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
    fun decide(vararg stays: FeeStay): Map<Long, FeeDecision> =
        ManagementFeeRules.decide(stays.toList()) { roomId, _ -> rates[roomId] }
            .mapKeys { it.key.reservationId }

    fun stay(id: Long, checkIn: String?, nights: Int?, name: String? = "홍길동", room: Long = resort, cancelled: Boolean = false) =
        FeeStay(id, room, name, cancelled, checkIn?.let { date(it) }, nights)

    test("박수와 무관하게 예약마다 1회") {
        decide(stay(1, "2026-08-03", 1), stay(2, "2026-08-10", 3)).values.map { it.amount } shouldBe
            listOf(BigDecimal(30_000), BigDecimal(30_000))
    }

    test("취소 예약과 투숙한 밤이 없는 예약은 0원") {
        val result = decide(
            stay(1, null, null, cancelled = true),
            stay(2, "2026-08-03", 1, cancelled = true),
            stay(3, "2026-08-03", 0),
        )
        result.getValue(1).basis shouldBe ManagementFeeBasis.CANCELLED
        result.getValue(2).basis shouldBe ManagementFeeBasis.CANCELLED
        result.getValue(3).basis shouldBe ManagementFeeBasis.NO_STAY
        result.values.all { it.amount.signum() == 0 } shouldBe true
    }

    test("같은 예약자가 같은 객실을 이어서 예약하면 뒤 예약은 0원, 세 건이 이어지면 첫 예약만 지급") {
        val result = decide(
            stay(1, "2026-08-03", 1),
            stay(2, "2026-08-04", 2, name = "홍 길동"), // 공백 차이는 같은 사람
            stay(3, "2026-08-06", 1),
        )
        result.getValue(1) shouldBe FeeDecision(ManagementFeeBasis.CHARGED, BigDecimal(30_000))
        result.getValue(2) shouldBe FeeDecision(ManagementFeeBasis.CONSECUTIVE, BigDecimal.ZERO, 1)
        result.getValue(3) shouldBe FeeDecision(ManagementFeeBasis.CONSECUTIVE, BigDecimal.ZERO, 2)
    }

    test("이름이 다르거나, 하루라도 비거나, 객실이 다르거나, 앞 예약이 취소면 따로 지급") {
        val result = decide(
            stay(1, "2026-08-03", 1),
            stay(2, "2026-08-04", 1, name = "김철수"),
            stay(3, "2026-08-05", 1), // 2와 이어지지만 이름이 다르고, 1과는 하루 비어 있다
            stay(4, "2026-08-10", 1, cancelled = true),
            stay(5, "2026-08-11", 1),
            stay(6, "2026-08-04", 1, room = villa2), // 1과 같은 사람, 다른 객실
            stay(7, "2026-08-20", 1, name = null),
            stay(8, "2026-08-21", 1, name = null), // 이름이 없으면 이어진 예약으로 보지 않는다
        )
        listOf(1L, 2L, 3L, 5L, 7L, 8L).forEach { result.getValue(it).basis shouldBe ManagementFeeBasis.CHARGED }
        result.getValue(6) shouldBe FeeDecision(ManagementFeeBasis.CHARGED, BigDecimal.ZERO) // Villa II 단가 0원
    }

    test("단가가 없으면 NO_RATE") {
        decide(stay(1, "2026-08-03", 1, room = 99)).getValue(1).basis shouldBe ManagementFeeBasis.NO_RATE
    }

    test("단가 적용 기간은 입실일 기준 [시작, 종료)") {
        val old = ManagementFeeRateEntity(roomId = resort, amount = BigDecimal(30_000), effectiveFrom = date("2020-01-01"), effectiveTo = date("2027-01-01"))
        old.appliesTo(date("2026-12-31")) shouldBe true
        old.appliesTo(date("2027-01-01")) shouldBe false
        old.appliesTo(date("2019-12-31")) shouldBe false
    }
})
