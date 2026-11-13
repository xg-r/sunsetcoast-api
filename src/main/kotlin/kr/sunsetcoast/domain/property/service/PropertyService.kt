package kr.sunsetcoast.domain.property.service

import kr.sunsetcoast.domain.property.entity.PropertyEntity
import kr.sunsetcoast.domain.property.repository.PropertyRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PropertyService(
    private val propertyRepository: PropertyRepository,
) {
    @Transactional(readOnly = true)
    fun findAll(): List<PropertyEntity> = propertyRepository.findAll().sortedBy { it.id }
}
