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
import kr.sunsetcoast.domain.settlement.vo.BusinessNumber
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
        val settledOnsByBatch = settlementLineRepository.settledOnsByBatch().groupBy({ it.batchId }, { it.settledOn })

        val parsed = withDdnayoContent(
            batches.map { batch -> batch to SettlementFileName.parse(batch.fileName, batch.createdAt.toLocalDate()) },
        )
        val propertyByAccount = parsed
            .mapNotNull { (batch, name) -> name.account?.let { (batch.source to it) to propertyByBatch[batch.id] } }
            .filter { it.second != null }
            .groupBy({ it.first }, { it.second!! })
            .mapValues { (_, ids) -> ids.groupingBy { it }.eachCount().maxBy { it.value }.key }

        return parsed.map { (batch, name) ->
            val settledOns = settledOnsByBatch[batch.id].orEmpty().sorted()
            // 기간 파일(여기어때 한 달 치)은 정산 행의 정산일 전부. 행이 없으면 기간 안의 주마다 하나씩
            val periodDates = name.period?.let { period -> settledOns.ifEmpty { weeklyDates(period) } }
            val issuedOn = name.issuedOn ?: periodDates?.last() ?: name.estimatedIssuedOn ?: settledOns.lastOrNull() ?: batch.createdAt.toLocalDate()
            CoverageFile(
                batch = batch,
                account = name.account,
                propertyId = propertyByBatch[batch.id] ?: name.account?.let { propertyByAccount[batch.source to it] },
                issuedOn = issuedOn,
                issuedOnFromFileName = name.issuedOn != null || name.period != null,
                settlementDates = when {
                    name.issuedOn != null -> listOf(issuedOn)
                    periodDates != null -> periodDates
                    else -> name.estimatedDates.ifEmpty { listOf(issuedOn) }
                },
            )
        }
    }

    /**
     * 떠나요에서 받은 그대로의 파일(`AdjustCalendar(…).xls`)은 파일명에 계정·정산일이 없어 파일 내용으로 채운다.
     * 계정 이름은 [ddnayoAccountName] 순서로 정한다. 정산일은 파일명에서 읽은 것으로 치지 않는다.
     */
    private fun withDdnayoContent(
        parsed: List<Pair<ImportBatchEntity, SettlementFileName>>,
    ): List<Pair<ImportBatchEntity, SettlementFileName>> {
        val ddnayo = parsed.filter { (batch, _) -> batch.source == OtaPlatform.DDNAYO }
        if (ddnayo.none { (_, name) -> name.account == null }) return parsed

        val rowsByBatch = importRowRepository.findByImportBatchIdIn(ddnayo.map { it.first.id!! })
            .groupBy({ it.importBatchId }, { jsonMapper.readValue(it.payload, PAYLOAD) })
        val contentByBatch = ddnayo.associate { (batch, _) -> batch.id!! to DdnayoFileContent.of(rowsByBatch[batch.id].orEmpty()) }
        val registered = settlementAccountRepository.findAll()
            .filter { it.source == OtaPlatform.DDNAYO }
            .mapNotNull { account -> account.businessNumber?.let { it to account.account } }
            .toMap()
        val fromNamedFiles = ddnayo
            .mapNotNull { (batch, name) ->
                BusinessNumber.normalize(contentByBatch.getValue(batch.id!!).businessNumber)?.let { number -> name.account?.let { number to it } }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, names) -> names.groupingBy { it }.eachCount().maxBy { it.value }.key }

        return parsed.map { (batch, name) ->
            if (batch.source != OtaPlatform.DDNAYO || name.account != null) return@map batch to name
            val content = contentByBatch.getValue(batch.id!!)
            val account = content.businessNumber?.let { ddnayoAccountName(it, registered, fromNamedFiles) }
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
    fun addAccount(
        source: OtaPlatform,
        account: String,
        propertyId: Long?,
        startedOn: LocalDate,
        businessNumber: String? = null,
    ): SettlementAccountEntity {
        val name = account.trim()
        require(name.isNotEmpty()) { "정산 계정 이름이 비어 있습니다." }
        require(!settlementAccountRepository.existsBySourceAndAccount(source, name)) { "이미 등록한 정산 계정입니다: $name" }
        val number = checkedBusinessNumber(source, businessNumber, null)
        return settlementAccountRepository.save(
            SettlementAccountEntity(
                source = source,
                account = name,
                propertyId = propertyId,
                businessNumber = number,
                startedOn = startedOn.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
            ),
        )
    }

    /** 등록한 정산 계정의 사업자번호를 바꾼다. 빈 값이면 지운다 */
    @Transactional
    fun changeBusinessNumber(id: Long, businessNumber: String?): SettlementAccountEntity {
        val entity = settlementAccountRepository.findById(id).orElseThrow { IllegalArgumentException("정산 계정이 없습니다: $id") }
        entity.businessNumber = checkedBusinessNumber(entity.source, businessNumber, id)
        return entity
    }

    /** 떠나요 계정만, 숫자 10자리, 다른 계정과 겹치지 않게 */
    private fun checkedBusinessNumber(source: OtaPlatform, input: String?, selfId: Long?): String? {
        if (input.isNullOrBlank()) return null
        require(source == OtaPlatform.DDNAYO) { "사업자번호는 떠나요 정산 계정에만 지정합니다." }
        val number = requireNotNull(BusinessNumber.normalize(input)) { "사업자번호는 숫자 10자리여야 합니다: $input" }
        settlementAccountRepository.findBySourceAndBusinessNumber(source, number)?.let { other ->
            require(other.id == selfId) { "사업자번호 ${BusinessNumber.format(number)}은(는) 이미 '${other.account}' 계정에 지정돼 있습니다." }
        }
        return number
    }

    @Transactional
    fun removeAccount(id: Long) {
        require(settlementAccountRepository.existsById(id)) { "정산 계정이 없습니다: $id" }
        settlementAccountRepository.deleteById(id)
    }
}

/** 기간의 시작일부터 7일 간격 날짜 (기간이 걸친 주마다 하나) */
internal fun weeklyDates(period: ClosedRange<LocalDate>): List<LocalDate> {
    val firstMonday = period.start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return generateSequence(firstMonday) { it.plusWeeks(1) }
        .takeWhile { it <= period.endInclusive }
        .map { maxOf(it, period.start) }
        .toList()
}

private val PAYLOAD = object : TypeReference<Map<String, String?>>() {}

/**
 * 떠나요 원본 파일(AdjustCalendar)의 사업자번호로 정산 계정 이름을 정한다.
 * 1. 이 사업자번호를 지정해 둔 등록 정산 계정
 * 2. 같은 사업자번호가 든 이름 붙은 파일(`정산내역_{업소명}_…_떠나요.xls`)의 업소명
 * 3. 둘 다 없으면 `사업자 {사업자번호}`
 * @param registered 사업자번호(숫자 10자리) → 등록 계정 이름
 * @param fromNamedFiles 사업자번호(숫자 10자리) → 이름 붙은 파일의 계정 이름
 */
internal fun ddnayoAccountName(fileBusinessNumber: String, registered: Map<String, String>, fromNamedFiles: Map<String, String>): String {
    val number = BusinessNumber.normalize(fileBusinessNumber) ?: return "사업자 $fileBusinessNumber"
    return registered[number] ?: fromNamedFiles[number] ?: "사업자 ${BusinessNumber.format(number)}"
}
