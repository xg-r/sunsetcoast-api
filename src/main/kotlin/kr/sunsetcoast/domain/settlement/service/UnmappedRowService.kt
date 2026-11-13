package kr.sunsetcoast.domain.settlement.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kr.sunsetcoast.domain.importbatch.entity.ImportBatchEntity
import kr.sunsetcoast.domain.importbatch.entity.ImportRowEntity
import kr.sunsetcoast.domain.importbatch.repository.ImportBatchRepository
import kr.sunsetcoast.domain.importbatch.repository.ImportRowRepository
import kr.sunsetcoast.domain.importbatch.vo.ImportRowResult
import kr.sunsetcoast.domain.room.entity.RoomAliasEntity
import kr.sunsetcoast.domain.room.repository.RoomAliasRepository
import kr.sunsetcoast.domain.room.repository.RoomRepository
import kr.sunsetcoast.domain.settlement.parser.ParsedSettlementLine
import kr.sunsetcoast.domain.settlement.parser.SettlementFileParsers
import kr.sunsetcoast.domain.settlement.vo.RoomNameMatcher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.json.JsonMapper

private val logger = KotlinLogging.logger {}

/**
 * 객실을 찾지 못한 행(UNMAPPED) 처리. 원본 행이 보존돼 있으므로 관리자가 객실을 지정하면 다시 파싱해서 반영한다.
 */
@Service
class UnmappedRowService(
    private val importRowRepository: ImportRowRepository,
    private val importBatchRepository: ImportBatchRepository,
    private val roomRepository: RoomRepository,
    private val roomAliasRepository: RoomAliasRepository,
    private val parsers: SettlementFileParsers,
    private val writer: SettlementLineWriter,
    private val jsonMapper: JsonMapper,
) {
    data class UnmappedRow(
        val row: ImportRowEntity,
        val batch: ImportBatchEntity,
        val lines: List<ParsedSettlementLine>,
        /** 객실명 조각 중 찾은 객실 (화면에서 미리 선택해 둔다) */
        val suggestedRoomIds: List<Long>,
    )

    data class ResolveResult(
        val row: ImportRowEntity,
        /** 별칭 저장·예약 생성으로 같이 처리된 다른 미매핑 행 수 */
        val autoResolved: Int,
    )

    @Transactional(readOnly = true)
    fun findPending(): List<UnmappedRow> {
        val rows = importRowRepository.findByResultOrderByIdAsc(ImportRowResult.UNMAPPED)
        val batches = importBatchRepository.findAllById(rows.map { it.importBatchId }.toSet()).associateBy { it.id }
        val aliases = batches.values.map { it.source }.toSet()
            .associateWith { source -> roomAliasRepository.findBySource(source).associate { it.externalName.trim() to it.room.id!! } }
        return rows.map { row ->
            val batch = batches.getValue(row.importBatchId)
            val lines = runCatching { parse(row, batch) }.getOrDefault(emptyList())
            val suggested = lines.firstOrNull()
                ?.let { RoomNameMatcher.match(it.roomName, aliases.getValue(batch.source)) }
                .let {
                    when (it) {
                        is RoomNameMatcher.Result.Matched -> it.roomIds
                        is RoomNameMatcher.Result.Unmatched -> it.partialRoomIds
                        null -> emptyList()
                    }
                }
            UnmappedRow(row, batch, lines, suggested)
        }
    }

    /**
     * 관리자가 지정한 객실로 행을 반영한다.
     *
     * @param saveAlias true이고 객실이 하나면(쉼표 없는 이름만), 파일의 객실명을 그 객실의 별칭으로 저장해 다음 업로드부터 자동으로 찾는다.
     * 반영 후 남은 미매핑 행을 다시 시도한다 (같은 객실명이나 같은 예약번호가 다른 파일에 있었던 경우 함께 처리된다).
     */
    @Transactional
    fun resolve(rowId: Long, roomIds: List<Long>, saveAlias: Boolean): ResolveResult {
        require(roomIds.isNotEmpty()) { "객실을 하나 이상 골라 주세요." }
        require(roomIds.toSet().size == roomIds.size) { "같은 객실을 두 번 고를 수 없습니다." }
        val row = importRowRepository.findById(rowId).orElseThrow { IllegalArgumentException("행을 찾을 수 없습니다: $rowId") }
        require(row.result == ImportRowResult.UNMAPPED) { "이미 처리된 행입니다." }
        val batch = importBatchRepository.findById(row.importBatchId).orElseThrow()

        val roomsById = roomRepository.findAllById(roomIds).associateBy { it.id }
        val rooms = roomIds.map { roomsById[it] ?: throw IllegalArgumentException("객실을 찾을 수 없습니다: $it") }
        require(rooms.map { it.property.id }.toSet().size == 1) { "한 예약의 객실은 같은 업소여야 합니다." }

        val lines = parse(row, batch)
        require(lines.isNotEmpty()) { "저장할 정산 금액이 없는 행입니다." }

        val context = writer.newContext(batch.source, batch.id!!)
        val (result, message) = writer.write(lines, rooms, context)
        require(result != ImportRowResult.FAILED) { message ?: "반영하지 못했습니다." }
        row.resolve(result, "객실 지정: ${rooms.joinToString { it.name }}" + (message?.let { " / $it" } ?: ""))
        batch.moveUnmapped(result)
        writer.finish(context)

        if (saveAlias && rooms.size == 1) {
            val name = lines.first().roomName.trim()
            // 쉼표로 이어진 다중 객실 이름은 객실 하나의 별칭으로 저장하지 않는다
            if (',' !in name && roomAliasRepository.findBySourceAndExternalName(batch.source, name) == null) {
                roomAliasRepository.save(RoomAliasEntity(source = batch.source, externalName = name, room = rooms.single()))
            }
        }

        val autoResolved = retryPending()
        logger.info { "unmapped row resolved row=$rowId rooms=${rooms.map { it.name }} result=$result autoResolved=$autoResolved" }
        return ResolveResult(row, autoResolved)
    }

    /** 남은 미매핑 행을 다시 시도한다. @return 처리된 행 수 */
    private fun retryPending(): Int {
        val pending = importRowRepository.findByResultOrderByIdAsc(ImportRowResult.UNMAPPED)
        if (pending.isEmpty()) return 0
        val batches = importBatchRepository.findAllById(pending.map { it.importBatchId }.toSet()).associateBy { it.id }
        var resolved = 0
        for (row in pending) {
            val batch = batches.getValue(row.importBatchId)
            val lines = runCatching { parse(row, batch) }.getOrNull()?.takeIf { it.isNotEmpty() } ?: continue
            val context = writer.newContext(batch.source, batch.id!!)
            val match = writer.matchRooms(lines.first(), context) as? SettlementLineWriter.RoomMatch.Found ?: continue
            val (result, message) = writer.write(lines, match.rooms, context)
            if (result == ImportRowResult.FAILED) continue
            row.resolve(result, "자동 처리: ${match.rooms.joinToString { it.name }}" + (message?.let { " / $it" } ?: ""))
            batch.moveUnmapped(result)
            writer.finish(context)
            resolved++
        }
        return resolved
    }

    private fun parse(row: ImportRowEntity, batch: ImportBatchEntity): List<ParsedSettlementLine> {
        val payload: Map<String, String> = jsonMapper.readValue(row.payload, object : TypeReference<Map<String, String>>() {})
        return parsers.parseRow(batch.source, row.rowNo, payload)
    }
}
