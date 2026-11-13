package kr.sunsetcoast.controller.reservation

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import kr.sunsetcoast.controller.reservation.dto.request.CancelDirectReservationRequestBody
import kr.sunsetcoast.controller.reservation.dto.request.DirectReservationRequestBody
import kr.sunsetcoast.controller.reservation.dto.response.DirectReservationQuoteResponseBody
import kr.sunsetcoast.controller.reservation.dto.response.ReservationDetailResponseBody
import java.time.LocalDate

@Tag(name = "Direct Reservation", description = "직접 예약 입력 (전화·현장)")
interface DirectReservationControllerDocs {
    @Operation(
        summary = "금액 제안·겹치는 예약 확인",
        description = "요금표·성수기·주말로 객실·숙박일별 금액을 제안하고, 같은 객실·날짜의 투숙 예약을 돌려준다.",
    )
    fun quote(
        @Parameter(description = "객실 ID (쉼표로 여러 개)", example = "5") roomIds: List<Long>,
        @Parameter(description = "입실일", example = "2026-11-20") checkIn: LocalDate,
        @Parameter(description = "박수", example = "2") nights: Int,
        @Parameter(description = "수정 중인 예약 ID (자기 자신은 겹침에서 뺀다)") excludeReservationId: Long?,
    ): DirectReservationQuoteResponseBody

    @Operation(summary = "직접 예약 입력", description = "겹치는 예약이 있으면 409. `allowOverlap=true`로 다시 보내면 저장한다.")
    @ApiResponse(responseCode = "409", description = "같은 객실·날짜에 예약이 있음 (conflicts 포함)")
    fun create(body: DirectReservationRequestBody): ReservationDetailResponseBody

    @Operation(summary = "직접 예약 수정", description = "객실·날짜·금액을 바꾼다. 남는 객실의 관리비 수정값은 유지한다.")
    fun update(@Parameter(description = "예약 ID") id: Long, body: DirectReservationRequestBody): ReservationDetailResponseBody

    @Operation(summary = "직접 예약 취소", description = "받은 취소수수료가 있으면 취소일에 취소수수료 매출로 남긴다.")
    fun cancel(@Parameter(description = "예약 ID") id: Long, body: CancelDirectReservationRequestBody): ReservationDetailResponseBody

    @Operation(summary = "직접 예약 삭제", description = "잘못 입력한 예약을 지운다 (관리비·정산 행 포함).")
    fun delete(@Parameter(description = "예약 ID") id: Long)
}
