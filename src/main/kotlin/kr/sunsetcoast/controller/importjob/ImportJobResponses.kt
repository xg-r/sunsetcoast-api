package kr.sunsetcoast.controller.importjob

import kr.sunsetcoast.common.response.AmountText
import io.swagger.v3.oas.annotations.media.Schema
import kr.sunsetcoast.controller.settlement.dto.response.SettlementImportResponseBody
import kr.sunsetcoast.domain.importjob.entity.ImportJobEntity
import kr.sunsetcoast.domain.importjob.service.ImportJobService
import kr.sunsetcoast.domain.importjob.vo.BankJobResult
import kr.sunsetcoast.domain.importjob.vo.ImportJobKind
import kr.sunsetcoast.domain.settlement.vo.SettlementImportResult
import tools.jackson.databind.json.JsonMapper

@Schema(description = "업로드 작업")
data class ImportJobResponseBody(
    val id: Long,
    @field:Schema(description = "SETTLEMENT(정산 파일), BANK(계좌 거래내역)", example = "SETTLEMENT") val kind: String,
    val fileName: String,
    @field:Schema(description = "QUEUED, RUNNING, SUCCEEDED, FAILED", example = "QUEUED") val status: String,
    @field:Schema(description = "실패 사유", nullable = true) val errorMessage: String?,
    @field:Schema(description = "정산 파일 처리 결과 (SETTLEMENT, 끝난 뒤)", nullable = true) val settlement: SettlementImportResponseBody.FileResult?,
    @field:Schema(description = "계좌 거래내역 처리 결과 (BANK, 성공한 뒤)", nullable = true) val bank: BankJobResult?,
    val createdAt: String,
    val startedAt: String?,
    val finishedAt: String?,
) {
    companion object {
        /** @param includeAmounts false면 안내 문구의 금액을 뺀다 (현장관리자) */
        fun from(job: ImportJobEntity, jsonMapper: JsonMapper, includeAmounts: Boolean = true): ImportJobResponseBody {
            val json = job.resultJson
            return ImportJobResponseBody(
                id = job.id!!,
                kind = job.kind.name,
                fileName = job.fileName,
                status = job.status.name,
                errorMessage = if (includeAmounts) job.errorMessage else AmountText.redact(job.errorMessage),
                settlement = if (json != null && job.kind == ImportJobKind.SETTLEMENT) {
                    SettlementImportResponseBody.from(false, listOf(jsonMapper.readValue(json, SettlementImportResult::class.java)), includeAmounts).files.first()
                } else {
                    null
                },
                bank = if (json != null && job.kind == ImportJobKind.BANK) jsonMapper.readValue(json, BankJobResult::class.java) else null,
                createdAt = job.createdAt.withNano(0).toString(),
                startedAt = job.startedAt?.withNano(0)?.toString(),
                finishedAt = job.finishedAt?.withNano(0)?.toString(),
            )
        }
    }
}

@Schema(description = "업로드 접수 결과. 처리는 백그라운드에서 하므로 jobs의 상태를 GET /import-jobs?ids= 로 확인한다")
data class ImportJobsCreatedResponseBody(
    val jobs: List<ImportJobResponseBody>,
    @field:Schema(description = "접수하지 않은 파일 (빈 파일, 같은 파일이 이미 대기 중 등)") val rejected: List<Rejected>,
) {
    data class Rejected(val fileName: String, val message: String)

    companion object {
        fun from(result: List<ImportJobService.Enqueued>, jsonMapper: JsonMapper, includeAmounts: Boolean = true) = ImportJobsCreatedResponseBody(
            jobs = result.mapNotNull { it.job }.map { ImportJobResponseBody.from(it, jsonMapper, includeAmounts) },
            rejected = result.filter { it.job == null }.map { Rejected(it.fileName, it.rejectedReason.orEmpty()) },
        )
    }
}
