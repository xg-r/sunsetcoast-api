package kr.sunsetcoast.domain.sunsettime.vo

import java.time.LocalDate
import java.time.LocalTime

data class SunsetTime(
    val today: SunsetDay,
    val tomorrow: SunsetDay,
)

data class SunsetDay(
    val date: LocalDate,
    val sunset: LocalTime,
)
