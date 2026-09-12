package kr.sunsetcoast.domain.car.repository

import kr.sunsetcoast.domain.car.vo.CarEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CarJpaRepository : JpaRepository<CarEntity, Long>
