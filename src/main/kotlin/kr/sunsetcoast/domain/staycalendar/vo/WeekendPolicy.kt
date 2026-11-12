package kr.sunsetcoast.domain.staycalendar.vo

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * 주말 밤 판단. 금·토요일 밤과 공휴일 전날 밤(예: 대체공휴일 전 일요일 밤, 연휴 첫날 전날 밤)을 주말로 본다.
 */
class WeekendPolicy(private val holidays: Set<LocalDate>) {
    fun isWeekendNight(night: LocalDate): Boolean =
        night.dayOfWeek == DayOfWeek.FRIDAY ||
            night.dayOfWeek == DayOfWeek.SATURDAY ||
            night.plusDays(1) in holidays
}
