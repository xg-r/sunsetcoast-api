package kr.sunsetcoast.controller.settlement

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import kr.sunsetcoast.common.response.ErrorResponse
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
}
