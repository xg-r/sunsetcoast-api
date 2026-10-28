package kr.sunsetcoast.domain.car.repository

import kr.sunsetcoast.domain.car.entity.CarEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CarRepository : JpaRepository<CarEntity, Long>
