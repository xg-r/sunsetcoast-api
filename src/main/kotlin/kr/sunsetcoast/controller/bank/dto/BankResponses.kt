package kr.sunsetcoast.controller.bank.dto

import io.swagger.v3.oas.annotations.media.Schema
import kr.sunsetcoast.domain.bank.entity.BankAccountEntity
import kr.sunsetcoast.domain.bank.entity.BankCategoryRuleEntity
import kr.sunsetcoast.domain.bank.entity.BankStatementUploadEntity
import kr.sunsetcoast.domain.bank.entity.BankTransactionEntity
import kr.sunsetcoast.domain.bank.service.BankAnalysisService
import kr.sunsetcoast.domain.bank.service.BankStatementImportService
import kr.sunsetcoast.domain.bank.vo.BankCategory
import java.math.BigDecimal

@Schema(description = "계좌")
data class BankAccountResponseBody(
    val id: Long,
    @field:Schema(example = "000-000000-00-000")
    val accountNumber: String,
    @field:Schema(description = "파일 머리글 괄호 안 이름", nullable = true, example = "선셋코스트")
    val holderName: String?,
    @field:Schema(description = "연결된 사업자 (관리비 비교에 씀)", nullable = true, example = "1")
    val businessOwnerId: Long?,
) {
    companion object {
        fun from(e: BankAccountEntity) = BankAccountResponseBody(e.id!!, e.accountNumber, e.holderName, e.businessOwnerId)
    }
}

@Schema(description = "거래내역 업로드")
data class BankUploadResponseBody(
    val id: Long,
    val bankAccountId: Long,
    val fileName: String,
    @field:Schema(description = "첫 거래 일시", nullable = true)
    val periodFrom: String?,
    @field:Schema(description = "마지막 거래 일시", nullable = true)
    val periodTo: String?,
    val totalRows: Int,
    val insertedRows: Int,
    @field:Schema(description = "이미 올린 거래 (기간이 겹친 부분)")
    val duplicateRows: Int,
    val createdAt: String,
) {
    companion object {
        fun from(e: BankStatementUploadEntity) = BankUploadResponseBody(
            e.id!!, e.bankAccountId, e.fileName, e.periodFrom?.toString(), e.periodTo?.toString(),
            e.totalRows, e.insertedRows, e.duplicateRows, e.createdAt.withNano(0).toString(),
        )
    }
}

@Schema(description = "거래내역 파일별 업로드 결과")
data class BankImportResponseBody(
    val fileName: String,
    @field:Schema(description = "IMPORTED 또는 REJECTED", example = "IMPORTED")
    val status: String,
    @field:Schema(nullable = true)
    val message: String?,
    @field:Schema(nullable = true)
    val upload: BankUploadResponseBody?,
    @field:Schema(nullable = true)
    val account: BankAccountResponseBody?,
    @field:Schema(description = "거래후 잔액이 앞 거래와 이어지지 않는 곳 (빠진 거래가 있을 수 있음)")
    val balanceBreaks: List<String>,
) {
    companion object {
        fun from(fileName: String, r: BankStatementImportService.Result) = BankImportResponseBody(
            fileName = fileName,
            status = if (r.upload != null) "IMPORTED" else "REJECTED",
            message = r.rejectedReason,
            upload = r.upload?.let(BankUploadResponseBody::from),
            account = r.account?.let(BankAccountResponseBody::from),
            balanceBreaks = r.balanceBreaks,
        )
    }
}

@Schema(description = "분류")
data class BankCategoryResponseBody(
    @field:Schema(example = "SUPPLIES") val code: String,
    @field:Schema(example = "소모품·비품") val label: String,
    @field:Schema(description = "REVENUE, NON_OPERATING_IN, EXPENSE, FINANCING, TRANSFER", example = "EXPENSE") val group: String,
    @field:Schema(example = "운영비") val groupLabel: String,
) {
    companion object {
        fun from(c: BankCategory) = BankCategoryResponseBody(c.name, c.label, c.group.name, c.group.label)
    }
}

@Schema(description = "거래")
data class BankTransactionResponseBody(
    val id: Long,
    val transactedAt: String,
    val withdrawal: BigDecimal,
    val deposit: BigDecimal,
    val balance: BigDecimal,
    val description: String,
    val counterpartyName: String?,
    val counterpartyBank: String?,
    val transactionType: String?,
    val category: String,
    @field:Schema(description = "AUTO: 자동 분류, MANUAL: 직접 고침", example = "AUTO")
    val categorySource: String,
    val note: String?,
) {
    companion object {
        fun from(e: BankTransactionEntity) = BankTransactionResponseBody(
            e.id!!, e.transactedAt.toString(), e.withdrawal, e.deposit, e.balance, e.description,
            e.counterpartyName, e.counterpartyBank, e.transactionType, e.category.name, e.categorySource.name, e.note,
        )
    }
}

