package kr.sunsetcoast.domain.bank.service

import kr.sunsetcoast.domain.bank.entity.BankCategoryRuleEntity
import kr.sunsetcoast.domain.bank.entity.BankTransactionEntity
import kr.sunsetcoast.domain.bank.repository.BankCategoryRuleRepository
import kr.sunsetcoast.domain.bank.repository.BankTransactionRepository
import kr.sunsetcoast.domain.bank.vo.BankCategorizer
import kr.sunsetcoast.domain.bank.vo.BankCategory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

@Service
class BankTransactionService(
    private val transactionRepository: BankTransactionRepository,
    private val ruleRepository: BankCategoryRuleRepository,
    private val categorizerFactory: BankCategorizerFactory,
) {
    @Transactional(readOnly = true)
    fun findMonthly(accountId: Long, month: YearMonth): List<BankTransactionEntity> =
        transactionRepository.findByBankAccountIdAndTransactedAtBetweenOrderByTransactedAtAscIdAsc(
            accountId, month.atDay(1).atStartOfDay(), month.atEndOfMonth().atTime(23, 59, 59),
        )

    /**
     * 거래 분류를 고친다. applyToSimilar면 같은 거래내용(같은 방향)에 관리자 규칙을 만들고,
     * 자동 분류된 거래를 모두 다시 분류한다. 직접 고친 거래는 그대로 둔다.
     * @return 분류가 바뀐 거래 수
     */
    @Transactional
    fun recategorize(transactionId: Long, category: BankCategory, applyToSimilar: Boolean, note: String?): Int {
        val tx = transactionRepository.findById(transactionId).orElseThrow { IllegalArgumentException("거래가 없습니다: $transactionId") }
        note?.let { tx.note = it.trim().ifEmpty { null }?.take(255) }
        if (!applyToSimilar) {
            tx.categorizeManually(category)
            return 1
        }
        val keyword = tx.description.trim()
        require(keyword.isNotEmpty()) { "거래내용이 비어 있어 규칙을 만들 수 없습니다." }
        val direction = if (tx.isDeposit) BankCategorizer.Direction.IN else BankCategorizer.Direction.OUT
        val rule = ruleRepository.findByKeywordAndDirection(keyword, direction)
        if (rule != null) rule.category = category
        else ruleRepository.save(BankCategoryRuleEntity(keyword = keyword, direction = direction, category = category))
        ruleRepository.flush()
        tx.autoCategorize(category)
        return recategorizeAuto()
    }

    @Transactional(readOnly = true)
    fun findRules(): List<BankCategoryRuleEntity> = ruleRepository.findAll().sortedBy { it.keyword }

    @Transactional
    fun deleteRule(id: Long): Int {
        require(ruleRepository.existsById(id)) { "규칙이 없습니다: $id" }
        ruleRepository.deleteById(id)
        ruleRepository.flush()
        return recategorizeAuto()
    }

    /** 자동 분류된 거래를 지금 규칙으로 다시 분류한다 */
    private fun recategorizeAuto(): Int {
        val categorizer = categorizerFactory.create()
        var changed = 0
        transactionRepository.findByCategorySource(BankTransactionEntity.CategorySource.AUTO).forEach { tx ->
            val category = categorizer.categorize(
                BankCategorizer.Input(tx.description, tx.counterpartyName, tx.transactionType, tx.isDeposit),
            )
            if (category != tx.category) {
                tx.autoCategorize(category)
                changed++
            }
        }
        return changed
    }
}
