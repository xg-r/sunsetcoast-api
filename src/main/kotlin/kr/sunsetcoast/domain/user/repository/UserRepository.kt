package kr.sunsetcoast.domain.user.repository

import kr.sunsetcoast.domain.user.entity.UserEntity
import kr.sunsetcoast.domain.user.vo.AuthProvider
import kr.sunsetcoast.domain.user.vo.UserRole
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface UserRepository : JpaRepository<UserEntity, Long> {
    fun findByProviderAndProviderUserId(provider: AuthProvider, providerUserId: String): UserEntity?

    /** 이메일로 찾는다. DB 정렬 규칙상 대소문자를 구분하지 않는다 (uq_users_email) */
    fun findByEmail(email: String): UserEntity?

    /** 이 역할을 가진 사용자 수 */
    @Query("select count(distinct u) from UserEntity u join u.roles r where r.code = :role")
    fun countByRole(role: UserRole): Long

    fun findAllByOrderByIdAsc(): List<UserEntity>
}
