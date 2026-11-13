package kr.sunsetcoast.controller.auth

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.sunsetcoast.controller.auth.dto.response.MeResponseBody

@Tag(
    name = "Auth",
    description = "로그인은 브라우저를 /oauth2/authorization/naver 또는 /oauth2/authorization/kakao 로 보내면 시작된다. " +
        "성공하면 세션 쿠키가 생기고 관리자 화면으로 돌아온다. 로그아웃은 POST /auth/logout (CSRF 헤더 필요).",
)
interface AuthControllerDocs {
    @Operation(summary = "내 정보", description = "로그인하지 않았으면 401.")
    fun me(): MeResponseBody

    @Operation(
        summary = "개발용 로그인",
        description = "로컬에서만 (sunsetcoast.auth.dev-login-enabled=true). 역할마다 개발용 사용자로 로그인한다. 꺼져 있으면 404.",
    )
    fun devLogin(
        @Parameter(description = "역할", example = "SITE_MANAGER") role: String,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): MeResponseBody
}
