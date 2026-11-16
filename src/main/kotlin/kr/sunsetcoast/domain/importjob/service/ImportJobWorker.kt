package kr.sunsetcoast.domain.importjob.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kr.sunsetcoast.domain.bank.service.BankStatementImportService
import kr.sunsetcoast.domain.importjob.vo.BankJobResult
import kr.sunsetcoast.domain.importjob.vo.ImportJobKind
import kr.sunsetcoast.domain.settlement.service.SettlementImportService
import kr.sunsetcoast.domain.settlement.vo.ImportStatus
import jakarta.annotation.PreDestroy
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private val logger = KotlinLogging.logger {}

/**
 * 업로드 작업을 백그라운드에서 처리한다. 실행기는 스레드 하나라 작업이 한 번에 하나씩, 들어온 순서대로 돈다.
 * 실패하면(파일 거부·예외) 요청한 사용자에게 알림을 남긴다.
 */
@Component
class ImportJobWorker(
    private val jobService: ImportJobService,
    private val settlementImportService: SettlementImportService,
    private val bankImportService: BankStatementImportService,
    private val jsonMapper: JsonMapper,
) {
    /**
     * 스레드 하나: 같은 예약·관리비를 동시에 고치지 않도록 순서대로 처리한다.
     * 빈으로 등록하지 않는다 (Executor 빈이 있으면 스프링 부트 기본 실행기가 빠진다)
     */
    private val executor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "import-job").apply { isDaemon = true }
    }

    fun submit(jobIds: List<Long>) = jobIds.forEach { id -> executor.execute { process(id) } }

    /** 처리 중인 작업은 트랜잭션이 롤백되고, 다음에 서버가 뜰 때 다시 처리한다 */
    @PreDestroy
    fun shutdown() {
        executor.shutdownNow()
    }

    @EventListener(ApplicationReadyEvent::class)
    fun resumeOnStartup() {
        val ids = jobService.recoverUnfinished()
        if (ids.isNotEmpty()) logger.info { "import jobs resumed after restart ids=$ids" }
        submit(ids)
    }

    fun process(id: Long) {
        val work = jobService.start(id) ?: return
        try {
            when (work.kind) {
                ImportJobKind.SETTLEMENT -> {
                    val result = settlementImportService.import(work.fileName, work.bytes, dryRun = false)
                    val json = jsonMapper.writeValueAsString(result)
                    if (result.status == ImportStatus.REJECTED) {
                        jobService.fail(id, result.message ?: "파일을 처리하지 못했습니다.", json)
                    } else {
                        val warning = if (result.failedRows > 0) {
                            "실패 ${result.failedRows}행: " + result.issues.filter { it.result.name == "FAILED" }.take(3)
                                .joinToString(" / ") { "${it.rowNo}행 ${it.message.orEmpty()}" }
                        } else {
                            null
                        }
                        jobService.succeed(id, json, warning)
                    }
                }
                ImportJobKind.BANK -> {
                    val ownerId = work.businessOwnerId ?: throw IllegalArgumentException("어느 사업자의 계좌인지 정해지지 않았습니다.")
                    val result = bankImportService.import(work.fileName, work.bytes, ownerId)
                    val upload = result.upload
                    if (upload == null) {
                        jobService.fail(id, result.rejectedReason ?: "파일을 처리하지 못했습니다.")
                    } else {
                        val json = jsonMapper.writeValueAsString(
                            BankJobResult(upload.id!!, upload.bankAccountId, upload.totalRows, upload.insertedRows, upload.duplicateRows, result.balanceBreaks),
                        )
                        val warning = result.balanceBreaks.takeIf { it.isNotEmpty() }
                            ?.let { "잔액이 이어지지 않는 곳 ${it.size}곳 (빠진 거래가 있을 수 있음): ${it.first()}" }
                        jobService.succeed(id, json, warning)
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "import job failed id=$id file=${work.fileName}" }
            runCatching { jobService.fail(id, "처리 중 오류가 났습니다: ${e.message ?: e::class.simpleName}") }
                .onFailure { logger.error(it) { "import job failure could not be recorded id=$id" } }
        }
    }
}
