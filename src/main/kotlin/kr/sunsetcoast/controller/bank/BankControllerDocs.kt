package kr.sunsetcoast.controller.bank

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import kr.sunsetcoast.controller.bank.dto.BankAccountOwnerRequestBody
import kr.sunsetcoast.controller.bank.dto.BankAccountResponseBody
import kr.sunsetcoast.controller.bank.dto.BankAnalysisResponseBody
import kr.sunsetcoast.controller.bank.dto.BankCategoryResponseBody
import kr.sunsetcoast.controller.bank.dto.BankImportResponseBody
import kr.sunsetcoast.controller.bank.dto.BankRecategorizeRequestBody
import kr.sunsetcoast.controller.bank.dto.BankRuleResponseBody
import kr.sunsetcoast.controller.bank.dto.BankTransactionResponseBody
import kr.sunsetcoast.controller.bank.dto.BankUploadResponseBody
import org.springframework.http.MediaType
import org.springframework.web.multipart.MultipartFile
import java.time.YearMonth

@Tag(name = "Bank", description = "계좌 거래내역 업로드·분석 (BANK-01, 슈퍼어드민)")
interface BankControllerDocs {
    @Operation(
        summary = "거래내역 업로드 (동기)",
        description = "은행에서 받은 거래내역 엑셀(거래내역조회_입출식 예금)을 바로 처리한다. 화면은 비동기 POST /bank/import-jobs를 쓴다. " +
            "어느 사업자의 계좌인지 businessOwnerId로 고르고, 파일에 계좌번호가 있으면 그 번호의 계좌로 넣는다. " +
            "같은 파일은 REJECTED, 기간이 겹친 거래는 건너뛴다(duplicateRows). 거래마다 자동 분류한다.",
    )
    fun importStatements(
        @Parameter(description = "사업자 ID", example = "1") businessOwnerId: Long,
        @Parameter(content = [Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, array = ArraySchema(schema = Schema(type = "string", format = "binary")))])
        files: List<MultipartFile>,
    ): List<BankImportResponseBody>

    @Operation(summary = "거래내역 업로드 목록", description = "최근 100개.")
    fun getUploads(): List<BankUploadResponseBody>

    @Operation(summary = "거래내역 업로드 삭제", description = "그 업로드로 새로 들어온 거래만 지운다.")
    fun deleteUpload(@Parameter(description = "업로드 ID") id: Long)

    @Operation(summary = "계좌 목록")
    fun getAccounts(): List<BankAccountResponseBody>

    @Operation(summary = "계좌 사업자 지정", description = "관리비 출금과 계산된 관리비를 비교할 사업자. 파일의 괄호 안 이름이 사업자명과 같으면 자동으로 정해진다.")
    fun setAccountOwner(@Parameter(description = "계좌 ID") id: Long, body: BankAccountOwnerRequestBody)

    @Operation(summary = "분류 목록")
    fun getCategories(): List<BankCategoryResponseBody>

    @Operation(summary = "월별 거래 목록")
    fun getTransactions(
        @Parameter(description = "계좌 ID") accountId: Long,
        @Parameter(description = "월 (yyyy-MM)", example = "2026-11") month: YearMonth,
    ): List<BankTransactionResponseBody>

    @Operation(
        summary = "거래 분류 변경",
        description = "applyToSimilar=true면 같은 거래내용에 관리자 규칙을 만들어 자동 분류된 거래를 모두 다시 분류한다 (앞으로 올릴 거래에도 적용). " +
            "직접 고친 거래(MANUAL)는 다시 분류하지 않는다. 응답의 changed는 분류가 바뀐 거래 수.",
    )
    fun recategorize(@Parameter(description = "거래 ID") id: Long, body: BankRecategorizeRequestBody): Map<String, Int>

    @Operation(summary = "관리자 분류 규칙 목록")
    fun getRules(): List<BankRuleResponseBody>

    @Operation(summary = "관리자 분류 규칙 삭제", description = "삭제 후 자동 분류된 거래를 다시 분류한다.")
    fun deleteRule(@Parameter(description = "규칙 ID") id: Long): Map<String, Int>

    @Operation(
        summary = "계좌 분석",
        description = "월별 입금·출금·잔액·매출성 입금·운영비, 분류별 합계, OTA 입금 ↔ 같은 금액 정산 파일 대조(정산일 ±10일), " +
            "관리비 출금 ↔ 계좌 사업자의 계산된 관리비, 인사이트. 최대 36개월.",
    )
    fun getAnalysis(
        @Parameter(description = "계좌 ID") accountId: Long,
        @Parameter(description = "시작 월", example = "2026-11") from: YearMonth,
        @Parameter(description = "끝 월", example = "2026-11") to: YearMonth,
    ): BankAnalysisResponseBody
}
