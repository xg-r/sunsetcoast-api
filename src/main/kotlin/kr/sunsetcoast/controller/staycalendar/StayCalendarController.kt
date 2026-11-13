package kr.sunsetcoast.controller.staycalendar

import kr.sunsetcoast.controller.staycalendar.dto.request.SeasonDayOverrideRequestBody
import kr.sunsetcoast.controller.staycalendar.dto.response.RecalculateResponseBody
import kr.sunsetcoast.controller.staycalendar.dto.response.SeasonDayResponseBody
import kr.sunsetcoast.domain.staycalendar.service.StayCalendarService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/stay-calendar")
class StayCalendarController(
    private val stayCalendarService: StayCalendarService,
) : StayCalendarControllerDocs {
    @GetMapping("/season-days", version = "1.0")
    override fun getSeasonDays(
        @RequestParam propertyId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
    ): List<SeasonDayResponseBody> =
        stayCalendarService.findSeasonDays(propertyId, from, to).map { SeasonDayResponseBody.from(it) }

    @PutMapping("/season-days/{propertyId}/{date}", version = "1.0")
    override fun overrideSeasonDay(
        @PathVariable propertyId: Long,
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
        @RequestBody body: SeasonDayOverrideRequestBody,
    ): SeasonDayResponseBody =
        SeasonDayResponseBody.from(stayCalendarService.overrideSeason(propertyId, date, body.peak))

    @PostMapping("/recalculate", version = "1.0")
    override fun recalculate(): RecalculateResponseBody =
        RecalculateResponseBody(reservations = stayCalendarService.recalculateAll())
}
