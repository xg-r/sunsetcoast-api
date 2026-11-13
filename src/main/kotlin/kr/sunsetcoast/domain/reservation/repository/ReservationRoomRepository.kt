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
}
