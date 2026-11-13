package kr.sunsetcoast.config.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.sunsetcoast.domain.user.service.UserService
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.web.filter.OncePerRequestFilter

/**
 * 세션의 로그인 사용자 역할을 요청마다 DB 값으로 바꾼다.
 * 역할을 바꾸거나 사용자를 지우면 다시 로그인하지 않아도 바로 적용된다. (세션에는 다시 저장하지 않는다)
 */
class CurrentRoleFilter(
    private val userService: UserService,
) : OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val context = SecurityContextHolder.getContext()
        val authentication = context.authentication
        val principal = authentication?.principal as? LoginUser
        if (principal != null) {
            val user = userService.find(principal.userId)
            if (user == null) {
                SecurityContextHolder.clearContext()
                request.getSession(false)?.invalidate()
            } else if (user.role != principal.role) {
                val current = LoginUser(principal.userId, user.role)
                val registrationId = (authentication as? OAuth2AuthenticationToken)?.authorizedClientRegistrationId ?: "dev"
                val replaced = SecurityContextHolder.createEmptyContext()
                replaced.authentication = OAuth2AuthenticationToken(current, current.authorities, registrationId)
                SecurityContextHolder.setContext(replaced)
            }
        }
        chain.doFilter(request, response)
    }
}
