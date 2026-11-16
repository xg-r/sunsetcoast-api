package kr.sunsetcoast.domain.loginevent.repository

import kr.sunsetcoast.domain.loginevent.entity.LoginEventEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

/**
 * 로그인 기록 저장소. JpaRepository가 아니라 필요한 것만 연다: 추가, 조회, 보관 기간이 지난 기록 삭제.
 * (수정·개별 삭제 메서드가 없다)
 */
interface LoginEventRepository : Repository<LoginEventEntity, Long> {
    fun save(event: LoginEventEntity): LoginEventEntity

    @Query(
        """
        select e from LoginEventEntity e
        where e.createdAt >= :from and e.createdAt < :to
          and (:userId is null or e.userId = :userId)
          and (:eventType is null or e.eventType = :eventType)
        order by e.createdAt desc, e.id desc
        """
    )
    fun search(
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime,
        @Param("userId") userId: Long?,
        @Param("eventType") eventType: LoginEventEntity.EventType?,
        pageable: Pageable,
    ): Page<LoginEventEntity>

    fun countByEventTypeAndCreatedAtGreaterThanEqual(eventType: LoginEventEntity.EventType, from: LocalDateTime): Long

    /**
     * 보관 기간이 지난 기록 삭제 (DB 트리거도 기간이 지나지 않은 기록의 삭제를 막는다).
     * 엔티티가 @Immutable이라 JPQL 일괄 삭제 대신 SQL로 지운다.
     */
    @Modifying
    @Query(value = "DELETE FROM login_events WHERE created_at < :cutoff", nativeQuery = true)
    fun deleteOlderThan(@Param("cutoff") cutoff: LocalDateTime): Int
}
