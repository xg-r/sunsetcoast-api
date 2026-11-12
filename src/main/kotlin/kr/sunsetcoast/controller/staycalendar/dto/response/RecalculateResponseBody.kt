package kr.sunsetcoast.controller.staycalendar.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "성수기/주말 재계산 결과")
data class RecalculateResponseBody(
    @field:Schema(description = "다시 계산한 예약 수", example = "268")
    val reservations: Int,
)
