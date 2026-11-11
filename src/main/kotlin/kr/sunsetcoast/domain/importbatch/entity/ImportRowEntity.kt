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
    val result: ImportRowResult,

    @Column(length = 500)
    val message: String? = null,

    /** 원본 행 JSON (개인정보 컬럼 제외) */
    @Column(nullable = false, columnDefinition = "json")
    val payload: String,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)
