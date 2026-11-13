package kr.sunsetcoast.controller.managementfee.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "관리비 수정")
data class ManagementFeeOverrideRequestBody(
    @field:Schema(description = "지급할 금액. null이면 수정을 취소하고 자동 계산값으로 돌아간다", nullable = true, example = "90000")
    val amount: BigDecimal?,
    @field:Schema(description = "수정 사유 (금액을 지정할 때 필수)", nullable = true, example = "한달살기, 주 1회 청소로 합의")
    val reason: String?,
)
