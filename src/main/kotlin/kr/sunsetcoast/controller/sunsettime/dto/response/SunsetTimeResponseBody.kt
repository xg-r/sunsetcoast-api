package kr.sunsetcoast.controller.sunsettime.dto.response

import kr.sunsetcoast.domain.sunsettime.vo.SunsetDay
import kr.sunsetcoast.domain.sunsettime.vo.SunsetTime
import java.time.format.DateTimeFormatter

data class SunsetTimeResponseBody(
    val today: SunsetDayResponseBody,
    val tomorrow: SunsetDayResponseBody,
) {
    companion object {
        fun from(sunsetTime: SunsetTime): SunsetTimeResponseBody {
            return SunsetTimeResponseBody(
                today = SunsetDayResponseBody.from(sunsetTime.today),
                tomorrow = SunsetDayResponseBody.from(sunsetTime.tomorrow),
            )
        }
    }
}

data class SunsetDayResponseBody(
    val date: String,   // YYYY-MM-DD (KST)
    val sunset: String, // HH:mm, 24시간, 두 자리 (KST)
) {
    companion object {
        private val HH_MM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        fun from(day: SunsetDay): SunsetDayResponseBody =
            SunsetDayResponseBody(
                date = day.date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                sunset = day.sunset.format(HH_MM),
            )
    }
}
