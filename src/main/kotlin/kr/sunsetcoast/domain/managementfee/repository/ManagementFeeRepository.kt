package kr.sunsetcoast.domain.managementfee.repository

import kr.sunsetcoast.domain.managementfee.entity.ManagementFeeEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.time.LocalDateTime

interface ManagementFeeRepository : JpaRepository<ManagementFeeEntity, Long> {
    fun findByReservationIdIn(reservationIds: Collection<Long>): List<ManagementFeeEntity>

    @Query(
        """
        select f from ManagementFeeEntity f
        join fetch f.reservation r
        join fetch f.room room
        where (:propertyId is null or room.property.id = :propertyId)
          and (:businessOwnerId is null or room.businessOwnerEntity.id = :businessOwnerId)
          and r.checkInDate between :from and :to
        """
    )
    fun findByCheckInBetween(
        @Param("propertyId") propertyId: Long?,
        @Param("businessOwnerId") businessOwnerId: Long?,
        @Param("from") from: LocalDate,
        @Param("to") to: LocalDate,
    ): List<ManagementFeeEntity>

    /** 입금 완료 관리비 (입실일 기준 기간) */
    @Query(
        """
        select f from ManagementFeeEntity f
        join fetch f.reservation r
        join fetch f.room room
        join fetch room.businessOwnerEntity
        where f.paidAt is not null and r.checkInDate between :from and :to
        """
    )
    fun findPaidByCheckInBetween(@Param("from") from: LocalDate, @Param("to") to: LocalDate): List<ManagementFeeEntity>

    /** 입금 완료 관리비 (입금 완료로 표시한 시각 기준 기간) */
    @Query(
        """
        select f from ManagementFeeEntity f
        join fetch f.reservation r
        join fetch f.room room
        join fetch room.businessOwnerEntity
        where f.paidAt between :from and :to
        """
    )
    fun findPaidBetween(@Param("from") from: LocalDateTime, @Param("to") to: LocalDateTime): List<ManagementFeeEntity>
}
