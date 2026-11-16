package kr.sunsetcoast.domain.settlement.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 재업로드로 금액이 바뀐 정산 행의 이전 값
 */
@Entity
@Table(name = "settlement_line_revisions")
class SettlementLineRevisionEntity(
    @Column(name = "settlement_line_id", nullable = false)
    val settlementLineId: Long,

    /** 이 변경을 만든 업로드 */
    @Column(name = "import_batch_id", nullable = false)
    val importBatchId: Long,

    /** 바뀌기 전 행을 저장한 업로드 (직접 입력했거나 V29 이전 이력이면 null) */
    @Column(name = "previous_import_batch_id")
    var previousImportBatchId: Long? = null,

    @Column(name = "previous_values", nullable = false, columnDefinition = "json")
    var previousValues: String,

    @Column(name = "previous_settled_on")
    var previousSettledOn: LocalDate? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)
