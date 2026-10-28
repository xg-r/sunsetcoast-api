package kr.sunsetcoast.domain.car.service

import kr.sunsetcoast.domain.car.repository.CarRepository
import kr.sunsetcoast.domain.car.entity.CarEntity
import org.springframework.stereotype.Service

@Service
class CarService(
    val carRepository: CarRepository,
) {
    fun getCars(): List<CarEntity> = carRepository.findAll()
}
