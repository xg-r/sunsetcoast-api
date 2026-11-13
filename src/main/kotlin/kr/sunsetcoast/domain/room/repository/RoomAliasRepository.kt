package kr.sunsetcoast.domain.room.repository

import kr.sunsetcoast.domain.reservation.vo.OtaPlatform
import kr.sunsetcoast.domain.room.entity.RoomAliasEntity
import org.springframework.data.jpa.repository.JpaRepository

interface RoomAliasRepository : JpaRepository<RoomAliasEntity, Long> {
    fun findBySourceAndExternalName(source: OtaPlatform, externalName: String): RoomAliasEntity?

    fun findBySource(source: OtaPlatform): List<RoomAliasEntity>
}
