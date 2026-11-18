package kr.sunsetcoast.domain.managementfee.entity

import jakarta.persistence.CascadeType
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
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import kr.sunsetcoast.domain.managementfee.vo.FeeDecision
import kr.sunsetcoast.domain.managementfee.vo.ManagementFeeBasis
import kr.sunsetcoast.domain.managementfee.vo.ManagementFeeExtraInput
import org.hibernate.annotations.BatchSize
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

    /** 현장에서 옮긴 객실 (실제로 묵은 객실). 관리비는 이 객실 단가로 계산한다. null이면 옮기지 않음 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moved_room_id")
    var movedRoom: RoomEntity? = null,

    /** 객실을 옮기면서 손님에게 받은 차액 (환불이면 음수). 매출에 더한다 */
    @Column(name = "room_change_difference", precision = 15, scale = 2)
    var roomChangeDifference: BigDecimal? = null,

    @Column(name = "room_change_memo")
    var roomChangeMemo: String? = null,

    @Column(name = "room_changed_at")
    var roomChangedAt: LocalDateTime? = null,

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
    /** 관리비와 함께 현장에 넘겨 줄 추가요금 (바베큐, 추가인원 등) */
    // 응답마다 쓰고 open-in-view가 꺼져 있어 바로 읽는다. 여러 관리비는 BatchSize로 한 번에
    @OneToMany(mappedBy = "fee", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @BatchSize(size = 200)
    val extras: MutableList<ManagementFeeExtraEntity> = mutableListOf()

    /** 관리비 (수정 금액이 있으면 수정 금액). 추가요금은 빠져 있다 */
    val amount: BigDecimal get() = overrideAmount ?: calculatedAmount

    /** 추가요금 합계 */
    val extraAmount: BigDecimal get() = extras.fold(BigDecimal.ZERO) { sum, e -> sum + e.amount }

    /** 현장에 이체할 금액 = 관리비 + 추가요금. 입금 완료·확인은 이 금액으로 한다 */
    val payableAmount: BigDecimal get() = amount + extraAmount

    val paid: Boolean get() = paidAt != null

    /** 입금 완료 뒤 업로드·재계산·수정으로 지급액(관리비 + 추가요금)이 달라졌는지 */
    val amountChangedAfterPayment: Boolean get() = paidAmount?.let { it.compareTo(payableAmount) != 0 } == true

    val verified: Boolean get() = verifiedAt != null

    /** 확인한 뒤 지급액(관리비 + 추가요금)이 바뀌었는지 (다시 확인해야 함) */
    val amountChangedAfterVerification: Boolean get() = verifiedAmount?.let { it.compareTo(payableAmount) != 0 } == true

    /** 지금 금액으로 확인. 금액이 바뀐 뒤 다시 확인할 때도 쓴다 */
    fun verify(userId: Long?) {
        verifiedAt = LocalDateTime.now()
        verifiedAmount = payableAmount
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
        require(payableAmount.signum() > 0) { "지급액이 0원인 관리비는 입금 완료로 표시할 수 없습니다: $id" }
        paidAt = LocalDateTime.now()
        paidAmount = payableAmount
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

    /**
     * 추가요금을 통째로 바꾼다 (화면에서 목록을 편집해 저장). 빈 목록이면 모두 지운다.
     * 박수를 따로 주지 않은 1박 단위 요금은 예약 박수로 계산한다.
     */
    fun replaceExtras(inputs: List<ManagementFeeExtraInput>) {
        check(!paid) { "입금 완료된 관리비는 추가요금을 고칠 수 없습니다. 입금 완료를 취소한 뒤 고쳐 주세요." }
        extras.clear()
        inputs.forEach { input ->
            val line = input.resolve(defaultNights = reservation.nights)
            extras.add(
                ManagementFeeExtraEntity(
                    fee = this,
                    item = input.item,
                    unitPrice = line.unitPrice,
                    quantity = input.quantity,
                    nights = line.nights,
                    amount = line.amount,
                    memo = input.memo?.trim()?.ifEmpty { null },
                )
            )
        }
        updatedAt = LocalDateTime.now()
    }

    /** 관리비 단가를 정하는 객실: 옮겼으면 옮긴 객실 */
    val feeRoom: RoomEntity get() = movedRoom ?: room

    /**
     * 현장 객실 변경을 적는다. 관리비는 서비스가 옮긴 객실 단가로 다시 계산한다.
     * @param difference 받은 차액 (없으면 null, 환불이면 음수)
     */
    fun changeRoom(to: RoomEntity, difference: BigDecimal?, memo: String?) {
        check(!paid) { "입금 완료된 관리비는 객실 변경을 고칠 수 없습니다. 입금 완료를 취소한 뒤 고쳐 주세요." }
        require(to.id != room.id) { "예약한 객실과 같은 객실로는 옮길 수 없습니다. 객실 변경을 취소하려면 '변경 취소'를 눌러 주세요." }
        require(to.property.id == room.property.id) { "같은 업소의 객실로만 옮길 수 있습니다: ${to.name}" }
        movedRoom = to
        roomChangeDifference = difference?.takeIf { it.signum() != 0 }
        roomChangeMemo = memo?.trim()?.ifEmpty { null }
        roomChangedAt = LocalDateTime.now()
        updatedAt = LocalDateTime.now()
    }

    fun clearRoomChange() {
        check(!paid) { "입금 완료된 관리비는 객실 변경을 고칠 수 없습니다. 입금 완료를 취소한 뒤 고쳐 주세요." }
        movedRoom = null
        roomChangeDifference = null
        roomChangeMemo = null
        roomChangedAt = null
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
