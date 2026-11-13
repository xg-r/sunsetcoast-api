package kr.sunsetcoast.controller.settlement.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "정산 파일이 없는 주를 확인 완료로 표시")
data class SettlementWeekCheckRequestBody(
    @field:Schema(description = "플랫폼", example = "NOL")
    val platform: String,
    @field:Schema(description = "정산 계정 (파일 현황의 account)", example = "대정시크릿")
    val account: String,
    @field:Schema(description = "그 주의 월요일", example = "2026-08-10")
    val weekStart: LocalDate,
    @field:Schema(description = "메모", nullable = true, example = "그 주 판매 없음")
    val note: String? = null,
)
