package kr.sunsetcoast.controller.user

import kr.sunsetcoast.controller.user.dto.request.ChangeRoleRequestBody
import kr.sunsetcoast.controller.user.dto.response.UserResponseBody
import kr.sunsetcoast.domain.user.service.UserService
import kr.sunsetcoast.domain.user.vo.UserRole
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/users")
class UserAdminController(
    private val userService: UserService,
) : UserAdminControllerDocs {
    @GetMapping(version = "1.0")
    override fun getUsers(): List<UserResponseBody> = userService.findAll().map { UserResponseBody.from(it) }

    @PutMapping("/{id}/role", version = "1.0")
    override fun changeRole(@PathVariable id: Long, @RequestBody body: ChangeRoleRequestBody): UserResponseBody {
        val role = runCatching { UserRole.valueOf(body.role) }.getOrNull() ?: throw IllegalArgumentException("알 수 없는 역할입니다: ${body.role}")
        return UserResponseBody.from(userService.changeRole(id, role))
    }
}
