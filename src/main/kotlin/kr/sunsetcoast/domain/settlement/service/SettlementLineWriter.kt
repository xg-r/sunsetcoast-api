package kr.sunsetcoast.domain.settlement.service

import kr.sunsetcoast.domain.importbatch.vo.ImportRowResult
import kr.sunsetcoast.domain.managementfee.service.ManagementFeeService
import kr.sunsetcoast.domain.reservation.entity.ReservationEntity
import kr.sunsetcoast.domain.reservation.entity.ReservationRoomEntity
import kr.sunsetcoast.domain.reservation.repository.ReservationRepository
import kr.sunsetcoast.domain.reservation.repository.ReservationRoomRepository
import kr.sunsetcoast.domain.reservation.vo.AmountStatus
import kr.sunsetcoast.domain.reservation.vo.OtaPlatform
import kr.sunsetcoast.domain.reservation.vo.ReservationStatus
import kr.sunsetcoast.domain.reservation.vo.SalesChannel
import kr.sunsetcoast.domain.room.entity.RoomEntity
import kr.sunsetcoast.domain.room.repository.RoomAliasRepository
import kr.sunsetcoast.domain.settlement.entity.SettlementLineEntity
import kr.sunsetcoast.domain.settlement.entity.SettlementLineRevisionEntity
import kr.sunsetcoast.domain.settlement.parser.ParsedSettlementLine
import kr.sunsetcoast.domain.settlement.repository.SettlementLineRepository
import kr.sunsetcoast.domain.settlement.repository.SettlementLineRevisionRepository
import kr.sunsetcoast.domain.settlement.vo.AmountSplitter
import kr.sunsetcoast.domain.settlement.vo.RoomNameMatcher
import kr.sunsetcoast.domain.settlement.vo.SettlementLineType
import kr.sunsetcoast.domain.staycalendar.repository.RoomListPriceRepository
import kr.sunsetcoast.domain.staycalendar.service.StayCalendarService
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 파싱된 정산 행을 예약·정산 행으로 저장한다. 파일 업로드와 미매핑 행 처리가 같이 쓴다.
 *
 * 다중 객실 예약은 정산 행을 객실마다 만들고, 금액은 객실 정가(비수기 평일) 비율로 나눈 추정값으로 둔다.
 */
