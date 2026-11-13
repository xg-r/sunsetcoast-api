package kr.sunsetcoast.domain.businessowner.service

import kr.sunsetcoast.domain.businessowner.entity.BusinessOwnerEntity
import kr.sunsetcoast.domain.businessowner.repository.BusinessOwnerRepository
import kr.sunsetcoast.domain.room.entity.RoomEntity
import kr.sunsetcoast.domain.room.repository.RoomRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BusinessOwnerService(
    private val businessOwnerRepository: BusinessOwnerRepository,
    private val roomRepository: RoomRepository,
) {
    data class OwnerRooms(val owner: BusinessOwnerEntity, val rooms: List<RoomEntity>)

    /** 사업자와 명의 객실. 한 사업자의 객실이 여러 업소에 걸칠 수 있다 (선셋코스트: 캠핑장 + 리조트) */
    @Transactional(readOnly = true)
    fun findAllWithRooms(): List<OwnerRooms> {
        val rooms = roomRepository.findAll().sortedBy { it.id }.groupBy { it.businessOwnerEntity.id }
        rooms.values.flatten().forEach { it.property.id }
        return businessOwnerRepository.findAll().sortedBy { it.id }.map { OwnerRooms(it, rooms[it.id].orEmpty()) }
    }
}
