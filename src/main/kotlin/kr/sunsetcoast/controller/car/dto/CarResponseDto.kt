package kr.sunsetcoast.controller.car.dto

import kr.sunsetcoast.domain.car.vo.CarEntity

data class CarResponseDto(
    val id: Long,
    val carNumber: String,
    val carModel: String,
) {
    companion object {
        fun from(carEntity: CarEntity): CarResponseDto =
            CarResponseDto(
                id = requireNotNull(carEntity.id),
                carNumber = carEntity.carNumber,
                carModel = carEntity.carModel,
            )
    }
}
