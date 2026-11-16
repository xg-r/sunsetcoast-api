package kr.sunsetcoast.domain.notification.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "notifications")
class NotificationEntity(
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    val level: Level,

    @Column(nullable = false)
    val title: String,

    @Column(length = 1000)
    val message: String?,

    /** 눌렀을 때 갈 화면 */
    val link: String?,

    @Column(name = "read_at")
    var readAt: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) {
    enum class Level { ERROR, WARN, INFO }

    fun markRead() {
        if (readAt == null) readAt = LocalDateTime.now()
    }
}
