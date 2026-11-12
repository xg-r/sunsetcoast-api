package kr.sunsetcoast.controller.sunsettime.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.sunsetcoast.domain.sunsettime.vo.SunsetDay
import kr.sunsetcoast.domain.sunsettime.vo.SunsetTime
import java.time.format.DateTimeFormatter

@Schema(description = "오늘·내일 일몰 시각")
data class SunsetTimeResponseBody(
    @field:Schema(description = "오늘")
    val today: SunsetDayResponseBody,
    @field:Schema(description = "내일")
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

@Schema(description = "하루의 일몰 시각")
data class SunsetDayResponseBody(
    @field:Schema(description = "날짜 (YYYY-MM-DD, KST)", example = "2026-11-12")
    val date: String,
    @field:Schema(description = "일몰 시각 (HH:mm, 24시간, KST)", example = "17:24")
    val sunset: String,
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
