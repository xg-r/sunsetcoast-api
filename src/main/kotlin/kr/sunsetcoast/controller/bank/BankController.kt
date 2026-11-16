package kr.sunsetcoast.controller.bank

import kr.sunsetcoast.controller.bank.dto.BankAccountOwnerRequestBody
import kr.sunsetcoast.controller.bank.dto.BankAccountResponseBody
import kr.sunsetcoast.controller.bank.dto.BankAnalysisResponseBody
import kr.sunsetcoast.controller.bank.dto.BankCategoryResponseBody
import kr.sunsetcoast.controller.bank.dto.BankImportResponseBody
import kr.sunsetcoast.controller.bank.dto.BankRecategorizeRequestBody
import kr.sunsetcoast.controller.bank.dto.BankRuleResponseBody
import kr.sunsetcoast.controller.bank.dto.BankTransactionResponseBody
import kr.sunsetcoast.controller.bank.dto.BankUploadResponseBody
import kr.sunsetcoast.domain.bank.service.BankAnalysisService
import kr.sunsetcoast.domain.bank.service.BankStatementImportService
import kr.sunsetcoast.domain.bank.service.BankTransactionService
import kr.sunsetcoast.domain.bank.vo.BankCategory
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.time.YearMonth

@RestController
@RequestMapping("/bank")
class BankController(
    private val importService: BankStatementImportService,
    private val transactionService: BankTransactionService,
    private val analysisService: BankAnalysisService,
) : BankControllerDocs {
    @PostMapping("/import", version = "1.0", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    override fun importStatements(
        @RequestParam businessOwnerId: Long,
        @RequestPart("files") files: List<MultipartFile>,
    ): List<BankImportResponseBody> =
        files.map { file ->
            val name = file.originalFilename ?: "unknown"
            BankImportResponseBody.from(name, importService.import(name, file.bytes, businessOwnerId))
        }

    @GetMapping("/uploads", version = "1.0")
    override fun getUploads(): List<BankUploadResponseBody> = importService.findUploads().map(BankUploadResponseBody::from)

    @DeleteMapping("/uploads/{id}", version = "1.0")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun deleteUpload(@PathVariable id: Long) = importService.deleteUpload(id)

    @GetMapping("/accounts", version = "1.0")
    override fun getAccounts(): List<BankAccountResponseBody> = importService.findAccounts().map(BankAccountResponseBody::from)

    @PutMapping("/accounts/{id}/owner", version = "1.0")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun setAccountOwner(@PathVariable id: Long, @RequestBody body: BankAccountOwnerRequestBody) =
        importService.setAccountOwner(id, body.businessOwnerId)

    @GetMapping("/categories", version = "1.0")
    override fun getCategories(): List<BankCategoryResponseBody> = BankCategory.entries.map(BankCategoryResponseBody::from)

    @GetMapping("/transactions", version = "1.0")
    override fun getTransactions(
        @RequestParam accountId: Long,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM") month: YearMonth,
    ): List<BankTransactionResponseBody> = transactionService.findMonthly(accountId, month).map(BankTransactionResponseBody::from)

    @PutMapping("/transactions/{id}/category", version = "1.0")
    override fun recategorize(@PathVariable id: Long, @RequestBody body: BankRecategorizeRequestBody): Map<String, Int> {
        val category = runCatching { BankCategory.valueOf(body.category) }.getOrNull()
            ?: throw IllegalArgumentException("알 수 없는 분류입니다: ${body.category}")
        return mapOf("changed" to transactionService.recategorize(id, category, body.applyToSimilar, body.note))
    }

    @GetMapping("/rules", version = "1.0")
    override fun getRules(): List<BankRuleResponseBody> = transactionService.findRules().map(BankRuleResponseBody::from)

    @DeleteMapping("/rules/{id}", version = "1.0")
    override fun deleteRule(@PathVariable id: Long): Map<String, Int> = mapOf("changed" to transactionService.deleteRule(id))

    @GetMapping("/analysis", version = "1.0")
    override fun getAnalysis(
        @RequestParam accountId: Long,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM") from: YearMonth,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM") to: YearMonth,
    ): BankAnalysisResponseBody = BankAnalysisResponseBody.from(analysisService.analyze(accountId, from, to))
}
