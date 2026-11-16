package kr.sunsetcoast.domain.loginevent.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import java.time.LocalDateTime

/**
 * 로그인 기록 한 줄. 만든 뒤에는 바꿀 수 없다 (@Immutable, DB 트리거도 UPDATE를 막는다).
 */
@Entity
@Immutable
@Table(name = "login_events")
class LoginEventEntity(
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    val eventType: EventType,

    @Column(name = "user_id")
    val userId: Long?,

    @Column(length = 20)
    val provider: String?,

    @Column(length = 100)
    val nickname: String?,

    /** 그때의 역할 (쉼표로 구분) */
    @Column(length = 100)
    val roles: String?,

    @Column(name = "failure_reason")
    val failureReason: String?,

    @Column(name = "ip_address", length = 45)
    val ipAddress: String?,

    @Column(name = "user_agent", length = 500)
    val userAgent: String?,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) {
    enum class EventType { LOGIN_SUCCESS, LOGIN_FAILURE, LOGOUT }
}
