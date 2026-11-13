package kr.sunsetcoast.domain.settlement.service

import kr.sunsetcoast.domain.importbatch.entity.ImportBatchEntity
import kr.sunsetcoast.domain.importbatch.repository.ImportBatchRepository
import kr.sunsetcoast.domain.settlement.repository.SettlementLineRepository
import kr.sunsetcoast.domain.settlement.vo.SettlementFileName
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/** 정산 계정별로 업로드한 정산 파일. 화면에서 주마다 빠진 파일을 찾는 데 쓴다 */
@Service
@Transactional(readOnly = true)
class SettlementCoverageService(
    private val importBatchRepository: ImportBatchRepository,
    private val settlementLineRepository: SettlementLineRepository,
) {
    data class CoverageFile(
        val batch: ImportBatchEntity,
        /** 정산 계정. 파일명을 읽지 못하면 null */
        val account: String?,
        /** 이 파일의 정산 행이 가장 많이 속한 업소 (행이 없으면 같은 계정의 다른 파일에서) */
        val propertyId: Long?,
        /** 정산일 (파일명 → 정산 행의 정산일 → 업로드일 순) */
        val issuedOn: LocalDate,
        val issuedOnFromFileName: Boolean,
    )

    fun findSettlementFiles(): List<CoverageFile> {
        val batches = importBatchRepository.findAll().filter { it.fileKind.name == "SETTLEMENT" }
        val propertyByBatch = settlementLineRepository.countByBatchAndProperty()
            .groupBy { it.batchId }
            .mapValues { (_, counts) -> counts.maxBy { it.lines }.propertyId }
        val settledOnByBatch = settlementLineRepository.maxSettledOnByBatch().associate { it.batchId to it.settledOn }

        val parsed = batches.map { batch -> batch to SettlementFileName.parse(batch.fileName, batch.createdAt.toLocalDate()) }
        val propertyByAccount = parsed
            .mapNotNull { (batch, name) -> name.account?.let { (batch.source to it) to propertyByBatch[batch.id] } }
            .filter { it.second != null }
            .groupBy({ it.first }, { it.second!! })
            .mapValues { (_, ids) -> ids.groupingBy { it }.eachCount().maxBy { it.value }.key }

        return parsed.map { (batch, name) ->
            CoverageFile(
                batch = batch,
                account = name.account,
                propertyId = propertyByBatch[batch.id] ?: name.account?.let { propertyByAccount[batch.source to it] },
                issuedOn = name.issuedOn ?: settledOnByBatch[batch.id] ?: batch.createdAt.toLocalDate(),
                issuedOnFromFileName = name.issuedOn != null,
            )
        }
    }
}
