package kr.sunsetcoast.domain.settlement.vo

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class SettlementFileNameTest : FunSpec({
    val uploaded = LocalDate.of(2026, 11, 13)

    test("떠나요 파일명") {
        SettlementFileName.parse("정산내역_서귀포 선셋코스트 카라반&캠핑장_2026-08-04_떠나요.xls", uploaded) shouldBe
            SettlementFileName("서귀포 선셋코스트 카라반&캠핑장", LocalDate.of(2026, 8, 4))
    }

    test("여기어때 파일명") {
        SettlementFileName.parse("정산내역_서귀포 스테이 디앤디 풀빌라_2026-08-26.xlsx", uploaded) shouldBe
            SettlementFileName("서귀포 스테이 디앤디 풀빌라", LocalDate.of(2026, 8, 26))
    }

    test("NOL 파일명은 판매자 계정과 MMdd") {
        SettlementFileName.parse("정산상세내역_스테이디앤디_대흥cns_0820.xls", uploaded) shouldBe
            SettlementFileName("대흥cns", LocalDate.of(2026, 8, 20))
    }

    test("NOL 계정과 날짜 사이 _가 빠져도 읽는다") {
        SettlementFileName.parse("정산상세내역_스테이디앤디_스테이디앤디0813.xls", uploaded) shouldBe
            SettlementFileName("스테이디앤디", LocalDate.of(2026, 8, 13))
    }

    test("NOL 날짜가 업로드일보다 한참 뒤면 작년 파일") {
        SettlementFileName.parse("정산상세내역_스테이디앤디_선셋코스트_1226.xls", LocalDate.of(2027, 1, 5)) shouldBe
            SettlementFileName("선셋코스트", LocalDate.of(2026, 12, 26))
    }

    test("이름을 바꾼 파일은 읽지 못한다") {
        SettlementFileName.parse("8월 정산.xlsx", uploaded) shouldBe SettlementFileName(null, null)
    }
})
