package kr.sunsetcoast.domain.user.repository

import kr.sunsetcoast.domain.user.entity.RoleEntity
import kr.sunsetcoast.domain.user.vo.UserRole
import org.springframework.data.jpa.repository.JpaRepository

interface RoleRepository : JpaRepository<RoleEntity, Long> {
    fun findAllByCodeIn(codes: Collection<UserRole>): List<RoleEntity>
}
