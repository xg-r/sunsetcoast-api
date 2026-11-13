package kr.sunsetcoast.controller.auth.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.sunsetcoast.domain.user.entity.UserEntity

@Schema(description = "로그인한 사용자")
data class MeResponseBody(
    @field:Schema(description = "사용자 ID", example = "1")
    val id: Long,
    @field:Schema(description = "로그인 수단 (NAVER, KAKAO, DEV)", example = "KAKAO")
    val provider: String,
    @field:Schema(description = "역할 확인용 계정 키. 첫 슈퍼어드민 설정(AUTH_SUPER_ADMINS)에 적는 값", example = "KAKAO:1234567890")
    val providerKey: String,
    @field:Schema(description = "닉네임", nullable = true, example = "홍길동")
    val nickname: String?,
    @field:Schema(description = "이메일 (동의한 경우)", nullable = true)
    val email: String?,
    @field:Schema(description = "역할 (SUPER_ADMIN, SITE_MANAGER, USER)", example = "SITE_MANAGER")
    val role: String,
) {
    companion object {
        fun from(user: UserEntity) = MeResponseBody(
            id = user.id!!,
            provider = user.provider.name,
            providerKey = user.providerKey,
            nickname = user.nickname,
            email = user.email,
            role = user.role.name,
        )
    }
}
