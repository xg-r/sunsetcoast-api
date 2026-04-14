package kr.sunsetcoast.domain.settlement.vo

import jakarta.persistence.Column
import jakarta.persistence.ConstraintMode
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import kr.sunsetcoast.domain.businessowner.vo.BusinessOwnerEntity
import kr.sunsetcoast.domain.room.vo.RoomEntity
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "settlements",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_settlements_reservation_number_platform",
            columnNames = ["reservation_number", "platform"]
        )
    ]
)
class SettlementEntity(

    @Column(name = "reservation_number", nullable = false, length = 50)
    val reservationNumber: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "room_id",
        nullable = false,
        foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT),
    )
    val room: RoomEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "business_id",
        nullable = false,
        foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT),
    )
    val business: BusinessOwnerEntity,

    @Column(name = "reserved_at", nullable = false)
    val reservedAt: LocalDate,

    @Column(name = "check_in_date", nullable = false)
    val checkInDate: LocalDate,

    @Column(name = "nights", nullable = false)
    val nights: Int,

    @Column(name = "booker_name", length = 50)
    val bookerName: String? = null,

    @Column(name = "booker_phone", length = 20)
    val bookerPhone: String? = null,

    @Column(name = "platform", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    val platform: OtaPlatform,

    @Column(name = "inflow_channel", length = 50)
    val inflowChannel: String? = null,

    @Column(name = "payment_type", length = 50)
    val paymentType: String? = null,

    @Column(name = "reservation_status", length = 50)
    val reservationStatus: String? = null,

    @Column(name = "settlement_status", length = 50)
    val settlementStatus: String? = null,

    @Column(name = "gross_amount", nullable = false, precision = 15, scale = 2)
    val grossAmount: BigDecimal,

    @Column(name = "commission_amount", nullable = false, precision = 15, scale = 2)
    val commissionAmount: BigDecimal,

    @Column(name = "net_amount", nullable = false, precision = 15, scale = 2)
    val netAmount: BigDecimal,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)
