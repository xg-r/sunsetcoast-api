package kr.sunsetcoast.controller.notification

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import kr.sunsetcoast.config.security.CurrentUser
import kr.sunsetcoast.domain.notification.service.NotificationService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Schema(description = "내 알림 (최근 30개)")
data class NotificationInboxResponseBody(val unreadCount: Long, val items: List<Item>) {
    data class Item(
        val id: Long,
        @field:Schema(description = "ERROR, WARN, INFO", example = "ERROR") val level: String,
        val title: String,
        val message: String?,
        @field:Schema(description = "눌렀을 때 갈 화면", nullable = true, example = "/settlements/history") val link: String?,
        val read: Boolean,
        val createdAt: String,
    )
}

@Tag(name = "Notification", description = "내 알림 (업로드 실패 등)")
interface NotificationControllerDocs {
    @Operation(summary = "내 알림")
    fun getInbox(): NotificationInboxResponseBody

    @Operation(summary = "알림 읽음")
    fun markRead(@Parameter(description = "알림 ID") id: Long)

    @Operation(summary = "알림 모두 읽음")
    fun markAllRead()
}

@RestController
@RequestMapping("/notifications")
class NotificationController(
    private val notificationService: NotificationService,
) : NotificationControllerDocs {
    @GetMapping(version = "1.0")
    override fun getInbox(): NotificationInboxResponseBody {
        val inbox = notificationService.inbox(userId())
        return NotificationInboxResponseBody(
            unreadCount = inbox.unreadCount,
            items = inbox.items.map {
                NotificationInboxResponseBody.Item(it.id!!, it.level.name, it.title, it.message, it.link, it.readAt != null, it.createdAt.withNano(0).toString())
            },
        )
    }

    @PostMapping("/{id}/read", version = "1.0")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun markRead(@PathVariable id: Long) = notificationService.markRead(userId(), id)

    @PostMapping("/read-all", version = "1.0")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun markAllRead() = notificationService.markAllRead(userId())

    private fun userId(): Long = CurrentUser.get()?.userId ?: throw IllegalArgumentException("로그인이 필요합니다.")
}
