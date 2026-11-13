package kr.sunsetcoast.domain.reservation.repository

import kr.sunsetcoast.domain.reservation.entity.ReservationEntity
import kr.sunsetcoast.domain.reservation.vo.OtaPlatform
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface ReservationRepository : JpaRepository<ReservationEntity, Long> {
    fun findBySourceAndReservationNumber(source: OtaPlatform, reservationNumber: String): ReservationEntity?

    @Query(
        """
        select r from ReservationEntity r join fetch r.room room
        where room.property.id = :propertyId and r.checkInDate between :from and :to
        """
    )
    fun findByPropertyAndCheckInBetween(
        @Param("propertyId") propertyId: Long,
        @Param("from") from: LocalDate,
        @Param("to") to: LocalDate,
    ): List<ReservationEntity>

    @Query(
        """
        select r from ReservationEntity r join fetch r.room room
        where room.id in :roomIds and r.checkInDate between :from and :to
        """
    )
    fun findByRoomsAndCheckInBetween(
        @Param("roomIds") roomIds: Collection<Long>,
        @Param("from") from: LocalDate,
        @Param("to") to: LocalDate,
    ): List<ReservationEntity>

    @Query("select r from ReservationEntity r join fetch r.room")
    fun findAllWithRoom(): List<ReservationEntity>
}
