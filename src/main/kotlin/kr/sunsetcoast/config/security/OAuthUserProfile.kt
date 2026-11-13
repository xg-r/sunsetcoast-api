package kr.sunsetcoast.config.security

import kr.sunsetcoast.domain.user.vo.AuthProvider

/**
 * 플랫폼마다 다른 사용자 정보 응답을 하나로 맞춘다.
 *
 * - 네이버: `{ "resultcode": "00", "response": { "id", "nickname", "email" } }`
 * - 카카오: `{ "id": 123, "properties": { "nickname" }, "kakao_account": { "profile": { "nickname" }, "email" } }`
 *
 * 닉네임·이메일은 사용자가 동의한 경우에만 온다.
 */
data class OAuthUserProfile(
    val provider: AuthProvider,
    val providerUserId: String,
    val nickname: String?,
    val email: String?,
) {
    companion object {
        fun of(registrationId: String, attributes: Map<String, Any?>): OAuthUserProfile =
            when (val provider = AuthProvider.fromRegistrationId(registrationId)) {
                AuthProvider.NAVER -> {
                    val response = attributes["response"].asMap()
                    OAuthUserProfile(
                        provider = provider,
                        providerUserId = response["id"]?.toString() ?: error("네이버 사용자 ID가 없습니다."),
                        nickname = response["nickname"].text() ?: response["name"].text(),
                        email = response["email"].text(),
                    )
                }

                AuthProvider.KAKAO -> {
                    val account = attributes["kakao_account"].asMap()
                    OAuthUserProfile(
                        provider = provider,
                        providerUserId = attributes["id"]?.toString() ?: error("카카오 회원번호가 없습니다."),
                        nickname = account["profile"].asMap()["nickname"].text() ?: attributes["properties"].asMap()["nickname"].text(),
                        email = account["email"].text(),
                    )
                }

                AuthProvider.DEV -> error("개발용 로그인은 OAuth로 들어올 수 없습니다.")
            }

        @Suppress("UNCHECKED_CAST")
        private fun Any?.asMap(): Map<String, Any?> = this as? Map<String, Any?> ?: emptyMap()

        private fun Any?.text(): String? = (this as? String)?.trim()?.takeIf { it.isNotEmpty() }
    }
}
