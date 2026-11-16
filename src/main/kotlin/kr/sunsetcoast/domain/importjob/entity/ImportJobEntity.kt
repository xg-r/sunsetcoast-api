package kr.sunsetcoast.domain.importjob.entity

import jakarta.persistence.Basic
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.Table
import kr.sunsetcoast.domain.importjob.vo.ImportJobKind
import kr.sunsetcoast.domain.importjob.vo.ImportJobStatus
import java.time.LocalDateTime

/** 업로드한 파일 하나를 백그라운드에서 처리하는 작업 */
@Entity
@Table(name = "import_jobs")
class ImportJobEntity(
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val kind: ImportJobKind,

    /** 계좌 거래내역: 업로드할 때 고른 사업자 */
    @Column(name = "business_owner_id")
    val businessOwnerId: Long? = null,

    @Column(name = "file_name", nullable = false)
    val fileName: String,

    @Column(name = "file_hash", nullable = false, length = 64)
    val fileHash: String,

    /** 처리가 끝나면 비운다 */
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "file_bytes", columnDefinition = "mediumblob")
    var fileBytes: ByteArray?,

    @Column(name = "requested_by")
    val requestedBy: Long?,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ImportJobStatus = ImportJobStatus.QUEUED,

    @Column(name = "result_json", columnDefinition = "json")
    var resultJson: String? = null,

    @Column(name = "error_message", length = 1000)
    var errorMessage: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "started_at")
    var startedAt: LocalDateTime? = null,

    @Column(name = "finished_at")
    var finishedAt: LocalDateTime? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) {
    fun start() {
        status = ImportJobStatus.RUNNING
        startedAt = LocalDateTime.now()
    }

    fun succeed(resultJson: String) {
        status = ImportJobStatus.SUCCEEDED
        this.resultJson = resultJson
        finish()
    }

    fun fail(message: String, resultJson: String? = null) {
        status = ImportJobStatus.FAILED
        errorMessage = message.take(1000)
        this.resultJson = resultJson
        finish()
    }

    /** 서버 재시작 등으로 처리 중에 멈춘 작업을 다시 대기열에 넣는다 (처리는 한 트랜잭션이라 롤백돼 있다) */
    fun requeue() {
        status = ImportJobStatus.QUEUED
        startedAt = null
    }

    private fun finish() {
        finishedAt = LocalDateTime.now()
        fileBytes = null
    }
}
