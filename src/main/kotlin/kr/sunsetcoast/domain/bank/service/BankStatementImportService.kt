package kr.sunsetcoast.domain.bank.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kr.sunsetcoast.domain.bank.entity.BankAccountEntity
import kr.sunsetcoast.domain.bank.entity.BankStatementUploadEntity
import kr.sunsetcoast.domain.bank.entity.BankTransactionEntity
import kr.sunsetcoast.domain.bank.repository.BankAccountRepository
import kr.sunsetcoast.domain.bank.repository.BankStatementUploadRepository
import kr.sunsetcoast.domain.bank.repository.BankTransactionRepository
import kr.sunsetcoast.domain.bank.vo.BankCategorizer
import kr.sunsetcoast.domain.bank.vo.BankStatementParser
import kr.sunsetcoast.domain.businessowner.repository.BusinessOwnerRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest

private val logger = KotlinLogging.logger {}

/**
 * 계좌 거래내역 업로드. 같은 파일은 거부하고, 기간이 겹친 거래는 (계좌, 일시, 출금, 입금, 잔액)이 같으면 건너뛴다.
 */
@Service
class BankStatementImportService(
    private val accountRepository: BankAccountRepository,
    private val uploadRepository: BankStatementUploadRepository,
    private val transactionRepository: BankTransactionRepository,
    private val businessOwnerRepository: BusinessOwnerRepository,
    private val categorizerFactory: BankCategorizerFactory,
) {
    data class Result(
        val upload: BankStatementUploadEntity?,
        val account: BankAccountEntity?,
        val rejectedReason: String?,
        val balanceBreaks: List<String>,
    )

    @Transactional(readOnly = true)
    fun findUploads(): List<BankStatementUploadEntity> = uploadRepository.findTop100ByOrderByIdDesc()

    @Transactional(readOnly = true)
    fun findAccounts(): List<BankAccountEntity> = accountRepository.findAll()

    /**
     * @param businessOwnerId 업로드할 때 고른 사업자. 이 사업자의 계좌로 넣는다.
     *   파일에 계좌번호가 있으면 그 번호의 계좌를 찾거나 만들고, 다른 사업자 계좌로 등록돼 있거나
     *   머리글 이름이 다른 사업자면 잘못 고른 것으로 보고 거부한다.
     *   계좌번호 줄이 빈 파일은 그 사업자의 계좌(하나뿐일 때)에 넣는다.
     */
    @Transactional
    fun import(fileName: String, bytes: ByteArray, businessOwnerId: Long): Result {
        val owner = businessOwnerRepository.findById(businessOwnerId).orElse(null)
            ?: return Result(null, null, "사업자를 찾을 수 없습니다: $businessOwnerId", emptyList())
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        uploadRepository.findByFileHash(hash)?.let {
            return Result(null, null, "이미 올린 파일입니다. (업로드 #${it.id}, ${it.createdAt.withNano(0)}, ${it.fileName})", emptyList())
        }
        val statement = try {
            BankStatementParser.parse(bytes)
        } catch (e: IllegalArgumentException) {
            return Result(null, null, e.message ?: "파일을 읽을 수 없습니다.", emptyList())
        }

        val account = try {
            findOrCreateAccount(statement, owner.id!!, owner.name)
        } catch (e: IllegalArgumentException) {
            return Result(null, null, e.message, emptyList())
        }
        val upload = uploadRepository.save(
            BankStatementUploadEntity(
                bankAccountId = account.id!!,
                fileName = fileName,
                fileHash = hash,
                periodFrom = statement.transactions.first().transactedAt,
                periodTo = statement.transactions.last().transactedAt,
            )
        )

        val categorizer = categorizerFactory.create()
        statement.transactions.forEach { row ->
            upload.totalRows++
            val exists = transactionRepository.existsByBankAccountIdAndTransactedAtAndWithdrawalAndDepositAndBalance(
                account.id, row.transactedAt, row.withdrawal.setScale(2), row.deposit.setScale(2), row.balance.setScale(2),
            )
            if (exists) {
                upload.duplicateRows++
                return@forEach
            }
            val category = categorizer.categorize(
                BankCategorizer.Input(row.description, row.counterpartyName, row.transactionType, row.deposit.signum() > 0),
            )
            transactionRepository.save(
                BankTransactionEntity(
                    bankAccountId = account.id,
                    uploadId = upload.id!!,
                    transactedAt = row.transactedAt,
                    withdrawal = row.withdrawal,
                    deposit = row.deposit,
                    balance = row.balance,
                    description = row.description.take(255),
                    counterpartyAccount = row.counterpartyAccount?.take(100),
                    counterpartyBank = row.counterpartyBank?.take(50),
                    counterpartyName = row.counterpartyName?.take(100),
                    memo = row.memo?.take(255),
                    transactionType = row.transactionType?.take(50),
                    category = category,
                )
            )
            upload.insertedRows++
        }
        logger.info {
            "bank statement imported file=$fileName account=${account.id} total=${upload.totalRows} " +
                "inserted=${upload.insertedRows} duplicate=${upload.duplicateRows} breaks=${statement.balanceBreaks.size}"
        }
        return Result(upload, account, null, statement.balanceBreaks)
    }

    /** 잘못 올린 거래내역 삭제. 그 업로드로 새로 들어온 거래만 지운다 */
    @Transactional
    fun deleteUpload(uploadId: Long) {
        val upload = uploadRepository.findById(uploadId).orElseThrow { IllegalArgumentException("업로드 기록이 없습니다: $uploadId") }
        transactionRepository.deleteAll(transactionRepository.findByUploadId(uploadId))
        uploadRepository.delete(upload)
        logger.info { "bank statement upload deleted id=$uploadId file=${upload.fileName}" }
    }

    @Transactional
    fun setAccountOwner(accountId: Long, businessOwnerId: Long?) {
        val account = accountRepository.findById(accountId).orElseThrow { IllegalArgumentException("계좌가 없습니다: $accountId") }
        require(businessOwnerId == null || businessOwnerRepository.existsById(businessOwnerId)) { "사업자가 없습니다: $businessOwnerId" }
        account.businessOwnerId = businessOwnerId
    }

    private fun findOrCreateAccount(statement: BankStatementParser.Statement, ownerId: Long, ownerName: String): BankAccountEntity {
        listOfNotNull(statement.depositorName, statement.holderName).forEach { name ->
            val fileOwner = ownerIdByName(name)
            require(fileOwner == null || fileOwner == ownerId) {
                "파일의 계좌는 '$name' 계좌인데 '$ownerName'을(를) 골랐습니다. 사업자를 확인해 주세요."
            }
        }
        val ownerAccounts = accountRepository.findAll().filter { it.businessOwnerId == ownerId }
        val number = statement.accountNumber
        if (number == null) {
            require(ownerAccounts.size <= 1) {
                "파일에 계좌번호가 없고 '$ownerName' 계좌가 ${ownerAccounts.size}개라 어느 계좌인지 알 수 없습니다."
            }
            return ownerAccounts.firstOrNull()
                ?: accountRepository.save(BankAccountEntity(accountNumber = "$NO_NUMBER$ownerName", holderName = ownerName, businessOwnerId = ownerId))
        }
        accountRepository.findByAccountNumber(number)?.let { existing ->
            require(existing.businessOwnerId == null || existing.businessOwnerId == ownerId) {
                "계좌 $number 은(는) 다른 사업자 계좌로 등록돼 있습니다. 사업자를 확인해 주세요."
            }
            existing.businessOwnerId = ownerId
            return existing
        }
        // 계좌번호 없이 먼저 만든 계좌가 있으면 번호를 채운다
        ownerAccounts.singleOrNull { it.accountNumber.startsWith(NO_NUMBER) }?.let {
            it.accountNumber = number
            statement.holderName?.let { holder -> it.holderName = holder }
            return it
        }
        return accountRepository.save(
            BankAccountEntity(accountNumber = number, holderName = statement.holderName ?: ownerName, businessOwnerId = ownerId),
        )
    }

    private fun ownerIdByName(name: String): Long? {
        val key = name.replace(" ", "").replace("(주)", "")
        return businessOwnerRepository.findAll().firstOrNull { it.name.replace(" ", "").replace("(주)", "") == key }?.id
    }
}

/** 계좌번호 없이 이름으로 만든 계좌의 번호 자리 */
private const val NO_NUMBER = "번호없음:"
