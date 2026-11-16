package kr.sunsetcoast.domain.bank.vo

/** 거래 분류. group으로 매출·운영비·이동(계좌·관계사 간)·기타를 나눠 분석한다. */
enum class BankCategory(val label: String, val group: Group) {
    OTA_DDNAYO("떠나요 정산", Group.REVENUE),
    OTA_YEOGI("여기어때 정산", Group.REVENUE),
    OTA_NOL("NOL 정산", Group.REVENUE),
    LODGING_OTHER("숙박비 (기타)", Group.REVENUE),
    CARD_SALES("카드 매출 입금", Group.REVENUE),

    /** 관계사가 보내 준 정산금 (스테이디앤디 계좌로 들어온 여기어때 리조트 정산 중 이 사업자 몫) */
    AFFILIATE_SETTLEMENT_IN("관계사 정산 입금", Group.REVENUE),

    /** 관계사 몫을 넘겨준 정산금. 매출에서 뺀다 (같은 매출이 두 사업자에 두 번 잡히지 않도록) */
    AFFILIATE_SETTLEMENT_OUT("관계사 정산 지급 (매출 차감)", Group.REVENUE),

    AFFILIATE_IN("관계사 입금 (자금 이동)", Group.TRANSFER),
    INTEREST("이자", Group.NON_OPERATING_IN),
    REFUND("취소·환불 입금", Group.NON_OPERATING_IN),
    OTHER_IN("기타 입금", Group.NON_OPERATING_IN),

    MANAGEMENT_FEE("현장 관리비", Group.EXPENSE),
    RENT("임대료", Group.EXPENSE),
    UTILITIES("공과금 (수도·전기·가스·통신)", Group.EXPENSE),
    SUPPLIES("소모품·비품", Group.EXPENSE),
    MEALS("식비·접대", Group.EXPENSE),
    VEHICLE("차량·주차·교통", Group.EXPENSE),
    PAYROLL("인건비", Group.EXPENSE),
    TAX("세금·공과", Group.EXPENSE),
    PROFESSIONAL("세무·전문 서비스", Group.EXPENSE),
    BANK_FEE("수수료", Group.EXPENSE),
    CARD_PAYMENT("카드대금", Group.EXPENSE),
    CARD_OTHER("체크카드 기타", Group.EXPENSE),
    OWNER_PAYOUT("외부 소유주 지급", Group.EXPENSE),
    LOAN("대출 원리금", Group.FINANCING),
    AFFILIATE_OUT("관계사 출금 (자금 이동)", Group.TRANSFER),
    OTHER_OUT("기타 출금", Group.EXPENSE),
    ;

    enum class Group(val label: String) {
        /** 영업 매출 (OTA 정산, 숙박비, 카드 매출, 관계사 정산). 관계사에 넘겨준 정산금은 출금이라 매출에서 빠진다 */
        REVENUE("매출"),
        NON_OPERATING_IN("영업외 입금"),
        EXPENSE("운영비"),
        FINANCING("금융 (대출)"),
        TRANSFER("관계사 이동"),
    }
}
