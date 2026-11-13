package kr.sunsetcoast.domain.property.repository

import kr.sunsetcoast.domain.property.entity.PropertyEntity
import org.springframework.data.jpa.repository.JpaRepository

interface PropertyRepository : JpaRepository<PropertyEntity, Long>
