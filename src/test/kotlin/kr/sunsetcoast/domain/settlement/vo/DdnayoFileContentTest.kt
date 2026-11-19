package kr.sunsetcoast.domain.settlement.vo

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class DdnayoFileContentTest : FunSpec({
    fun row(useDate: String, status: String = "결제완료", biz: String = "278-27-01574") =
        mapOf("이용일" to useDate, "예약상태" to status, "사업자번호" to biz)

    test("이용일이 가장 많은 주의 다음 주 화요일이 정산일") {
        // 정산내역_..._2026-08-04_떠나요.xls 와 같은 구성: 7/27(월)~8/1(토) 이용분
        val content = DdnayoFileContent.of(listOf(row("2026-07-27"), row("2026-07-31"), row("2026-08-01")))
        content shouldBe DdnayoFileContent("278-27-01574", LocalDate.of(2026, 8, 4))
    }

    test("취소 행의 먼 이용일은 보지 않는다") {
        val content = DdnayoFileContent.of(
            listOf(row("2026-11-27", status = "예약취소"), row("2026-11-27", status = "예약취소"), row("2026-11-03"), row("2026-11-06")),
        )
        content.issuedOn shouldBe LocalDate.of(2026, 11, 10)
    }

    test("앞 주에 걸친 행이 조금 있어도 대부분이 속한 주로 본다") {
        val content = DdnayoFileContent.of(listOf(row("2026-08-01"), row("2026-08-03"), row("2026-08-05"), row("2026-08-08")))
        content.issuedOn shouldBe LocalDate.of(2026, 8, 11)
    }

    test("날짜·시각 형식 이용일과 사업자번호 다수결") {
        val content = DdnayoFileContent.of(
            listOf(row("2025-12-27T00:00", biz = "126-81-73051"), row("2025-12-26"), row("2025-12-24")),
        )
        content shouldBe DdnayoFileContent("278-27-01574", LocalDate.of(2025, 12, 30))
    }

    test("행이 없으면 모른다") {
        DdnayoFileContent.of(emptyList()) shouldBe DdnayoFileContent(null, null, emptyList())
    }

    test("체크아웃일 기준: 일요일 1박이나 앞 주에 들어온 연박도 체크아웃 주로 정산된다") {
        // 9/27(일) 1박 → 9/28(월) 체크아웃, 9/23 5박 → 9/28 체크아웃 → 9/28 주 → 10/6 정산
        val content = DdnayoFileContent.of(
            listOf(row("2026-09-27"), row("2026-09-30"), mapOf("이용일" to "2026-09-23", "이용박수" to "5", "예약상태" to "결제완료")),
        )
        content.settlementDates shouldBe listOf(LocalDate.of(2026, 10, 6))
    }

    test("한 달 치 파일은 정산일이 여러 개, 한두 건 섞여 들어온 주는 빼고") {
        val weekly = { start: String, n: Int -> List(n) { row(LocalDate.parse(start).plusDays((it % 5).toLong()).toString()) } }
        val rows = weekly("2026-09-28", 30) + weekly("2026-10-05", 20) + weekly("2026-10-12", 30) + listOf(row("2026-10-20"))
        val content = DdnayoFileContent.of(rows)
        content.settlementDates shouldBe listOf(LocalDate.of(2026, 10, 6), LocalDate.of(2026, 10, 13), LocalDate.of(2026, 10, 20))
        content.issuedOn shouldBe LocalDate.of(2026, 10, 20)
    }

    test("예약이 적은 한 달 치 파일은 1건뿐인 주도 정산일로 본다") {
        // AdjustCalendar(2026-03) (12).xls (스테이디앤디): 3/2·3/20·3/28 이용 1박씩
        val content = DdnayoFileContent.of(listOf(row("2026-03-02"), row("2026-03-20"), row("2026-03-28")))
        content.settlementDates shouldBe listOf(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 24), LocalDate.of(2026, 3, 31))
    }

    test("주간 파일에 1건 섞인 다른 주는 빼고 본다") {
        val rows = List(14) { row(LocalDate.of(2026, 11, 9).plusDays((it % 5).toLong()).toString()) } + listOf(row("2026-11-03"))
        DdnayoFileContent.of(rows).settlementDates shouldBe listOf(LocalDate.of(2026, 11, 17))
    }
})
