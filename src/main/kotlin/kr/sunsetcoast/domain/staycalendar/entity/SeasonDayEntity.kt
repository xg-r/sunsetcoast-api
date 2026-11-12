package kr.sunsetcoast.domain.staycalendar.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import kr.sunsetcoast.domain.staycalendar.vo.SeasonSource
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 업소·날짜별 성수기 여부. 예약 금액으로 자동 판단하고(INFERRED), 관리자가 고칠 수 있다(MANUAL).
 */
@Entity
@Table(
    name = "season_days",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_season_days_property_date", columnNames = ["property_id", "stay_date"]),
    ]
)
class SeasonDayEntity(
    @Column(name = "property_id", nullable = false)
    val propertyId: Long,

    @Column(name = "stay_date", nullable = false)
    val stayDate: LocalDate,

    /** 판단 근거가 없거나 동률이면 null */
    @Column(name = "peak")
    var peak: Boolean? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var source: SeasonSource = SeasonSource.INFERRED,

    @Column(name = "peak_votes", nullable = false)
    var peakVotes: Int = 0,

    @Column(name = "offpeak_votes", nullable = false)
    var offpeakVotes: Int = 0,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) {
    /** 표를 갱신한다. 관리자가 고친 날짜는 표만 남기고 값은 유지한다. */
    fun applyVotes(peakVotes: Int, offpeakVotes: Int) {
        this.peakVotes = peakVotes
        this.offpeakVotes = offpeakVotes
        if (source == SeasonSource.INFERRED) {
            peak = when {
                peakVotes > offpeakVotes -> true
                offpeakVotes > peakVotes -> false
                else -> null
            }
        }
        updatedAt = LocalDateTime.now()
    }

    fun overrideManually(peak: Boolean) {
        this.peak = peak
        this.source = SeasonSource.MANUAL
        updatedAt = LocalDateTime.now()
    }

    fun clearManual() {
        source = SeasonSource.INFERRED
        applyVotes(peakVotes, offpeakVotes)
    }
}
