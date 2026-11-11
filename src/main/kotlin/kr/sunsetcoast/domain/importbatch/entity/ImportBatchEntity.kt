package kr.sunsetcoast.domain.importbatch.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import kr.sunsetcoast.domain.importbatch.vo.FileKind
import kr.sunsetcoast.domain.importbatch.vo.ImportRowResult
import kr.sunsetcoast.domain.reservation.vo.OtaPlatform
import java.time.LocalDateTime

@Entity
@Table(name = "import_batches")
class ImportBatchEntity(
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val source: OtaPlatform,

    @Enumerated(EnumType.STRING)
    @Column(name = "file_kind", nullable = false, length = 20)
    val fileKind: FileKind,

    @Column(name = "file_name", nullable = false)
    val fileName: String,

    @Column(name = "file_hash", nullable = false, length = 64, unique = true)
    val fileHash: String,

    @Column(name = "total_rows", nullable = false)
    var totalRows: Int = 0,

    @Column(name = "inserted_rows", nullable = false)
    var insertedRows: Int = 0,

    @Column(name = "duplicate_rows", nullable = false)
    var duplicateRows: Int = 0,

    @Column(name = "changed_rows", nullable = false)
    var changedRows: Int = 0,

    @Column(name = "skipped_rows", nullable = false)
    var skippedRows: Int = 0,

    @Column(name = "failed_rows", nullable = false)
    var failedRows: Int = 0,

    @Column(name = "unmapped_rows", nullable = false)
    var unmappedRows: Int = 0,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) {
    fun count(result: ImportRowResult) {
        totalRows++
        when (result) {
            ImportRowResult.INSERTED -> insertedRows++
            ImportRowResult.DUPLICATE -> duplicateRows++
            ImportRowResult.CHANGED -> changedRows++
            ImportRowResult.SKIPPED -> skippedRows++
            ImportRowResult.UNMAPPED -> unmappedRows++
            ImportRowResult.FAILED -> failedRows++
        }
    }
}
