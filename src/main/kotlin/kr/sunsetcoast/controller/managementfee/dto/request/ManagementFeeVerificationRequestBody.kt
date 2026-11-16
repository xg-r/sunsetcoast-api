package kr.sunsetcoast.controller.managementfee.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "관리비 확인 표시")
data class ManagementFeeVerificationRequestBody(
    @field:Schema(description = "관리비 ID 목록", example = "[12]")
    val ids: List<Long>,
    @field:Schema(description = "true: 맞다고 확인 (지금 금액으로), false: 확인 취소", example = "true")
    val verified: Boolean,
)
