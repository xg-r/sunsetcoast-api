package kr.sunsetcoast.domain.businessowner.repository

import kr.sunsetcoast.domain.businessowner.entity.BusinessOwnerEntity
import org.springframework.data.jpa.repository.JpaRepository

interface BusinessOwnerRepository : JpaRepository<BusinessOwnerEntity, Long>
