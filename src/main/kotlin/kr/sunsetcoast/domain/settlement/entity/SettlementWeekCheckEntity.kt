package kr.sunsetcoast.domain.settlement.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import kr.sunsetcoast.domain.reservation.vo.OtaPlatform
import java.time.LocalDate
import java.time.LocalDateTime

/** 정산 파일이 없는 주를 관리자가 확인했다는 표시 (예: 그 주 판매 없음) */
@Entity
@Table(name = "settlement_week_checks")
class SettlementWeekCheckEntity(
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val source: OtaPlatform,

    @Column(nullable = false)
    val account: String,

    /** 그 주의 월요일 */
    @Column(name = "week_start", nullable = false)
    val weekStart: LocalDate,

    @Column
    var note: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)
