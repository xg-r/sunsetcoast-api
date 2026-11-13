package kr.sunsetcoast.domain.settlement.vo

import kr.sunsetcoast.domain.importbatch.entity.ImportBatchEntity
import kr.sunsetcoast.domain.importbatch.vo.ImportRowResult
import kr.sunsetcoast.domain.reservation.vo.OtaPlatform

enum class ImportStatus {
    /** 저장 완료 */
    IMPORTED,

    /** 미리보기 (저장하지 않음) */
    PREVIEW,

    /** 파일 단위로 거부 (중복 파일, 지원하지 않는 형식) */
    REJECTED,
}

data class RowIssue(
    val rowNo: Int,
    val result: ImportRowResult,
    val message: String?,
)

data class SettlementImportResult(
    val fileName: String,
    val status: ImportStatus,
    val platform: OtaPlatform? = null,
    val importBatchId: Long? = null,
    val message: String? = null,
    val totalRows: Int = 0,
    val insertedRows: Int = 0,
    val duplicateRows: Int = 0,
    val changedRows: Int = 0,
    val skippedRows: Int = 0,
    val unmappedRows: Int = 0,
    val failedRows: Int = 0,
    /** 확인이 필요한 행 (변경/미매핑/실패) */
    val issues: List<RowIssue> = emptyList(),
) {
    companion object {
        fun rejected(fileName: String, message: String) =
            SettlementImportResult(fileName = fileName, status = ImportStatus.REJECTED, message = message)

        fun of(batch: ImportBatchEntity, dryRun: Boolean, issues: List<RowIssue>) = SettlementImportResult(
            fileName = batch.fileName,
            status = if (dryRun) ImportStatus.PREVIEW else ImportStatus.IMPORTED,
            platform = batch.source,
            importBatchId = if (dryRun) null else batch.id,
            totalRows = batch.totalRows,
            insertedRows = batch.insertedRows,
            duplicateRows = batch.duplicateRows,
            changedRows = batch.changedRows,
            skippedRows = batch.skippedRows,
            unmappedRows = batch.unmappedRows,
            failedRows = batch.failedRows,
            issues = issues,
        )
    }
}
