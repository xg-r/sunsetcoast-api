package kr.sunsetcoast.config.security

import kr.sunsetcoast.domain.user.vo.UserRole
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.user.OAuth2User

/**
 * 세션에 저장하는 로그인 사용자. 역할은 요청마다 DB에서 다시 읽는다 ([CurrentRoleFilter]).
 * 플랫폼이 준 원본 정보(attributes)는 세션에 남기지 않는다.
 */
data class LoginUser(
    val userId: Long,
    val roles: Set<UserRole>,
) : OAuth2User, java.io.Serializable {
    fun hasRole(role: UserRole): Boolean = role in roles

    override fun getName(): String = userId.toString()

    override fun getAttributes(): Map<String, Any> = emptyMap()

    override fun getAuthorities(): Collection<GrantedAuthority> = roles.map { SimpleGrantedAuthority(it.authority) }
}
