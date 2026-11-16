package kr.sunsetcoast.config.security

import kr.sunsetcoast.common.response.ErrorResponse
import kr.sunsetcoast.domain.loginevent.entity.LoginEventEntity
import kr.sunsetcoast.domain.loginevent.service.LoginEventService
import kr.sunsetcoast.domain.user.service.UserService
import kr.sunsetcoast.domain.user.vo.AuthProvider
import kr.sunsetcoast.domain.user.vo.UserRole
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.intercept.AuthorizationFilter
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.security.web.csrf.CsrfFilter
import tools.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletResponse

/**
 * 인증: 네이버·카카오 OAuth 로그인 → 세션 쿠키 (JSESSIONID).
 * 관리자 화면(Next.js)이 /api 아래 요청을 같은 출처로 넘겨 주므로 쿠키·CSRF 모두 같은 출처 기준이다.
 *
 * 인가:
 * - 관리비 (/management-fees 아래), 사용자 관리 (/admin 아래), 전화 예약 입력·수정·취소 (/reservations/direct 아래): SUPER_ADMIN
 *   (예약 목록 GET /reservations 는 객실 현황 등 현장관리자 화면도 쓰므로 열어 둔다)
 * - 그 밖의 관리자 API: SUPER_ADMIN, SITE_MANAGER
 * - 내 정보 (/auth 아래): 로그인한 누구나 (USER 포함)
 * - API 문서 (Swagger): springdoc 이 켜진 환경(local)에서만 누구나. 운영은 springdoc 을 끄므로 문서 경로가 없고 열어 주지도 않는다
 * - 일반 사용자 기능을 만들면 /me 아래처럼 따로 경로를 두고 authenticated()로 연다
 */
@Configuration
class SecurityConfiguration(
    private val objectMapper: ObjectMapper,
    @Value("\${sunsetcoast.auth.login-success-url:/}") private val loginSuccessUrl: String,
    @Value("\${sunsetcoast.auth.login-failure-url:/login?error}") private val loginFailureUrl: String,
    @Value("\${springdoc.api-docs.enabled:false}") private val apiDocsEnabled: Boolean,
) {
    @Bean
    fun securityContextRepository(): SecurityContextRepository = HttpSessionSecurityContextRepository()

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        oAuth2UserService: SunsetcoastOAuth2UserService,
        userService: UserService,
        securityContextRepository: SecurityContextRepository,
        loginEventService: LoginEventService,
    ): SecurityFilterChain {
        val superAdmin = UserRole.SUPER_ADMIN.name
        val siteManager = UserRole.SITE_MANAGER.name
        http
            .authorizeHttpRequests {
                it.requestMatchers("/oauth2/**", "/login/**", "/error").permitAll()
                    .requestMatchers("/auth/dev-login").permitAll()
                    .apply { if (apiDocsEnabled) requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll() }
                    .requestMatchers("/auth/**").authenticated()
                    .requestMatchers("/management-fees/**", "/admin/**", "/reservations/direct/**", "/bank/**", "/analytics/**").hasRole(superAdmin)
                    // 잘못 올린 정산 파일 삭제: 예약·관리비까지 지울 수 있어 본사만
                    .requestMatchers(HttpMethod.DELETE, "/settlements/import-batches/**").hasRole(superAdmin)
                    .anyRequest().hasAnyRole(superAdmin, siteManager)
            }
            .securityContext { it.securityContextRepository(securityContextRepository) }
            // API라 401 뒤에 원래 요청으로 돌려보낼 일이 없다 (로그인 성공은 항상 login-success-url). 익명 요청에 세션을 만들지 않는다
            .requestCache { it.disable() }
            .oauth2Login {
                it.userInfoEndpoint { endpoint -> endpoint.userService(oAuth2UserService) }
                // 로그인 성공: 기록을 남기고 화면으로
                it.successHandler { request, response, authentication ->
                    val login = authentication.principal as? LoginUser
                    loginEventService.record(LoginEventEntity.EventType.LOGIN_SUCCESS, request, userId = login?.userId)
                    response.sendRedirect(request.contextPath + loginSuccessUrl)
                }
                // 같은 이메일로 다른 플랫폼에 가입돼 있으면 ?error=already_registered&provider=KAKAO 로 돌려보낸다
                it.failureHandler { request, response, exception ->
                    val error = (exception as? OAuth2AuthenticationException)?.error
                    // 로그인 실패도 기록한다 (어느 플랫폼으로 시도했는지는 콜백 주소 /login/oauth2/code/{naver|kakao})
                    loginEventService.record(
                        LoginEventEntity.EventType.LOGIN_FAILURE,
                        request,
                        provider = request.requestURI.substringAfterLast('/').uppercase().takeIf { it == "NAVER" || it == "KAKAO" },
                        failureReason = loginFailureReason(error?.errorCode, error?.description, exception.message),
                    )
                    val target = if (error?.errorCode == EMAIL_ALREADY_REGISTERED) {
                        val base = loginFailureUrl.substringBefore('?')
                        "$base?error=already_registered&provider=${error.description}"
                    } else {
                        loginFailureUrl
                    }
                    response.sendRedirect(request.contextPath + target)
                }
            }
            .logout {
                it.logoutUrl("/auth/logout")
                it.addLogoutHandler { request, _, authentication ->
                    val login = authentication?.principal as? LoginUser ?: return@addLogoutHandler
                    loginEventService.record(LoginEventEntity.EventType.LOGOUT, request, userId = login.userId)
                }
                it.logoutSuccessHandler(HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
            }
            .csrf {
                it.spa()
                it.ignoringRequestMatchers("/auth/dev-login")
            }
            .exceptionHandling {
                it.authenticationEntryPoint { _, response, _ ->
                    writeError(response, HttpStatus.UNAUTHORIZED, "Unauthorized", "로그인이 필요합니다.")
                }
                it.accessDeniedHandler { _, response, _ ->
                    writeError(response, HttpStatus.FORBIDDEN, "AccessDenied", "권한이 없습니다.")
                }
            }
            .addFilterBefore(CurrentRoleFilter(userService), AuthorizationFilter::class.java)
            .addFilterAfter(CsrfCookieFilter(), CsrfFilter::class.java)
        return http.build()
    }

    /** 로그인 실패 이유를 관리자가 읽을 수 있는 말로 (OAuth 오류 코드 → 한국어) */
    private fun loginFailureReason(code: String?, description: String?, message: String?): String = when (code) {
        EMAIL_ALREADY_REGISTERED ->
            "이미 ${description?.let { d -> runCatching { AuthProvider.valueOf(d).label }.getOrDefault(d) } ?: "다른 플랫폼"}(으)로 가입된 이메일"
        "access_denied" -> "네이버·카카오 로그인 화면에서 취소함 (동의 안 함)"
        "authorization_request_not_found", "invalid_state_parameter" -> "잘못된 로그인 요청 (시간이 지났거나 로그인 주소로 바로 접근)"
        "invalid_token_response", "invalid_user_info_response" -> "네이버·카카오에서 사용자 정보를 받지 못함"
        null -> message ?: "알 수 없는 이유"
        else -> "로그인 실패 ($code)"
    }

    private fun writeError(response: HttpServletResponse, status: HttpStatus, exception: String, message: String) {
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        objectMapper.writeValue(response.outputStream, ErrorResponse(exception, message))
    }
}
