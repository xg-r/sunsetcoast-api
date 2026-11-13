package kr.sunsetcoast.controller.user.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.sunsetcoast.domain.user.entity.UserEntity

@Schema(description = "사용자 (사용자 관리)")
data class UserResponseBody(
    val id: Long,
    @field:Schema(description = "로그인 수단", example = "KAKAO")
    val provider: String,
    @field:Schema(description = "계정 키", example = "KAKAO:1234567890")
    val providerKey: String,
    @field:Schema(description = "닉네임", nullable = true)
    val nickname: String?,
    @field:Schema(description = "이메일", nullable = true)
    val email: String?,
    @field:Schema(description = "역할", example = "USER")
    val role: String,
    @field:Schema(description = "가입 시각", example = "2026-11-13T15:02:11")
    val createdAt: String,
    @field:Schema(description = "마지막 로그인", nullable = true)
    val lastLoginAt: String?,
) {
    companion object {
        fun from(user: UserEntity) = UserResponseBody(
            id = user.id!!,
            provider = user.provider.name,
            providerKey = user.providerKey,
            nickname = user.nickname,
            email = user.email,
            role = user.role.name,
            createdAt = user.createdAt.withNano(0).toString(),
            lastLoginAt = user.lastLoginAt?.withNano(0)?.toString(),
        )
    }
}
