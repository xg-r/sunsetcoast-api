package kr.sunsetcoast.domain.bank.repository

import kr.sunsetcoast.domain.bank.entity.BankAccountEntity
import kr.sunsetcoast.domain.bank.entity.BankCategoryRuleEntity
import kr.sunsetcoast.domain.bank.entity.BankStatementUploadEntity
import kr.sunsetcoast.domain.bank.entity.BankTransactionEntity
import kr.sunsetcoast.domain.bank.vo.BankCategorizer
import org.springframework.data.jpa.repository.JpaRepository
import java.math.BigDecimal
import java.time.LocalDateTime

interface BankAccountRepository : JpaRepository<BankAccountEntity, Long> {
    fun findByAccountNumber(accountNumber: String): BankAccountEntity?
}

interface BankStatementUploadRepository : JpaRepository<BankStatementUploadEntity, Long> {
    fun findByFileHash(fileHash: String): BankStatementUploadEntity?

    fun findTop100ByOrderByIdDesc(): List<BankStatementUploadEntity>
}

interface BankTransactionRepository : JpaRepository<BankTransactionEntity, Long> {
    fun existsByBankAccountIdAndTransactedAtAndWithdrawalAndDepositAndBalance(
        bankAccountId: Long,
        transactedAt: LocalDateTime,
        withdrawal: BigDecimal,
        deposit: BigDecimal,
        balance: BigDecimal,
    ): Boolean

    fun findByBankAccountIdAndTransactedAtBetweenOrderByTransactedAtAscIdAsc(
        bankAccountId: Long,
        from: LocalDateTime,
        to: LocalDateTime,
    ): List<BankTransactionEntity>

    /** 기간 시작 전 마지막 거래 (기초 잔액) */
    fun findFirstByBankAccountIdAndTransactedAtBeforeOrderByTransactedAtDescIdDesc(
        bankAccountId: Long,
        before: LocalDateTime,
    ): BankTransactionEntity?

    fun findByCategorySource(source: BankTransactionEntity.CategorySource): List<BankTransactionEntity>

    fun findByUploadId(uploadId: Long): List<BankTransactionEntity>
}

interface BankCategoryRuleRepository : JpaRepository<BankCategoryRuleEntity, Long> {
    fun findByKeywordAndDirection(keyword: String, direction: BankCategorizer.Direction): BankCategoryRuleEntity?
}
