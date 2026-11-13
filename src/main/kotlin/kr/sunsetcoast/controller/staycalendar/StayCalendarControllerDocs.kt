package kr.sunsetcoast.controller.staycalendar

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kr.sunsetcoast.controller.staycalendar.dto.request.SeasonDayOverrideRequestBody
import kr.sunsetcoast.controller.staycalendar.dto.response.RecalculateResponseBody
import kr.sunsetcoast.controller.staycalendar.dto.response.SeasonDayResponseBody
import java.time.LocalDate

@Tag(name = "Stay Calendar", description = "성수기/주말 판단")
interface StayCalendarControllerDocs {
    @Operation(
        summary = "날짜별 성수기 조회",
        description = "예약 금액이 요금표와 정확히 일치한 예약 수(표)와 다수결 결과를 보여준다.",
    )
    fun getSeasonDays(
        @Parameter(description = "업소 ID", example = "1") propertyId: Long,
        @Parameter(description = "시작일", example = "2026-08-01") from: LocalDate,
        @Parameter(description = "종료일", example = "2026-08-31") to: LocalDate,
    ): List<SeasonDayResponseBody>

    @Operation(
        summary = "날짜 성수기 여부 수정",
        description = "자동 판단이 틀린 날짜만 고친다. 고친 날짜는 이후 업로드로 덮어쓰지 않으며, 그날에 걸친 예약의 성수기 여부도 다시 계산한다.",
    )
    fun overrideSeasonDay(
        @Parameter(description = "업소 ID", example = "1") propertyId: Long,
        @Parameter(description = "숙박일", example = "2026-08-20") date: LocalDate,
        body: SeasonDayOverrideRequestBody,
    ): SeasonDayResponseBody

    @Operation(
        summary = "성수기/주말 전체 재계산",
        description = "공휴일 시드를 추가했거나 요금표를 고친 뒤 실행한다.",
    )
    fun recalculate(): RecalculateResponseBody
}
