package kr.sunsetcoast.controller.reservation.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDate

@Schema(description = "직접 예약 취소")
data class CancelDirectReservationRequestBody(
    @field:Schema(description = "받은 취소수수료 (환불하지 않은 금액). 0이면 전액 환불", example = "0")
    val cancellationFee: BigDecimal = BigDecimal.ZERO,
    @field:Schema(description = "취소일 (취소수수료의 매출인식일). 생략하면 오늘", nullable = true, example = "2026-11-13")
    val cancelledOn: LocalDate? = null,
)
