package kr.sunsetcoast.domain.room.repository

import kr.sunsetcoast.domain.room.vo.RoomEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RoomRepository : JpaRepository<RoomEntity, Long> {
}
