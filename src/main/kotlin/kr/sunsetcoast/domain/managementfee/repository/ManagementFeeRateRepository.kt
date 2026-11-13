package kr.sunsetcoast.domain.managementfee.repository

import kr.sunsetcoast.domain.managementfee.entity.ManagementFeeRateEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ManagementFeeRateRepository : JpaRepository<ManagementFeeRateEntity, Long> {
    fun findByRoomIdIn(roomIds: Collection<Long>): List<ManagementFeeRateEntity>
}
