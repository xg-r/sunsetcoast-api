package kr.sunsetcoast.controller.managementfee.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import kr.sunsetcoast.domain.managementfee.vo.ManagementFeeExtraInput
import kr.sunsetcoast.domain.managementfee.vo.ManagementFeeExtraItem
import java.math.BigDecimal

@Schema(description = "관리비 추가요금. 보낸 목록으로 통째로 바꾼다 (빈 목록이면 모두 지움)")
data class ManagementFeeExtrasRequestBody(
    val extras: List<Extra>,
) {
    data class Extra(
        @field:Schema(description = "BBQ: 바베큐(기본 30,000), EXTRA_PERSON: 추가인원 1박 1인당(기본 20,000), OTHER: 기타", example = "EXTRA_PERSON")
        val item: String,
        @field:Schema(description = "수량 (추가인원은 인원)", example = "2")
        val quantity: Int,
        @field:Schema(description = "단가. 생략하면 항목 기본 단가 (기타는 필수)", nullable = true, example = "20000")
        val unitPrice: BigDecimal? = null,
        @field:Schema(description = "추가인원 박수. 생략하면 예약 박수", nullable = true, example = "1")
        val nights: Int? = null,
        @field:Schema(description = "메모 (기타는 필수)", nullable = true, example = "숯 추가")
        val memo: String? = null,
    ) {
        fun toInput(): ManagementFeeExtraInput {
            val type = runCatching { ManagementFeeExtraItem.valueOf(item) }.getOrNull()
                ?: throw IllegalArgumentException("알 수 없는 추가요금 항목입니다: $item")
            return ManagementFeeExtraInput(type, quantity, unitPrice, nights, memo)
        }
    }
}
