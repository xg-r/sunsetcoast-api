package kr.sunsetcoast.controller.settlement.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.sunsetcoast.domain.settlement.entity.SettlementWeekCheckEntity

@Schema(description = "확인 완료로 표시한 주")
data class SettlementWeekCheckResponseBody(
    val id: Long,
    @field:Schema(description = "플랫폼", example = "NOL")
    val platform: String,
    @field:Schema(description = "정산 계정", example = "대정시크릿")
    val account: String,
    @field:Schema(description = "그 주의 월요일", example = "2026-08-10")
    val weekStart: String,
    @field:Schema(description = "메모", nullable = true, example = "그 주 판매 없음")
    val note: String?,
    @field:Schema(description = "표시한 시각", example = "2026-11-13T15:02:11")
    val createdAt: String,
) {
    companion object {
        fun from(entity: SettlementWeekCheckEntity) = SettlementWeekCheckResponseBody(
            id = entity.id!!,
            platform = entity.source.name,
            account = entity.account,
            weekStart = entity.weekStart.toString(),
            note = entity.note,
            createdAt = entity.createdAt.withNano(0).toString(),
        )
    }
}
