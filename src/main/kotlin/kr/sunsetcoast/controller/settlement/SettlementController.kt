package kr.sunsetcoast.controller.settlement

import kr.sunsetcoast.controller.settlement.dto.response.SettlementImportResponseBody
import kr.sunsetcoast.domain.settlement.service.SettlementImportService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/settlements")
class SettlementController(
    private val settlementImportService: SettlementImportService,
) {
    /**
     * 정산 파일 업로드. 플랫폼은 파일 내용으로 자동 판별한다.
     *
     * @param dryRun true면 저장하지 않고 신규/중복/변경/오류 건수만 돌려준다 (미리보기).
     */
    @PostMapping("/import", version = "1.0")
    fun importSettlements(
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
}
