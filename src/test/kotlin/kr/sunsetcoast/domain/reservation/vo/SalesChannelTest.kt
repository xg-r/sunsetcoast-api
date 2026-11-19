package kr.sunsetcoast.domain.reservation.vo

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SalesChannelTest : FunSpec({
    test("여기어때 정산 파일의 판매채널") {
        SalesChannel.fromRaw("여기어때") shouldBe SalesChannel.YEOGI
        SalesChannel.fromRaw("여기어때 연동 예약 사이트") shouldBe SalesChannel.YEOGI_PARTNER
        SalesChannel.fromRaw("여기어때연동예약사이트") shouldBe SalesChannel.YEOGI_PARTNER
        SalesChannel.fromRaw("홈페이지") shouldBe SalesChannel.HOMEPAGE
    }
})