@Component
class SettlementLineWriter(
    private val roomAliasRepository: RoomAliasRepository,
    private val roomListPriceRepository: RoomListPriceRepository,
    private val reservationRepository: ReservationRepository,
    private val reservationRoomRepository: ReservationRoomRepository,
    private val settlementLineRepository: SettlementLineRepository,
    private val revisionRepository: SettlementLineRevisionRepository,
    private val jsonMapper: JsonMapper,
    private val stayCalendarService: StayCalendarService,
    private val managementFeeService: ManagementFeeService,
) {
    fun newContext(platform: OtaPlatform, batchId: Long): WriteContext {
        val aliases = roomAliasRepository.findBySource(platform)
        return WriteContext(
            platform = platform,
            batchId = batchId,
            aliases = aliases.associate { it.externalName.trim() to it.room.id!! },
            roomsById = aliases.associateBy({ it.room.id!! }, { it.room }).toMutableMap(),
        )
    }

    sealed interface RoomMatch {
        data class Found(val rooms: List<RoomEntity>) : RoomMatch

        data class NotFound(val message: String, val suggestedRoomIds: List<Long>) : RoomMatch
    }

    /**
     * 객실명으로 객실을 찾는다. 이름으로 못 찾아도 같은 예약번호의 예약이 이미 있으면 그 예약의 객실을 쓴다
     * (관리자가 객실을 지정한 다중 객실 예약이 다음 파일에 또 나오는 경우).
     */
    fun matchRooms(line: ParsedSettlementLine, context: WriteContext): RoomMatch =
        when (val result = RoomNameMatcher.match(line.roomName, context.aliases)) {
            is RoomNameMatcher.Result.Matched -> RoomMatch.Found(result.roomIds.map { context.roomsById.getValue(it) })
            is RoomNameMatcher.Result.Unmatched -> {
                val existing = reservationRepository.findBySourceAndReservationNumber(context.platform, line.reservationNumber)
                if (existing != null) RoomMatch.Found(roomsOf(existing, context)) else RoomMatch.NotFound(result.reason, result.partialRoomIds)
            }
        }

    /** 한 원본 행의 정산 행들을 정해진 객실로 저장한다. */
    fun write(lines: List<ParsedSettlementLine>, rooms: List<RoomEntity>, context: WriteContext): Pair<ImportRowResult, String?> {
        require(rooms.isNotEmpty()) { "객실이 없습니다" }
        rooms.forEach { context.roomsById[it.id!!] = it }
        val weights = weights(rooms)
        var rowResult: ImportRowResult? = null
        var message: String? = null

        for (line in lines) {
            val reservation = findOrCreateReservation(line, rooms, context)
            val existingRooms = roomsOf(reservation, context)
            if (existingRooms.map { it.id }.toSet() != rooms.map { it.id }.toSet()) {
                return ImportRowResult.FAILED to
                    "예약 ${line.reservationNumber}의 객실이 기존(${existingRooms.joinToString { it.name }})과 다릅니다: ${rooms.joinToString { it.name }}"
            }
            reservation.fillMissing(line.salesChannelRaw, line.bookerName, line.reservedAt)
            context.touchedReservations[reservation.id!!] = reservation
            if (line.checkInDate != null) context.explicitStays[reservation.id] = line.checkInDate to line.nights

            val parts = AmountSplitter.split(line.amounts, weights)
            existingRooms.zip(parts).forEach { (room, amounts) ->
                val (lineResult, lineMessage) = upsertLine(
                    reservation,
                    room,
                    line.copy(amounts = amounts, splitEstimated = line.splitEstimated || rooms.size > 1),
                    context,
                )
                rowResult = rowResult?.mergeWith(lineResult) ?: lineResult
                message = lineMessage ?: message
            }
        }
        return (rowResult ?: ImportRowResult.SKIPPED) to message
    }

    /** 저장이 끝난 뒤 바뀐 예약의 박수·금액 합계, 성수기/주말, 관리비를 다시 계산한다. */
    fun finish(context: WriteContext) {
        val touched = context.touchedReservations.values
        touched.forEach { refreshStay(it, context.explicitStays[it.id]) }
        stayCalendarService.refresh(touched)
        managementFeeService.refresh(touched)
    }

    private fun weights(rooms: List<RoomEntity>): List<BigDecimal> {
        if (rooms.size == 1) return listOf(BigDecimal.ONE)
        val prices = roomListPriceRepository.findAllById(rooms.map { it.id!! }).associate { it.roomId to it.offpeakWeekday }
        // 요금표에 없는 객실(Villa II·III)이 섞이면 균등하게 나눈다
        return if (rooms.all { prices[it.id] != null }) rooms.map { prices.getValue(it.id!!) } else rooms.map { BigDecimal.ONE }
    }

    private fun roomsOf(reservation: ReservationEntity, context: WriteContext): List<RoomEntity> =
        context.reservationRooms.getOrPut(reservation.id!!) {
            reservationRoomRepository.findByReservationIds(listOf(reservation.id)).map { it.room }
                .ifEmpty { listOf(reservation.room) }
        }

    private fun findOrCreateReservation(line: ParsedSettlementLine, rooms: List<RoomEntity>, context: WriteContext): ReservationEntity {
        reservationRepository.findBySourceAndReservationNumber(context.platform, line.reservationNumber)?.let { return it }
        val reservation = reservationRepository.save(
            ReservationEntity(
                source = context.platform,
                reservationNumber = line.reservationNumber,
                room = rooms.first(),
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
        rooms.forEachIndexed { i, room ->
            reservationRoomRepository.save(ReservationRoomEntity(reservation = reservation, room = room, sortOrder = i))
        }
        context.reservationRooms[reservation.id!!] = rooms
        return reservation
    }

    private fun upsertLine(
        reservation: ReservationEntity,
        room: RoomEntity,
        line: ParsedSettlementLine,
        context: WriteContext,
    ): Pair<ImportRowResult, String?> {
        // 같은 파일 안에서 같은 키가 반복되면(예: NOL 취소수수료 2건) 순번으로 구분한다.
        // 같은 파일을 다시 올리면 같은 순번이 나오므로 중복으로 판정된다.
        val key = LineKey(reservation.id!!, room.id!!, line.lineType, line.stayDate)
        val seq = context.seqCounter.merge(key, 1, Int::plus)!! - 1

        val existing = settlementLineRepository.findByReservationIdAndRoomIdAndLineTypeAndStayDateAndSeq(
            reservation.id, room.id, line.lineType, line.stayDate, seq,
        )
        if (existing == null) {
            settlementLineRepository.save(
                SettlementLineEntity.create(
                    reservation = reservation,
                    room = room,
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
     * 정산 행으로 입실일/박수/상태/금액 합계를 다시 계산한다.
     * 여기어때/NOL은 1박마다 행이 생기고 연박이 여러 주에 걸쳐 정산되므로, 예약번호로 묶인 행을 모두 본다.
     * (예: 3박 예약 → 1박 행 3개 → 예약 1건, 박수 3, 매출 = 세 박 매출의 합. 다중 객실은 날짜가 겹치므로 박수는 날짜 수)
     */
    private fun refreshStay(reservation: ReservationEntity, explicitStay: Pair<LocalDate, Int?>?) {
        val lines = settlementLineRepository.findByReservationId(reservation.id!!)
        val reversed = lines.filter { it.lineType == SettlementLineType.ROOM_NIGHT_REVERSAL }.map { it.stayDate }.toSet()
        val stayDates = lines.filter { it.lineType == SettlementLineType.ROOM_NIGHT }.map { it.stayDate }.toSet() - reversed
        val status = if (stayDates.isEmpty()) ReservationStatus.CANCELLED else ReservationStatus.CONFIRMED

        reservation.applyTotals(
            gross = lines.sumOf { it.grossAmount },
            commission = lines.sumOf { it.commissionAmount },
            net = lines.sumOf { it.netAmount },
        )

        when {
            explicitStay != null -> reservation.applyStay(explicitStay.first, explicitStay.second, status)
            stayDates.isNotEmpty() -> reservation.applyStay(stayDates.min(), stayDates.size, status)
            else -> reservation.applyStay(null, null, status)
        }
    }

    data class LineKey(val reservationId: Long, val roomId: Long, val lineType: SettlementLineType, val stayDate: LocalDate)

    class WriteContext(
        val platform: OtaPlatform,
        val batchId: Long,
        /** 파일 객실명 → 객실 ID */
        val aliases: Map<String, Long>,
        internal val roomsById: MutableMap<Long, RoomEntity>,
    ) {
        internal val seqCounter = HashMap<LineKey, Int>()
        internal val reservationRooms = HashMap<Long, List<RoomEntity>>()
        val touchedReservations = LinkedHashMap<Long, ReservationEntity>()
        internal val explicitStays = HashMap<Long, Pair<LocalDate, Int?>>()
    }
}
