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

/** 정산 파일 현황에 나와야 하는 정산 계정 (파일을 아직 올리지 않았어도) */
@Entity
@Table(name = "settlement_accounts")
class SettlementAccountEntity(
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val source: OtaPlatform,

    /** 파일명에서 읽는 계정 이름과 같아야 한다 */
    @Column(nullable = false)
    val account: String,

    @Column(name = "property_id")
    val propertyId: Long?,

    /** 이 주(월요일)부터 정산 파일이 있어야 한다 */
    @Column(name = "started_on", nullable = false)
    val startedOn: LocalDate,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)
