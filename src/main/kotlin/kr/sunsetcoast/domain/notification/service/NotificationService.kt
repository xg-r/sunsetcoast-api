package kr.sunsetcoast.domain.notification.service

import kr.sunsetcoast.domain.notification.entity.NotificationEntity
import kr.sunsetcoast.domain.notification.repository.NotificationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class NotificationService(
    private val notificationRepository: NotificationRepository,
) {
    data class Inbox(val items: List<NotificationEntity>, val unreadCount: Long)

    fun inbox(userId: Long): Inbox =
        Inbox(notificationRepository.findTop30ByUserIdOrderByIdDesc(userId), notificationRepository.countByUserIdAndReadAtIsNull(userId))

    @Transactional
    fun notify(userId: Long, level: NotificationEntity.Level, title: String, message: String?, link: String?): NotificationEntity =
        notificationRepository.save(
            NotificationEntity(userId = userId, level = level, title = title.take(255), message = message?.take(1000), link = link),
        )

    @Transactional
    fun markRead(userId: Long, id: Long) {
        val notification = notificationRepository.findById(id).orElseThrow { IllegalArgumentException("알림이 없습니다: $id") }
        require(notification.userId == userId) { "알림이 없습니다: $id" }
        notification.markRead()
    }

    @Transactional
    fun markAllRead(userId: Long) = notificationRepository.findByUserIdAndReadAtIsNull(userId).forEach { it.markRead() }
}
