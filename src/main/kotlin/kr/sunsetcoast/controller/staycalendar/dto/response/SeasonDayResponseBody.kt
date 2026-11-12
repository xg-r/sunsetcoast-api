package kr.sunsetcoast.controller.staycalendar.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.sunsetcoast.domain.staycalendar.entity.SeasonDayEntity

@Schema(description = "업소·날짜별 성수기 여부")
data class SeasonDayResponseBody(
    @field:Schema(description = "업소 ID", example = "1")
    val propertyId: Long,
    @field:Schema(description = "숙박일 (그날 밤)", example = "2026-08-08")
    val stayDate: String,
    @field:Schema(description = "성수기 여부. 판단 근거가 없거나 동률이면 null", nullable = true, example = "true")
    val peak: Boolean?,
    @field:Schema(
        description = "INFERRED: 예약 금액으로 자동 판단, MANUAL: 관리자 지정",
        allowableValues = ["INFERRED", "MANUAL"],
        example = "INFERRED",
    )
    val source: String,
    @field:Schema(description = "성수기 요금과 일치한 예약 수", example = "3")
    val peakVotes: Int,
    @field:Schema(description = "비수기 요금과 일치한 예약 수", example = "0")
    val offpeakVotes: Int,
) {
    companion object {
        fun from(entity: SeasonDayEntity) = SeasonDayResponseBody(
            propertyId = entity.propertyId,
            stayDate = entity.stayDate.toString(),
            peak = entity.peak,
            source = entity.source.name,
            peakVotes = entity.peakVotes,
            offpeakVotes = entity.offpeakVotes,
        )
    }
}
