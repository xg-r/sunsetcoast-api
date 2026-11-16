package kr.sunsetcoast.controller.loginevent

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import kr.sunsetcoast.domain.loginevent.entity.LoginEventEntity
import kr.sunsetcoast.domain.loginevent.service.LoginEventService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@Schema(description = "로그인 기록 (최신순)")
data class LoginEventPageResponseBody(
    val items: List<Item>,
    val page: Int,
    val size: Int,
    val totalCount: Long,
    val totalPages: Int,
    @field:Schema(description = "오늘 로그인 성공 수") val todaySuccess: Long,
    @field:Schema(description = "최근 7일 로그인 실패 수") val failuresLast7Days: Long,
    @field:Schema(description = "보관 기간 (년). 지난 기록은 매일 새벽 자동 삭제") val retentionYears: Long,
) {
    data class Item(
        val id: Long,
        @field:Schema(example = "2026-11-16T15:30:12") val at: String,
        @field:Schema(description = "LOGIN_SUCCESS, LOGIN_FAILURE, LOGOUT", example = "LOGIN_SUCCESS") val type: String,
        @field:Schema(nullable = true) val userId: Long?,
        @field:Schema(description = "NAVER, KAKAO, DEV", nullable = true) val provider: String?,
        @field:Schema(description = "그때의 이름", nullable = true) val nickname: String?,
        @field:Schema(description = "그때의 역할", example = "[\"SUPER_ADMIN\"]") val roles: List<String>,
        @field:Schema(nullable = true) val failureReason: String?,
        @field:Schema(nullable = true) val ipAddress: String?,
        @field:Schema(nullable = true) val userAgent: String?,
    ) {
        companion object {
            fun from(e: LoginEventEntity) = Item(
                id = e.id!!,
                at = e.createdAt.withNano(0).toString(),
                type = e.eventType.name,
                userId = e.userId,
                provider = e.provider,
                nickname = e.nickname,
                roles = e.roles?.split(',')?.filter { it.isNotBlank() }.orEmpty(),
                failureReason = e.failureReason,
                ipAddress = e.ipAddress,
                userAgent = e.userAgent,
            )
        }
    }
}

@Tag(name = "Login Event", description = "로그인 기록 (슈퍼어드민, 조회만)")
interface LoginEventControllerDocs {
    @Operation(
        summary = "로그인 기록",
        description = "로그인 성공·실패·로그아웃 기록 (최신순). 기록은 서버가 자동으로 남기며 등록·수정·삭제 API는 없다 " +
            "(DB 트리거도 수정과 1년이 안 된 기록의 삭제를 막는다). 보관 기간 1년.",
    )
    fun getLoginEvents(
        @Parameter(description = "시작일", example = "2026-11-01") from: LocalDate,
        @Parameter(description = "끝일 (포함)", example = "2026-11-16") to: LocalDate,
        @Parameter(description = "사용자 ID (생략하면 전체)") userId: Long?,
        @Parameter(description = "LOGIN_SUCCESS, LOGIN_FAILURE, LOGOUT (생략하면 전체)") type: String?,
        @Parameter(description = "페이지 (0부터)") page: Int,
        @Parameter(description = "페이지 크기 (최대 200)") size: Int,
    ): LoginEventPageResponseBody
}

@RestController
@RequestMapping("/admin/login-events")
class LoginEventController(
    private val loginEventService: LoginEventService,
) : LoginEventControllerDocs {
    @GetMapping(version = "1.0")
    override fun getLoginEvents(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
        @RequestParam(required = false) userId: Long?,
        @RequestParam(required = false) type: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
    ): LoginEventPageResponseBody {
        val eventType = type?.takeIf { it.isNotBlank() }?.let {
            runCatching { LoginEventEntity.EventType.valueOf(it) }.getOrNull() ?: throw IllegalArgumentException("알 수 없는 기록 종류입니다: $it")
        }
        val result = loginEventService.search(from, to, userId, eventType, page, size)
        val summary = loginEventService.summary()
        return LoginEventPageResponseBody(
            items = result.content.map { LoginEventPageResponseBody.Item.from(it) },
            page = result.number,
            size = result.size,
            totalCount = result.totalElements,
            totalPages = result.totalPages,
            todaySuccess = summary.todaySuccess,
            failuresLast7Days = summary.failuresLast7Days,
            retentionYears = LoginEventService.RETENTION_YEARS,
        )
    }
}
