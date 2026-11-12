package kr.sunsetcoast.domain.staycalendar

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kr.sunsetcoast.domain.staycalendar.entity.SeasonDayEntity
import kr.sunsetcoast.domain.staycalendar.vo.ListPrice
import kr.sunsetcoast.domain.staycalendar.vo.NightAmount
import kr.sunsetcoast.domain.staycalendar.vo.ReservationStayFlags
import kr.sunsetcoast.domain.staycalendar.vo.SeasonMatcher
import kr.sunsetcoast.domain.staycalendar.vo.SeasonSource
import kr.sunsetcoast.domain.staycalendar.vo.WeekendPolicy
import java.math.BigDecimal
import java.time.LocalDate

class StayCalendarRulesTest : FunSpec({
    fun won(value: Long) = BigDecimal(value)
    fun date(value: String) = LocalDate.parse(value)

    // 2026-08-17(월) 광복절 대체공휴일
    val policy = WeekendPolicy(setOf(date("2026-08-15"), date("2026-08-17")))
    // 에어스트림: 비수기 150k/170k, 성수기 220k/220k
    val airstream = ListPrice(won(150_000), won(170_000), won(220_000), won(220_000))

    context("주말 밤") {
        test("금·토요일 밤과 공휴일 전날 밤은 주말이다") {
            policy.isWeekendNight(date("2026-08-07")) shouldBe true // 금
            policy.isWeekendNight(date("2026-08-08")) shouldBe true // 토
            policy.isWeekendNight(date("2026-08-09")) shouldBe false // 일
            policy.isWeekendNight(date("2026-08-16")) shouldBe true // 일, 다음날 대체공휴일
            policy.isWeekendNight(date("2026-08-17")) shouldBe false // 대체공휴일 당일 밤 (다음날 평일)
        }
    }

    context("성수기 판단") {
        test("박별 실제 금액이면 박마다 비교한다") {
            val votes = SeasonMatcher.votes(
                airstream,
                listOf(
                    NightAmount(date("2026-08-08"), won(220_000)), // 성수기 주말
                    NightAmount(date("2026-08-19"), won(150_000)), // 비수기 평일
                    NightAmount(date("2026-08-20"), won(185_000)), // 일치 없음
                ),
                splitEstimated = false,
                weekendPolicy = policy,
            )
            votes shouldBe mapOf(date("2026-08-08") to true, date("2026-08-19") to false)
        }

        test("균등 분할 금액(떠나요 연박)이면 합계를 비교해 모든 박에 같은 표를 준다") {
            // 토(주말 170k) + 일(평일 150k) = 비수기 320k, 박당 160k로 분할돼 들어온다
            val nights = listOf(
                NightAmount(date("2026-08-22"), won(160_000)),
                NightAmount(date("2026-08-23"), won(160_000)),
            )
            SeasonMatcher.votes(airstream, nights, splitEstimated = true, weekendPolicy = policy) shouldBe
                mapOf(date("2026-08-22") to false, date("2026-08-23") to false)
        }

        test("성수기·비수기 요금이 같아 구분할 수 없으면 표를 주지 않는다") {
            val flat = ListPrice(won(139_000), won(139_000), won(139_000), won(139_000))
            SeasonMatcher.votes(
                flat, listOf(NightAmount(date("2026-08-19"), won(139_000))), false, policy,
            ).shouldBeEmpty()
        }

        test("날짜 판단은 다수결, 동률이면 null, 관리자 지정값은 재계산이 덮어쓰지 않는다") {
            val day = SeasonDayEntity(propertyId = 1, stayDate = date("2026-08-20"))
            day.applyVotes(peakVotes = 1, offpeakVotes = 3)
            day.peak shouldBe false
            day.applyVotes(peakVotes = 2, offpeakVotes = 2)
            day.peak shouldBe null

            day.overrideManually(true)
            day.applyVotes(peakVotes = 0, offpeakVotes = 5)
            day.peak shouldBe true
            day.source shouldBe SeasonSource.MANUAL

            day.clearManual()
            day.peak shouldBe false
            day.source shouldBe SeasonSource.INFERRED
        }
    }

    context("예약 단위 피처") {
        test("주말 밤이 하나라도 있으면 주말 예약이다") {
            ReservationStayFlags.weekend(listOf(date("2026-08-08"), date("2026-08-09")), policy) shouldBe true
            ReservationStayFlags.weekend(listOf(date("2026-08-18"), date("2026-08-19")), policy) shouldBe false
            ReservationStayFlags.weekend(emptyList(), policy) shouldBe null
        }

        test("성수기 밤이 하나라도 있으면 성수기, 판단된 밤이 없으면 null") {
            val season = mapOf(date("2026-08-18") to true, date("2026-08-19") to false)
            ReservationStayFlags.peakSeason(listOf(date("2026-08-18"), date("2026-08-19")), season::get) shouldBe true
            ReservationStayFlags.peakSeason(listOf(date("2026-08-19"), date("2026-08-20")), season::get) shouldBe false
            ReservationStayFlags.peakSeason(listOf(date("2026-08-25")), season::get) shouldBe null
        }
    }
})
