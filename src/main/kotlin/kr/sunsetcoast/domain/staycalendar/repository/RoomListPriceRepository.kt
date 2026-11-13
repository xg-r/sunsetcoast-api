package kr.sunsetcoast.domain.staycalendar.repository

import kr.sunsetcoast.domain.staycalendar.entity.RoomListPriceEntity
import org.springframework.data.jpa.repository.JpaRepository

interface RoomListPriceRepository : JpaRepository<RoomListPriceEntity, Long>
