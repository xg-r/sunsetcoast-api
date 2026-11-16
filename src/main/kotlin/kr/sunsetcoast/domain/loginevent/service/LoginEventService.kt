package kr.sunsetcoast.domain.loginevent.service

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import kr.sunsetcoast.domain.loginevent.entity.LoginEventEntity
import kr.sunsetcoast.domain.loginevent.repository.LoginEventRepository
import kr.sunsetcoast.domain.user.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

/**
 * 로그인 기록. 로그인·로그아웃 때 자동으로 남기고, 조회만 할 수 있다. 1년이 지나면 지운다.
 * 기록을 남기다 실패해도 로그인은 막지 않는다 (서버 로그에만 남김).
 */
@Service
class LoginEventService(
    private val loginEventRepository: LoginEventRepository,
    private val userRepository: UserRepository,
) {
    data class Summary(val todaySuccess: Long, val failuresLast7Days: Long)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun record(
        type: LoginEventEntity.EventType,
        request: HttpServletRequest,
        userId: Long? = null,
        provider: String? = null,
        failureReason: String? = null,
    ) {
        try {
            val user = userId?.let { userRepository.findById(it).orElse(null) }
            loginEventRepository.save(
                LoginEventEntity(
                    eventType = type,
                    userId = userId,
                    provider = (provider ?: user?.provider?.name)?.take(20),
                    nickname = user?.nickname?.take(100),
                    roles = user?.roleCodes?.sortedBy { it.ordinal }?.joinToString(",") { it.name }?.take(100),
                    failureReason = failureReason?.take(255),
                    // 화면(Next)이 넘겨 준 X-Forwarded-For를 스프링이 반영한 실제 접속 주소
                    ipAddress = request.remoteAddr?.take(45),
                    userAgent = request.getHeader("User-Agent")?.take(500),
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "login event could not be recorded type=$type user=$userId" }
        }
    }

    @Transactional(readOnly = true)
    fun search(from: LocalDate, to: LocalDate, userId: Long?, type: LoginEventEntity.EventType?, page: Int, size: Int): Page<LoginEventEntity> {
        require(!from.isAfter(to)) { "시작일이 끝일보다 늦습니다: $from ~ $to" }
        return loginEventRepository.search(
            from.atStartOfDay(), to.plusDays(1).atStartOfDay(), userId, type,
            PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 200)),
        )
    }

    @Transactional(readOnly = true)
    fun summary(): Summary {
        val today = LocalDate.now().atStartOfDay()
        return Summary(
            todaySuccess = loginEventRepository.countByEventTypeAndCreatedAtGreaterThanEqual(LoginEventEntity.EventType.LOGIN_SUCCESS, today),
            failuresLast7Days = loginEventRepository.countByEventTypeAndCreatedAtGreaterThanEqual(LoginEventEntity.EventType.LOGIN_FAILURE, today.minusDays(6)),
        )
    }

    /** 보관 기간 1년: 매일 새벽 3시 30분에 1년이 지난 기록을 지운다 (시각은 설정으로 바꿀 수 있음) */
    @Scheduled(cron = "\${sunsetcoast.login-events.purge-cron:0 30 3 * * *}")
    @Transactional
    fun purgeExpired(): Int {
        // 1년 + 하루: DB 트리거(DB 시간대 기준 1년)와 앱 시간대(한국)가 달라도 경계에서 삭제가 막히지 않도록
        val deleted = loginEventRepository.deleteOlderThan(LocalDateTime.now().minusYears(RETENTION_YEARS).minusDays(1))
        if (deleted > 0) logger.info { "login events older than $RETENTION_YEARS year purged count=$deleted" }
        return deleted
    }

    companion object {
        const val RETENTION_YEARS = 1L
    }
}
