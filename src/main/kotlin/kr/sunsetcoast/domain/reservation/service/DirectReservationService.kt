package kr.sunsetcoast.domain.reservation.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kr.sunsetcoast.domain.businessowner.vo.OwnershipType
import kr.sunsetcoast.domain.managementfee.repository.ManagementFeeRepository
import kr.sunsetcoast.domain.managementfee.service.ManagementFeeService
import kr.sunsetcoast.domain.reservation.entity.ReservationEntity
import kr.sunsetcoast.domain.reservation.entity.ReservationRoomEntity
import kr.sunsetcoast.domain.reservation.repository.ReservationRepository
import kr.sunsetcoast.domain.reservation.repository.ReservationRoomRepository
import kr.sunsetcoast.domain.reservation.vo.AmountStatus
import kr.sunsetcoast.domain.reservation.vo.OtaPlatform
import kr.sunsetcoast.domain.reservation.vo.PaymentMethod
import kr.sunsetcoast.domain.reservation.vo.PaymentReceiver
import kr.sunsetcoast.domain.reservation.vo.ReservationStatus
import kr.sunsetcoast.domain.reservation.vo.SalesChannel
import kr.sunsetcoast.domain.room.entity.RoomEntity
import kr.sunsetcoast.domain.room.repository.RoomRepository
import kr.sunsetcoast.domain.settlement.entity.SettlementLineEntity
import kr.sunsetcoast.domain.settlement.repository.SettlementLineRepository
import kr.sunsetcoast.domain.settlement.vo.AmountSplitter
import kr.sunsetcoast.domain.settlement.vo.SettlementAmounts
import kr.sunsetcoast.domain.settlement.vo.SettlementLineType
import kr.sunsetcoast.domain.staycalendar.repository.HolidayRepository
import kr.sunsetcoast.domain.staycalendar.repository.RoomListPriceRepository
import kr.sunsetcoast.domain.staycalendar.repository.SeasonDayRepository
import kr.sunsetcoast.domain.staycalendar.service.StayCalendarService
import kr.sunsetcoast.domain.staycalendar.vo.WeekendPolicy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val logger = KotlinLogging.logger {}

/** 같은 객실·날짜에 이미 투숙 예약이 있다 */
class ReservationConflictException(val conflicts: List<DirectReservationService.Conflict>) :
    RuntimeException(
        "같은 객실·날짜에 예약이 있습니다: " +
            conflicts.joinToString { "${it.roomName} ${it.checkInDate}~${it.checkOutDate} (${it.source} ${it.reservationNumber})" }
    )

/**
 * 직접 예약 입력 (RESERVATION-01). 전화·현장 예약은 플랫폼 파일에 나오지 않아 관리자가 입력한다.
 *
 * OTA 예약과 같은 구조로 저장한다: 예약 + 예약 객실 + 1박·객실마다 정산 행(수수료 0, 업로드 배치 없음).
 * 그래서 예약 내역·관리비·채널 분석에 그대로 반영된다.
 */
