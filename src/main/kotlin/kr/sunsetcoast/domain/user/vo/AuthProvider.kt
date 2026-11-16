package kr.sunsetcoast.domain.user.vo

/** 로그인 수단. registrationId(spring.security.oauth2.client.registration.*)는 소문자 */
enum class AuthProvider(val label: String) {
    NAVER("네이버"),
    KAKAO("카카오"),

    /** 로컬 개발용 로그인 (운영에서는 꺼 둔다) */
    DEV("개발용 로그인"),
    ;

    companion object {
        fun fromRegistrationId(registrationId: String): AuthProvider =
            entries.firstOrNull { it.name.equals(registrationId, ignoreCase = true) && it != DEV }
                ?: throw IllegalArgumentException("지원하지 않는 로그인입니다: $registrationId")
    }
}
