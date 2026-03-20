package kr.sunsetcoast.controller.settlement

import kr.sunsetcoast.controller.settlement.dto.request.SettlementImportCsvRequestBody
import kr.sunsetcoast.controller.settlement.dto.response.SettlementImportCsvResponseBody
import kr.sunsetcoast.domain.settlement.vo.OtaPlatform
import kr.sunsetcoast.domain.settlement.vo.SettlementService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/settlements")
class SettlementController(
    private val settlementService: SettlementService,
) {
    @PostMapping(
        "/import", version = "1.0",
    )
    fun importSettlement(
        @RequestPart(value = "otaPlatform") otaPlatform: String,
        @RequestPart("files") files: List<MultipartFile>,
    ): ResponseEntity<SettlementImportCsvResponseBody> {
        val csvRows = settlementService.import(
            OtaPlatform.from(otaPlatform),
            files,
        )
        return ResponseEntity.ok(SettlementImportCsvResponseBody())
    }
}
