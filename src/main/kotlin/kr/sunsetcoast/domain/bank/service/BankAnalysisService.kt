package kr.sunsetcoast.domain.bank.service

import kr.sunsetcoast.domain.bank.entity.BankAccountEntity
import kr.sunsetcoast.domain.bank.entity.BankTransactionEntity
import kr.sunsetcoast.domain.bank.repository.BankAccountRepository
import kr.sunsetcoast.domain.bank.repository.BankTransactionRepository
import kr.sunsetcoast.domain.bank.vo.BankCategory
import kr.sunsetcoast.domain.importbatch.repository.ImportRowRepository
import kr.sunsetcoast.domain.managementfee.service.ManagementFeeService
import kr.sunsetcoast.domain.reservation.vo.OtaPlatform
import kr.sunsetcoast.domain.settlement.service.SettlementCoverageService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.json.JsonMapper
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * 계좌 거래내역 분석: 월별 현금흐름, 분류별 금액, OTA 입금 ↔ 정산 파일 대조, 관리비 출금 ↔ 계산된 관리비, 인사이트.
 *
 * 금액은 모두 양수로 돌려준다 (입금·출금 구분은 분류 그룹으로).
 *
 * 현금흐름(입금·출금·잔액)은 거래일 기준이고, 매출은 귀속월 기준이다:
 * 관계사 정산(스테이디앤디가 받은 여기어때 리조트 정산을 다음 달 초 '10월숙박비'처럼 나눠 보냄)은 거래내용의 달에 넣는다.
 * 그래서 끝 월 뒤 [SETTLEMENT_LAG_MONTHS]개월 안에 보낸 관계사 정산도 함께 읽는다.
 */
