package kr.sunsetcoast.common

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kr.sunsetcoast.common.response.AmountText

class AmountTextTest : FunSpec({
    test("금액 변경·검증 실패 문구에서 금액을 뺀다") {
        AmountText.redact("71681294 2026-10-22 ROOM_NIGHT 금액 변경: 정산금액 178000.00 -> 177000.00") shouldBe "71681294 2026-10-22 ROOM_NIGHT 금액 변경"
        AmountText.redact("금액 검증 실패: 매출(100000) - 수수료(8000) != 정산금액(91000)") shouldBe "금액 검증 실패"
    }

    test("금액이 없는 문구와 null은 그대로") {
        AmountText.redact("객실 매핑이 없습니다: 오션 101") shouldBe "객실 매핑이 없습니다: 오션 101"
        AmountText.redact(null) shouldBe null
    }
})
