package kr.sunsetcoast.config.security

import kr.sunsetcoast.domain.user.service.UserService
import kr.sunsetcoast.domain.user.vo.EmailAlreadyRegisteredException
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Component

/** 네이버·카카오 사용자 정보를 받아 users에 가입·갱신하고, 세션에는 [LoginUser]만 남긴다 */
@Component
class SunsetcoastOAuth2UserService(
    private val userService: UserService,
) : DefaultOAuth2UserService() {
    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val attributes = super.loadUser(userRequest).attributes
        val profile = OAuthUserProfile.of(userRequest.clientRegistration.registrationId, attributes)
        val user = try {
            userService.recordLogin(
            provider = profile.provider,
            providerUserId = profile.providerUserId,
            nickname = profile.nickname,
            email = profile.email,
            name = profile.name,
            gender = profile.gender,
            birthday = profile.birthday,
            mobile = profile.mobile,
            )
        } catch (e: EmailAlreadyRegisteredException) {
            // 실패 처리기가 로그인 화면 주소에 가입된 플랫폼을 붙인다
            throw OAuth2AuthenticationException(
                OAuth2Error(EMAIL_ALREADY_REGISTERED, e.registeredProvider.name, null),
                e.message,
                e,
            )
        }
        return LoginUser(user.id!!, user.roleCodes)
    }
}

/** OAuth2Error 코드: 같은 이메일로 다른 플랫폼에 이미 가입. description에 그 플랫폼(NAVER/KAKAO) */
const val EMAIL_ALREADY_REGISTERED = "email_already_registered"
