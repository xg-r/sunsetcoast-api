package kr.sunsetcoast.controller.settlement

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import kr.sunsetcoast.common.response.ErrorResponse
import kr.sunsetcoast.controller.settlement.dto.request.ResolveUnmappedRowRequestBody
import kr.sunsetcoast.controller.settlement.dto.response.ImportBatchResponseBody
import kr.sunsetcoast.controller.settlement.dto.response.ResolveUnmappedRowResponseBody
import kr.sunsetcoast.controller.settlement.dto.response.UnmappedRowResponseBody
import kr.sunsetcoast.controller.settlement.dto.response.SettlementImportResponseBody
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.multipart.MultipartFile

@Tag(name = "Settlement", description = "OTA 정산 파일 업로드")
interface SettlementControllerDocs {
    @Operation(
        summary = "정산 파일 업로드",
        description = """
떠나요(DDNAYO)·여기어때(YEOGI)·NOL 정산 파일(xlsx/xls/csv)을 여러 개 올린다. 플랫폼은 파일 내용으로 자동 판별한다.

- 파일별로 따로 처리되며, 결과는 `files[]`에 업로드 순서대로 담긴다.
- 이미 올린 파일이거나 지원하지 않는 형식이면 해당 파일만 `status=REJECTED`로 돌아오고 응답은 200이다.
- `dryRun=true`면 저장하지 않고 건수만 계산한다 (`status=PREVIEW`, `importBatchId=null`).
- `issues[]`에는 확인이 필요한 행(CHANGED/UNMAPPED/FAILED)만 담긴다.
""",
    )
    @ApiResponse(responseCode = "200", description = "파일별 처리 결과")
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 요청",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    @ApiResponse(
        responseCode = "500",
        description = "서버 오류",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun importSettlements(
        @Parameter(
            description = "정산 파일 (여러 개 가능)",
            content = [
                Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    array = ArraySchema(schema = Schema(type = "string", format = "binary")),
                ),
            ],
        )
        files: List<MultipartFile>,
        @Parameter(description = "true면 저장하지 않고 미리보기 결과만 돌려준다")
        dryRun: Boolean,
    ): ResponseEntity<SettlementImportResponseBody>

    @Operation(summary = "업로드 내역", description = "최근 업로드한 파일부터 최대 200개.")
    fun getImportBatches(): List<ImportBatchResponseBody>

    @Operation(
        summary = "미매핑 행 목록",
        description = "객실을 찾지 못해 저장되지 않은 정산 행. 다중 객실 예약의 잘린 객실명, 새 객실명 등.",
    )
    fun getUnmappedRows(): List<UnmappedRowResponseBody>

    @Operation(
        summary = "미매핑 행에 객실 지정",
        description = """
보존된 원본 행을 다시 파싱해서 지정한 객실로 저장한다. 다중 객실 예약은 객실을 파일 순서대로 여러 개 보내며, 금액은 객실 정가 비율로 나눈다.

- `saveAlias=true`이고 객실이 하나면 파일의 객실명을 별칭으로 저장한다.
- 처리 후 남은 미매핑 행을 다시 시도하므로, 같은 객실명이나 같은 예약번호의 행이 함께 처리될 수 있다 (`autoResolved`).
""",
    )
    fun resolveUnmappedRow(
        @Parameter(description = "원본 행 ID", example = "11") id: Long,
        body: ResolveUnmappedRowRequestBody,
    ): ResolveUnmappedRowResponseBody
}
