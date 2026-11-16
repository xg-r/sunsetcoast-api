package kr.sunsetcoast.domain.bank

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import kr.sunsetcoast.domain.bank.vo.BankCategorizer
import kr.sunsetcoast.domain.bank.vo.BankCategory
import kr.sunsetcoast.domain.bank.vo.BankStatementParser
import java.io.File
import java.math.BigDecimal
import java.time.LocalDateTime

class BankStatementTest : FunSpec({
    val categorizer = BankCategorizer(emptyList(), listOf("선셋코스트", "대흥씨엔에스(주)", "스테이디앤디풀빌라"))
    fun cat(description: String, deposit: Boolean, name: String? = null, type: String? = null) =
        categorizer.categorize(BankCategorizer.Input(description, name, type, deposit))

    test("기본 분류") {
        cat("주식회사떠나", true, "(주)떠나요", "타행이체") shouldBe BankCategory.OTA_DDNAYO
        cat("(주)여기어때컴퍼니", true) shouldBe BankCategory.OTA_YEOGI
        cat("NOL펜션1102", true, "(주)하이픈코퍼레이") shouldBe BankCategory.OTA_NOL
        cat("2026년결산", true, type = "이자") shouldBe BankCategory.INTEREST
        cat("10월캠핑장관리비", false, "현장사장") shouldBe BankCategory.MANAGEMENT_FEE
        cat("10월임대료", false, "임대인") shouldBe BankCategory.RENT
        cat("수도2610", false, type = "자동이체") shouldBe BankCategory.UTILITIES
        cat("쿠팡 주식회사", false, type = "체크") shouldBe BankCategory.SUPPLIES
        cat("비씨카드출금", false, type = "BC") shouldBe BankCategory.CARD_PAYMENT
        cat("공동인증수수료", false) shouldBe BankCategory.BANK_FEE
        cat("모담 송도점", false, type = "체크") shouldBe BankCategory.CARD_OTHER
        cat("Amazon_AWS", true, type = "취소") shouldBe BankCategory.REFUND
    }

    test("관계사 정산: 받는 쪽은 매출, 보내는 쪽은 매출 차감. 그 밖의 관계사 이체는 자금 이동") {
        // 선셋코스트 계좌: 스테이디앤디가 보내 준 여기어때 리조트 정산
        cat("10월숙박비", true, "스테이디앤디풀빌라") shouldBe BankCategory.AFFILIATE_SETTLEMENT_IN
        // 스테이디앤디 계좌: 선셋코스트·대흥씨엔에스 몫을 넘겨줌 (9월에는 '관리비'라고 적어 보냄)
        cat("10월숙박비", false, "선셋코스트") shouldBe BankCategory.AFFILIATE_SETTLEMENT_OUT
        cat("10월숙박비", false, "대흥씨엔에스(주)") shouldBe BankCategory.AFFILIATE_SETTLEMENT_OUT
        cat("제주도9월관리비", false, "대흥씨엔에스(주)") shouldBe BankCategory.AFFILIATE_SETTLEMENT_OUT
        BankCategory.AFFILIATE_SETTLEMENT_OUT.group shouldBe BankCategory.Group.REVENUE
        cat("자금이동", true, "대흥씨엔에스") shouldBe BankCategory.AFFILIATE_IN
        cat("자금이동", false, "대흥씨엔에스") shouldBe BankCategory.AFFILIATE_OUT
        // 현장 관리비는 관계사가 아닌 현장 사장님에게 보낸다
        cat("10월관리비", false, "현장사장") shouldBe BankCategory.MANAGEMENT_FEE
    }

    test("카드사 입금은 카드 매출, 세무법인·통신·전기") {
        cat("롯데44667623", true, "롯데카드(점") shouldBe BankCategory.CARD_SALES
        cat("SHC0126731", true, "신한카드(주)") shouldBe BankCategory.CARD_SALES
        cat("광교세무법인", false, "나이스정보통신주식회사") shouldBe BankCategory.PROFESSIONAL
        cat("SK텔레콤", false) shouldBe BankCategory.UTILITIES
        cat("10월전기료", false, "한국전력공사") shouldBe BankCategory.UTILITIES
    }

    test("관리자 규칙이 기본 규칙보다 먼저") {
        val custom = BankCategorizer(listOf(BankCategorizer.Rule("모담", BankCategorizer.Direction.OUT, BankCategory.MEALS)), emptyList())
        custom.categorize(BankCategorizer.Input("모담 송도점", null, "체크", false)) shouldBe BankCategory.MEALS
    }

    val sample = File("docs/거래내역샘플").listFiles()?.firstOrNull { it.name.startsWith("거래내역조회") }
    test("은행 샘플 파일: 계좌·거래·잔액 연속성").config(enabled = sample != null) {
        val statement = BankStatementParser.parse(sample!!.readBytes())
        // 실제 계좌번호는 git에 남기지 않는다: 형식만 확인
        statement.accountNumber!! shouldMatch Regex("""\d{3}-\d{6}-\d{2}-\d{3}""")
        statement.holderName shouldBe "선셋코스트"
        statement.transactions.size shouldBe 31
        statement.transactions.first().transactedAt shouldBe LocalDateTime.of(2026, 11, 1, 12, 24, 55)
        statement.transactions.last().balance.compareTo(BigDecimal(27_915_782)) shouldBe 0
        statement.transactions.sumOf { it.withdrawal }.compareTo(BigDecimal(17_134_430)) shouldBe 0
        statement.transactions.sumOf { it.deposit }.compareTo(BigDecimal(18_081_448)) shouldBe 0
        statement.balanceBreaks.shouldBeEmpty()
        val uncategorized = statement.transactions.map {
            it.description to categorizer.categorize(BankCategorizer.Input(it.description, it.counterpartyName, it.transactionType, it.deposit.signum() > 0))
        }.filter { it.second in setOf(BankCategory.OTHER_IN, BankCategory.OTHER_OUT) }
        uncategorized.shouldBeEmpty()
    }

    val stayDnd = File("docs/거래내역샘플").listFiles()?.firstOrNull { it.name.contains("스테이디앤디") }
    test("스테이디앤디 샘플: 계좌·예금주, 관계사 정산 지급이 매출에서 빠진다").config(enabled = stayDnd != null) {
        val statement = BankStatementParser.parse(stayDnd!!.readBytes())
        statement.accountNumber!! shouldMatch Regex("""\d{3}-\d{6}-\d{2}-\d{3}""")
        statement.holderName shouldBe "스테이디앤디"
        statement.depositorName shouldBe "스테이디앤디풀빌라"
        statement.transactions.size shouldBe 23
        statement.transactions.sumOf { it.withdrawal }.compareTo(BigDecimal(27_342_075)) shouldBe 0
        statement.transactions.sumOf { it.deposit }.compareTo(BigDecimal(20_586_838)) shouldBe 0
        statement.balanceBreaks.shouldBeEmpty()

        val categorized = statement.transactions.map {
            it to categorizer.categorize(BankCategorizer.Input(it.description, it.counterpartyName, it.transactionType, it.deposit.signum() > 0))
        }
        categorized.filter { it.second == BankCategory.AFFILIATE_SETTLEMENT_OUT }.sumOf { it.first.withdrawal }
            .compareTo(BigDecimal(5_360_499 + 5_404_847 + 7_653_959 + 6_892_480)) shouldBe 0
        // 남는 기타: '하나종합１'(나이스정보통신 펌이체) 하나뿐
        categorized.filter { it.second in setOf(BankCategory.OTHER_IN, BankCategory.OTHER_OUT) }.map { it.first.description } shouldBe listOf("하나종합１")
    }
})
