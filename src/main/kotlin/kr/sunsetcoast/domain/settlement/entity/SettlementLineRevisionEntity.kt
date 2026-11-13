package kr.sunsetcoast.domain.settlement.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * 재업로드로 금액이 바뀐 정산 행의 이전 값
 */
@Entity
@Table(name = "settlement_line_revisions")
class SettlementLineRevisionEntity(
    @Column(name = "settlement_line_id", nullable = false)
    val settlementLineId: Long,

    @Column(name = "import_batch_id", nullable = false)
    val importBatchId: Long,

    @Column(name = "previous_values", nullable = false, columnDefinition = "json")
    val previousValues: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)
