package kr.sunsetcoast.domain.settlement.vo

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kr.sunsetcoast.domain.settlement.service.ddnayoAccountName

class DdnayoAccountNameTest : FunSpec({
    val camping = "서귀포 선셋코스트 카라반&캠핑장"

    test("사업자번호를 지정한 등록 정산 계정이 가장 먼저. 파일의 하이픈 유무와 상관없이") {
        ddnayoAccountName("278-27-01574", mapOf("2782701574" to camping), mapOf("2782701574" to "다른 이름")) shouldBe camping
        ddnayoAccountName("2782701574", mapOf("2782701574" to camping), emptyMap()) shouldBe camping
    }

    test("등록 계정이 없으면 같은 사업자번호의 이름 붙은 파일 계정") {
        ddnayoAccountName("278-27-01574", emptyMap(), mapOf("2782701574" to camping)) shouldBe camping
    }

    test("둘 다 없으면 '사업자 번호'") {
        ddnayoAccountName("2782701574", emptyMap(), emptyMap()) shouldBe "사업자 278-27-01574"
        ddnayoAccountName("12-34", emptyMap(), emptyMap()) shouldBe "사업자 12-34"
    }

    test("사업자번호는 숫자 10자리로 맞춘다") {
        BusinessNumber.normalize(" 278-27-01574 ") shouldBe "2782701574"
        BusinessNumber.normalize("278-27-0157") shouldBe null
        BusinessNumber.format("2782701574") shouldBe "278-27-01574"
    }
})
