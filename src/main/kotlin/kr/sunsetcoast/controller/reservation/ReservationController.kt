package kr.sunsetcoast.controller.reservation

import kr.sunsetcoast.config.security.CurrentUser
import kr.sunsetcoast.controller.reservation.dto.response.ReservationDetailResponseBody
import kr.sunsetcoast.controller.reservation.dto.response.ReservationResponseBody
import kr.sunsetcoast.domain.reservation.service.ReservationQueryService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/reservations")
class ReservationController(
    private val reservationQueryService: ReservationQueryService,
) : ReservationControllerDocs {
    @GetMapping(version = "1.0")
    override fun getReservations(
        @RequestParam propertyId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
    ): List<ReservationResponseBody> =
        reservationQueryService.findByPeriod(propertyId, from, to).map { ReservationResponseBody.from(it, CurrentUser.canSeeManagementFees()) }

    @GetMapping("/{id}", version = "1.0")
    override fun getReservation(@PathVariable id: Long): ReservationDetailResponseBody =
        ReservationDetailResponseBody.from(reservationQueryService.findDetail(id), CurrentUser.canSeeManagementFees())
}
