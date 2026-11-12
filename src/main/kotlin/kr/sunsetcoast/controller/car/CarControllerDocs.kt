package kr.sunsetcoast.controller.car

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kr.sunsetcoast.controller.car.dto.response.CarResponseBody

@Tag(name = "Car", description = "차량")
interface CarControllerDocs {
    @Operation(summary = "차량 목록 조회", description = "등록된 모든 차량을 조회한다.")
    fun getCars(): List<CarResponseBody>
}
