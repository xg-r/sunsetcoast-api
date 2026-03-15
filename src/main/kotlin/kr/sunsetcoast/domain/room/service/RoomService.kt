package kr.sunsetcoast.domain.room.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kr.sunsetcoast.domain.room.repository.RoomRepository
import kr.sunsetcoast.domain.room.vo.RoomEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
class RoomService(
    private val roomRepository: RoomRepository,
) {

    @Transactional(readOnly = true)
    fun findRooms(size: Int, page: Int): Page<RoomEntity> {
        val pageable = PageRequest.of(page - 1, size)
        val foundRooms = roomRepository.findRooms(pageable)
        logger.info { "room names: ${foundRooms.forEach { room -> room.name }}" }
        logger.info { "business name: ${foundRooms.forEach { it.businessOwnerEntity.name }}" }
        return foundRooms
    }
}
