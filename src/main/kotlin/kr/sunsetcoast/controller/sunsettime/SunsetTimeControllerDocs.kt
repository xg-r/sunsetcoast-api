package kr.sunsetcoast.controller.sunsettime

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kr.sunsetcoast.controller.sunsettime.dto.response.SunsetTimeResponseBody
import org.springframework.http.ResponseEntity

@Tag(name = "Sunset Time", description = "일몰 시각")
interface SunsetTimeControllerDocs {
    @Operation(summary = "오늘·내일 일몰 시각 조회", description = "날짜와 시각은 모두 KST 기준이다.")
    fun getSunsetTime(): ResponseEntity<SunsetTimeResponseBody>
}
