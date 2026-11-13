package kr.sunsetcoast.controller.reservation

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kr.sunsetcoast.controller.reservation.dto.response.ReservationDetailResponseBody
import kr.sunsetcoast.controller.reservation.dto.response.ReservationResponseBody
import java.time.LocalDate

@Tag(name = "Reservation", description = "예약 조회")
interface ReservationControllerDocs {
    @Operation(
        summary = "기간별 예약 목록",
        description = "입실일이 기간 안에 있는 예약. 입실일을 모르는 예약(취소수수료만 있는 예약)은 매출인식일 기준으로 포함한다. 최대 1년.",
    )
    fun getReservations(
        @Parameter(description = "업소 ID", example = "1") propertyId: Long,
        @Parameter(description = "시작일", example = "2026-08-01") from: LocalDate,
        @Parameter(description = "종료일 (포함)", example = "2026-08-31") to: LocalDate,
    ): List<ReservationResponseBody>

    @Operation(summary = "예약 상세", description = "예약 정보와 정산 행(1박·취소수수료·입실 후 취소), 현장 관리비.")
    fun getReservation(@Parameter(description = "예약 ID", example = "656") id: Long): ReservationDetailResponseBody
}
