package kr.sunsetcoast.domain.managementfee.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate

/** 객실별 관리비 단가. 입실일이 [effectiveFrom, effectiveTo) 안에 있으면 적용한다. */
@Entity
@Table(name = "management_fee_rates")
class ManagementFeeRateEntity(
    @Column(name = "room_id", nullable = false)
    val roomId: Long,

    @Column(nullable = false, precision = 15, scale = 2)
    val amount: BigDecimal,

    @Column(name = "effective_from", nullable = false)
    val effectiveFrom: LocalDate,

    /** 이 날짜부터는 적용하지 않는다. null이면 현재 단가 */
    @Column(name = "effective_to")
    val effectiveTo: LocalDate? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) {
    fun appliesTo(date: LocalDate): Boolean =
        !date.isBefore(effectiveFrom) && (effectiveTo == null || date.isBefore(effectiveTo))
}
