package kr.sunsetcoast.config.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.web.filter.OncePerRequestFilter

/**
 * CSRF 토큰은 꺼낼 때 만들어지므로 (지연 로딩), 요청마다 꺼내서 XSRF-TOKEN 쿠키가 항상 내려가게 한다.
 * 화면은 이 쿠키 값을 X-XSRF-TOKEN 헤더로 돌려보낸다.
 */
class CsrfCookieFilter : OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        (request.getAttribute(CsrfToken::class.java.name) as? CsrfToken)?.token
        chain.doFilter(request, response)
    }
}
