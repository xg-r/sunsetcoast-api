package kr.sunsetcoast.controller.settlement.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.sunsetcoast.domain.settlement.service.ImportBatchDeleteService

@Schema(description = "업로드 삭제 결과")
data class ImportBatchDeleteResponseBody(
    @field:Schema(description = "삭제한 업로드 ID", example = "28")
    val importBatchId: Long,
    val fileName: String,
    @field:Schema(description = "이 파일이 새로 만들어서 삭제한 정산 행 수")
    val deletedLines: Int,
    @field:Schema(description = "이 파일이 금액을 바꿔서 이전 값으로 되돌린 정산 행 수")
    val restoredLines: Int,
    @field:Schema(description = "정산 행이 남지 않아 삭제한 예약 수")
    val deletedReservations: Int,
) {
    companion object {
        fun from(result: ImportBatchDeleteService.Result) = ImportBatchDeleteResponseBody(
            importBatchId = result.batchId,
            fileName = result.fileName,
            deletedLines = result.deletedLines,
            restoredLines = result.restoredLines,
            deletedReservations = result.deletedReservations,
        )
    }
}
