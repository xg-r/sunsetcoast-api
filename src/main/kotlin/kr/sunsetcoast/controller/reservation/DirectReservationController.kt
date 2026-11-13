package kr.sunsetcoast.controller.reservation

import kr.sunsetcoast.config.security.CurrentUser
import kr.sunsetcoast.controller.reservation.dto.request.CancelDirectReservationRequestBody
import kr.sunsetcoast.controller.reservation.dto.request.DirectReservationRequestBody
import kr.sunsetcoast.controller.reservation.dto.response.DirectReservationQuoteResponseBody
import kr.sunsetcoast.controller.reservation.dto.response.ReservationDetailResponseBody
import kr.sunsetcoast.domain.reservation.service.DirectReservationService
import kr.sunsetcoast.domain.reservation.service.ReservationQueryService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/reservations/direct")
class DirectReservationController(
    private val directReservationService: DirectReservationService,
    private val reservationQueryService: ReservationQueryService,
) : DirectReservationControllerDocs {
    @GetMapping("/quote", version = "1.0")
    override fun quote(
        @RequestParam roomIds: List<Long>,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) checkIn: LocalDate,
        @RequestParam nights: Int,
        @RequestParam(required = false) excludeReservationId: Long?,
    ): DirectReservationQuoteResponseBody =
        DirectReservationQuoteResponseBody.from(directReservationService.quote(roomIds, checkIn, nights, excludeReservationId))

    @PostMapping(version = "1.0")
    @ResponseStatus(HttpStatus.CREATED)
    override fun create(@RequestBody body: DirectReservationRequestBody): ReservationDetailResponseBody {
        val reservation = directReservationService.create(body.toCommand())
        return ReservationDetailResponseBody.from(reservationQueryService.findDetail(reservation.id!!), CurrentUser.canSeeManagementFees())
    }

    @PutMapping("/{id}", version = "1.0")
    override fun update(@PathVariable id: Long, @RequestBody body: DirectReservationRequestBody): ReservationDetailResponseBody {
        directReservationService.update(id, body.toCommand())
        return ReservationDetailResponseBody.from(reservationQueryService.findDetail(id), CurrentUser.canSeeManagementFees())
    }

    @PostMapping("/{id}/cancel", version = "1.0")
    override fun cancel(@PathVariable id: Long, @RequestBody body: CancelDirectReservationRequestBody): ReservationDetailResponseBody {
        directReservationService.cancel(id, body.cancellationFee, body.cancelledOn ?: LocalDate.now())
        return ReservationDetailResponseBody.from(reservationQueryService.findDetail(id), CurrentUser.canSeeManagementFees())
    }

    @DeleteMapping("/{id}", version = "1.0")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun delete(@PathVariable id: Long) = directReservationService.delete(id)
}
