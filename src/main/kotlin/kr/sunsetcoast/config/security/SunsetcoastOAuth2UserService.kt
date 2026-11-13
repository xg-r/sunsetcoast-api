package kr.sunsetcoast.config.security

import kr.sunsetcoast.domain.user.service.UserService
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
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
        val user = userService.recordLogin(
            provider = profile.provider,
            providerUserId = profile.providerUserId,
            nickname = profile.nickname,
            email = profile.email,
            name = profile.name,
            gender = profile.gender,
            birthday = profile.birthday,
            mobile = profile.mobile,
        )
        return LoginUser(user.id!!, user.roleCodes)
    }
}
