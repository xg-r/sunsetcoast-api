package kr.sunsetcoast.domain.staycalendar.vo

import java.math.BigDecimal

/** 객실 1박 정가 (비수기/성수기 × 평일/주말) */
data class ListPrice(
    val offpeakWeekday: BigDecimal,
    val offpeakWeekend: BigDecimal,
    val peakWeekday: BigDecimal,
    val peakWeekend: BigDecimal,
) {
    fun of(peak: Boolean, weekend: Boolean): BigDecimal = when {
        peak && weekend -> peakWeekend
        peak -> peakWeekday
        weekend -> offpeakWeekend
        else -> offpeakWeekday
    }
}
