package kr.sunsetcoast.controller.car.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.sunsetcoast.domain.car.entity.CarEntity

@Schema(description = "차량")
data class CarResponseBody(
    @field:Schema(description = "차량 ID", example = "1")
    val id: Long,
    @field:Schema(description = "차량 번호", example = "12가3456")
    val carNumber: String,
    @field:Schema(description = "차종", example = "카니발")
    val carModel: String,
) {
    companion object {
        fun from(carEntity: CarEntity): CarResponseBody =
            CarResponseBody(
                id = requireNotNull(carEntity.id),
                carNumber = carEntity.carNumber,
                carModel = carEntity.carModel,
            )
    }
}
