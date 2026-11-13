package kr.sunsetcoast.controller.settlement.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.sunsetcoast.domain.settlement.vo.SettlementImportResult

@Schema(description = "정산 파일 업로드 결과")
data class SettlementImportResponseBody(
    @field:Schema(description = "미리보기 요청이었는지", example = "false")
    val dryRun: Boolean,
    @field:Schema(description = "파일별 결과 (업로드 순서)")
    val files: List<FileResult>,
) {
    @Schema(name = "SettlementImportFileResult", description = "파일 한 개의 처리 결과")
    data class FileResult(
        @field:Schema(description = "원본 파일 이름", example = "떠나요_정산_2026-10.xlsx")
        val fileName: String,
        @field:Schema(
            description = "IMPORTED: 저장 완료, PREVIEW: 미리보기(저장 안 함), REJECTED: 파일 단위 거부",
            allowableValues = ["IMPORTED", "PREVIEW", "REJECTED"],
            example = "IMPORTED",
        )
        val status: String,
        @field:Schema(
            description = "판별된 플랫폼. REJECTED면 null일 수 있다",
            allowableValues = ["DDNAYO", "YEOGI", "NOL"],
            nullable = true,
            example = "DDNAYO",
        )
        val platform: String?,
        @field:Schema(description = "저장된 import batch ID. 미리보기·거부 시 null", nullable = true, example = "42")
        val importBatchId: Long?,
        @field:Schema(description = "거부 사유 등 파일 단위 메시지", nullable = true, example = "이미 업로드된 파일입니다. (업로드 #41, 2026-11-01T10:00, 파일명: 떠나요_정산_2026-10.xlsx)")
        val message: String?,
        @field:Schema(description = "전체 행 수", example = "120")
        val totalRows: Int,
        @field:Schema(description = "새로 저장된 행 수", example = "100")
        val insertedRows: Int,
        @field:Schema(description = "같은 값이 이미 있어 건너뛴 행 수", example = "15")
        val duplicateRows: Int,
        @field:Schema(description = "금액이 달라 갱신된 행 수 (이전 값은 revision으로 보존)", example = "2")
        val changedRows: Int,
        @field:Schema(description = "저장할 금액이 없어 건너뛴 행 수", example = "1")
        val skippedRows: Int,
        @field:Schema(description = "객실 매핑에 실패한 행 수", example = "1")
        val unmappedRows: Int,
        @field:Schema(description = "파싱/검증에 실패한 행 수", example = "1")
        val failedRows: Int,
        @field:Schema(description = "확인이 필요한 행 (CHANGED/UNMAPPED/FAILED)")
        val issues: List<Issue>,
    )

    @Schema(name = "SettlementImportIssue", description = "확인이 필요한 행")
    data class Issue(
        @field:Schema(description = "원본 파일의 행 번호", example = "17")
        val rowNo: Int,
        @field:Schema(
            description = "행 처리 결과",
            allowableValues = ["SKIPPED", "DUPLICATE", "INSERTED", "CHANGED", "UNMAPPED", "FAILED"],
            example = "UNMAPPED",
        )
        val result: String,
        @field:Schema(description = "상세 메시지", nullable = true, example = "객실 매핑이 없습니다: 오션 101")
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
