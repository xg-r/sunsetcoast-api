package kr.sunsetcoast.domain.bank.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import kr.sunsetcoast.domain.bank.vo.BankCategory
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "bank_transactions")
class BankTransactionEntity(
    @Column(name = "bank_account_id", nullable = false)
    val bankAccountId: Long,

    @Column(name = "upload_id", nullable = false)
    val uploadId: Long,

    @Column(name = "transacted_at", nullable = false)
    val transactedAt: LocalDateTime,

    @Column(nullable = false, precision = 15, scale = 2)
    val withdrawal: BigDecimal,

    @Column(nullable = false, precision = 15, scale = 2)
    val deposit: BigDecimal,

    @Column(nullable = false, precision = 15, scale = 2)
    val balance: BigDecimal,

    @Column(nullable = false)
    val description: String,

    @Column(name = "counterparty_account", length = 100)
    val counterpartyAccount: String?,

    @Column(name = "counterparty_bank", length = 50)
    val counterpartyBank: String?,

    @Column(name = "counterparty_name", length = 100)
    val counterpartyName: String?,

    val memo: String?,

    @Column(name = "transaction_type", length = 50)
    val transactionType: String?,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var category: BankCategory,

    @Enumerated(EnumType.STRING)
    @Column(name = "category_source", nullable = false, length = 10)
    var categorySource: CategorySource = CategorySource.AUTO,

    /** 관리자 메모 */
    var note: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) {
    enum class CategorySource { AUTO, MANUAL }

    val isDeposit: Boolean get() = deposit.signum() > 0

    /** 입금은 +, 출금은 - */
    val signedAmount: BigDecimal get() = deposit - withdrawal

    fun autoCategorize(category: BankCategory) {
        if (categorySource == CategorySource.AUTO) this.category = category
    }

    fun categorizeManually(category: BankCategory) {
        this.category = category
        categorySource = CategorySource.MANUAL
    }
}
