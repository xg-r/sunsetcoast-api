package kr.sunsetcoast.controller.settlement.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.sunsetcoast.domain.settlement.service.UnmappedRowService
import java.math.BigDecimal

@Schema(description = "객실을 찾지 못한 정산 행")
data class UnmappedRowResponseBody(
    @field:Schema(description = "원본 행 ID", example = "11")
    val id: Long,
    @field:Schema(description = "업로드 ID", example = "5")
    val importBatchId: Long,
    @field:Schema(description = "업로드 파일명")
    val fileName: String,
    @field:Schema(description = "플랫폼", allowableValues = ["DDNAYO", "YEOGI", "NOL"])
    val platform: String,
    @field:Schema(description = "엑셀 행 번호", example = "11")
    val rowNo: Int,
    @field:Schema(description = "찾지 못한 이유")
    val message: String?,
    @field:Schema(description = "파일에 적힌 객실명", nullable = true, example = "제돌이 (애견동반가능)((오션뷰&돌고래뷰),제")
    val roomName: String?,
    @field:Schema(description = "예약번호", nullable = true, example = "72861814")
    val reservationNumber: String?,
    @field:Schema(description = "예약자", nullable = true)
    val bookerName: String?,
    @field:Schema(description = "숙박 시작일 (첫 행 기준)", nullable = true, example = "2026-08-08")
    val stayDate: String?,
    @field:Schema(description = "박수 (떠나요만)", nullable = true, example = "2")
    val nights: Int?,
    @field:Schema(description = "행 유형", nullable = true, allowableValues = ["ROOM_NIGHT", "CANCELLATION_FEE", "ROOM_NIGHT_REVERSAL"])
    val lineType: String?,
    @field:Schema(description = "매출 합계", nullable = true, example = "500000")
    val grossAmount: BigDecimal?,
    @field:Schema(description = "정산금액 합계", nullable = true, example = "460000")
    val netAmount: BigDecimal?,
    @field:Schema(description = "객실명 조각 중 찾은 객실 ID (미리 선택해 둘 후보)")
    val suggestedRoomIds: List<Long>,
    @field:Schema(description = "업로드 시각", example = "2026-11-13T10:40:42")
    val uploadedAt: String,
) {
    companion object {
        fun from(value: UnmappedRowService.UnmappedRow): UnmappedRowResponseBody {
            val first = value.lines.firstOrNull()
            return UnmappedRowResponseBody(
                id = value.row.id!!,
                importBatchId = value.batch.id!!,
                fileName = value.batch.fileName,
                platform = value.batch.source.name,
                rowNo = value.row.rowNo,
                message = value.row.message,
                roomName = first?.roomName,
                reservationNumber = first?.reservationNumber,
                bookerName = first?.bookerName,
                stayDate = first?.stayDate?.toString(),
                nights = first?.nights,
                lineType = first?.lineType?.name,
                grossAmount = value.lines.takeIf { it.isNotEmpty() }?.sumOf { it.amounts.gross },
                netAmount = value.lines.takeIf { it.isNotEmpty() }?.sumOf { it.amounts.net },
                suggestedRoomIds = value.suggestedRoomIds,
                uploadedAt = value.batch.createdAt.withNano(0).toString(),
            )
        }
    }
}
