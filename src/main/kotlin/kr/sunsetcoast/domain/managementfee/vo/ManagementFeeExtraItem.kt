package kr.sunsetcoast.domain.managementfee.vo

import java.math.BigDecimal

/** 관리비와 함께 현장에 넘겨 줄 추가요금 항목. defaultUnitPrice는 입력할 때의 기본 단가 */
enum class ManagementFeeExtraItem(val label: String, val defaultUnitPrice: BigDecimal?, val perNight: Boolean) {
    /** 바베큐 1회 */
    BBQ("바베큐", BigDecimal(30_000), perNight = false),

    /** 추가인원, 1박 1인당 */
    EXTRA_PERSON("추가인원", BigDecimal(20_000), perNight = true),

    /** 기타 (단가 직접 입력, 메모 필수) */
    OTHER("기타", null, perNight = false),
}
