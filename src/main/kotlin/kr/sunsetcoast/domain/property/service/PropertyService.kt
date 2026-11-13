package kr.sunsetcoast.domain.property.service

import kr.sunsetcoast.domain.property.entity.PropertyEntity
import kr.sunsetcoast.domain.property.repository.PropertyRepository
import kr.sunsetcoast.domain.room.entity.RoomEntity
import kr.sunsetcoast.domain.room.repository.RoomRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PropertyService(
    private val propertyRepository: PropertyRepository,
    private val roomRepository: RoomRepository,
) {
    @Transactional(readOnly = true)
    fun findAll(): List<PropertyEntity> = propertyRepository.findAll().sortedBy { it.id }

    @Transactional(readOnly = true)
    fun findRooms(propertyId: Long): List<RoomEntity> = roomRepository.findByPropertyIdOrderById(propertyId)
}
