package kr.sunsetcoast.domain.settlement.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kr.sunsetcoast.domain.importbatch.entity.ImportBatchEntity
import kr.sunsetcoast.domain.importbatch.entity.ImportRowEntity
import kr.sunsetcoast.domain.importbatch.repository.ImportBatchRepository
import kr.sunsetcoast.domain.importbatch.repository.ImportRowRepository
import kr.sunsetcoast.domain.importbatch.vo.FileKind
import kr.sunsetcoast.domain.importbatch.vo.ImportRowResult
import kr.sunsetcoast.domain.settlement.parser.ParsedRow
import kr.sunsetcoast.domain.settlement.parser.SettlementFileParsers
import kr.sunsetcoast.domain.settlement.vo.RowIssue
import kr.sunsetcoast.domain.settlement.vo.SettlementImportResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.interceptor.TransactionAspectSupport
import tools.jackson.databind.json.JsonMapper
import java.security.MessageDigest

private val logger = KotlinLogging.logger {}

/**
 * 정산 파일 업로드.
 *
 * 중복 방지:
 * 1. 같은 파일(바이트 해시)은 거부
 * 2. 행 단위 자연키 (예약, 객실, 행 유형, 날짜, seq) 로 업서트 - 같은 값이면 DUPLICATE, 다르면 CHANGED(이전 값 보존)
 *
 * dryRun이면 같은 처리를 한 뒤 트랜잭션을 롤백해서 결과만 돌려준다 (미리보기).
 */
@Service
class SettlementImportService(
    private val parsers: SettlementFileParsers,
    private val writer: SettlementLineWriter,
    private val importBatchRepository: ImportBatchRepository,
    private val importRowRepository: ImportRowRepository,
    private val jsonMapper: JsonMapper,
) {
    @Transactional(readOnly = true)
    fun findRecentBatches(): List<ImportBatchEntity> = importBatchRepository.findTop200ByOrderByIdDesc()

    @Transactional
    fun import(fileName: String, bytes: ByteArray, dryRun: Boolean): SettlementImportResult {
        val fileHash = sha256(bytes)
        importBatchRepository.findByFileHash(fileHash)?.let {
            return SettlementImportResult.rejected(
                fileName,
                "이미 업로드된 파일입니다. (업로드 #${it.id}, ${it.createdAt}, 파일명: ${it.fileName})",
            )
        }

        val parsed = try {
            parsers.parse(bytes)
        } catch (e: IllegalArgumentException) {
            return SettlementImportResult.rejected(fileName, e.message ?: "파일을 읽을 수 없습니다.")
        }

        val batch = importBatchRepository.save(
            ImportBatchEntity(
                source = parsed.platform,
                fileKind = FileKind.SETTLEMENT,
                fileName = fileName,
                fileHash = fileHash,
            )
        )
        val context = writer.newContext(parsed.platform, batch.id!!)
        val issues = mutableListOf<RowIssue>()

        for (row in parsed.rows) {
            val (result, message) = importRow(row, context)
            batch.count(result)
            importRowRepository.save(
                ImportRowEntity(
                    importBatchId = batch.id,
                    rowNo = row.rowNo,
                    result = result,
                    message = message?.take(500),
                    payload = jsonMapper.writeValueAsString(row.payload),
                )
            )
            if (result.needsAttention) issues += RowIssue(row.rowNo, result, message)
        }

        writer.finish(context)

        logger.info {
            "settlement import file=$fileName platform=${parsed.platform} dryRun=$dryRun total=${batch.totalRows} " +
                "inserted=${batch.insertedRows} duplicate=${batch.duplicateRows} changed=${batch.changedRows} " +
                "unmapped=${batch.unmappedRows} failed=${batch.failedRows}"
        }

        if (dryRun) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()
        }
        return SettlementImportResult.of(batch, dryRun, issues)
    }

    private fun importRow(row: ParsedRow, context: SettlementLineWriter.WriteContext): Pair<ImportRowResult, String?> {
        if (row.error != null) return ImportRowResult.FAILED to row.error
        if (row.lines.isEmpty()) return ImportRowResult.SKIPPED to null

        return when (val match = writer.matchRooms(row.lines.first(), context)) {
            is SettlementLineWriter.RoomMatch.NotFound -> ImportRowResult.UNMAPPED to match.message
            is SettlementLineWriter.RoomMatch.Found -> writer.write(row.lines, match.rooms, context)
        }
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
