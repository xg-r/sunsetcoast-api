package kr.sunsetcoast.config.security

import kr.sunsetcoast.domain.user.vo.AuthProvider
import kr.sunsetcoast.domain.user.vo.Gender

/**
 * 플랫폼마다 다른 사용자 정보 응답을 하나로 맞춘다.
 *
 * - 네이버: `{ "resultcode": "00", "response": { "id", "nickname", "name", "email", "gender": "F|M|U", "birthday": "MM-DD", "mobile": "010-1234-5678" } }`
 * - 카카오: `{ "id": 123, "properties": { "nickname" }, "kakao_account": { "profile": { "nickname" }, "name", "email", "gender": "female|male", "birthday": "MMDD", "phone_number": "+82 10-1234-5678" } }`
 *
 * 닉네임 외의 정보는 사용자가 동의한 경우에만 온다.
 */
data class OAuthUserProfile(
    val provider: AuthProvider,
    val providerUserId: String,
    val nickname: String?,
    val email: String?,
    /** 실명 */
    val name: String? = null,
    val gender: Gender? = null,
    /** "MM-DD" */
    val birthday: String? = null,
    /** "010-1234-5678" */
    val mobile: String? = null,
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
                        name = response["name"].text(),
                        gender = gender(response["gender"].text()),
                        birthday = birthday(response["birthday"].text()),
                        mobile = mobile(response["mobile"].text()),
                    )
                }

                AuthProvider.KAKAO -> {
                    val account = attributes["kakao_account"].asMap()
                    OAuthUserProfile(
                        provider = provider,
                        providerUserId = attributes["id"]?.toString() ?: error("카카오 회원번호가 없습니다."),
                        nickname = account["profile"].asMap()["nickname"].text() ?: attributes["properties"].asMap()["nickname"].text(),
                        email = account["email"].text(),
                        name = account["name"].text(),
                        gender = gender(account["gender"].text()),
                        birthday = birthday(account["birthday"].text()),
                        mobile = mobile(account["phone_number"].text()),
                    )
                }

                AuthProvider.DEV -> error("개발용 로그인은 OAuth로 들어올 수 없습니다.")
            }

        /** 네이버 "M"/"F"/"U", 카카오 "male"/"female". 그 외(알 수 없음)는 null */
        private fun gender(value: String?): Gender? = when (value?.lowercase()) {
            "m", "male" -> Gender.MALE
            "f", "female" -> Gender.FEMALE
            else -> null
        }

        /** 네이버 "MM-DD", 카카오 "MMDD" → "MM-DD" */
        private fun birthday(value: String?): String? {
            val digits = value?.filter { it.isDigit() } ?: return null
            return if (digits.length == 4) "${digits.take(2)}-${digits.drop(2)}" else null
        }

        /** 네이버 "010-1234-5678", 카카오 "+82 10-1234-5678" → "010-1234-5678" */
        private fun mobile(value: String?): String? {
            value ?: return null
            val digits = value.filter { it.isDigit() }.let { if (value.trim().startsWith("+82")) "0" + it.removePrefix("82") else it }
            return when (digits.length) {
                11 -> "${digits.take(3)}-${digits.substring(3, 7)}-${digits.drop(7)}"
                10 -> "${digits.take(3)}-${digits.substring(3, 6)}-${digits.drop(6)}"
                else -> value.trim().take(20)
            }
        }

        @Suppress("UNCHECKED_CAST")
        private fun Any?.asMap(): Map<String, Any?> = this as? Map<String, Any?> ?: emptyMap()

        private fun Any?.text(): String? = (this as? String)?.trim()?.takeIf { it.isNotEmpty() }
    }
}
