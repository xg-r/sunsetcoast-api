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
import kr.sunsetcoast.domain.managementfee.vo.ManagementFeeExtraItem
import java.math.BigDecimal
import java.time.LocalDateTime

/** 관리비와 함께 현장에 넘겨 줄 추가요금 한 건. 금액 = 단가 × 수량 × 박수 */
@Entity
@Table(name = "management_fee_extras")
class ManagementFeeExtraEntity(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "management_fee_id", nullable = false)
    val fee: ManagementFeeEntity,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val item: ManagementFeeExtraItem,

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    val unitPrice: BigDecimal,

    @Column(nullable = false)
    val quantity: Int,

    /** 1박마다 받는 요금(추가인원)의 박수. 아니면 null */
    @Column
    val nights: Int?,

    @Column(nullable = false, precision = 15, scale = 2)
    val amount: BigDecimal,

    @Column
    val memo: String?,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)
