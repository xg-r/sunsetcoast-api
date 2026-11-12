package kr.sunsetcoast.controller.room

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Room", description = "객실")
interface RoomControllerDocs {
    @Operation(summary = "객실 목록 조회 (작업 중)", description = "현재는 조회만 수행하고 `OK` 문자열을 돌려준다.")
    fun getRooms(
        @Parameter(description = "페이지 크기", example = "20") size: Int,
        @Parameter(description = "페이지 번호 (0부터)", example = "0") page: Int,
    ): String
}
