package kr.sunsetcoast.config.security

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kr.sunsetcoast.domain.user.vo.AuthProvider

class OAuthUserProfileTest : FunSpec({
    test("네이버: response 안의 id, nickname, email") {
        val attributes = mapOf(
            "resultcode" to "00",
            "response" to mapOf("id" to "abcDEF123", "nickname" to "선셋", "email" to "a@example.com"),
        )
        OAuthUserProfile.of("naver", attributes) shouldBe OAuthUserProfile(AuthProvider.NAVER, "abcDEF123", "선셋", "a@example.com")
    }

    test("카카오: 회원번호(숫자)와 kakao_account.profile.nickname") {
        val attributes = mapOf(
            "id" to 1234567890L,
            "properties" to mapOf("nickname" to "옛 닉네임"),
            "kakao_account" to mapOf("profile" to mapOf("nickname" to "코스트"), "email" to null),
        )
        OAuthUserProfile.of("kakao", attributes) shouldBe OAuthUserProfile(AuthProvider.KAKAO, "1234567890", "코스트", null)
    }

    test("카카오: 프로필 동의가 없으면 properties.nickname, 그것도 없으면 null") {
        OAuthUserProfile.of("kakao", mapOf("id" to 1L, "properties" to mapOf("nickname" to "  "))) shouldBe
            OAuthUserProfile(AuthProvider.KAKAO, "1", null, null)
    }

    test("지원하지 않는 로그인") {
        shouldThrow<IllegalArgumentException> { OAuthUserProfile.of("google", mapOf("sub" to "x")) }
        shouldThrow<IllegalArgumentException> { OAuthUserProfile.of("dev", emptyMap()) }
    }
})
