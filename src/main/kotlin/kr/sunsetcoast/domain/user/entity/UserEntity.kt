package kr.sunsetcoast.domain.user.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table
import kr.sunsetcoast.domain.user.vo.AuthProvider
import kr.sunsetcoast.domain.user.vo.Gender
import kr.sunsetcoast.domain.user.vo.UserRole
import java.time.LocalDateTime

@Entity
@Table(name = "users")
class UserEntity(
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val provider: AuthProvider,

    @Column(name = "provider_user_id", nullable = false, length = 100)
    val providerUserId: String,

    @Column(length = 100)
    var nickname: String?,

    /** 실명 */
    @Column(length = 100)
    var name: String? = null,

    @Column
    var email: String?,

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    var gender: Gender? = null,

    /** "MM-DD" */
    @Column(length = 5)
    var birthday: String? = null,

    /** "010-1234-5678" */
    @Column(length = 20)
    var mobile: String? = null,

    /** 역할 (user_roles). 요청마다 권한 확인에 쓰고 사용자 수가 적어 바로 읽는다 */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "role_id")],
    )
    val roles: MutableSet<RoleEntity> = mutableSetOf(),

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "last_login_at")
    var lastLoginAt: LocalDateTime? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) {
    /** 설정에 적는 형식 ("KAKAO:12345") */
    val providerKey: String get() = "${provider.name}:$providerUserId"

    val roleCodes: Set<UserRole> get() = roles.map { it.code }.toSortedSet()

    /** 가장 높은 역할 (SUPER_ADMIN > SITE_MANAGER > USER). 역할을 하나만 보여 줄 때 */
    val primaryRole: UserRole get() = roleCodes.minOrNull() ?: UserRole.USER

    fun hasRole(role: UserRole): Boolean = roles.any { it.code == role }
}
