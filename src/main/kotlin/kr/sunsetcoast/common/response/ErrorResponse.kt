package kr.sunsetcoast.common.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "오류 응답")
data class ErrorResponse(
    @field:Schema(description = "예외 클래스 이름", example = "IllegalArgumentException")
    val exception: String,
    @field:Schema(description = "오류 메시지", example = "잘못된 요청입니다.")
    val message: String,
)
