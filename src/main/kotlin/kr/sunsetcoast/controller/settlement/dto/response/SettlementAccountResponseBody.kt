package kr.sunsetcoast.controller.settlement.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.sunsetcoast.domain.settlement.entity.SettlementAccountEntity

@Schema(description = "등록한 정산 계정")
data class SettlementAccountResponseBody(
    val id: Long,
    @field:Schema(description = "플랫폼", example = "DDNAYO")
    val platform: String,
    @field:Schema(description = "파일명에 나오는 계정 이름", example = "서귀포 스테이 디앤디 풀빌라")
    val account: String,
    @field:Schema(description = "업소 ID", nullable = true, example = "2")
    val propertyId: Long?,
    @field:Schema(description = "정산 파일이 있어야 하는 첫 주 (월요일)", example = "2026-08-03")
    val startedOn: String,
) {
    companion object {
        fun from(entity: SettlementAccountEntity) = SettlementAccountResponseBody(
            id = entity.id!!,
            platform = entity.source.name,
            account = entity.account,
            propertyId = entity.propertyId,
            startedOn = entity.startedOn.toString(),
        )
    }
}
