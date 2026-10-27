package kr.sunsetcoast.domain.sunsettime

import kr.sunsetcoast.domain.sunsettime.vo.SunsetDay
import kr.sunsetcoast.domain.sunsettime.vo.SunsetTime
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime

@Service
class SunsetTimeService {
    fun getSunsetTime(): SunsetTime {
        // TODO: 실제 일몰 계산으로 교체. 임시로 오늘 = 지금 + 1시간, 내일 = 지금 + 25시간
        val now = ZonedDateTime.now(ZoneId.of("Asia/Seoul"))
        val today = now.toLocalDate()

        return SunsetTime(
            today = SunsetDay(
                date = today,
                sunset = now.plusHours(1).toLocalTime(),
            ),
            tomorrow = SunsetDay(
                date = today.plusDays(1),
                sunset = now.plusHours(25).toLocalTime(),
            ),
        )
    }
}