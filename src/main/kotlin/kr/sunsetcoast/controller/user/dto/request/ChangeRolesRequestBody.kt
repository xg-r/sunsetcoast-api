package kr.sunsetcoast.controller.user.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "역할 목록 변경")
data class ChangeRolesRequestBody(
    @field:Schema(description = "가질 역할 전체 (SUPER_ADMIN, SITE_MANAGER, USER). 하나 이상", example = "[\"SITE_MANAGER\", \"USER\"]")
    val roles: List<String>,
)
