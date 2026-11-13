package kr.sunsetcoast.controller.settlement.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "미매핑 행 처리 결과")
data class ResolveUnmappedRowResponseBody(
    @field:Schema(description = "처리 결과", allowableValues = ["INSERTED", "DUPLICATE", "CHANGED", "SKIPPED"], example = "INSERTED")
    val result: String,
    @field:Schema(description = "처리 내용", example = "객실 지정: 제돌이, 수애기")
    val message: String?,
    @field:Schema(description = "같이 처리된 다른 미매핑 행 수 (같은 객실명·같은 예약번호)", example = "0")
    val autoResolved: Int,
)
