package kr.sunsetcoast.domain.user.service

import kr.sunsetcoast.domain.user.entity.RoleEntity
import kr.sunsetcoast.domain.user.entity.UserEntity
import kr.sunsetcoast.domain.user.repository.RoleRepository
import kr.sunsetcoast.domain.user.repository.UserRepository
import kr.sunsetcoast.domain.user.vo.AuthProvider
import kr.sunsetcoast.domain.user.vo.Gender
import kr.sunsetcoast.domain.user.vo.UserRole
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    /**
     * 로그인하면 SUPER_ADMIN이 되는 계정. 첫 본사 계정을 정할 때 쓴다.
     * "플랫폼:계정 번호" 또는 "플랫폼:이메일" ("KAKAO:12345,NAVER:someone@naver.com").
     * 이메일은 그 플랫폼으로 로그인했을 때 플랫폼이 보내 준 이메일과만 맞춰 본다 (다른 플랫폼에 같은 이메일을 적어 둔 계정은 안 된다)
     */
    @Value("\${sunsetcoast.auth.super-admins:}") superAdmins: String,
) {
    private val superAdminKeys: Set<String> =
        superAdmins.split(',').map { it.trim() }.filter { it.isNotEmpty() }.map { it.uppercaseProvider() }.toSet()

    /**
     * OAuth 로그인 성공. 처음이면 USER로 가입시키고, 회원 정보는 최신으로 맞춘다.
     * 플랫폼이 보내지 않은 값(동의 철회 등)은 기존 값을 지우지 않는다.
     */
    @Transactional
    fun recordLogin(
        provider: AuthProvider,
        providerUserId: String,
        nickname: String?,
        email: String?,
        name: String? = null,
        gender: Gender? = null,
        birthday: String? = null,
        mobile: String? = null,
    ): UserEntity {
        val user = userRepository.findByProviderAndProviderUserId(provider, providerUserId)
            ?: userRepository.save(
                UserEntity(provider = provider, providerUserId = providerUserId, nickname = null, email = null)
                    .apply { roles += rolesOf(setOf(UserRole.USER)) },
            )
        nickname?.let { user.nickname = it.take(100) }
        name?.let { user.name = it.take(100) }
        email?.let { user.email = it.take(255) }
        gender?.let { user.gender = it }
        birthday?.let { user.birthday = it }
        mobile?.let { user.mobile = it }
        user.lastLoginAt = LocalDateTime.now()
        if (isConfiguredSuperAdmin(user) && !user.hasRole(UserRole.SUPER_ADMIN)) {
            user.roles += rolesOf(setOf(UserRole.SUPER_ADMIN))
        }
        return user
    }

    fun find(id: Long): UserEntity? = userRepository.findById(id).orElse(null)

    fun findAll(): List<UserEntity> = userRepository.findAllByOrderByIdAsc()

    /** 역할을 이 목록으로 바꾼다 (SUPER_ADMIN만). 하나 이상이어야 하고, 마지막 SUPER_ADMIN은 내릴 수 없다 */
    @Transactional
    fun changeRoles(targetId: Long, roles: Set<UserRole>): UserEntity {
        require(roles.isNotEmpty()) { "역할을 하나 이상 골라야 합니다." }
        val user = userRepository.findById(targetId).orElseThrow { IllegalArgumentException("사용자가 없습니다: $targetId") }
        if (user.hasRole(UserRole.SUPER_ADMIN) && UserRole.SUPER_ADMIN !in roles) {
            check(userRepository.countByRole(UserRole.SUPER_ADMIN) > 1) { "슈퍼어드민이 한 명뿐이라 역할을 바꿀 수 없습니다." }
        }
        user.roles.clear()
        user.roles += rolesOf(roles)
        return user
    }

    /** 로컬 개발용 로그인. 역할마다 사용자 하나 */
    @Transactional
    fun devUser(role: UserRole): UserEntity {
        val user = userRepository.findByProviderAndProviderUserId(AuthProvider.DEV, role.name)
            ?: userRepository.save(
                UserEntity(provider = AuthProvider.DEV, providerUserId = role.name, nickname = "개발용 ${role.label}", email = null)
                    .apply { roles += rolesOf(setOf(role)) },
            )
        user.lastLoginAt = LocalDateTime.now()
        return user
    }

    private fun rolesOf(codes: Set<UserRole>): List<RoleEntity> =
        roleRepository.findAllByCodeIn(codes).also { found ->
            check(found.size == codes.size) { "roles 테이블에 없는 역할입니다: ${codes - found.map { it.code }.toSet()}" }
        }

    private fun isConfiguredSuperAdmin(user: UserEntity): Boolean {
        if (user.providerKey.uppercaseProvider() in superAdminKeys) return true
        val email = user.email?.trim()?.lowercase() ?: return false
        return "${user.provider.name}:$email" in superAdminKeys
    }

    /** "naver:Abc@Naver.com" → "NAVER:abc@naver.com". 계정 번호는 대소문자를 구분하므로 이메일만 소문자로 맞춘다 */
    private fun String.uppercaseProvider(): String {
        val value = substringAfter(':', "")
        return substringBefore(':').uppercase() + ":" + if ('@' in value) value.lowercase() else value
    }
}
