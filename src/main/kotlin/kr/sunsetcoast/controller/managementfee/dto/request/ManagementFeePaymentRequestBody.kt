package kr.sunsetcoast.controller.managementfee.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "관리비 입금 확인")
data class ManagementFeePaymentRequestBody(
    @field:Schema(description = "관리비 ID 목록", example = "[12, 13]")
    val ids: List<Long>,
    @field:Schema(description = "true: 입금 완료로 표시, false: 입금 완료 취소", example = "true")
    val paid: Boolean,
)
