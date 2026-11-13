package kr.sunsetcoast.controller.user.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "역할 변경")
data class ChangeRoleRequestBody(
    @field:Schema(description = "SUPER_ADMIN, SITE_MANAGER, USER", example = "SITE_MANAGER")
    val role: String,
)
