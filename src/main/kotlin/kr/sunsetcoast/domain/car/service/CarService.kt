package kr.sunsetcoast.domain.car.service

import kr.sunsetcoast.domain.car.repository.CarJpaRepository
import kr.sunsetcoast.domain.car.vo.CarEntity
import org.springframework.stereotype.Service

@Service
class CarService(
    val carJpaRepository: CarJpaRepository,
) {
    fun getCars(): List<CarEntity> = carJpaRepository.findAll()
}
