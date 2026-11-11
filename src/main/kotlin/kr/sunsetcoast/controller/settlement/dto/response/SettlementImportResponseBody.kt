package kr.sunsetcoast.controller.settlement.dto.response

import kr.sunsetcoast.domain.settlement.vo.SettlementImportResult

data class SettlementImportResponseBody(
    val dryRun: Boolean,
    val files: List<FileResult>,
) {
    data class FileResult(
        val fileName: String,
        val status: String,
        val platform: String?,
        val importBatchId: Long?,
        val message: String?,
        val totalRows: Int,
        val insertedRows: Int,
        val duplicateRows: Int,
        val changedRows: Int,
        val skippedRows: Int,
        val unmappedRows: Int,
        val failedRows: Int,
        val issues: List<Issue>,
    )

    data class Issue(
        val rowNo: Int,
        val result: String,
        val message: String?,
    )

    companion object {
        fun from(dryRun: Boolean, results: List<SettlementImportResult>) = SettlementImportResponseBody(
            dryRun = dryRun,
            files = results.map { r ->
                FileResult(
                    fileName = r.fileName,
                    status = r.status.name,
                    platform = r.platform?.name,
                    importBatchId = r.importBatchId,
                    message = r.message,
                    totalRows = r.totalRows,
                    insertedRows = r.insertedRows,
                    duplicateRows = r.duplicateRows,
                    changedRows = r.changedRows,
                    skippedRows = r.skippedRows,
                    unmappedRows = r.unmappedRows,
                    failedRows = r.failedRows,
                    issues = r.issues.map { Issue(it.rowNo, it.result.name, it.message) },
                )
            },
        )
    }
}
