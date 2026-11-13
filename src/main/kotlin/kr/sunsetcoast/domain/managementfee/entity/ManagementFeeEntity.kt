package kr.sunsetcoast.domain.managementfee.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import kr.sunsetcoast.domain.managementfee.vo.FeeDecision
import kr.sunsetcoast.domain.managementfee.vo.ManagementFeeBasis
import kr.sunsetcoast.domain.reservation.entity.ReservationEntity
import kr.sunsetcoast.domain.room.entity.RoomEntity
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * (예약, 객실)별 현장 관리비. 자동 계산값(calculatedAmount)과 관리자 수정값(overrideAmount)을 따로 둔다.
 * 재계산은 calculatedAmount만 바꾸므로 관리자가 고친 금액은 유지된다.
 */
@Entity
@Table(
    name = "management_fees",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_management_fees_reservation_room", columnNames = ["reservation_id", "room_id"]),
    ]
)
class ManagementFeeEntity(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    val reservation: ReservationEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    val room: RoomEntity,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var basis: ManagementFeeBasis,

    @Column(name = "calculated_amount", nullable = false, precision = 15, scale = 2)
    var calculatedAmount: BigDecimal,

    @Column(name = "override_amount", precision = 15, scale = 2)
    var overrideAmount: BigDecimal? = null,

    @Column(name = "override_reason")
    var overrideReason: String? = null,

    @Column(name = "overridden_at")
    var overriddenAt: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) {
    /** 실제 지급액 */
    val amount: BigDecimal get() = overrideAmount ?: calculatedAmount

    fun apply(decision: FeeDecision) {
        if (basis == decision.basis && calculatedAmount.compareTo(decision.amount) == 0) return
        basis = decision.basis
        calculatedAmount = decision.amount
        updatedAt = LocalDateTime.now()
    }

    fun overrideManually(amount: BigDecimal, reason: String) {
        require(amount.signum() >= 0) { "관리비는 0원 이상이어야 합니다: $amount" }
        require(reason.isNotBlank()) { "관리비를 고친 사유를 입력해 주세요." }
        overrideAmount = amount
        overrideReason = reason.trim()
        overriddenAt = LocalDateTime.now()
        updatedAt = LocalDateTime.now()
    }

    fun clearOverride() {
        overrideAmount = null
        overrideReason = null
        overriddenAt = null
        updatedAt = LocalDateTime.now()
    }
}
