package kr.sunsetcoast.controller.settlement.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "정산 계정 등록")
data class SettlementAccountRequestBody(
    @field:Schema(description = "플랫폼", example = "DDNAYO")
    val platform: String,
    @field:Schema(description = "파일명에 나오는 계정 이름", example = "서귀포 스테이 디앤디 풀빌라")
    val account: String,
    @field:Schema(description = "업소 ID", nullable = true, example = "2")
    val propertyId: Long? = null,
    @field:Schema(description = "이 날짜가 속한 주부터 정산 파일이 있어야 한다", example = "2026-08-03")
    val startedOn: LocalDate,
    @field:Schema(
        description = "떠나요 사업자번호 (하이픈 있어도 됨). 떠나요에서 받은 그대로의 AdjustCalendar 파일을 이 계정에 붙인다",
        nullable = true, example = "278-27-01574",
    )
    val businessNumber: String? = null,
)

@Schema(description = "정산 계정 사업자번호 변경")
data class SettlementAccountBusinessNumberRequestBody(
    @field:Schema(description = "떠나요 사업자번호. 비우면 지운다", nullable = true, example = "278-27-01574")
    val businessNumber: String?,
)
