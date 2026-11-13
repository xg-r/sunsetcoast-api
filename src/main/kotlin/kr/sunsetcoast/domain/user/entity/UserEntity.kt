package kr.sunsetcoast.domain.user.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import kr.sunsetcoast.domain.user.vo.AuthProvider
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

    @Column
    var email: String?,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var role: UserRole = UserRole.USER,

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
}