@Service
@Transactional(readOnly = true)
class BankAnalysisService(
    private val accountRepository: BankAccountRepository,
    private val transactionRepository: BankTransactionRepository,
    private val coverageService: SettlementCoverageService,
    private val importRowRepository: ImportRowRepository,
    private val managementFeeService: ManagementFeeService,
    private val jsonMapper: JsonMapper,
) {
    data class Analysis(
        val account: BankAccountEntity,
        val from: YearMonth,
        val to: YearMonth,
        val months: List<MonthFlow>,
        val categories: List<CategoryTotal>,
        val otaDeposits: List<OtaDeposit>,
        val feeComparisons: List<FeeComparison>,
        val insights: List<Insight>,
    )

    data class MonthFlow(
        val month: YearMonth,
        /** 그 달 첫 거래 전 잔액 (앞 거래가 없으면 첫 거래로 거꾸로 계산) */
        val openingBalance: BigDecimal?,
        val closingBalance: BigDecimal?,
        val deposits: BigDecimal,
        val withdrawals: BigDecimal,
        val revenue: BigDecimal,
        val expense: BigDecimal,
        val transactionCount: Int,
        val byCategory: Map<BankCategory, BigDecimal>,
    ) {
        val net: BigDecimal get() = deposits - withdrawals
    }

    data class CategoryTotal(val category: BankCategory, val amount: BigDecimal, val count: Int, val share: BigDecimal?)

    data class OtaDeposit(val transaction: BankTransactionEntity, val platform: OtaPlatform, val match: SettlementMatch?)

    data class SettlementMatch(val importBatchId: Long, val fileName: String, val account: String?, val issuedOn: LocalDate)

    data class FeeComparison(
        /** 관리비가 속한 입실 월 (거래내용 'N월…관리비'에서 읽음, 없으면 출금 전달) */
        val feeMonth: YearMonth,
        val withdrawals: List<BankTransactionEntity>,
        val withdrawnTotal: BigDecimal,
        /** 계좌 사업자의 그 달 관리비 합계. 사업자를 모르거나 그 달 관리비가 하나도 없으면(정산 파일 전) null */
        val calculatedTotal: BigDecimal?,
        val paidTotal: BigDecimal?,
        val unpaidCount: Int?,
    ) {
        val difference: BigDecimal? get() = calculatedTotal?.let { withdrawnTotal - it }
    }

    enum class Level { GOOD, INFO, WARN }

    data class Insight(val level: Level, val title: String, val detail: String)

    fun analyze(accountId: Long, from: YearMonth, to: YearMonth): Analysis {
        require(!from.isAfter(to)) { "시작 월이 끝 월보다 늦습니다: $from ~ $to" }
        require(ChronoUnit.MONTHS.between(from, to) < 36) { "한 번에 36개월까지 볼 수 있습니다." }
        val account = accountRepository.findById(accountId).orElseThrow { IllegalArgumentException("계좌가 없습니다: $accountId") }
        val start = from.atDay(1).atStartOfDay()
        val end = to.atEndOfMonth().atTime(23, 59, 59)
        val txs = transactionRepository.findByBankAccountIdAndTransactedAtBetweenOrderByTransactedAtAscIdAsc(accountId, start, end)
        val before = transactionRepository.findFirstByBankAccountIdAndTransactedAtBeforeOrderByTransactedAtDescIdDesc(accountId, start)
        val later = transactionRepository.findByBankAccountIdAndTransactedAtBetweenOrderByTransactedAtAscIdAsc(
            accountId, end.plusSeconds(1), to.plusMonths(SETTLEMENT_LAG_MONTHS).atEndOfMonth().atTime(23, 59, 59),
        ).filter { it.category in AFFILIATE_SETTLEMENT }
        // 매출로 셀 거래: 귀속월이 기간 안인 것
        val revenueTxs = (txs + later).filter { it.category.group == BankCategory.Group.REVENUE && revenueMonthOf(it) in from..to }

        val months = monthFlows(from, to, txs, before, revenueTxs)
        val categories = categoryTotals(txs.filter { it.category.group != BankCategory.Group.REVENUE } + revenueTxs)
        val ota = otaDeposits(txs)
        val fees = feeComparisons(account, txs)
        val pendingSettlement = (txs + later).any { it.category == BankCategory.AFFILIATE_SETTLEMENT_OUT } &&
            revenueTxs.none { it.category == BankCategory.AFFILIATE_SETTLEMENT_OUT && revenueMonthOf(it) == to }
        return Analysis(account, from, to, months, categories, ota, fees, insights(months, categories, ota, fees, if (pendingSettlement) to else null))
    }

    private fun monthFlows(
        from: YearMonth,
        to: YearMonth,
        txs: List<BankTransactionEntity>,
        before: BankTransactionEntity?,
        revenueTxs: List<BankTransactionEntity>,
    ): List<MonthFlow> {
        val byMonth = txs.groupBy { YearMonth.from(it.transactedAt) }
        val revenueByMonth = revenueTxs.groupBy { revenueMonthOf(it) }
        var carry: BigDecimal? = before?.balance ?: txs.firstOrNull()?.let { it.balance - it.signedAmount }
        return generateSequence(from) { it.plusMonths(1) }.takeWhile { !it.isAfter(to) }.map { month ->
            val list = byMonth[month].orEmpty()
            val opening = carry
            val closing = list.lastOrNull()?.balance ?: carry
            carry = closing
            MonthFlow(
                month = month,
                openingBalance = opening,
                closingBalance = closing,
                deposits = list.sumOf { it.deposit },
                withdrawals = list.sumOf { it.withdrawal },
                revenue = revenueByMonth[month].orEmpty().sumOf { it.signedAmount },
                expense = list.filter { it.category.group == BankCategory.Group.EXPENSE }.sumOf { -it.signedAmount },
                transactionCount = list.size,
                byCategory = (list.filter { it.category.group != BankCategory.Group.REVENUE } + revenueByMonth[month].orEmpty())
                    .groupBy { it.category }.mapValues { (_, l) -> l.sumOf { it.signedAmount }.abs() },
            )
        }.toList()
    }

    private fun categoryTotals(txs: List<BankTransactionEntity>): List<CategoryTotal> {
        // 매출 비율은 들어온 매출(입금) 기준. 관계사에 넘겨준 정산금(매출 차감)까지 빼면 100%를 넘는다
        val groupTotals = txs.groupBy { it.category.group }.mapValues { (group, l) ->
            if (group == BankCategory.Group.REVENUE) l.sumOf { it.deposit } else l.sumOf { it.signedAmount }.abs()
        }
        return txs.groupBy { it.category }.map { (category, list) ->
            val amount = list.sumOf { it.signedAmount }.abs()
            val groupTotal = groupTotals[category.group] ?: BigDecimal.ZERO
            CategoryTotal(category, amount, list.size, ratio(amount, groupTotal))
        }.sortedWith(compareBy<CategoryTotal> { it.category.group.ordinal }.thenByDescending { it.amount })
    }

    /** OTA 입금마다 금액이 똑같은 정산 파일(정산일이 가까운 것)을 찾는다. 파일 하나는 한 번만 짝짓는다 */
    private fun otaDeposits(txs: List<BankTransactionEntity>): List<OtaDeposit> {
        val deposits = txs.filter { it.isDeposit && it.category in OTA_CATEGORIES }
        if (deposits.isEmpty()) return emptyList()
        val files = coverageService.findSettlementFiles()
        val totals = importRowRepository.findByImportBatchIdIn(files.map { it.batch.id!! })
            .groupBy { it.importBatchId }
            .mapValues { (_, rows) -> rows.sumOf { amountOf(jsonMapper.readValue(it.payload, PAYLOAD)["정산금액"]) } }
        val used = mutableSetOf<Long>()
        return deposits.map { tx ->
            val platform = OTA_CATEGORIES.getValue(tx.category)
            val date = tx.transactedAt.toLocalDate()
            val match = files
                .filter { it.batch.source == platform && it.batch.id !in used && totals[it.batch.id]?.compareTo(tx.deposit) == 0 }
                .filter { abs(ChronoUnit.DAYS.between(it.issuedOn, date)) <= MATCH_DAYS }
                .minByOrNull { abs(ChronoUnit.DAYS.between(it.issuedOn, date)) }
            match?.let { used += it.batch.id!! }
            OtaDeposit(tx, platform, match?.let { SettlementMatch(it.batch.id!!, it.batch.fileName, it.account, it.issuedOn) })
        }
    }

    private fun feeComparisons(account: BankAccountEntity, txs: List<BankTransactionEntity>): List<FeeComparison> =
        txs.filter { !it.isDeposit && it.category == BankCategory.MANAGEMENT_FEE }
            .groupBy { feeMonthOf(it) }
            .toSortedMap()
            .map { (month, list) ->
                val fees = account.businessOwnerId?.let { managementFeeService.findMonthly(null, it, month) }?.ifEmpty { null }
                FeeComparison(
                    feeMonth = month,
                    withdrawals = list,
                    withdrawnTotal = list.sumOf { it.withdrawal },
                    calculatedTotal = fees?.sumOf { it.amount },
                    paidTotal = fees?.filter { it.paid }?.sumOf { it.paidAmount!! },
                    unpaidCount = fees?.count { !it.paid && it.amount.signum() > 0 },
                )
            }

    private fun insights(
        months: List<MonthFlow>,
        categories: List<CategoryTotal>,
        ota: List<OtaDeposit>,
        fees: List<FeeComparison>,
        /** 이 달 몫 관계사 정산을 아직 보내지 않았으면 그 달 */
        pendingSettlementMonth: YearMonth?,
    ): List<Insight> {
        val result = mutableListOf<Insight>()
        val active = months.filter { it.transactionCount > 0 }
        val last = active.lastOrNull() ?: return listOf(Insight(Level.INFO, "거래가 없습니다", "이 기간에 올린 거래내역이 없습니다."))
        val prev = active.dropLast(1).lastOrNull()

        val net = last.net
        result += if (net.signum() >= 0) {
            Insight(Level.GOOD, "${label(last.month)} 순현금흐름 +${won(net)}", "입금 ${won(last.deposits)} - 출금 ${won(last.withdrawals)}. 잔액 ${won(last.closingBalance)}")
        } else {
            Insight(Level.WARN, "${label(last.month)} 순현금흐름 ${won(net)}", "출금(${won(last.withdrawals)})이 입금(${won(last.deposits)})보다 많습니다. 잔액 ${won(last.closingBalance)}")
        }

        if (last.revenue.signum() > 0) {
            val ratio = ratio(last.expense, last.revenue)!!
            val level = if (ratio > BigDecimal(80)) Level.WARN else Level.INFO
            result += Insight(level, "매출 대비 운영비 ${pct(ratio)}", "매출성 입금 ${won(last.revenue)}, 운영비 출금 ${won(last.expense)} (영업 현금 마진 ${pct(BigDecimal(100) - ratio)}). 관리비처럼 전달 몫을 이번 달에 내는 비용이 섞여 있습니다.")
            last.byCategory[BankCategory.MANAGEMENT_FEE]?.let {
                result += Insight(Level.INFO, "현장 관리비는 매출의 ${pct(ratio(it, last.revenue)!!)}", "${label(last.month)} 관리비 출금 ${won(it)}.")
            }
        }

        categories.firstOrNull { it.category == BankCategory.AFFILIATE_SETTLEMENT_OUT }?.let { out ->
            val inflow = categories.filter { it.category.group == BankCategory.Group.REVENUE && it.category != BankCategory.AFFILIATE_SETTLEMENT_OUT }.sumOf { it.amount }
            result += Insight(Level.INFO, "관계사 정산 지급 ${won(out.amount)}은 매출에서 뺐습니다",
                "들어온 매출 ${won(inflow)} 중 관계사(선셋코스트·대흥씨엔에스 등) 몫을 넘겨준 금액입니다. 이 계좌 사업자 몫 매출은 ${won(inflow - out.amount)}입니다.")
        }
        pendingSettlementMonth?.let {
            result += Insight(Level.INFO, "${label(it)} 몫 관계사 정산은 아직 보내지 않았습니다",
                "보통 다음 달 초에 보냅니다. 보내기 전까지 ${label(it)} 매출에는 관계사 몫이 포함돼 있습니다.")
        }
        categories.firstOrNull { it.category == BankCategory.AFFILIATE_SETTLEMENT_IN }?.let {
            result += Insight(Level.INFO, "관계사 정산 입금 ${won(it.amount)} 포함", "다른 관계사 계좌로 들어온 여기어때 리조트 정산 중 이 사업자 몫을 받은 금액입니다 (매출로 셈).")
        }
        categories.filter { it.category.group == BankCategory.Group.EXPENSE }.maxByOrNull { it.amount }?.let { top ->
            result += Insight(Level.INFO, "가장 큰 지출: ${top.category.label} (${pct(top.share)})", "기간 합계 ${won(top.amount)}, ${top.count}건.")
        }

        if (prev != null) {
            changeInsight("매출성 입금", last.month, prev.revenue, last.revenue)?.let(result::add)
            changeInsight("운영비", last.month, prev.expense, last.expense, higherIsBad = true)?.let(result::add)
        }

        val avgExpense = active.map { it.expense }.fold(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal(active.size), 0, RoundingMode.HALF_UP)
        if (avgExpense.signum() > 0 && last.closingBalance != null) {
            val months = last.closingBalance.divide(avgExpense, 1, RoundingMode.DOWN)
            val level = if (months < BigDecimal(3)) Level.WARN else Level.GOOD
            result += Insight(level, "잔액으로 운영비 ${months}개월분", "월평균 운영비 ${won(avgExpense)} 기준. 매출이 없을 때 버틸 수 있는 기간입니다.")
        }

        val fixed = setOf(BankCategory.RENT, BankCategory.UTILITIES, BankCategory.LOAN, BankCategory.PAYROLL)
        val fixedAmount = fixed.mapNotNull { last.byCategory[it] }.fold(BigDecimal.ZERO, BigDecimal::add)
        if (fixedAmount.signum() > 0 && last.expense.signum() > 0) {
            result += Insight(Level.INFO, "고정비 ${won(fixedAmount)} (운영비의 ${pct(ratio(fixedAmount, last.expense))})", "임대료·공과금·인건비·대출 원리금. 비수기에도 나가는 돈입니다.")
        }

        val unmatched = ota.filter { it.match == null }
        if (ota.isNotEmpty()) {
            result += if (unmatched.isEmpty()) {
                Insight(Level.GOOD, "OTA 입금 ${ota.size}건 모두 정산 파일과 일치", "입금액과 같은 금액의 정산 파일을 찾았습니다.")
            } else {
                Insight(Level.WARN, "정산 파일과 맞지 않는 OTA 입금 ${unmatched.size}건", "해당 주 정산 파일을 아직 올리지 않았거나 금액이 다릅니다: " +
                    unmatched.take(3).joinToString { "${it.transaction.transactedAt.toLocalDate()} ${it.transaction.description} ${won(it.transaction.deposit)}" })
            }
        }

        fees.filter { it.difference != null && it.difference!!.signum() != 0 }.forEach {
            result += Insight(Level.WARN, "${label(it.feeMonth)} 관리비: 출금과 계산이 ${won(it.difference!!.abs())} 다릅니다",
                "출금 ${won(it.withdrawnTotal)}, 계산 ${won(it.calculatedTotal)}. 관리비 화면에서 예외 건(한달살기 등)이나 빠진 정산 파일을 확인하세요.")
        }
        fees.filter { it.calculatedTotal == null }.forEach {
            result += Insight(Level.INFO, "${label(it.feeMonth)} 관리비는 비교할 데이터가 없습니다",
                "출금 ${won(it.withdrawnTotal)}. 그 달 정산 파일을 올리고 계좌에 사업자가 연결돼 있으면 계산된 관리비와 비교합니다.")
        }
        fees.filter { it.difference?.signum() == 0 && (it.unpaidCount ?: 0) > 0 }.forEach {
            result += Insight(Level.INFO, "${label(it.feeMonth)} 관리비 출금이 계산과 일치", "관리비 화면에서 미입금 ${it.unpaidCount}건을 입금 완료로 표시할 수 있습니다.")
        }

        val uncategorized = categories.filter { it.category in setOf(BankCategory.CARD_OTHER, BankCategory.OTHER_OUT) }.sumOf { it.amount }
        val expenseTotal = categories.filter { it.category.group == BankCategory.Group.EXPENSE }.sumOf { it.amount }
        if (expenseTotal.signum() > 0 && ratio(uncategorized, expenseTotal)!! > BigDecimal(15)) {
            result += Insight(Level.INFO, "분류 안 된 지출 ${pct(ratio(uncategorized, expenseTotal))}", "체크카드 기타·기타 출금 ${won(uncategorized)}. 거래 목록에서 분류를 고치면 같은 거래내용에 다음부터 자동 적용됩니다.")
        }
        return result
    }

    private fun changeInsight(name: String, month: YearMonth, before: BigDecimal, now: BigDecimal, higherIsBad: Boolean = false): Insight? {
        if (before.signum() == 0) return null
        val change = (now - before).multiply(BigDecimal(100)).divide(before, 1, RoundingMode.HALF_UP)
        if (change.abs() < BigDecimal(10)) return null
        val bad = (change.signum() > 0) == higherIsBad
        return Insight(if (bad) Level.WARN else Level.GOOD, "$name 전월 대비 ${if (change.signum() > 0) "+" else ""}${change}%",
            "${label(month.minusMonths(1))} ${won(before)} → ${label(month)} ${won(now)}")
    }

    /** 매출 귀속월: 관계사 정산은 거래내용의 달 ('10월숙박비' → 10월), 그 밖에는 거래일의 달 */
    private fun revenueMonthOf(tx: BankTransactionEntity): YearMonth =
        if (tx.category in AFFILIATE_SETTLEMENT) feeMonthOf(tx) else YearMonth.from(tx.transactedAt)

    /** 거래내용의 'N월' (거래일보다 뒤면 작년). 없으면 거래일의 전달 */
    private fun feeMonthOf(tx: BankTransactionEntity): YearMonth {
        val paidMonth = YearMonth.from(tx.transactedAt)
        val named = FEE_MONTH.find(tx.description)?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it in 1..12 }
            ?: return paidMonth.minusMonths(1)
        val candidate = YearMonth.of(paidMonth.year, named)
        return if (candidate.isAfter(paidMonth)) candidate.minusYears(1) else candidate
    }

    companion object {
        private val OTA_CATEGORIES = mapOf(
            BankCategory.OTA_DDNAYO to OtaPlatform.DDNAYO,
            BankCategory.OTA_YEOGI to OtaPlatform.YEOGI,
            BankCategory.OTA_NOL to OtaPlatform.NOL,
        )
        private const val MATCH_DAYS = 10L
        private const val SETTLEMENT_LAG_MONTHS = 2L
        private val AFFILIATE_SETTLEMENT = setOf(BankCategory.AFFILIATE_SETTLEMENT_IN, BankCategory.AFFILIATE_SETTLEMENT_OUT)
        private val FEE_MONTH = Regex("""(\d{1,2})월""")
        private val PAYLOAD = object : TypeReference<Map<String, String?>>() {}

        private fun amountOf(value: String?): BigDecimal {
            val text = value?.replace(",", "")?.replace("원", "")?.trim().orEmpty()
            return text.toBigDecimalOrNull() ?: BigDecimal.ZERO
        }

        fun ratio(part: BigDecimal, whole: BigDecimal): BigDecimal? =
            if (whole.signum() == 0) null else part.multiply(BigDecimal(100)).divide(whole, 1, RoundingMode.HALF_UP)

        private fun pct(value: BigDecimal?) = value?.let { "${it.stripTrailingZeros().toPlainString()}%" } ?: "-"

        private fun won(value: BigDecimal?): String =
            value?.let { "%,d원".format(it.setScale(0, RoundingMode.HALF_UP).toLong()) } ?: "-"

        private fun label(month: YearMonth) = "${month.year}년 ${month.monthValue}월"
    }
}
