package kr.sunsetcoast.domain.reservation.entity

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
import kr.sunsetcoast.domain.reservation.vo.AmountStatus
import kr.sunsetcoast.domain.reservation.vo.OtaPlatform
import kr.sunsetcoast.domain.reservation.vo.PaymentReceiver
import kr.sunsetcoast.domain.reservation.vo.ReservationStatus
import kr.sunsetcoast.domain.reservation.vo.SalesChannel
import kr.sunsetcoast.domain.room.entity.RoomEntity
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "reservations",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_reservations_source_number", columnNames = ["source", "reservation_number"]),
    ]
)
class ReservationEntity(
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val source: OtaPlatform,

    @Column(name = "reservation_number", nullable = false, length = 50)
    val reservationNumber: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    val room: RoomEntity,

    @Enumerated(EnumType.STRING)
    @Column(name = "sales_channel", nullable = false, length = 20)
    var salesChannel: SalesChannel,

    @Column(name = "sales_channel_raw", length = 100)
    var salesChannelRaw: String? = null,

    @Column(name = "booker_name", length = 100)
    var bookerName: String? = null,

    @Column(name = "reserved_at")
    var reservedAt: LocalDateTime? = null,

    /** 정산 파일만으로는 알 수 없는 경우(취소수수료만 있는 예약) null */
    @Column(name = "check_in_date")
    var checkInDate: LocalDate? = null,

    /** 총 박수 (떠나요: 파일의 이용박수, 여기어때/NOL: 입실 후 취소를 뺀 1박 행 수) */
    @Column(name = "nights")
    var nights: Int? = null,

    /** 주말 밤(금·토, 공휴일 전날)이 하나라도 있으면 true. 숙박일을 모르면 null */
    @Column(name = "weekend")
    var weekend: Boolean? = null,

    /** 성수기 밤이 하나라도 있으면 true, 판단된 밤이 모두 비수기면 false, 판단된 밤이 없으면 null */
    @Column(name = "peak_season")
    var peakSeason: Boolean? = null,

    /** 매출 합계 (1박 매출 + 입실 후 취소 차감 + 취소수수료). 박별 요금이 달라도 그대로 합산한다. */
    @Column(name = "gross_amount", nullable = false, precision = 15, scale = 2)
    var grossAmount: BigDecimal = BigDecimal.ZERO,

    @Column(name = "commission_amount", nullable = false, precision = 15, scale = 2)
    var commissionAmount: BigDecimal = BigDecimal.ZERO,

    /** 정산금액(입금액) 합계 */
    @Column(name = "net_amount", nullable = false, precision = 15, scale = 2)
    var netAmount: BigDecimal = BigDecimal.ZERO,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ReservationStatus,

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_receiver", nullable = false, length = 20)
    var paymentReceiver: PaymentReceiver = PaymentReceiver.US,

    @Enumerated(EnumType.STRING)
    @Column(name = "amount_status", nullable = false, length = 20)
    var amountStatus: AmountStatus,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) {
    /**
     * 파일에서 읽은 부가 정보로 비어 있는 값만 채운다. (나중 파일이 앞선 값을 지우지 않도록)
     */
    fun fillMissing(
        salesChannelRaw: String?,
        bookerName: String?,
        reservedAt: LocalDateTime?,
    ) {
        if (this.salesChannelRaw.isNullOrBlank() && !salesChannelRaw.isNullOrBlank()) {
            this.salesChannelRaw = salesChannelRaw
            this.salesChannel = SalesChannel.fromRaw(salesChannelRaw)
        }
        if (this.bookerName.isNullOrBlank() && !bookerName.isNullOrBlank()) this.bookerName = bookerName
        if (this.reservedAt == null && reservedAt != null) this.reservedAt = reservedAt
    }

    fun applyStayFlags(weekend: Boolean?, peakSeason: Boolean?) {
        this.weekend = weekend
        this.peakSeason = peakSeason
        this.updatedAt = LocalDateTime.now()
    }

    fun applyTotals(gross: BigDecimal, commission: BigDecimal, net: BigDecimal) {
        this.grossAmount = gross
        this.commissionAmount = commission
        this.netAmount = net
        this.updatedAt = LocalDateTime.now()
    }

    fun applyStay(checkInDate: LocalDate?, nights: Int?, status: ReservationStatus) {
        this.checkInDate = checkInDate ?: this.checkInDate
        this.nights = nights ?: this.nights
        this.status = status
        this.amountStatus = AmountStatus.CONFIRMED
        this.updatedAt = LocalDateTime.now()
    }
}
