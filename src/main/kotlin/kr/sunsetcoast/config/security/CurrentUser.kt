package kr.sunsetcoast.config.security

import kr.sunsetcoast.domain.user.vo.UserRole
import org.springframework.security.core.context.SecurityContextHolder

/** 지금 요청한 사용자 (컨트롤러에서 응답을 역할에 맞게 거를 때) */
object CurrentUser {
    fun get(): LoginUser? = SecurityContextHolder.getContext().authentication?.principal as? LoginUser

    /** 관리비는 SUPER_ADMIN만 본다 */
    fun canSeeManagementFees(): Boolean = get()?.role == UserRole.SUPER_ADMIN
}
