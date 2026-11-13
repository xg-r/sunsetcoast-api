package kr.sunsetcoast.controller.staycalendar.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "날짜 성수기 여부 수정")
data class SeasonDayOverrideRequestBody(
    @field:Schema(description = "true: 성수기, false: 비수기, null: 수정 취소(자동 판단으로 복귀)", nullable = true, example = "true")
    val peak: Boolean?,
)
