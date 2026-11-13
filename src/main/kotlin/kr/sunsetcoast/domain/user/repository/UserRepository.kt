package kr.sunsetcoast.domain.user.repository

import kr.sunsetcoast.domain.user.entity.UserEntity
import kr.sunsetcoast.domain.user.vo.AuthProvider
import kr.sunsetcoast.domain.user.vo.UserRole
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<UserEntity, Long> {
    fun findByProviderAndProviderUserId(provider: AuthProvider, providerUserId: String): UserEntity?

    fun countByRole(role: UserRole): Long

    fun findAllByOrderByIdAsc(): List<UserEntity>
}
