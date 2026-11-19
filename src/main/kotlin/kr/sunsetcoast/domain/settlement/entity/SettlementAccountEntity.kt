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

    /** 떠나요 사업자번호 (숫자 10자리). AdjustCalendar 원본 파일을 이 계정에 붙이는 데 쓴다 */
    @Column(name = "business_number", length = 10)
    var businessNumber: String? = null,

    /** NOL 계정의 사업자(객실 명의). 판매자 계정이 없는 NOL 원본 파일을 객실 명의로 이 계정에 붙인다 */
    @Column(name = "business_owner_id")
    var businessOwnerId: Long? = null,

    /** 이 주(월요일)부터 정산 파일이 있어야 한다 */
    @Column(name = "started_on", nullable = false)
    val startedOn: LocalDate,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)
