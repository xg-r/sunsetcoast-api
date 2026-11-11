package kr.sunsetcoast.domain.settlement.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kr.sunsetcoast.domain.importbatch.entity.ImportBatchEntity
import kr.sunsetcoast.domain.importbatch.entity.ImportRowEntity
import kr.sunsetcoast.domain.importbatch.repository.ImportBatchRepository
import kr.sunsetcoast.domain.importbatch.repository.ImportRowRepository
import kr.sunsetcoast.domain.importbatch.vo.FileKind
import kr.sunsetcoast.domain.importbatch.vo.ImportRowResult
import kr.sunsetcoast.domain.reservation.entity.ReservationEntity
import kr.sunsetcoast.domain.reservation.repository.ReservationRepository
import kr.sunsetcoast.domain.reservation.vo.AmountStatus
import kr.sunsetcoast.domain.reservation.vo.OtaPlatform
import kr.sunsetcoast.domain.reservation.vo.ReservationStatus
import kr.sunsetcoast.domain.reservation.vo.SalesChannel
import kr.sunsetcoast.domain.room.entity.RoomEntity
import kr.sunsetcoast.domain.room.repository.RoomAliasRepository
import kr.sunsetcoast.domain.settlement.entity.SettlementLineEntity
import kr.sunsetcoast.domain.settlement.entity.SettlementLineRevisionEntity
import kr.sunsetcoast.domain.settlement.parser.ParsedRow
import kr.sunsetcoast.domain.settlement.parser.ParsedSettlementLine
import kr.sunsetcoast.domain.settlement.parser.SettlementFileParsers
import kr.sunsetcoast.domain.settlement.repository.SettlementLineRepository
import kr.sunsetcoast.domain.settlement.repository.SettlementLineRevisionRepository
import kr.sunsetcoast.domain.settlement.vo.RowIssue
import kr.sunsetcoast.domain.settlement.vo.SettlementImportResult
import kr.sunsetcoast.domain.settlement.vo.SettlementLineType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.interceptor.TransactionAspectSupport
import tools.jackson.databind.json.JsonMapper
import java.security.MessageDigest
import java.time.LocalDate

private val logger = KotlinLogging.logger {}

/**
 * 정산 파일 업로드.
 *
 * 중복 방지:
 * 1. 같은 파일(바이트 해시)은 거부
 * 2. 행 단위 자연키 (예약, 행 유형, 날짜, seq) 로 업서트 - 같은 값이면 DUPLICATE, 다르면 CHANGED(이전 값 보존)
 *
 * dryRun이면 같은 처리를 한 뒤 트랜잭션을 롤백해서 결과만 돌려준다 (미리보기).
 */
