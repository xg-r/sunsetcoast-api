package kr.sunsetcoast.controller.user

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kr.sunsetcoast.controller.user.dto.request.ChangeRoleRequestBody
import kr.sunsetcoast.controller.user.dto.response.UserResponseBody

@Tag(name = "User Admin", description = "사용자 관리 (슈퍼어드민 전용)")
interface UserAdminControllerDocs {
    @Operation(summary = "사용자 목록", description = "로그인한 적 있는 모든 사용자. 처음 로그인하면 일반사용자(USER)다.")
    fun getUsers(): List<UserResponseBody>

    @Operation(
        summary = "역할 변경",
        description = "SUPER_ADMIN, SITE_MANAGER, USER. 마지막 슈퍼어드민은 내릴 수 없다. 바꾼 역할은 그 사용자의 다음 요청부터 적용된다.",
    )
    fun changeRole(
        @Parameter(description = "사용자 ID", example = "3") id: Long,
        body: ChangeRoleRequestBody,
    ): UserResponseBody
}
