package kr.sunsetcoast.domain.businessowner.vo

enum class OwnershipType {
    /** 가족 사업자: 매출에 포함 */
    FAMILY,

    /** 외부 소유주: 매출 KPI에서 제외하고 월 정산으로 지급 */
    EXTERNAL,
}
