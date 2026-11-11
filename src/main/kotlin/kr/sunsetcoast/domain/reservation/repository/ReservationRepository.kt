package kr.sunsetcoast.domain.reservation.repository

import kr.sunsetcoast.domain.reservation.entity.ReservationEntity
import kr.sunsetcoast.domain.reservation.vo.OtaPlatform
import org.springframework.data.jpa.repository.JpaRepository

interface ReservationRepository : JpaRepository<ReservationEntity, Long> {
    fun findBySourceAndReservationNumber(source: OtaPlatform, reservationNumber: String): ReservationEntity?
}
