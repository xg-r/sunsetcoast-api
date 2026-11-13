package kr.sunsetcoast.controller.auth

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.sunsetcoast.config.security.CurrentUser
import kr.sunsetcoast.config.security.LoginUser
import kr.sunsetcoast.controller.auth.dto.response.MeResponseBody
import kr.sunsetcoast.domain.user.service.UserService
import kr.sunsetcoast.domain.user.vo.UserRole
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/auth")
class AuthController(
    private val userService: UserService,
    private val securityContextRepository: SecurityContextRepository,
    @Value("\${sunsetcoast.auth.dev-login-enabled:false}") private val devLoginEnabled: Boolean,
) : AuthControllerDocs {
    @GetMapping("/me", version = "1.0")
    override fun me(): MeResponseBody {
        val login = CurrentUser.get() ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        val user = userService.find(login.userId) ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        return MeResponseBody.from(user)
    }

    @PostMapping("/dev-login", version = "1.0")
    override fun devLogin(
        @RequestParam role: String,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): MeResponseBody {
        if (!devLoginEnabled) throw ResponseStatusException(HttpStatus.NOT_FOUND)
        val userRole = runCatching { UserRole.valueOf(role) }.getOrNull() ?: throw IllegalArgumentException("알 수 없는 역할입니다: $role")
        val user = userService.devUser(userRole)
        val login = LoginUser(user.id!!, user.role)
        // 세션 고정 공격 방지: 로그인하면 세션 ID를 바꾼다
        request.getSession(false)?.let { request.changeSessionId() }
        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = OAuth2AuthenticationToken(login, login.authorities, "dev")
        SecurityContextHolder.setContext(context)
        securityContextRepository.saveContext(context, request, response)
        return MeResponseBody.from(user)
    }
}