@Schema(description = "관리자 분류 규칙")
data class BankRuleResponseBody(val id: Long, val keyword: String, val direction: String, val category: String) {
    companion object {
        fun from(e: BankCategoryRuleEntity) = BankRuleResponseBody(e.id!!, e.keyword, e.direction.name, e.category.name)
    }
}

@Schema(description = "계좌 분석")
data class BankAnalysisResponseBody(
    val account: BankAccountResponseBody,
    val from: String,
    val to: String,
    val months: List<Month>,
    val categories: List<CategoryTotal>,
    val otaDeposits: List<OtaDeposit>,
    val feeComparisons: List<FeeComparison>,
    val insights: List<Insight>,
) {
    data class Month(
        val month: String,
        val openingBalance: BigDecimal?,
        val closingBalance: BigDecimal?,
        val deposits: BigDecimal,
        val withdrawals: BigDecimal,
        val net: BigDecimal,
        @field:Schema(description = "매출성 입금 (분류 그룹 REVENUE)") val revenue: BigDecimal,
        @field:Schema(description = "운영비 (분류 그룹 EXPENSE)") val expense: BigDecimal,
        val transactionCount: Int,
        @field:Schema(description = "분류 코드 → 금액") val byCategory: Map<String, BigDecimal>,
    )

    data class CategoryTotal(
        val category: String,
        val amount: BigDecimal,
        val count: Int,
        @field:Schema(description = "같은 그룹 합계 대비 비율 (%)", nullable = true) val share: BigDecimal?,
    )

    data class OtaDeposit(
        val transactionId: Long,
        val transactedAt: String,
        val amount: BigDecimal,
        val description: String,
        val platform: String,
        @field:Schema(description = "같은 금액의 정산 파일. 없으면 null", nullable = true) val importBatchId: Long?,
        val fileName: String?,
        val settlementAccount: String?,
        val issuedOn: String?,
    )

    data class FeeComparison(
        @field:Schema(description = "관리비 입실 월", example = "2026-10") val feeMonth: String,
        val withdrawnTotal: BigDecimal,
        @field:Schema(description = "계좌 사업자의 그 달 관리비 합계 (사업자 미연결이면 null)", nullable = true) val calculatedTotal: BigDecimal?,
        @field:Schema(description = "출금 - 계산", nullable = true) val difference: BigDecimal?,
        val paidTotal: BigDecimal?,
        val unpaidCount: Int?,
        val withdrawals: List<BankTransactionResponseBody>,
    )

    data class Insight(
        @field:Schema(description = "GOOD, INFO, WARN", example = "INFO") val level: String,
        val title: String,
        val detail: String,
    )

    companion object {
        fun from(a: BankAnalysisService.Analysis) = BankAnalysisResponseBody(
            account = BankAccountResponseBody.from(a.account),
            from = a.from.toString(),
            to = a.to.toString(),
            months = a.months.map {
                Month(
                    it.month.toString(), it.openingBalance, it.closingBalance, it.deposits, it.withdrawals, it.net,
                    it.revenue, it.expense, it.transactionCount, it.byCategory.mapKeys { (k, _) -> k.name },
                )
            },
            categories = a.categories.map { CategoryTotal(it.category.name, it.amount, it.count, it.share) },
            otaDeposits = a.otaDeposits.map {
                OtaDeposit(
                    it.transaction.id!!, it.transaction.transactedAt.toString(), it.transaction.deposit, it.transaction.description,
                    it.platform.name, it.match?.importBatchId, it.match?.fileName, it.match?.account, it.match?.issuedOn?.toString(),
                )
            },
            feeComparisons = a.feeComparisons.map {
                FeeComparison(
                    it.feeMonth.toString(), it.withdrawnTotal, it.calculatedTotal, it.difference, it.paidTotal, it.unpaidCount,
                    it.withdrawals.map(BankTransactionResponseBody::from),
                )
            },
            insights = a.insights.map { Insight(it.level.name, it.title, it.detail) },
        )
    }
}

@Schema(description = "거래 분류 변경")
data class BankRecategorizeRequestBody(
    @field:Schema(example = "SUPPLIES") val category: String,
    @field:Schema(description = "true면 같은 거래내용(같은 입출금 방향)에 규칙을 만들어 자동 분류된 거래를 모두 다시 분류") val applyToSimilar: Boolean = false,
    @field:Schema(nullable = true) val note: String? = null,
)

@Schema(description = "계좌 사업자 지정")
data class BankAccountOwnerRequestBody(@field:Schema(nullable = true, example = "1") val businessOwnerId: Long?)
