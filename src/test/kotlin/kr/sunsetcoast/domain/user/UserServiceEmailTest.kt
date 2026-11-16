package kr.sunsetcoast.domain.user

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kr.sunsetcoast.domain.user.entity.RoleEntity
import kr.sunsetcoast.domain.user.entity.UserEntity
import kr.sunsetcoast.domain.user.repository.RoleRepository
import kr.sunsetcoast.domain.user.repository.UserRepository
import kr.sunsetcoast.domain.user.service.UserService
import kr.sunsetcoast.domain.user.vo.AuthProvider
import kr.sunsetcoast.domain.user.vo.EmailAlreadyRegisteredException
import kr.sunsetcoast.domain.user.vo.UserRole
import java.lang.reflect.Proxy

/** 이메일 하나에 계정 하나 (uq_users_email) */
class UserServiceEmailTest : FunSpec({
    val roleRepository = Proxy.newProxyInstance(RoleRepository::class.java.classLoader, arrayOf(RoleRepository::class.java)) { _, method, args ->
        when (method.name) {
            "findAllByCodeIn" -> (args[0] as Collection<*>).map { RoleEntity(code = it as UserRole, name = it.label) }
            else -> throw UnsupportedOperationException(method.name)
        }
    } as RoleRepository

    /** 메모리 저장소. 이메일은 DB처럼 대소문자를 구분하지 않고 찾는다 */
    fun service(vararg users: UserEntity): Pair<UserService, MutableList<UserEntity>> {
        val store = users.toMutableList()
        val repo = Proxy.newProxyInstance(UserRepository::class.java.classLoader, arrayOf(UserRepository::class.java)) { _, method, args ->
            when (method.name) {
                "findByProviderAndProviderUserId" -> store.firstOrNull { it.provider == args[0] && it.providerUserId == args[1] }
                "findByEmail" -> store.firstOrNull { it.email.equals(args[0] as String, ignoreCase = true) }
                "save" -> (args[0] as UserEntity).also { store += it }
                else -> throw UnsupportedOperationException(method.name)
            }
        } as UserRepository
        return UserService(repo, roleRepository, "") to store
    }

    fun kakaoUser() = UserEntity(provider = AuthProvider.KAKAO, providerUserId = "111", nickname = "kakao", email = "owner@example.com")

    test("다른 플랫폼에 같은 이메일로 가입돼 있으면 가입하지 않고 그 플랫폼을 알려 준다 (대소문자 무시)") {
        val (service, store) = service(kakaoUser())
        val e = shouldThrow<EmailAlreadyRegisteredException> {
            service.recordLogin(AuthProvider.NAVER, "abc", nickname = null, email = "Owner@Example.com")
        }
        e.registeredProvider shouldBe AuthProvider.KAKAO
        e.message shouldBe "이미 카카오(으)로 가입된 이메일입니다."
        store.size shouldBe 1
    }

    test("이미 가입한 계정은 같은 이메일로 다시 로그인할 수 있다") {
        val (service, store) = service(kakaoUser())
        service.recordLogin(AuthProvider.KAKAO, "111", nickname = "new", email = "owner@example.com").nickname shouldBe "new"
        store.size shouldBe 1
    }

    test("이메일이 없는 계정은 여럿이어도 된다") {
        val (service, store) = service(kakaoUser())
        service.recordLogin(AuthProvider.NAVER, "abc", nickname = null, email = null)
        service.recordLogin(AuthProvider.NAVER, "def", nickname = null, email = null)
        store.size shouldBe 3
    }

    test("가입한 계정의 이메일이 다른 계정과 겹치게 바뀌면 이메일만 그대로 둔다") {
        val naver = UserEntity(provider = AuthProvider.NAVER, providerUserId = "abc", nickname = null, email = "naver@example.com")
        val (service, _) = service(kakaoUser(), naver)
        service.recordLogin(AuthProvider.NAVER, "abc", nickname = "n", email = "owner@example.com").email shouldBe "naver@example.com"
    }
})
