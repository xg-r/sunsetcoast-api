package kr.sunsetcoast.controller.car.dto.response

import kr.sunsetcoast.domain.car.entity.CarEntity

data class CarResponseBody(
    val id: Long,
    val carNumber: String,
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
