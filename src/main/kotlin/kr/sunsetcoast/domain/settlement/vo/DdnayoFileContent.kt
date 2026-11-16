package kr.sunsetcoast.domain.settlement.vo

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * 떠나요 정산 파일 내용으로 정산 계정과 정산일을 추정한다. 파일명을 읽지 못할 때 쓴다.
 *
 * 떠나요에서 받은 파일 이름은 `AdjustCalendar(yyyy-MM).xls`라 업소명·정산일이 없다.
 * - 계정: 행마다 있는 `사업자번호` (떠나요는 사업자번호별로 정산한다). 가장 많은 값을 쓴다.
 * - 정산일: 떠나요는 체크아웃일(이용일 + 이용박수)이 속한 주(월~일)를 다음 주 화요일에 정산한다.
 *   (8월 주간 파일 4개가 모두 한 주로 떨어지고 파일명 날짜와 같다)
 *   한 달 치를 한 번에 받은 파일은 정산일이 여러 개다 → [settlementDates]. 취소 행은 이용일이 먼 미래일 수 있어 빼고 본다.
 */
data class DdnayoFileContent(
    val businessNumber: String?,
    /** 행이 가장 많은 정산일 */
    val issuedOn: LocalDate?,
    /** 이 파일이 담은 정산일들 (주간 파일은 하나, 월간 파일은 4~5개) */
    val settlementDates: List<LocalDate> = listOfNotNull(issuedOn),
) {
    companion object {
        const val BUSINESS_NUMBER = "사업자번호"
        private const val USE_DATE = "이용일"
        private const val NIGHTS = "이용박수"
        private const val RESERVATION_STATUS = "예약상태"

        /** 다른 주 행이 한두 건 섞여 들어온 것은 그 주를 정산한 파일로 보지 않는다 */
        private const val MIN_ROWS_PER_WEEK = 2
        private const val MIN_SHARE_PER_WEEK = 0.05

        fun of(rows: List<Map<String, String?>>): DdnayoFileContent {
            val businessNumber = rows.mapNotNull { it[BUSINESS_NUMBER]?.trim()?.takeIf(String::isNotEmpty) }
                .groupingBy { it }.eachCount()
                .maxByOrNull { it.value }?.key

            val active = rows.filterNot { it[RESERVATION_STATUS].orEmpty().contains("취소") }.ifEmpty { rows }
            val counts = active.mapNotNull { checkOut(it) }
                .map { it.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }
                .groupingBy { it }.eachCount()
            if (counts.isEmpty()) return DdnayoFileContent(businessNumber, null, emptyList())

            val main = counts.maxWithOrNull(compareBy<Map.Entry<LocalDate, Int>> { it.value }.thenBy { it.key })!!.key
            val total = counts.values.sum()
            val weeks = counts.filter { (week, n) -> week == main || (n >= MIN_ROWS_PER_WEEK && n >= total * MIN_SHARE_PER_WEEK) }.keys
            return DdnayoFileContent(businessNumber, settlementOf(main), weeks.sorted().map(::settlementOf))
        }

        /** 정산일: 체크아웃 주 월요일 + 8일 (다음 주 화요일) */
        private fun settlementOf(weekMonday: LocalDate): LocalDate = weekMonday.plusDays(8)

        private fun checkOut(row: Map<String, String?>): LocalDate? {
            val checkIn = dateOrNull(row[USE_DATE]) ?: return null
            val nights = row[NIGHTS]?.replace(",", "")?.trim()?.toBigDecimalOrNull()?.toLong()?.takeIf { it > 0 } ?: 1
            return checkIn.plusDays(nights)
        }

        /** "2026-08-01" 또는 "2026-08-01T00:00" */
        private fun dateOrNull(value: String?): LocalDate? =
            value?.trim()?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    }
}
