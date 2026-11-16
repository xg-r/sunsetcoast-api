package kr.sunsetcoast.config.security

import kr.sunsetcoast.domain.user.vo.UserRole
import org.springframework.security.core.context.SecurityContextHolder

/** 지금 요청한 사용자 (컨트롤러에서 응답을 역할에 맞게 거를 때) */
object CurrentUser {
    fun get(): LoginUser? = SecurityContextHolder.getContext().authentication?.principal as? LoginUser

    /** 관리비는 SUPER_ADMIN만 본다 */
    fun canSeeManagementFees(): Boolean = get()?.hasRole(UserRole.SUPER_ADMIN) == true

    /**
     * 매출·정산금액 등 돈은 SUPER_ADMIN만 본다. 현장관리자에게 가는 응답에서는 금액을 빼고(null),
     * 금액이 들어간 안내 문구도 줄인다 ([AmountText.redact]).
     */
    fun canSeeAmounts(): Boolean = get()?.hasRole(UserRole.SUPER_ADMIN) == true
}
