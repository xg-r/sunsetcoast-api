package kr.sunsetcoast.controller.settlement.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "미매핑 행에 객실 지정")
data class ResolveUnmappedRowRequestBody(
    @field:Schema(description = "객실 ID. 다중 객실 예약은 파일에 적힌 순서대로 여러 개", example = "[7, 3]")
    val roomIds: List<Long>,
    @field:Schema(description = "객실이 하나일 때, 파일의 객실명을 별칭으로 저장해 다음 업로드부터 자동으로 찾는다", example = "true")
    val saveAlias: Boolean = false,
)
