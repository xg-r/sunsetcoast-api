package kr.sunsetcoast.domain.bank.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "bank_statement_uploads")
class BankStatementUploadEntity(
    @Column(name = "bank_account_id", nullable = false)
    val bankAccountId: Long,

    @Column(name = "file_name", nullable = false)
    val fileName: String,

    @Column(name = "file_hash", nullable = false, length = 64)
    val fileHash: String,

    @Column(name = "period_from")
    var periodFrom: LocalDateTime? = null,

    @Column(name = "period_to")
    var periodTo: LocalDateTime? = null,

    @Column(name = "total_rows", nullable = false)
    var totalRows: Int = 0,

    @Column(name = "inserted_rows", nullable = false)
    var insertedRows: Int = 0,

    @Column(name = "duplicate_rows", nullable = false)
    var duplicateRows: Int = 0,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)
