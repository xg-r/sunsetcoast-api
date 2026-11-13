package kr.sunsetcoast.domain.importbatch.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import kr.sunsetcoast.domain.importbatch.vo.ImportRowResult
import java.time.LocalDateTime

/**
 * 업로드한 파일의 원본 행. 파서를 고친 뒤 재처리하거나 정산 근거를 추적할 때 사용한다.
 */
@Entity
@Table(name = "import_rows")
class ImportRowEntity(
    @Column(name = "import_batch_id", nullable = false)
    val importBatchId: Long,

    @Column(name = "row_no", nullable = false)
    val rowNo: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var result: ImportRowResult,

    @Column(length = 500)
    var message: String? = null,

    /** 원본 행 JSON (개인정보 컬럼 제외) */
    @Column(nullable = false, columnDefinition = "json")
    val payload: String,

    /** 미매핑 행을 관리자가 객실을 지정해 처리한 시각 */
    @Column(name = "resolved_at")
    var resolvedAt: LocalDateTime? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) {
    fun resolve(result: ImportRowResult, message: String) {
        check(this.result == ImportRowResult.UNMAPPED && resolvedAt == null) { "이미 처리된 행입니다: $id" }
        this.result = result
        this.message = message.take(500)
        resolvedAt = LocalDateTime.now()
    }
}
