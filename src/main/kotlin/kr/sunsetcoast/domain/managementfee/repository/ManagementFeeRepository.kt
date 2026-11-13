package kr.sunsetcoast.domain.managementfee.repository

import kr.sunsetcoast.domain.managementfee.entity.ManagementFeeEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface ManagementFeeRepository : JpaRepository<ManagementFeeEntity, Long> {
    fun findByReservationIdIn(reservationIds: Collection<Long>): List<ManagementFeeEntity>

    @Query(
        """
        select f from ManagementFeeEntity f
        join fetch f.reservation r
        join fetch f.room room
        where room.property.id = :propertyId and r.checkInDate between :from and :to
        """
    )
    fun findByPropertyAndCheckInBetween(
        @Param("propertyId") propertyId: Long,
        @Param("from") from: LocalDate,
        @Param("to") to: LocalDate,
    ): List<ManagementFeeEntity>
}
