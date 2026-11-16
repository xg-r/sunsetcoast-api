package kr.sunsetcoast.domain.bank.vo

/**
 * 거래를 분류한다. 관리자가 만든 규칙 → 관계사 이름 → 기본 규칙 순으로 처음 맞는 것을 쓴다.
 * 맞는 것이 없으면 입금은 기타 입금, 체크카드 출금은 체크카드 기타, 나머지 출금은 기타 출금.
 */
class BankCategorizer(
    private val userRules: List<Rule>,
    /** 관계사(가족 사업자) 이름. 상대 예금주명에 들어 있으면 관계사 입출금 */
    private val affiliateNames: Collection<String>,
) {
    enum class Direction { IN, OUT, ANY }

    data class Rule(val keyword: String, val direction: Direction, val category: BankCategory)

    data class Input(
        val description: String,
        val counterpartyName: String?,
        val transactionType: String?,
        val isDeposit: Boolean,
    )

    fun categorize(tx: Input): BankCategory {
        val text = "${tx.description} ${tx.counterpartyName.orEmpty()}".normalize()
        fun matches(rule: Rule) =
            (rule.direction == Direction.ANY || (rule.direction == Direction.IN) == tx.isDeposit) &&
                text.contains(rule.keyword.normalize())

        userRules.firstOrNull(::matches)?.let { return it.category }

        // 관계사끼리 매달 보내는 정산금(여기어때 리조트 정산을 스테이디앤디가 받아 선셋코스트·대흥씨엔에스에 나눠 줌)은 매출.
        // 거래내용이 '10월숙박비', '제주도9월관리비'처럼 정산을 뜻할 때만이고, 그 밖의 관계사 이체는 자금 이동으로 본다.
        if (isAffiliate(tx.counterpartyName)) {
            val settlement = SETTLEMENT_WORDS.any { tx.description.normalize().contains(it) }
            return when {
                settlement && tx.isDeposit -> BankCategory.AFFILIATE_SETTLEMENT_IN
                settlement -> BankCategory.AFFILIATE_SETTLEMENT_OUT
                tx.isDeposit -> BankCategory.AFFILIATE_IN
                else -> BankCategory.AFFILIATE_OUT
            }
        }

        DEFAULT_RULES.firstOrNull(::matches)?.let { return it.category }

        if (tx.isDeposit) return if (tx.transactionType == "취소") BankCategory.REFUND else BankCategory.OTHER_IN
        return if (tx.transactionType == "체크") BankCategory.CARD_OTHER else BankCategory.OTHER_OUT
    }

    private fun isAffiliate(counterpartyName: String?): Boolean {
        val counterparty = counterpartyName?.normalize()?.replace("(주)", "").orEmpty()
        if (counterparty.isEmpty()) return false
        return affiliateNames.map { it.normalize().replace("(주)", "") }.filter { it.isNotEmpty() }
            .any { counterparty.contains(it) || it.contains(counterparty) }
    }

    companion object {
        private val SETTLEMENT_WORDS = listOf("숙박비", "정산", "관리비")

        /** 2026-11 선셋코스트 계좌 거래내역 기준. 화면에서 거래 분류를 고치면 관리자 규칙이 추가된다 */
        val DEFAULT_RULES: List<Rule> = listOf(
            Rule("떠나", Direction.IN, BankCategory.OTA_DDNAYO),
            Rule("여기어때", Direction.IN, BankCategory.OTA_YEOGI),
            Rule("NOL", Direction.IN, BankCategory.OTA_NOL),
            Rule("하이픈코퍼레이", Direction.IN, BankCategory.OTA_NOL),
            Rule("야놀자", Direction.IN, BankCategory.OTA_NOL),
            Rule("숙박비", Direction.IN, BankCategory.LODGING_OTHER),
            // 카드사 입금 = 현장·전화 예약 카드 결제 대금
            Rule("롯데카드", Direction.IN, BankCategory.CARD_SALES),
            Rule("신한카드", Direction.IN, BankCategory.CARD_SALES),
            Rule("삼성카드", Direction.IN, BankCategory.CARD_SALES),
            Rule("현대카드", Direction.IN, BankCategory.CARD_SALES),
            Rule("국민카드", Direction.IN, BankCategory.CARD_SALES),
            Rule("KB카드", Direction.IN, BankCategory.CARD_SALES),
            Rule("비씨카드", Direction.IN, BankCategory.CARD_SALES),
            Rule("하나카드", Direction.IN, BankCategory.CARD_SALES),
            Rule("우리카드", Direction.IN, BankCategory.CARD_SALES),
            Rule("농협카드", Direction.IN, BankCategory.CARD_SALES),
            Rule("결산", Direction.IN, BankCategory.INTEREST),
            Rule("이자", Direction.IN, BankCategory.INTEREST),
            Rule("관리비", Direction.OUT, BankCategory.MANAGEMENT_FEE),
            Rule("임대료", Direction.OUT, BankCategory.RENT),
            Rule("월세", Direction.OUT, BankCategory.RENT),
            // 세무 수수료는 VAN(나이스정보통신)을 거쳐 나가므로 '통신'보다 먼저 본다
            Rule("세무법인", Direction.OUT, BankCategory.PROFESSIONAL),
            Rule("세무사", Direction.OUT, BankCategory.PROFESSIONAL),
            Rule("회계", Direction.OUT, BankCategory.PROFESSIONAL),
            Rule("노무", Direction.OUT, BankCategory.PROFESSIONAL),
            Rule("수도", Direction.OUT, BankCategory.UTILITIES),
            Rule("전기", Direction.OUT, BankCategory.UTILITIES),
            Rule("한전", Direction.OUT, BankCategory.UTILITIES),
            Rule("가스", Direction.OUT, BankCategory.UTILITIES),
            Rule("KT", Direction.OUT, BankCategory.UTILITIES),
            Rule("SKT", Direction.OUT, BankCategory.UTILITIES),
            Rule("텔레콤", Direction.OUT, BankCategory.UTILITIES),
            Rule("전기료", Direction.OUT, BankCategory.UTILITIES),
            Rule("한국전력", Direction.OUT, BankCategory.UTILITIES),
            Rule("LG유플러스", Direction.OUT, BankCategory.UTILITIES),
            Rule("AWS", Direction.OUT, BankCategory.UTILITIES),
            Rule("쿠팡", Direction.OUT, BankCategory.SUPPLIES),
            Rule("다이소", Direction.OUT, BankCategory.SUPPLIES),
            Rule("이마트", Direction.OUT, BankCategory.SUPPLIES),
            Rule("주차", Direction.OUT, BankCategory.VEHICLE),
            Rule("주유", Direction.OUT, BankCategory.VEHICLE),
            Rule("하이패스", Direction.OUT, BankCategory.VEHICLE),
            Rule("급여", Direction.OUT, BankCategory.PAYROLL),
            Rule("국세", Direction.OUT, BankCategory.TAX),
            Rule("지방세", Direction.OUT, BankCategory.TAX),
            Rule("부가세", Direction.OUT, BankCategory.TAX),
            Rule("세무서", Direction.OUT, BankCategory.TAX),
            Rule("국민연금", Direction.OUT, BankCategory.TAX),
            Rule("건강보험", Direction.OUT, BankCategory.TAX),
            Rule("수수료", Direction.OUT, BankCategory.BANK_FEE),
            Rule("카드출금", Direction.OUT, BankCategory.CARD_PAYMENT),
            Rule("카드대금", Direction.OUT, BankCategory.CARD_PAYMENT),
            Rule("대출", Direction.OUT, BankCategory.LOAN),
            Rule("이자", Direction.OUT, BankCategory.LOAN),
        )

        private fun String.normalize() = replace(" ", "").uppercase()
    }
}