@Service
class DirectReservationService(
    private val reservationRepository: ReservationRepository,
    private val reservationRoomRepository: ReservationRoomRepository,
    private val roomRepository: RoomRepository,
    private val settlementLineRepository: SettlementLineRepository,
    private val managementFeeRepository: ManagementFeeRepository,
    private val roomListPriceRepository: RoomListPriceRepository,
    private val seasonDayRepository: SeasonDayRepository,
    private val holidayRepository: HolidayRepository,
    private val stayCalendarService: StayCalendarService,
    private val managementFeeService: ManagementFeeService,
) {
    data class RoomNightPrice(val roomId: Long, val date: LocalDate, val amount: BigDecimal)

    data class Command(
        val roomIds: List<Long>,
        val checkInDate: LocalDate,
        val nights: Int,
        val guestCount: Int?,
        val bookerName: String,
        val salesChannel: SalesChannel,
        val nightlyPrices: List<RoomNightPrice>,
        val discount: BigDecimal,
        val paymentMethod: PaymentMethod?,
        val paid: Boolean,
        val memo: String?,
        /** 겹치는 예약이 있어도 저장 */
        val allowOverlap: Boolean,
    )

    data class Conflict(
        val reservationId: Long,
        val source: OtaPlatform,
        val reservationNumber: String,
        val roomName: String,
        val bookerName: String?,
        val checkInDate: LocalDate,
        val checkOutDate: LocalDate,
    )

    data class NightQuote(
        val date: LocalDate,
        val weekend: Boolean,
        /** 성수기 여부. 판단 근거가 없으면 null (비수기 요금으로 제안) */
        val peak: Boolean?,
        /** 객실별 제안 금액 (요금표에 없는 객실은 null) */
        val prices: Map<Long, BigDecimal?>,
    )

    data class Quote(
        val nights: List<NightQuote>,
        val conflicts: List<Conflict>,
        /** 외부 소유주 객실이 있어 소유주 계좌로 입금되는 예약 */
        val ownerPaid: Boolean,
    )

    /** 요금표·성수기·주말로 1박 금액을 제안하고, 겹치는 예약을 찾는다. */
    @Transactional(readOnly = true)
    fun quote(roomIds: List<Long>, checkIn: LocalDate, nights: Int, excludeReservationId: Long?): Quote {
        val rooms = loadRooms(roomIds)
        validateStay(nights)
        val dates = (0 until nights).map { checkIn.plusDays(it.toLong()) }
        val policy = WeekendPolicy(holidayRepository.findByHolidayDateBetween(checkIn, dates.last().plusDays(1)).map { it.holidayDate }.toSet())
        val prices = roomListPriceRepository.findAllById(roomIds).associate { it.roomId to it.toListPrice() }
        val season = rooms.map { it.property.id!! }.toSet().associateWith { propertyId ->
            seasonDayRepository.findByPropertyIdAndStayDateBetween(propertyId, checkIn, dates.last()).associate { it.stayDate to it.peak }
        }
        val nightQuotes = dates.map { date ->
            val weekend = policy.isWeekendNight(date)
            val peak = season[rooms.first().property.id]?.get(date)
            NightQuote(
                date = date,
                weekend = weekend,
                peak = peak,
                prices = rooms.associate { room ->
                    val roomPeak = season[room.property.id]?.get(date) ?: false
                    room.id!! to prices[room.id]?.of(roomPeak, weekend)
                },
            )
        }
        return Quote(nightQuotes, findConflicts(rooms, checkIn, nights, excludeReservationId), isOwnerPaid(rooms))
    }

    @Transactional
    fun create(command: Command): ReservationEntity {
        val rooms = validate(command, excludeReservationId = null)
        val reservation = reservationRepository.save(
            ReservationEntity(
                source = OtaPlatform.DIRECT,
                reservationNumber = nextReservationNumber(),
                room = rooms.first(),
                salesChannel = command.salesChannel,
                bookerName = command.bookerName.trim(),
                reservedAt = LocalDateTime.now(),
                checkInDate = command.checkInDate,
                nights = command.nights,
                status = ReservationStatus.CONFIRMED,
                amountStatus = AmountStatus.CONFIRMED,
            )
        )
        write(reservation, rooms, command)
        logger.info { "direct reservation created id=${reservation.id} number=${reservation.reservationNumber} rooms=${rooms.map { it.name }}" }
        return reservation
    }

    @Transactional
    fun update(id: Long, command: Command): ReservationEntity {
        val reservation = findDirect(id)
        val rooms = validate(command, excludeReservationId = id)

        // 객실·날짜가 바뀔 수 있으므로 객실과 정산 행을 지우고 다시 만든다. 남는 객실의 관리비 수정값은 유지한다.
        val roomIds = rooms.map { it.id!! }.toSet()
        managementFeeRepository.deleteAll(managementFeeRepository.findByReservationIdIn(listOf(id)).filter { it.room.id !in roomIds })
        settlementLineRepository.deleteAll(settlementLineRepository.findByReservationId(id))
        reservationRoomRepository.deleteAll(reservationRoomRepository.findByReservationIds(listOf(id)))
        settlementLineRepository.flush()

        reservation.room = rooms.first()
        reservation.salesChannel = command.salesChannel
        reservation.bookerName = command.bookerName.trim()
        write(reservation, rooms, command)
        logger.info { "direct reservation updated id=$id rooms=${rooms.map { it.name }}" }
        return reservation
    }

    /**
     * 예약 취소. 1박 행을 지우고, 받은 취소수수료(환불하지 않은 금액)가 있으면 취소수수료 행을 남긴다.
     */
    @Transactional
    fun cancel(id: Long, cancellationFee: BigDecimal, cancelledOn: LocalDate): ReservationEntity {
        val reservation = findDirect(id)
        require(reservation.status == ReservationStatus.CONFIRMED) { "이미 취소된 예약입니다." }
        require(cancellationFee.signum() >= 0) { "취소수수료는 0원 이상이어야 합니다." }
        val rooms = reservationRoomRepository.findByReservationIds(listOf(id)).map { it.room }.ifEmpty { listOf(reservation.room) }

        settlementLineRepository.deleteAll(settlementLineRepository.findByReservationId(id))
        settlementLineRepository.flush()
        if (cancellationFee.signum() > 0) {
            settlementLineRepository.save(
                SettlementLineEntity.create(
                    reservation = reservation,
                    room = rooms.first(),
                    importBatchId = null,
                    lineType = SettlementLineType.CANCELLATION_FEE,
                    stayDate = cancelledOn,
                    seq = 0,
                    settledOn = null,
                    amounts = SettlementAmounts(gross = cancellationFee, commissionRate = BigDecimal.ZERO, net = cancellationFee),
                    splitEstimated = false,
                )
            )
        }
        reservation.applyTotals(cancellationFee, BigDecimal.ZERO, cancellationFee)
        reservation.applyStay(reservation.checkInDate, reservation.nights, ReservationStatus.CANCELLED)
        refresh(reservation)
        logger.info { "direct reservation cancelled id=$id fee=$cancellationFee" }
        return reservation
    }

    /** 잘못 입력한 예약 삭제. 관리비·정산 행·객실도 같이 지운다. */
    @Transactional
    fun delete(id: Long) {
        val reservation = findDirect(id)
        managementFeeRepository.deleteAll(managementFeeRepository.findByReservationIdIn(listOf(id)))
        settlementLineRepository.deleteAll(settlementLineRepository.findByReservationId(id))
        reservationRoomRepository.deleteAll(reservationRoomRepository.findByReservationIds(listOf(id)))
        reservationRepository.delete(reservation)
        reservationRepository.flush()
        logger.info { "direct reservation deleted id=$id number=${reservation.reservationNumber}" }
    }

    private fun write(reservation: ReservationEntity, rooms: List<RoomEntity>, command: Command) {
        rooms.forEachIndexed { i, room ->
            reservationRoomRepository.save(ReservationRoomEntity(reservation = reservation, room = room, sortOrder = i))
        }

        val prices = command.nightlyPrices.sortedWith(compareBy({ it.date }, { command.roomIds.indexOf(it.roomId) }))
        val discounts = AmountSplitter.split(command.discount, prices.map { it.amount })
        val roomsById = rooms.associateBy { it.id }
        prices.zip(discounts).forEach { (price, discount) ->
            settlementLineRepository.save(
                SettlementLineEntity.create(
                    reservation = reservation,
                    room = roomsById.getValue(price.roomId),
                    importBatchId = null,
                    lineType = SettlementLineType.ROOM_NIGHT,
                    stayDate = price.date,
                    seq = 0,
                    settledOn = null,
                    amounts = SettlementAmounts(
                        gross = price.amount,
                        partnerDiscount = discount,
                        commissionRate = BigDecimal.ZERO,
                        net = price.amount - discount,
                    ),
                    splitEstimated = false,
                )
            )
        }

        val gross = prices.fold(BigDecimal.ZERO) { sum, p -> sum + p.amount }
        reservation.applyTotals(gross, BigDecimal.ZERO, gross - command.discount)
        reservation.applyStay(command.checkInDate, command.nights, ReservationStatus.CONFIRMED)
        reservation.guestCount = command.guestCount
        reservation.paymentMethod = command.paymentMethod
        reservation.paid = command.paid
        reservation.memo = command.memo?.trim()?.ifEmpty { null }
        reservation.paymentReceiver = if (isOwnerPaid(rooms)) PaymentReceiver.OWNER else PaymentReceiver.US
        refresh(reservation)
    }

    private fun refresh(reservation: ReservationEntity) {
        settlementLineRepository.flush()
        stayCalendarService.refresh(listOf(reservation))
        managementFeeService.refresh(listOf(reservation))
    }

    private fun validate(command: Command, excludeReservationId: Long?): List<RoomEntity> {
        val rooms = loadRooms(command.roomIds)
        validateStay(command.nights)
        require(command.bookerName.isNotBlank()) { "예약자 이름을 입력해 주세요." }
        require(command.salesChannel in DIRECT_CHANNELS) { "직접 예약 채널은 전화·현장·기타만 고를 수 있습니다." }
        require(command.guestCount == null || command.guestCount > 0) { "인원은 1명 이상이어야 합니다." }
        require((command.memo?.length ?: 0) <= 1000) { "메모는 1000자까지 입력할 수 있습니다." }

        val expected = command.roomIds.flatMap { roomId -> (0 until command.nights).map { roomId to command.checkInDate.plusDays(it.toLong()) } }.toSet()
        val given = command.nightlyPrices.map { it.roomId to it.date }
        require(given.size == given.toSet().size && given.toSet() == expected) { "객실마다 숙박일별 금액을 하나씩 입력해 주세요." }
        require(command.nightlyPrices.all { it.amount.signum() >= 0 }) { "금액은 0원 이상이어야 합니다." }
        val total = command.nightlyPrices.fold(BigDecimal.ZERO) { sum, p -> sum + p.amount }
        require(command.discount.signum() >= 0 && command.discount <= total) { "할인은 0원 이상, 숙박 금액 합계 이하여야 합니다." }

        if (!command.allowOverlap) {
            val conflicts = findConflicts(rooms, command.checkInDate, command.nights, excludeReservationId)
            if (conflicts.isNotEmpty()) throw ReservationConflictException(conflicts)
        }
        return rooms
    }

    private fun validateStay(nights: Int) {
        require(nights in 1..MAX_NIGHTS) { "박수는 1~${MAX_NIGHTS}박이어야 합니다." }
    }

    private fun loadRooms(roomIds: List<Long>): List<RoomEntity> {
        require(roomIds.isNotEmpty()) { "객실을 하나 이상 골라 주세요." }
        require(roomIds.toSet().size == roomIds.size) { "같은 객실을 두 번 고를 수 없습니다." }
        val byId = roomRepository.findAllById(roomIds).associateBy { it.id }
        val rooms = roomIds.map { byId[it] ?: throw IllegalArgumentException("객실을 찾을 수 없습니다: $it") }
        require(rooms.map { it.property.id }.toSet().size == 1) { "한 예약의 객실은 같은 업소여야 합니다." }
        return rooms
    }

    /** 같은 객실에 날짜가 겹치는 투숙 예약 (퇴실일 = 입실일은 겹치지 않음) */
    private fun findConflicts(rooms: List<RoomEntity>, checkIn: LocalDate, nights: Int, excludeReservationId: Long?): List<Conflict> {
        val checkOut = checkIn.plusDays(nights.toLong())
        return reservationRoomRepository
            .findConfirmedByRoomsAndCheckInBetween(rooms.map { it.id!! }, checkIn.minusDays(MAX_NIGHTS.toLong()), checkOut.minusDays(1))
            .filter { it.reservation.id != excludeReservationId }
            .mapNotNull { rr ->
                val r = rr.reservation
                val start = r.checkInDate ?: return@mapNotNull null
                val end = start.plusDays((r.nights ?: 1).toLong())
                if (start < checkOut && end > checkIn) {
                    Conflict(r.id!!, r.source, r.reservationNumber, rr.room.name, r.bookerName, start, end)
                } else {
                    null
                }
            }
            .sortedBy { it.checkInDate }
    }

    private fun isOwnerPaid(rooms: List<RoomEntity>) = rooms.any { it.businessOwnerEntity.ownershipType == OwnershipType.EXTERNAL }

    private fun findDirect(id: Long): ReservationEntity {
        val reservation = reservationRepository.findById(id).orElseThrow { IllegalArgumentException("예약을 찾을 수 없습니다: $id") }
        require(reservation.source == OtaPlatform.DIRECT) { "직접 입력한 예약만 수정·취소·삭제할 수 있습니다." }
        return reservation
    }

    /** D261113-01: 입력한 날짜 + 그날 순번 */
    private fun nextReservationNumber(): String {
        val prefix = "D" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd")) + "-"
        val last = reservationRepository.findBySourceAndReservationNumberStartingWith(OtaPlatform.DIRECT, prefix)
            .mapNotNull { it.reservationNumber.removePrefix(prefix).toIntOrNull() }
            .maxOrNull() ?: 0
        return prefix + (last + 1).toString().padStart(2, '0')
    }

    companion object {
        private const val MAX_NIGHTS = 90
        val DIRECT_CHANNELS = setOf(SalesChannel.PHONE, SalesChannel.WALK_IN, SalesChannel.OTHER)
    }
}
