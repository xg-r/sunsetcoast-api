package kr.sunsetcoast.domain.user.service

import kr.sunsetcoast.domain.user.entity.UserEntity
import kr.sunsetcoast.domain.user.repository.UserRepository
import kr.sunsetcoast.domain.user.vo.AuthProvider
import kr.sunsetcoast.domain.user.vo.UserRole
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
    /** 로그인하면 SUPER_ADMIN이 되는 계정 ("KAKAO:12345,NAVER:abc"). 첫 본사 계정을 정할 때 쓴다 */
    @Value("\${sunsetcoast.auth.super-admins:}") superAdmins: String,
) {
    private val superAdminKeys: Set<String> =
        superAdmins.split(',').map { it.trim() }.filter { it.isNotEmpty() }.map { it.uppercaseProvider() }.toSet()

    /** OAuth 로그인 성공. 처음이면 USER로 가입시키고, 닉네임·이메일은 최신으로 맞춘다 */
    @Transactional
    fun recordLogin(provider: AuthProvider, providerUserId: String, nickname: String?, email: String?): UserEntity {
        val user = userRepository.findByProviderAndProviderUserId(provider, providerUserId)
            ?: userRepository.save(UserEntity(provider = provider, providerUserId = providerUserId, nickname = nickname, email = email))
        nickname?.let { user.nickname = it.take(100) }
        email?.let { user.email = it }
        user.lastLoginAt = LocalDateTime.now()
        if (user.providerKey.uppercaseProvider() in superAdminKeys && user.role != UserRole.SUPER_ADMIN) {
            user.role = UserRole.SUPER_ADMIN
        }
        return user
    }

    fun find(id: Long): UserEntity? = userRepository.findById(id).orElse(null)

    fun findAll(): List<UserEntity> = userRepository.findAllByOrderByIdAsc()

    /** 역할 변경 (SUPER_ADMIN만). 마지막 SUPER_ADMIN은 내릴 수 없다 */
    @Transactional
    fun changeRole(targetId: Long, role: UserRole): UserEntity {
        val user = userRepository.findById(targetId).orElseThrow { IllegalArgumentException("사용자가 없습니다: $targetId") }
        if (user.role == UserRole.SUPER_ADMIN && role != UserRole.SUPER_ADMIN) {
            check(userRepository.countByRole(UserRole.SUPER_ADMIN) > 1) { "슈퍼어드민이 한 명뿐이라 역할을 바꿀 수 없습니다." }
        }
        user.role = role
        return user
    }

    /** 로컬 개발용 로그인. 역할마다 사용자 하나 */
    @Transactional
    fun devUser(role: UserRole): UserEntity {
        val user = userRepository.findByProviderAndProviderUserId(AuthProvider.DEV, role.name)
            ?: userRepository.save(
                UserEntity(provider = AuthProvider.DEV, providerUserId = role.name, nickname = "개발용 ${role.label}", email = null, role = role),
            )
        user.lastLoginAt = LocalDateTime.now()
        return user
    }

    private fun String.uppercaseProvider(): String = substringBefore(':').uppercase() + ":" + substringAfter(':', "")
}
