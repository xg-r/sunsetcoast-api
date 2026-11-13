package kr.sunsetcoast.controller.settlement.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.sunsetcoast.domain.settlement.service.SettlementCoverageService

@Schema(description = "정산 계정별 업로드 파일 (누락 확인용)")
data class SettlementFileCoverageResponseBody(
    @field:Schema(description = "업로드 ID", example = "21")
    val importBatchId: Long,
    @field:Schema(description = "플랫폼", example = "NOL")
    val platform: String,
    @field:Schema(description = "정산 계정 (파일명에서 읽음). 읽지 못하면 null", nullable = true, example = "선셋코스트")
    val account: String?,
    @field:Schema(description = "업소 ID (정산 행 기준)", nullable = true, example = "2")
    val propertyId: Long?,
    @field:Schema(description = "정산일", example = "2026-08-20")
    val issuedOn: String,
    @field:Schema(description = "정산일을 파일명에서 읽었는지 (false면 정산 행이나 업로드일로 추정)")
    val issuedOnFromFileName: Boolean,
    val fileName: String,
    val totalRows: Int,
    val insertedRows: Int,
    val duplicateRows: Int,
    val unmappedRows: Int,
    val failedRows: Int,
    @field:Schema(description = "업로드 시각", example = "2026-11-13T10:28:54")
    val createdAt: String,
) {
    companion object {
        fun from(file: SettlementCoverageService.CoverageFile) = SettlementFileCoverageResponseBody(
            importBatchId = file.batch.id!!,
            platform = file.batch.source.name,
            account = file.account,
            propertyId = file.propertyId,
            issuedOn = file.issuedOn.toString(),
            issuedOnFromFileName = file.issuedOnFromFileName,
            fileName = file.batch.fileName,
            totalRows = file.batch.totalRows,
            insertedRows = file.batch.insertedRows,
            duplicateRows = file.batch.duplicateRows,
            unmappedRows = file.batch.unmappedRows,
            failedRows = file.batch.failedRows,
            createdAt = file.batch.createdAt.withNano(0).toString(),
        )
    }
}
