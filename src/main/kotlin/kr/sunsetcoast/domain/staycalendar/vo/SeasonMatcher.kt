package kr.sunsetcoast.domain.staycalendar.vo

import java.math.BigDecimal
import java.time.LocalDate

/** 1박 금액 (할인 전: 정산 기준 금액 + 우리 부담 할인 + 플랫폼 부담 할인) */
data class NightAmount(
    val night: LocalDate,
    val amount: BigDecimal,
)

/**
 * 예약 금액을 정가와 비교해 날짜별 성수기/비수기 표를 만든다.
 *
 * - 박별 금액이 실제 값이면(여기어때/NOL) 박마다 비교한다.
 * - 박별 금액이 균등 분할 추정치면(떠나요 연박) 예약 합계를 비교하고, 일치하면 모든 박에 같은 표를 준다.
 * - 성수기·비수기 중 정확히 하나만 일치할 때만 표를 준다. 할인·인원 추가 등으로 안 맞으면 표가 없다.
 */
object SeasonMatcher {
    fun votes(
        price: ListPrice,
        nights: List<NightAmount>,
        splitEstimated: Boolean,
        weekendPolicy: WeekendPolicy,
    ): Map<LocalDate, Boolean> {
        if (nights.isEmpty()) return emptyMap()

        if (!splitEstimated) {
            return nights.mapNotNull { (night, amount) ->
                val weekend = weekendPolicy.isWeekendNight(night)
                uniqueMatch { peak -> price.of(peak, weekend).compareTo(amount) == 0 }?.let { night to it }
            }.toMap()
        }

        val total = nights.sumOf { it.amount }
        val peak = uniqueMatch { peak ->
            nights.sumOf { price.of(peak, weekendPolicy.isWeekendNight(it.night)) }.compareTo(total) == 0
        } ?: return emptyMap()
        return nights.associate { it.night to peak }
    }

    private fun uniqueMatch(matches: (Boolean) -> Boolean): Boolean? =
        listOf(false, true).filter(matches).singleOrNull()
}

/** 예약 단위 피처 */
object ReservationStayFlags {
    /** 주말 밤이 하나라도 있으면 주말 예약. 숙박일을 모르면 null */
    fun weekend(nights: Collection<LocalDate>, weekendPolicy: WeekendPolicy): Boolean? =
        if (nights.isEmpty()) null else nights.any(weekendPolicy::isWeekendNight)

    /** 성수기 밤이 하나라도 있으면 true, 판단된 밤이 모두 비수기면 false, 판단된 밤이 없으면 null */
    fun peakSeason(nights: Collection<LocalDate>, seasonByNight: (LocalDate) -> Boolean?): Boolean? {
        val known = nights.mapNotNull(seasonByNight)
        return when {
            known.isEmpty() -> null
            known.any { it } -> true
            else -> false
        }
    }
}
