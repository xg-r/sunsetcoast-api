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

    /** 관리자가 입금을 확인한 시각. null이면 미입금 */
    @Column(name = "paid_at")
    var paidAt: LocalDateTime? = null,

    /** 입금 완료로 표시할 때의 지급액 */
    @Column(name = "paid_amount", precision = 15, scale = 2)
    var paidAmount: BigDecimal? = null,

    /** 입금 완료로 표시한 사용자 (users.id) */
    @Column(name = "paid_by")
    var paidBy: Long? = null,

    /** 관리자가 실제 자료와 맞춰 보고 맞다고 확인한 시각. null이면 아직 확인 안 함 */
    @Column(name = "verified_at")
    var verifiedAt: LocalDateTime? = null,

    /** 확인할 때의 지급액 */
    @Column(name = "verified_amount", precision = 15, scale = 2)
    var verifiedAmount: BigDecimal? = null,

    @Column(name = "verified_by")
    var verifiedBy: Long? = null,

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

    val paid: Boolean get() = paidAt != null

    /** 입금 완료 뒤 업로드·재계산·수정으로 지급액이 달라졌는지 */
    val amountChangedAfterPayment: Boolean get() = paidAmount?.let { it.compareTo(amount) != 0 } == true

    val verified: Boolean get() = verifiedAt != null

    /** 확인한 뒤 지급액이 바뀌었는지 (다시 확인해야 함) */
    val amountChangedAfterVerification: Boolean get() = verifiedAmount?.let { it.compareTo(amount) != 0 } == true

    /** 지금 금액으로 확인. 금액이 바뀐 뒤 다시 확인할 때도 쓴다 */
    fun verify(userId: Long?) {
        verifiedAt = LocalDateTime.now()
        verifiedAmount = amount
        verifiedBy = userId
        updatedAt = LocalDateTime.now()
    }

    fun unverify() {
        verifiedAt = null
        verifiedAmount = null
        verifiedBy = null
        updatedAt = LocalDateTime.now()
    }

    fun markPaid(userId: Long?) {
        require(amount.signum() > 0) { "지급액이 0원인 관리비는 입금 완료로 표시할 수 없습니다: $id" }
        paidAt = LocalDateTime.now()
        paidAmount = amount
        paidBy = userId
        updatedAt = LocalDateTime.now()
    }

    fun unmarkPaid() {
        paidAt = null
        paidAmount = null
        paidBy = null
        updatedAt = LocalDateTime.now()
    }

    fun apply(decision: FeeDecision) {
        if (basis == decision.basis && calculatedAmount.compareTo(decision.amount) == 0) return
        basis = decision.basis
        calculatedAmount = decision.amount
        updatedAt = LocalDateTime.now()
    }

    fun overrideManually(amount: BigDecimal, reason: String) {
        check(!paid) { "입금 완료된 관리비는 금액을 고칠 수 없습니다. 입금 완료를 취소한 뒤 고쳐 주세요." }
        require(amount.signum() >= 0) { "관리비는 0원 이상이어야 합니다: $amount" }
        require(reason.isNotBlank()) { "관리비를 고친 사유를 입력해 주세요." }
        overrideAmount = amount
        overrideReason = reason.trim()
        overriddenAt = LocalDateTime.now()
        updatedAt = LocalDateTime.now()
    }

    fun clearOverride() {
        check(!paid) { "입금 완료된 관리비는 금액을 고칠 수 없습니다. 입금 완료를 취소한 뒤 고쳐 주세요." }
        overrideAmount = null
        overrideReason = null
        overriddenAt = null
        updatedAt = LocalDateTime.now()
    }
}
