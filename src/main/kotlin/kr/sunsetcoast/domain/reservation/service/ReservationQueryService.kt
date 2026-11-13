package kr.sunsetcoast.domain.reservation.service

import kr.sunsetcoast.domain.importbatch.repository.ImportBatchRepository
import kr.sunsetcoast.domain.managementfee.repository.ManagementFeeRepository
import kr.sunsetcoast.domain.reservation.repository.ReservationRepository
import kr.sunsetcoast.domain.reservation.repository.ReservationRoomRepository
import kr.sunsetcoast.domain.reservation.vo.ReservationDetail
import kr.sunsetcoast.domain.reservation.vo.ReservationSummary
import kr.sunsetcoast.domain.reservation.vo.SettlementLineWithFile
import kr.sunsetcoast.domain.settlement.repository.SettlementLineRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

/** 예약 조회 (관리자 화면) */
@Service
class ReservationQueryService(
    private val reservationRepository: ReservationRepository,
    private val settlementLineRepository: SettlementLineRepository,
    private val managementFeeRepository: ManagementFeeRepository,
    private val importBatchRepository: ImportBatchRepository,
    private val reservationRoomRepository: ReservationRoomRepository,
) {
    /**
     * 업소의 기간 내 예약. 입실일 기준이고, 입실일을 모르는 취소 예약은 매출인식일 기준으로 넣는다.
     * 플랫폼·상태·검색어 필터는 화면에서 한다 (한 달 수백 건 규모).
     */
    @Transactional(readOnly = true)
    fun findByPeriod(propertyId: Long, from: LocalDate, to: LocalDate): List<ReservationSummary> {
        require(!from.isAfter(to)) { "조회 시작일이 종료일보다 늦습니다: $from ~ $to" }
        require(from.plusYears(1).isAfter(to)) { "조회 기간은 1년을 넘을 수 없습니다: $from ~ $to" }

        val reservations = reservationRepository.findByPropertyAndBaseDateBetween(propertyId, from, to)
        if (reservations.isEmpty()) return emptyList()
        val ids = reservations.map { it.id!! }
        val revenueDates = settlementLineRepository.findByReservationIdIn(ids)
            .groupBy { it.reservation.id!! }
            .mapValues { (_, lines) -> lines.minOf { it.stayDate } }
        val rooms = reservationRoomRepository.findByReservationIds(ids).groupBy({ it.reservation.id!! }, { it.room })
        val fees = managementFeeRepository.findByReservationIdIn(ids)
            .groupBy({ it.reservation.id!! }, { it.amount })
            .mapValues { (_, amounts) -> amounts.fold(BigDecimal.ZERO, BigDecimal::add) }

        return reservations
            .map { ReservationSummary(it, rooms[it.id].orEmpty().ifEmpty { listOf(it.room) }, it.checkInDate ?: revenueDates[it.id], fees[it.id]) }
            .sortedWith(compareBy<ReservationSummary> { it.baseDate }.thenBy { it.reservation.room.id }.thenBy { it.reservation.id })
    }

    @Transactional(readOnly = true)
    fun findDetail(id: Long): ReservationDetail {
        val reservation = reservationRepository.findById(id)
            .orElseThrow { IllegalArgumentException("예약을 찾을 수 없습니다: $id") }
        reservation.room.name // 응답에서 쓰므로 트랜잭션 안에서 읽어 둔다
        val rooms = reservationRoomRepository.findByReservationIds(listOf(id)).map { it.room }.ifEmpty { listOf(reservation.room) }
        val order = rooms.mapIndexed { i, room -> room.id to i }.toMap()
        val lines = settlementLineRepository.findByReservationId(id)
            .onEach { it.room.name }
            .sortedWith(compareBy({ it.stayDate }, { order[it.room.id] ?: 0 }, { it.lineType }, { it.seq }))
        val files = importBatchRepository.findAllById(lines.mapNotNull { it.importBatchId }.toSet()).associate { it.id to it.fileName }
        val fees = managementFeeRepository.findByReservationIdIn(listOf(id)).onEach { it.room.name }
        return ReservationDetail(reservation, rooms, lines.map { SettlementLineWithFile(it, files[it.importBatchId]) }, fees)
    }
}
