package kr.sunsetcoast.controller.managementfee.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "현장 객실 변경")
data class ManagementFeeRoomChangeRequestBody(
    @field:Schema(description = "옮긴 객실 ID. null이거나 예약한 객실이면 변경 취소", nullable = true, example = "3")
    val roomId: Long?,
    @field:Schema(description = "손님에게 받은 차액 (환불이면 음수, 없으면 null)", nullable = true, example = "120000")
    val difference: BigDecimal? = null,
    @field:Schema(description = "메모", nullable = true, example = "온수 고장으로 이동")
    val memo: String? = null,
)
