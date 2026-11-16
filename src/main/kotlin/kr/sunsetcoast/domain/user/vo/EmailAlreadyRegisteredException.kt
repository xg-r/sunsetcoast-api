package kr.sunsetcoast.domain.user.vo

/** 다른 플랫폼 계정이 이미 이 이메일로 가입돼 있다 */
class EmailAlreadyRegisteredException(
    val registeredProvider: AuthProvider,
) : RuntimeException("이미 ${registeredProvider.label}(으)로 가입된 이메일입니다.")
