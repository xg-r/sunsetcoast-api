package kr.sunsetcoast.domain.room.repository

import kr.sunsetcoast.domain.room.entity.RoomEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface RoomRepository : JpaRepository<RoomEntity, Long> {
    fun findByPropertyIdOrderById(propertyId: Long): List<RoomEntity>

    /** 객실 명의 사업자의 사업자번호(하이픈 포함 그대로)별 업소 */
    @Query(
        """
        select distinct new kr.sunsetcoast.domain.room.repository.BusinessNumberProperty(o.businessNumber, r.property.id)
        from RoomEntity r join r.businessOwnerEntity o
        where o.businessNumber is not null
        """
    )
    fun findPropertiesByBusinessNumber(): List<BusinessNumberProperty>
}

data class BusinessNumberProperty(val businessNumber: String, val propertyId: Long)