@Service
class SettlementImportService(
    private val parsers: SettlementFileParsers,
    private val roomAliasRepository: RoomAliasRepository,
    private val reservationRepository: ReservationRepository,
    private val settlementLineRepository: SettlementLineRepository,
    private val revisionRepository: SettlementLineRevisionRepository,
    private val importBatchRepository: ImportBatchRepository,
    private val importRowRepository: ImportRowRepository,
    private val jsonMapper: JsonMapper,
) {
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
        val context = ImportContext(parsed.platform, batch.id!!)
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

        context.touchedReservations.values.forEach { refreshStay(it, context.explicitStays[it.id]) }

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

    private fun importRow(row: ParsedRow, context: ImportContext): Pair<ImportRowResult, String?> {
        if (row.error != null) return ImportRowResult.FAILED to row.error
        if (row.lines.isEmpty()) return ImportRowResult.SKIPPED to null

        var rowResult: ImportRowResult? = null
        var message: String? = null

        for (line in row.lines) {
            val room = context.rooms.getOrPut(line.roomName) {
                roomAliasRepository.findBySourceAndExternalName(context.platform, line.roomName)?.room
            } ?: return ImportRowResult.UNMAPPED to "객실 매핑이 없습니다: ${line.roomName}"

            val reservation = findOrCreateReservation(line, room, context.platform)
            if (reservation.room.id != room.id) {
                return ImportRowResult.FAILED to
                    "예약 ${line.reservationNumber}의 객실이 기존(${reservation.room.name})과 다릅니다: ${room.name}"
            }
            reservation.fillMissing(line.salesChannelRaw, line.bookerName, line.reservedAt)
            context.touchedReservations[reservation.id!!] = reservation
            if (line.checkInDate != null) {
                context.explicitStays[reservation.id] = line.checkInDate to line.nights
            }

            val (lineResult, lineMessage) = upsertLine(reservation, line, context)
            rowResult = rowResult?.mergeWith(lineResult) ?: lineResult
            message = lineMessage ?: message
        }
        return rowResult!! to message
    }

    private fun findOrCreateReservation(
        line: ParsedSettlementLine,
        room: RoomEntity,
        platform: OtaPlatform,
    ): ReservationEntity =
        reservationRepository.findBySourceAndReservationNumber(platform, line.reservationNumber)
            ?: reservationRepository.save(
                ReservationEntity(
                    source = platform,
                    reservationNumber = line.reservationNumber,
                    room = room,
                    salesChannel = SalesChannel.fromRaw(line.salesChannelRaw),
                    salesChannelRaw = line.salesChannelRaw,
                    bookerName = line.bookerName,
                    reservedAt = line.reservedAt,
                    checkInDate = line.checkInDate,
                    nights = line.nights,
                    status = ReservationStatus.CONFIRMED,
                    amountStatus = AmountStatus.CONFIRMED,
                )
            )

    private fun upsertLine(
        reservation: ReservationEntity,
        line: ParsedSettlementLine,
        context: ImportContext,
    ): Pair<ImportRowResult, String?> {
        // 같은 파일 안에서 같은 키가 반복되면(예: NOL 취소수수료 2건) 순번으로 구분한다.
        // 같은 파일을 다시 올리면 같은 순번이 나오므로 중복으로 판정된다.
        val key = LineKey(reservation.id!!, line.lineType, line.stayDate)
        val seq = context.seqCounter.merge(key, 1, Int::plus)!! - 1

        val existing = settlementLineRepository.findByReservationIdAndLineTypeAndStayDateAndSeq(
            reservation.id, line.lineType, line.stayDate, seq,
        )
        if (existing == null) {
            settlementLineRepository.save(
                SettlementLineEntity.create(
                    reservation = reservation,
                    importBatchId = context.batchId,
                    lineType = line.lineType,
                    stayDate = line.stayDate,
                    seq = seq,
                    settledOn = line.settledOn,
                    amounts = line.amounts,
                    splitEstimated = line.splitEstimated,
                )
            )
            return ImportRowResult.INSERTED to null
        }

        val previous = existing.amounts()
        val current = line.amounts.normalized()
        if (previous == current) return ImportRowResult.DUPLICATE to null

        // TODO: 정산서 마감 기능이 생기면, 마감된 정산서에 포함된 행은 수정하지 않고 조정 행을 만든다.
        revisionRepository.save(
            SettlementLineRevisionEntity(
                settlementLineId = existing.id!!,
                importBatchId = context.batchId,
                previousValues = jsonMapper.writeValueAsString(previous),
            )
        )
        existing.update(current, line.settledOn, line.splitEstimated, context.batchId)
        return ImportRowResult.CHANGED to
            "${line.reservationNumber} ${line.stayDate} ${line.lineType} 금액 변경: 정산금액 ${previous.net} -> ${current.net}"
    }

    /**
     * 정산 행으로 입실일/박수/상태를 다시 계산한다.
     * 여기어때/NOL은 연박이 여러 주에 걸쳐 정산되므로 지금까지 들어온 1박 행을 모두 본다.
     */
    private fun refreshStay(reservation: ReservationEntity, explicitStay: Pair<LocalDate, Int?>?) {
        val lines = settlementLineRepository.findByReservationId(reservation.id!!)
        val reversed = lines.filter { it.lineType == SettlementLineType.ROOM_NIGHT_REVERSAL }.map { it.stayDate }.toSet()
        val stayDates = lines.filter { it.lineType == SettlementLineType.ROOM_NIGHT }.map { it.stayDate }.toSet() - reversed
        val status = if (stayDates.isEmpty()) ReservationStatus.CANCELLED else ReservationStatus.CONFIRMED

        when {
            explicitStay != null -> reservation.applyStay(explicitStay.first, explicitStay.second, status)
            stayDates.isNotEmpty() -> reservation.applyStay(stayDates.min(), stayDates.size, status)
            else -> reservation.applyStay(null, null, status)
        }
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private data class LineKey(val reservationId: Long, val lineType: SettlementLineType, val stayDate: LocalDate)

    private class ImportContext(
        val platform: OtaPlatform,
        val batchId: Long,
    ) {
        val rooms = HashMap<String, RoomEntity?>()
        val seqCounter = HashMap<LineKey, Int>()
        val touchedReservations = LinkedHashMap<Long, ReservationEntity>()
        val explicitStays = HashMap<Long, Pair<LocalDate, Int?>>()
    }
}
