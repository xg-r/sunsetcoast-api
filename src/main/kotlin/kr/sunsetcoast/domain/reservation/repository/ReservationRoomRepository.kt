package kr.sunsetcoast.domain.reservation.repository

import kr.sunsetcoast.domain.reservation.entity.ReservationRoomEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ReservationRoomRepository : JpaRepository<ReservationRoomEntity, Long> {
    @Query(
        """
        select rr from ReservationRoomEntity rr join fetch rr.room
        where rr.reservation.id in :reservationIds
        order by rr.reservation.id, rr.sortOrder
        """
    )
    fun findByReservationIds(@Param("reservationIds") reservationIds: Collection<Long>): List<ReservationRoomEntity>

    /** 객실의 투숙 예약 중 입실일이 기간 안인 것 (겹치는 예약 검사용) */
    @Query(
        """
        select rr from ReservationRoomEntity rr join fetch rr.reservation r join fetch rr.room
        where rr.room.id in :roomIds and r.status = kr.sunsetcoast.domain.reservation.vo.ReservationStatus.CONFIRMED
          and r.checkInDate between :from and :to
        """
    )
    fun findConfirmedByRoomsAndCheckInBetween(
        @Param("roomIds") roomIds: Collection<Long>,
        @Param("from") from: java.time.LocalDate,
        @Param("to") to: java.time.LocalDate,
    ): List<ReservationRoomEntity>
}
