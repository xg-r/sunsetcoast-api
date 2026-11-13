package kr.sunsetcoast.domain.user

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kr.sunsetcoast.domain.user.entity.RoleEntity
import kr.sunsetcoast.domain.user.entity.UserEntity
import kr.sunsetcoast.domain.user.repository.RoleRepository
import kr.sunsetcoast.domain.user.repository.UserRepository
import kr.sunsetcoast.domain.user.service.UserService
import kr.sunsetcoast.domain.user.vo.AuthProvider
import kr.sunsetcoast.domain.user.vo.UserRole
import java.lang.reflect.Proxy

/** 설정한 계정(AUTH_SUPER_ADMINS)이 로그인하면 슈퍼어드민이 되는지 */
class UserServiceSuperAdminTest : FunSpec({
    // 처음 로그인하는 사용자만 다루므로 저장소는 "없음 → 받은 것을 그대로 저장"만 흉내 낸다
    fun userRepository(): UserRepository = Proxy.newProxyInstance(UserRepository::class.java.classLoader, arrayOf(UserRepository::class.java)) { _, method, args ->
        when (method.name) {
            "findByProviderAndProviderUserId" -> null
            "save" -> args[0]
            else -> throw UnsupportedOperationException(method.name)
        }
    } as UserRepository

    val roleRepository = Proxy.newProxyInstance(RoleRepository::class.java.classLoader, arrayOf(RoleRepository::class.java)) { _, method, args ->
        when (method.name) {
            "findAllByCodeIn" -> (args[0] as Collection<*>).map { RoleEntity(code = it as UserRole, name = it.label) }
            else -> throw UnsupportedOperationException(method.name)
        }
    } as RoleRepository

    fun login(superAdmins: String, provider: AuthProvider, id: String, email: String?): UserEntity =
        UserService(userRepository(), roleRepository, superAdmins).recordLogin(provider, id, nickname = null, email = email)

    test("플랫폼:이메일 — 그 플랫폼 이메일이 같으면 슈퍼어드민 (대소문자 무시)") {
        login("NAVER:thk98k@naver.com", AuthProvider.NAVER, "abc", "THK98K@naver.com").hasRole(UserRole.SUPER_ADMIN) shouldBe true
    }

    test("같은 이메일이라도 다른 플랫폼 계정은 슈퍼어드민이 아니다") {
        login("NAVER:thk98k@naver.com", AuthProvider.KAKAO, "123", "thk98k@naver.com").hasRole(UserRole.SUPER_ADMIN) shouldBe false
    }

    test("이메일이 없거나 다르면 일반사용자") {
        login("NAVER:thk98k@naver.com", AuthProvider.NAVER, "abc", null).hasRole(UserRole.SUPER_ADMIN) shouldBe false
        login("NAVER:thk98k@naver.com", AuthProvider.NAVER, "abc", "other@naver.com").hasRole(UserRole.SUPER_ADMIN) shouldBe false
    }

    test("플랫폼:계정 번호도 그대로 된다") {
        login("kakao:123, NAVER:x@naver.com", AuthProvider.KAKAO, "123", null).hasRole(UserRole.SUPER_ADMIN) shouldBe true
    }
})
