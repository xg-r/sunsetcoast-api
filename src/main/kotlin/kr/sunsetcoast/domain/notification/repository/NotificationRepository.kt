package kr.sunsetcoast.domain.notification.repository

import kr.sunsetcoast.domain.notification.entity.NotificationEntity
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationRepository : JpaRepository<NotificationEntity, Long> {
    fun findTop30ByUserIdOrderByIdDesc(userId: Long): List<NotificationEntity>

    fun countByUserIdAndReadAtIsNull(userId: Long): Long

    fun findByUserIdAndReadAtIsNull(userId: Long): List<NotificationEntity>
}
