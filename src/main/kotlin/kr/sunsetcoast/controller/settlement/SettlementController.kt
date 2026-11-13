package kr.sunsetcoast.controller.settlement

import kr.sunsetcoast.controller.settlement.dto.request.ResolveUnmappedRowRequestBody
import kr.sunsetcoast.controller.settlement.dto.request.SettlementAccountRequestBody
import kr.sunsetcoast.controller.settlement.dto.request.SettlementWeekCheckRequestBody
import kr.sunsetcoast.controller.settlement.dto.response.SettlementAccountResponseBody
import kr.sunsetcoast.controller.settlement.dto.response.SettlementWeekCheckResponseBody
import kr.sunsetcoast.domain.reservation.vo.OtaPlatform
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import kr.sunsetcoast.controller.settlement.dto.response.ImportBatchResponseBody
import kr.sunsetcoast.controller.settlement.dto.response.ResolveUnmappedRowResponseBody
import kr.sunsetcoast.controller.settlement.dto.response.SettlementFileCoverageResponseBody
import kr.sunsetcoast.controller.settlement.dto.response.UnmappedRowResponseBody
import kr.sunsetcoast.controller.settlement.dto.response.SettlementImportResponseBody
import kr.sunsetcoast.domain.settlement.service.SettlementCoverageService
import kr.sunsetcoast.domain.settlement.service.SettlementImportService
import kr.sunsetcoast.domain.settlement.service.UnmappedRowService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/settlements")
class SettlementController(
    private val settlementImportService: SettlementImportService,
    private val unmappedRowService: UnmappedRowService,
    private val settlementCoverageService: SettlementCoverageService,
) : SettlementControllerDocs {
    /**
     * 정산 파일 업로드. 플랫폼은 파일 내용으로 자동 판별한다.
     *
     * @param dryRun true면 저장하지 않고 신규/중복/변경/오류 건수만 돌려준다 (미리보기).
     */
    @PostMapping("/import", version = "1.0", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    override fun importSettlements(
        @RequestPart("files") files: List<MultipartFile>,
        @RequestParam(defaultValue = "false") dryRun: Boolean,
    ): ResponseEntity<SettlementImportResponseBody> {
        val results = files.map { file ->
            settlementImportService.import(
                fileName = file.originalFilename ?: "unknown",
                bytes = file.bytes,
                dryRun = dryRun,
            )
        }
        return ResponseEntity.ok(SettlementImportResponseBody.from(dryRun, results))
    }

    @GetMapping("/import-batches", version = "1.0")
    override fun getImportBatches(): List<ImportBatchResponseBody> =
        settlementImportService.findRecentBatches().map { ImportBatchResponseBody.from(it) }

    @GetMapping("/coverage", version = "1.0")
    override fun getCoverage(): List<SettlementFileCoverageResponseBody> =
        settlementCoverageService.findSettlementFiles().map { SettlementFileCoverageResponseBody.from(it) }

    @GetMapping("/coverage/accounts", version = "1.0")
    override fun getAccounts(): List<SettlementAccountResponseBody> =
        settlementCoverageService.findAccounts().map { SettlementAccountResponseBody.from(it) }

    @PostMapping("/coverage/accounts", version = "1.0")
    override fun addAccount(@RequestBody body: SettlementAccountRequestBody): SettlementAccountResponseBody =
        SettlementAccountResponseBody.from(
            settlementCoverageService.addAccount(platformOf(body.platform), body.account, body.propertyId, body.startedOn),
        )

    @DeleteMapping("/coverage/accounts/{id}", version = "1.0")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun removeAccount(@PathVariable id: Long) = settlementCoverageService.removeAccount(id)

    @GetMapping("/coverage/checks", version = "1.0")
    override fun getWeekChecks(): List<SettlementWeekCheckResponseBody> =
        settlementCoverageService.findWeekChecks().map { SettlementWeekCheckResponseBody.from(it) }

    @PostMapping("/coverage/checks", version = "1.0")
    override fun checkWeek(@RequestBody body: SettlementWeekCheckRequestBody): SettlementWeekCheckResponseBody {
        return SettlementWeekCheckResponseBody.from(
            settlementCoverageService.checkWeek(platformOf(body.platform), body.account, body.weekStart, body.note),
        )
    }

    @DeleteMapping("/coverage/checks/{id}", version = "1.0")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun uncheckWeek(@PathVariable id: Long) = settlementCoverageService.uncheckWeek(id)

    @GetMapping("/unmapped-rows", version = "1.0")
    override fun getUnmappedRows(): List<UnmappedRowResponseBody> =
        unmappedRowService.findPending().map { UnmappedRowResponseBody.from(it) }

    @PostMapping("/unmapped-rows/{id}/resolve", version = "1.0")
    override fun resolveUnmappedRow(
        @PathVariable id: Long,
        @RequestBody body: ResolveUnmappedRowRequestBody,
    ): ResolveUnmappedRowResponseBody {
        val result = unmappedRowService.resolve(id, body.roomIds, body.saveAlias)
        return ResolveUnmappedRowResponseBody(result.row.result.name, result.row.message, result.autoResolved)
    }

    private fun platformOf(value: String): OtaPlatform =
        runCatching { OtaPlatform.valueOf(value) }.getOrNull() ?: throw IllegalArgumentException("알 수 없는 플랫폼입니다: $value")
}
