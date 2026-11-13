package kr.sunsetcoast.domain.settlement.vo

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.math.BigDecimal

class MultiRoomTest : FunSpec({
    // 떠나요 별칭 (실제 시드 일부)
    val aliases = mapOf(
        "수애기 (오션뷰&돌고래뷰)" to 5L,
        "곰새기 (오션뷰&돌고래뷰)" to 6L,
        "제돌이 (애견동반가능)((오션뷰&돌고래뷰)" to 7L,
        "스페이스카라반 쉬멍(오션뷰&돌고래뷰)" to 3L,
        "스페이스카라반 물멍(오션뷰&돌고래뷰)" to 2L,
        "Baram 바람 (애견동반가능)(오션뷰&돌고래" to 14L,
        "Baram 바람 (애견동반가능)(오션뷰&돌고래뷰)" to 14L,
    )

    context("객실명 매칭") {
        test("별칭과 정확히 같으면 객실 하나") {
            RoomNameMatcher.match("수애기 (오션뷰&돌고래뷰)", aliases) shouldBe RoomNameMatcher.Result.Matched(listOf(5))
        }

        test("쉼표로 이어진 다중 객실, 잘린 이름은 앞부분이 맞는 객실이 하나뿐이면 찾는다") {
            RoomNameMatcher.match("수애기 (오션뷰&돌고래뷰),스페이스카라반 쉬멍(오..", aliases) shouldBe
                RoomNameMatcher.Result.Matched(listOf(5, 3))
        }

        test("같은 객실의 별칭이 여러 개여도 객실이 하나면 찾는다") {
            RoomNameMatcher.match("수애기 (오션뷰&돌고래뷰),Baram 바람 (애견..", aliases) shouldBe
                RoomNameMatcher.Result.Matched(listOf(5, 14))
        }

        test("잘린 이름의 후보가 여럿이면 실패하고, 찾은 객실은 후보로 돌려준다") {
            val result = RoomNameMatcher.match("수애기 (오션뷰&돌고래뷰),스페이스카라반 ..", aliases)
            result.shouldBeInstanceOf<RoomNameMatcher.Result.Unmatched>()
            result.partialRoomIds shouldBe listOf(5)
        }

        test("잘린 이름이 앞 객실과 같은 객실로 풀리면 실패 (8/18 떠나요 72861814)") {
            val result = RoomNameMatcher.match("제돌이 (애견동반가능)((오션뷰&돌고래뷰),제", aliases)
            result.shouldBeInstanceOf<RoomNameMatcher.Result.Unmatched>()
            result.partialRoomIds shouldBe listOf(7)
        }

        test("쉼표가 없고 별칭에 없으면 실패") {
            RoomNameMatcher.match("새 객실", aliases).shouldBeInstanceOf<RoomNameMatcher.Result.Unmatched>()
        }
    }

    context("금액 분할") {
        test("정가 비율로 나누고 나머지는 마지막 객실에 더한다") {
            // 수애기 150,000 : 쉬멍 230,000
            val parts = AmountSplitter.split(BigDecimal(500_001), listOf(BigDecimal(150_000), BigDecimal(230_000)))
            parts shouldBe listOf(BigDecimal(197_368), BigDecimal(302_633))
            parts.sumOf { it } shouldBe BigDecimal(500_001)
        }

        test("모든 금액 항목을 나누고 합계가 원래 금액과 같다") {
            val total = SettlementAmounts(
                gross = BigDecimal(250_000),
                commission = BigDecimal(20_000),
                net = BigDecimal(230_000),
                commissionRate = BigDecimal(8),
            )
            val parts = AmountSplitter.split(total, listOf(BigDecimal(150_000), BigDecimal(150_000)))
            parts.map { it.gross } shouldBe listOf(BigDecimal(125_000), BigDecimal(125_000))
            parts.sumOf { it.net } shouldBe BigDecimal(230_000)
            parts.all { it.isBalanced() } shouldBe true
            parts.all { it.commissionRate == BigDecimal(8) } shouldBe true
        }

        test("음수(입실 후 취소)도 나눌 수 있다") {
            AmountSplitter.split(BigDecimal(-100_001), listOf(BigDecimal.ONE, BigDecimal.ONE)).sumOf { it } shouldBe BigDecimal(-100_001)
        }
    }
})
