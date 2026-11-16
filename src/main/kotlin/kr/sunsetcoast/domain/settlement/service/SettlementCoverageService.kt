package kr.sunsetcoast.domain.settlement.service

import kr.sunsetcoast.domain.importbatch.entity.ImportBatchEntity
import kr.sunsetcoast.domain.importbatch.repository.ImportBatchRepository
import kr.sunsetcoast.domain.importbatch.repository.ImportRowRepository
import kr.sunsetcoast.domain.reservation.vo.OtaPlatform
import kr.sunsetcoast.domain.settlement.entity.SettlementAccountEntity
import kr.sunsetcoast.domain.settlement.entity.SettlementWeekCheckEntity
import kr.sunsetcoast.domain.settlement.repository.SettlementAccountRepository
import kr.sunsetcoast.domain.settlement.repository.SettlementWeekCheckRepository
import kr.sunsetcoast.domain.settlement.repository.SettlementLineRepository
import kr.sunsetcoast.domain.settlement.vo.DdnayoFileContent
import kr.sunsetcoast.domain.settlement.vo.SettlementFileName
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.json.JsonMapper
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** 정산 계정별로 업로드한 정산 파일. 화면에서 주마다 빠진 파일을 찾는 데 쓴다 */
@Service
@Transactional(readOnly = true)
class SettlementCoverageService(
    private val importBatchRepository: ImportBatchRepository,
    private val settlementLineRepository: SettlementLineRepository,
    private val settlementWeekCheckRepository: SettlementWeekCheckRepository,
    private val settlementAccountRepository: SettlementAccountRepository,
    private val importRowRepository: ImportRowRepository,
    private val jsonMapper: JsonMapper,
) {
    data class CoverageFile(
        val batch: ImportBatchEntity,
        /** 정산 계정. 파일명(떠나요는 파일 내용)으로도 알 수 없으면 null */
        val account: String?,
        /** 이 파일의 정산 행이 가장 많이 속한 업소 (행이 없으면 같은 계정의 다른 파일에서) */
        val propertyId: Long?,
        /** 정산일 (파일명 → 파일 내용 추정 → 정산 행의 정산일 → 업로드일 순) */
        val issuedOn: LocalDate,
        val issuedOnFromFileName: Boolean,
        /** 이 파일이 담은 정산일 전부. 파일 현황은 이 날짜들이 속한 주를 모두 '업로드됨'으로 본다 */
        val settlementDates: List<LocalDate> = listOf(issuedOn),
    )

    fun findSettlementFiles(): List<CoverageFile> {
        val batches = importBatchRepository.findAll().filter { it.fileKind.name == "SETTLEMENT" }
        val propertyByBatch = settlementLineRepository.countByBatchAndProperty()
            .groupBy { it.batchId }
            .mapValues { (_, counts) -> counts.maxBy { it.lines }.propertyId }
        val settledOnByBatch = settlementLineRepository.maxSettledOnByBatch().associate { it.batchId to it.settledOn }

        val parsed = withDdnayoContent(
            batches.map { batch -> batch to SettlementFileName.parse(batch.fileName, batch.createdAt.toLocalDate()) },
        )
        val propertyByAccount = parsed
            .mapNotNull { (batch, name) -> name.account?.let { (batch.source to it) to propertyByBatch[batch.id] } }
            .filter { it.second != null }
            .groupBy({ it.first }, { it.second!! })
            .mapValues { (_, ids) -> ids.groupingBy { it }.eachCount().maxBy { it.value }.key }

        return parsed.map { (batch, name) ->
            val issuedOn = name.issuedOn ?: name.estimatedIssuedOn ?: settledOnByBatch[batch.id] ?: batch.createdAt.toLocalDate()
            CoverageFile(
                batch = batch,
                account = name.account,
                propertyId = propertyByBatch[batch.id] ?: name.account?.let { propertyByAccount[batch.source to it] },
                issuedOn = issuedOn,
                issuedOnFromFileName = name.issuedOn != null,
                settlementDates = if (name.issuedOn != null) listOf(issuedOn) else name.estimatedDates.ifEmpty { listOf(issuedOn) },
            )
        }
    }

    /**
     * 떠나요에서 받은 그대로의 파일(`AdjustCalendar(…).xls`)은 파일명에 계정·정산일이 없어 파일 내용으로 채운다.
     * 계정 이름은 같은 사업자번호의 이름 붙은 파일(`정산내역_{업소명}_…_떠나요.xls`)에서 가져오고,
     * 그런 파일이 없으면 `사업자 {사업자번호}`로 둔다. 정산일은 파일명에서 읽은 것으로 치지 않는다.
     */
    private fun withDdnayoContent(
        parsed: List<Pair<ImportBatchEntity, SettlementFileName>>,
    ): List<Pair<ImportBatchEntity, SettlementFileName>> {
        val ddnayo = parsed.filter { (batch, _) -> batch.source == OtaPlatform.DDNAYO }
        if (ddnayo.none { (_, name) -> name.account == null }) return parsed

        val rowsByBatch = importRowRepository.findByImportBatchIdIn(ddnayo.map { it.first.id!! })
            .groupBy({ it.importBatchId }, { jsonMapper.readValue(it.payload, PAYLOAD) })
        val contentByBatch = ddnayo.associate { (batch, _) -> batch.id!! to DdnayoFileContent.of(rowsByBatch[batch.id].orEmpty()) }
        val accountByBusinessNumber = ddnayo
            .mapNotNull { (batch, name) -> contentByBatch.getValue(batch.id!!).businessNumber?.let { number -> name.account?.let { number to it } } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, names) -> names.groupingBy { it }.eachCount().maxBy { it.value }.key }

        return parsed.map { (batch, name) ->
            if (batch.source != OtaPlatform.DDNAYO || name.account != null) return@map batch to name
            val content = contentByBatch.getValue(batch.id!!)
            val account = content.businessNumber?.let { accountByBusinessNumber[it] ?: "사업자 $it" }
            batch to SettlementFileName(account, null, content.issuedOn, content.settlementDates)
        }
    }

    fun findWeekChecks(): List<SettlementWeekCheckEntity> = settlementWeekCheckRepository.findAll()

    /** 파일이 없는 주를 확인 완료로 표시. 이미 표시돼 있으면 메모만 바꾼다 */
    @Transactional
    fun checkWeek(source: OtaPlatform, account: String, weekStart: LocalDate, note: String?): SettlementWeekCheckEntity {
        require(account.isNotBlank()) { "정산 계정이 비어 있습니다." }
        require(weekStart.dayOfWeek == DayOfWeek.MONDAY) { "주 시작일은 월요일이어야 합니다: $weekStart" }
        val trimmedNote = note?.trim()?.takeIf { it.isNotEmpty() }?.take(255)
        val existing = settlementWeekCheckRepository.findBySourceAndAccountAndWeekStart(source, account.trim(), weekStart)
        if (existing != null) {
            existing.note = trimmedNote
            return existing
        }
        return settlementWeekCheckRepository.save(
            SettlementWeekCheckEntity(source = source, account = account.trim(), weekStart = weekStart, note = trimmedNote),
        )
    }

    @Transactional
    fun uncheckWeek(id: Long) {
        require(settlementWeekCheckRepository.existsById(id)) { "확인 표시가 없습니다: $id" }
        settlementWeekCheckRepository.deleteById(id)
    }

    fun findAccounts(): List<SettlementAccountEntity> = settlementAccountRepository.findAll()

    /** 정산 계정 등록. 시작일은 그 주 월요일로 맞춘다 */
    @Transactional
    fun addAccount(source: OtaPlatform, account: String, propertyId: Long?, startedOn: LocalDate): SettlementAccountEntity {
        val name = account.trim()
        require(name.isNotEmpty()) { "정산 계정 이름이 비어 있습니다." }
        require(!settlementAccountRepository.existsBySourceAndAccount(source, name)) { "이미 등록한 정산 계정입니다: $name" }
        return settlementAccountRepository.save(
            SettlementAccountEntity(
                source = source,
                account = name,
                propertyId = propertyId,
                startedOn = startedOn.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
            ),
        )
    }

    @Transactional
    fun removeAccount(id: Long) {
        require(settlementAccountRepository.existsById(id)) { "정산 계정이 없습니다: $id" }
        settlementAccountRepository.deleteById(id)
    }
}

private val PAYLOAD = object : TypeReference<Map<String, String?>>() {}
