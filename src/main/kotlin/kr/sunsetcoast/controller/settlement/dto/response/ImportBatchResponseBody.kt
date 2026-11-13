package kr.sunsetcoast.controller.settlement.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.sunsetcoast.domain.importbatch.entity.ImportBatchEntity

@Schema(description = "업로드 내역 (파일 1개)")
data class ImportBatchResponseBody(
    @field:Schema(description = "업로드 ID", example = "28")
    val id: Long,
    @field:Schema(description = "플랫폼", example = "YEOGI")
    val platform: String,
    @field:Schema(description = "파일 종류", example = "SETTLEMENT")
    val fileKind: String,
    @field:Schema(description = "파일명")
    val fileName: String,
    val totalRows: Int,
    val insertedRows: Int,
    val duplicateRows: Int,
    val changedRows: Int,
    val skippedRows: Int,
    val unmappedRows: Int,
    val failedRows: Int,
    @field:Schema(description = "업로드 시각", example = "2026-11-13T10:28:54")
    val createdAt: String,
) {
    companion object {
        fun from(entity: ImportBatchEntity) = ImportBatchResponseBody(
            id = entity.id!!,
            platform = entity.source.name,
            fileKind = entity.fileKind.name,
            fileName = entity.fileName,
            totalRows = entity.totalRows,
            insertedRows = entity.insertedRows,
            duplicateRows = entity.duplicateRows,
            changedRows = entity.changedRows,
            skippedRows = entity.skippedRows,
            unmappedRows = entity.unmappedRows,
            failedRows = entity.failedRows,
            createdAt = entity.createdAt.withNano(0).toString(),
        )
    }
}
