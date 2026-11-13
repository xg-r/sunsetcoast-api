package kr.sunsetcoast.controller.reservation.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.sunsetcoast.domain.reservation.service.DirectReservationService
import java.math.BigDecimal

@Schema(description = "직접 예약 금액 제안과 겹치는 예약")
data class DirectReservationQuoteResponseBody(
    val nights: List<Night>,
    val conflicts: List<ConflictBody>,
    @field:Schema(description = "외부 소유주 객실(Villa II·III)이 있어 소유주 계좌로 입금되는 예약")
    val ownerPaid: Boolean,
) {
    @Schema(name = "DirectReservationQuoteNight")
    data class Night(
        @field:Schema(example = "2026-11-20")
        val date: String,
        @field:Schema(description = "주말 밤(금·토, 공휴일 전날)")
        val weekend: Boolean,
        @field:Schema(description = "성수기 여부. 판단 근거가 없으면 null(비수기 요금으로 제안)", nullable = true)
        val peak: Boolean?,
        @field:Schema(description = "객실 ID → 요금표 금액 (요금표에 없으면 null)")
        val prices: Map<Long, BigDecimal?>,
    )

    companion object {
        fun from(quote: DirectReservationService.Quote) = DirectReservationQuoteResponseBody(
            nights = quote.nights.map { Night(it.date.toString(), it.weekend, it.peak, it.prices) },
            conflicts = quote.conflicts.map { ConflictBody.from(it) },
            ownerPaid = quote.ownerPaid,
        )
    }
}

@Schema(name = "ReservationConflict", description = "같은 객실·날짜의 기존 예약")
data class ConflictBody(
    val reservationId: Long,
    val source: String,
    val reservationNumber: String,
    val roomName: String,
    val bookerName: String?,
    val checkInDate: String,
    val checkOutDate: String,
) {
    companion object {
        fun from(c: DirectReservationService.Conflict) = ConflictBody(
            c.reservationId, c.source.name, c.reservationNumber, c.roomName, c.bookerName, c.checkInDate.toString(), c.checkOutDate.toString(),
        )
    }
}
