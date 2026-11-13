package kr.sunsetcoast.config.security

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kr.sunsetcoast.domain.user.vo.AuthProvider
import kr.sunsetcoast.domain.user.vo.Gender

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

    test("네이버: 이름, 성별, 생일, 휴대전화") {
        val attributes = mapOf(
            "response" to mapOf(
                "id" to "abc", "nickname" to "선셋", "name" to "홍길동", "email" to "a@example.com",
                "gender" to "F", "birthday" to "03-15", "mobile" to "010-1234-5678",
            ),
        )
        OAuthUserProfile.of("naver", attributes) shouldBe OAuthUserProfile(
            AuthProvider.NAVER, "abc", "선셋", "a@example.com",
            name = "홍길동", gender = Gender.FEMALE, birthday = "03-15", mobile = "010-1234-5678",
        )
    }

    test("네이버: 성별 U(알 수 없음)는 저장하지 않는다") {
        OAuthUserProfile.of("naver", mapOf("response" to mapOf("id" to "abc", "gender" to "U"))).gender shouldBe null
    }

    test("카카오: 생일 MMDD, 전화번호 +82 형식을 맞춘다") {
        val attributes = mapOf(
            "id" to 1L,
            "kakao_account" to mapOf(
                "name" to "홍길동", "gender" to "male", "birthday" to "0315", "phone_number" to "+82 10-1234-5678",
            ),
        )
        val profile = OAuthUserProfile.of("kakao", attributes)
        profile.name shouldBe "홍길동"
        profile.gender shouldBe Gender.MALE
        profile.birthday shouldBe "03-15"
        profile.mobile shouldBe "010-1234-5678"
    }

    test("지원하지 않는 로그인") {
        shouldThrow<IllegalArgumentException> { OAuthUserProfile.of("google", mapOf("sub" to "x")) }
        shouldThrow<IllegalArgumentException> { OAuthUserProfile.of("dev", emptyMap()) }
    }
})
