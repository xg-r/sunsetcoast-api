package kr.sunsetcoast.domain.user.vo

/** 사용자 역할. Spring Security 권한은 "ROLE_" + name */
enum class UserRole(val label: String) {
    /** 본사 직원. 모든 관리자 기능 + 관리비 + 사용자 관리 */
    SUPER_ADMIN("슈퍼어드민"),

    /** 현장 관리자. 관리비·사용자 관리를 뺀 관리자 기능 */
    SITE_MANAGER("현장관리자"),

    /** 일반 사용자. 관리자 기능 없음 (추후 고객용 기능) */
    USER("일반사용자"),
    ;

    val authority: String get() = "ROLE_$name"

    val isAdmin: Boolean get() = this == SUPER_ADMIN || this == SITE_MANAGER
}
