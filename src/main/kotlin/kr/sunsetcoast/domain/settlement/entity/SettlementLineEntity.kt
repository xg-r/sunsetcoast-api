package kr.sunsetcoast.domain.settlement.entity

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
import kr.sunsetcoast.domain.reservation.entity.ReservationEntity
import kr.sunsetcoast.domain.room.entity.RoomEntity
import kr.sunsetcoast.domain.settlement.vo.SettlementAmounts
import kr.sunsetcoast.domain.settlement.vo.SettlementLineType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "settlement_lines",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_settlement_lines_room_key",
            columnNames = ["reservation_id", "room_id", "line_type", "stay_date", "seq"],
        ),
    ]
)
class SettlementLineEntity(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    val reservation: ReservationEntity,

    /** 다중 객실 예약은 객실마다 행이 따로 있다 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    val room: RoomEntity,

    /** 마지막으로 이 행을 저장/갱신한 업로드 */
    @Column(name = "import_batch_id", nullable = false)
    var importBatchId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "line_type", nullable = false, length = 30)
    val lineType: SettlementLineType,

    /** 숙박일(1박) 또는 취소수수료 매출인식일 */
    @Column(name = "stay_date", nullable = false)
    val stayDate: LocalDate,

    @Column(nullable = false)
    val seq: Int = 0,

    @Column(name = "settled_on")
    var settledOn: LocalDate? = null,

    @Column(name = "gross_amount", nullable = false, precision = 15, scale = 2)
    var grossAmount: BigDecimal,

    @Column(name = "partner_discount", nullable = false, precision = 15, scale = 2)
    var partnerDiscount: BigDecimal,

    @Column(name = "platform_discount", nullable = false, precision = 15, scale = 2)
    var platformDiscount: BigDecimal,

    @Column(name = "commission_rate", precision = 5, scale = 2)
    var commissionRate: BigDecimal?,

    @Column(name = "commission_amount", nullable = false, precision = 15, scale = 2)
    var commissionAmount: BigDecimal,

    @Column(name = "other_fee", nullable = false, precision = 15, scale = 2)
    var otherFee: BigDecimal,

    @Column(name = "net_amount", nullable = false, precision = 15, scale = 2)
    var netAmount: BigDecimal,

    /** 예약 단위 금액을 박수로 나눈 추정치인지 (떠나요 연박) */
    @Column(name = "split_estimated", nullable = false)
    var splitEstimated: Boolean = false,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) {
    fun amounts(): SettlementAmounts = SettlementAmounts(
        gross = grossAmount,
        partnerDiscount = partnerDiscount,
        platformDiscount = platformDiscount,
        commissionRate = commissionRate,
        commission = commissionAmount,
        otherFee = otherFee,
        net = netAmount,
    ).normalized()

    fun update(amounts: SettlementAmounts, settledOn: LocalDate?, splitEstimated: Boolean, importBatchId: Long) {
        val normalized = amounts.normalized()
        grossAmount = normalized.gross
        partnerDiscount = normalized.partnerDiscount
        platformDiscount = normalized.platformDiscount
        commissionRate = normalized.commissionRate
        commissionAmount = normalized.commission
        otherFee = normalized.otherFee
        netAmount = normalized.net
        this.settledOn = settledOn ?: this.settledOn
        this.splitEstimated = splitEstimated
        this.importBatchId = importBatchId
        updatedAt = LocalDateTime.now()
    }

    companion object {
        fun create(
            reservation: ReservationEntity,
            room: RoomEntity,
            importBatchId: Long,
            lineType: SettlementLineType,
            stayDate: LocalDate,
            seq: Int,
            settledOn: LocalDate?,
            amounts: SettlementAmounts,
            splitEstimated: Boolean,
        ): SettlementLineEntity {
            val normalized = amounts.normalized()
            return SettlementLineEntity(
                reservation = reservation,
                room = room,
                importBatchId = importBatchId,
                lineType = lineType,
                stayDate = stayDate,
                seq = seq,
                settledOn = settledOn,
                grossAmount = normalized.gross,
                partnerDiscount = normalized.partnerDiscount,
                platformDiscount = normalized.platformDiscount,
                commissionRate = normalized.commissionRate,
                commissionAmount = normalized.commission,
                otherFee = normalized.otherFee,
                netAmount = normalized.net,
                splitEstimated = splitEstimated,
            )
        }
    }
}
