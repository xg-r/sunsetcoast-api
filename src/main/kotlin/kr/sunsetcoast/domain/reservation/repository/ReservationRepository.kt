package kr.sunsetcoast.domain.reservation.repository

import kr.sunsetcoast.domain.reservation.entity.ReservationEntity
import kr.sunsetcoast.domain.reservation.vo.OtaPlatform
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface ReservationRepository : JpaRepository<ReservationEntity, Long> {
    fun findBySourceAndReservationNumber(source: OtaPlatform, reservationNumber: String): ReservationEntity?

    fun findBySourceAndReservationNumberStartingWith(source: OtaPlatform, prefix: String): List<ReservationEntity>

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

    /**
     * 기준일이 기간 안에 있는 예약. 기준일은 입실일이고, 입실일을 모르는 예약(취소수수료만 있는 예약)은 매출인식일.
     */
    @Query(
        """
        select r from ReservationEntity r join fetch r.room room
        where room.property.id = :propertyId
          and (
            r.checkInDate between :from and :to
            or (r.checkInDate is null and exists (
                select 1 from SettlementLineEntity s where s.reservation = r and s.stayDate between :from and :to
            ))
          )
        """
    )
    fun findByPropertyAndBaseDateBetween(
        @Param("propertyId") propertyId: Long,
        @Param("from") from: LocalDate,
        @Param("to") to: LocalDate,
    ): List<ReservationEntity>

    @Query("select r from ReservationEntity r join fetch r.room")
    fun findAllWithRoom(): List<ReservationEntity>
}
