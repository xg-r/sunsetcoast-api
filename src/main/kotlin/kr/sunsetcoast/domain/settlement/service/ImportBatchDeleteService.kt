package kr.sunsetcoast.domain.settlement.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kr.sunsetcoast.domain.importbatch.repository.ImportBatchRepository
import kr.sunsetcoast.domain.importbatch.repository.ImportRowRepository
import kr.sunsetcoast.domain.managementfee.repository.ManagementFeeRepository
import kr.sunsetcoast.domain.reservation.entity.ReservationEntity
import kr.sunsetcoast.domain.reservation.repository.ReservationRepository
import kr.sunsetcoast.domain.reservation.repository.ReservationRoomRepository
import kr.sunsetcoast.domain.reservation.vo.OtaPlatform
import kr.sunsetcoast.domain.settlement.entity.SettlementLineEntity
import kr.sunsetcoast.domain.settlement.entity.SettlementLineRevisionEntity
import kr.sunsetcoast.domain.settlement.repository.SettlementLineRepository
import kr.sunsetcoast.domain.settlement.repository.SettlementLineRevisionRepository
import kr.sunsetcoast.domain.settlement.vo.SettlementAmounts
import kr.sunsetcoast.domain.settlement.vo.SettlementLineType
import kr.sunsetcoast.domain.staycalendar.service.StayCalendarService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate

private val logger = KotlinLogging.logger {}

/**
 * 잘못 올린 정산 파일 삭제. 그 파일이 남긴 것을 되돌린다.
 *
 * - 이 파일이 새로 만든 정산 행 → 삭제
 * - 이 파일이 금액을 바꾼 정산 행 → 바뀌기 전 값으로 되돌림 (변경 이력에서)
 * - 이 파일이 바꿨지만 그 뒤 다른 파일이 또 바꾼 행 → 나중 파일의 값을 유지하고, 이력만 이어 붙임
 * - 정산 행이 하나도 남지 않은 OTA 예약 → 예약·객실·관리비까지 삭제 (관리자가 고친 관리비도 사라진다).
 *   입금 완료된 관리비가 있으면 삭제를 거부한다 (트랜잭션 전체 롤백)
 * - 남은 예약은 박수·금액 합계·성수기/주말·관리비를 다시 계산
 * - 원본 행(import_rows)과 업로드 기록(import_batches) 삭제 → 같은 파일을 다시 올릴 수 있다
 */
@Service
class ImportBatchDeleteService(
    private val importBatchRepository: ImportBatchRepository,
    private val importRowRepository: ImportRowRepository,
    private val settlementLineRepository: SettlementLineRepository,
    private val revisionRepository: SettlementLineRevisionRepository,
    private val reservationRepository: ReservationRepository,
    private val reservationRoomRepository: ReservationRoomRepository,
    private val managementFeeRepository: ManagementFeeRepository,
    private val writer: SettlementLineWriter,
    private val stayCalendarService: StayCalendarService,
    private val jsonMapper: JsonMapper,
) {
    data class Result(
        val batchId: Long,
        val fileName: String,
        val deletedLines: Int,
        val restoredLines: Int,
        val deletedReservations: Int,
    )

    @Transactional
    fun delete(batchId: Long): Result {
        val batch = importBatchRepository.findById(batchId)
            .orElseThrow { IllegalArgumentException("업로드 기록이 없습니다: $batchId") }

        val lines = settlementLineRepository.findByImportBatchId(batchId)
        val reservations = lines.map { it.reservation }.distinctBy { it.id }
        // 지우거나 되돌리는 밤의 성수기 표를 다시 세려고, 바꾸기 전에 날짜를 모아 둔다
        val nightsByProperty = nightsByProperty(reservations)

        val revisionsByLine = revisionRepository.findBySettlementLineIdIn(lines.map { it.id!! }).groupBy { it.settlementLineId }
        var deleted = 0
        var restored = 0
        lines.forEach { line ->
            val history = revisionsByLine[line.id].orEmpty().sortedBy { it.id }
            val own = history.lastOrNull { it.importBatchId == batchId }
            if (own == null) {
                revisionRepository.deleteAll(history)
                settlementLineRepository.delete(line)
                deleted++
            } else {
                val earlier = history.lastOrNull { it.id!! < own.id!! }
                line.restore(
                    amounts = jsonMapper.readValue(own.previousValues, SettlementAmounts::class.java),
                    settledOn = own.previousSettledOn,
                    importBatchId = own.previousImportBatchId ?: earlier?.importBatchId,
                )
                revisionRepository.delete(own)
                restored++
            }
        }

        // 이 파일이 바꾼 뒤 다른 파일이 또 바꾼 행: 다음 이력이 이 파일 이전 값을 가리키게 한다
        revisionRepository.findByImportBatchId(batchId).forEach { superseded ->
            val next = revisionRepository.findBySettlementLineIdIn(listOf(superseded.settlementLineId))
                .filter { it.id!! > superseded.id!! }
                .minByOrNull { it.id!! }
            next?.apply {
                previousValues = superseded.previousValues
                previousSettledOn = superseded.previousSettledOn
                previousImportBatchId = superseded.previousImportBatchId
            }
            revisionRepository.delete(superseded)
        }
        revisionRepository.findByPreviousImportBatchId(batchId).forEach { it.previousImportBatchId = null }
        settlementLineRepository.flush()

        val remaining = settlementLineRepository.findByReservationIdIn(reservations.map { it.id!! }).groupBy { it.reservation.id }
        val (empty, kept) = reservations.partition { remaining[it.id].isNullOrEmpty() && it.source != OtaPlatform.DIRECT }
        val paidFees = managementFeeRepository.findByReservationIdIn(empty.map { it.id!! }).filter { it.paid }
        require(paidFees.isEmpty()) {
            "입금 완료된 관리비가 있는 예약이 함께 삭제되므로 지울 수 없습니다: " +
                paidFees.joinToString { it.reservation.reservationNumber } + ". 관리비 화면에서 입금 완료를 취소한 뒤 다시 시도해 주세요."
        }
        deleteReservations(empty)
        writer.refresh(kept)
        stayCalendarService.refreshDates(nightsByProperty)

        importRowRepository.deleteByImportBatchId(batchId)
        importBatchRepository.delete(batch)
        importBatchRepository.flush()

        logger.info {
            "import batch deleted id=$batchId file=${batch.fileName} deletedLines=$deleted restoredLines=$restored " +
                "deletedReservations=${empty.size}"
        }
        return Result(batchId, batch.fileName, deleted, restored, empty.size)
    }

    private fun deleteReservations(reservations: List<ReservationEntity>) {
        if (reservations.isEmpty()) return
        val ids = reservations.map { it.id!! }
        managementFeeRepository.deleteAll(managementFeeRepository.findByReservationIdIn(ids))
        reservationRoomRepository.deleteAll(reservationRoomRepository.findByReservationIds(ids))
        reservationRepository.deleteAll(reservations)
        reservationRepository.flush()
    }

    private fun nightsByProperty(reservations: List<ReservationEntity>): Map<Long, Set<LocalDate>> {
        if (reservations.isEmpty()) return emptyMap()
        return settlementLineRepository.findByReservationIdIn(reservations.map { it.id!! })
            .filter { it.isNight() }
            .groupBy({ it.room.property.id!! }, { it.stayDate })
            .mapValues { (_, dates) -> dates.toSet() }
    }

    private fun SettlementLineEntity.isNight() =
        lineType == SettlementLineType.ROOM_NIGHT || lineType == SettlementLineType.ROOM_NIGHT_REVERSAL
}
