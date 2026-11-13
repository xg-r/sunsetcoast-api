package kr.sunsetcoast.domain.user.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import kr.sunsetcoast.domain.user.vo.UserRole

/** 역할 (roles). 행은 마이그레이션으로만 만든다 */
@Entity
@Table(name = "roles")
class RoleEntity(
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, unique = true)
    val code: UserRole,

    @Column(nullable = false, length = 50)
    val name: String,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)
