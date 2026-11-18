package kr.sunsetcoast.domain.managementfee.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kr.sunsetcoast.domain.managementfee.entity.ManagementFeeEntity
import kr.sunsetcoast.domain.managementfee.repository.ManagementFeeRateRepository
import kr.sunsetcoast.domain.managementfee.repository.ManagementFeeRepository
import kr.sunsetcoast.domain.managementfee.vo.FeeStay
import kr.sunsetcoast.domain.managementfee.vo.ManagementFeeExtraInput
import kr.sunsetcoast.domain.managementfee.vo.ManagementFeeRules
import kr.sunsetcoast.domain.reservation.entity.ReservationEntity
import kr.sunsetcoast.domain.reservation.repository.ReservationRepository
import kr.sunsetcoast.domain.reservation.repository.ReservationRoomRepository
import kr.sunsetcoast.domain.reservation.vo.ReservationStatus
import kr.sunsetcoast.domain.settlement.repository.SettlementLineRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.YearMonth

private val logger = KotlinLogging.logger {}

/**
 * 현장 관리비 (SETTLE-02). 정산 파일 업로드로 예약이 바뀔 때마다 다시 계산한다.
 * 관리비는 입실일이 속한 달에 넣는다.
 */
@Service
class ManagementFeeService(
    private val reservationRepository: ReservationRepository,
    private val managementFeeRepository: ManagementFeeRepository,
    private val managementFeeRateRepository: ManagementFeeRateRepository,
    private val reservationRoomRepository: ReservationRoomRepository,
    private val settlementLineRepository: SettlementLineRepository,
) {
    /** 바뀐 예약의 관리비를 다시 계산한다. (정산 파일 업로드 후 호출) */
    @Transactional
    fun refresh(reservations: Collection<ReservationEntity>) = recalculate(reservations)

    /** 전체 다시 계산. 단가를 고친 뒤 실행한다. 관리자가 고친 금액은 유지된다. */
    @Transactional
    fun recalculateAll(): Int {
        val reservations = reservationRepository.findAllWithRoom()
        recalculate(reservations)
        logger.info { "management fees recalculated reservations=${reservations.size}" }
        return reservations.size
    }

    /** 입실 월의 관리비. 업소나 사업자(명의 객실, 여러 업소에 걸칠 수 있음)로 좁힌다. */
    @Transactional(readOnly = true)
    fun findMonthly(propertyId: Long?, businessOwnerId: Long?, month: YearMonth): List<ManagementFeeEntity> =
        managementFeeRepository
            .findByCheckInBetween(propertyId, businessOwnerId, month.atDay(1), month.atEndOfMonth())
            .sortedWith(compareBy({ it.reservation.checkInDate }, { it.room.id }, { it.reservation.id }))

    /** 관리비 행의 매출. gross: 판매가 합계, net: 플랫폼 수수료를 뺀 정산금액 합계 */
    data class FeeRevenue(val gross: BigDecimal, val net: BigDecimal)

    /**
     * 관리비 행(예약 × 객실)마다 매출. 정산 행은 객실별로 있어서 그 객실 몫만 더한다 (취소수수료 포함).
     * 정산 행이 없는 예약(예약 파일로만 들어온 예약)은 객실이 하나일 때만 예약 합계를 쓰고, 여러 객실이면 모른다(null).
     */
    @Transactional(readOnly = true)
    fun revenueOf(fees: Collection<ManagementFeeEntity>): Map<Long, FeeRevenue> {
        if (fees.isEmpty()) return emptyMap()
        val reservationIds = fees.map { it.reservation.id!! }.toSet()
        val lines = settlementLineRepository.findByReservationIdIn(reservationIds)
        val byRoom = lines.groupBy { it.reservation.id!! to it.room.id!! }
        val withLines = lines.map { it.reservation.id!! }.toSet()
        val roomCount = fees.groupingBy { it.reservation.id!! }.eachCount()
        return fees.mapNotNull { fee ->
            val reservation = fee.reservation
            val revenue = when {
                reservation.id in withLines -> byRoom[reservation.id!! to fee.room.id!!].orEmpty().let { list ->
                    FeeRevenue(list.sumOf { it.grossAmount }, list.sumOf { it.netAmount })
                }
                roomCount[reservation.id] == 1 -> FeeRevenue(reservation.grossAmount, reservation.netAmount)
                else -> null
            }
            revenue?.let { fee.id!! to it }
        }.toMap()
    }

    /** 추가요금을 통째로 바꾼다. 빈 목록이면 모두 지운다. 입금 완료된 관리비는 바꿀 수 없다 */
    @Transactional
    fun replaceExtras(feeId: Long, inputs: List<ManagementFeeExtraInput>): ManagementFeeEntity {
        val fee = managementFeeRepository.findById(feeId)
            .orElseThrow { IllegalArgumentException("관리비를 찾을 수 없습니다: $feeId") }
        fee.replaceExtras(inputs)
        managementFeeRepository.flush()
        logger.info { "management fee extras replaced id=$feeId extras=${inputs.size} total=${fee.extraAmount}" }
        return fee
    }

    /** 관리자가 금액을 고친다. amount가 null이면 수정을 취소하고 자동 계산값으로 돌아간다. */
    @Transactional
    fun override(feeId: Long, amount: BigDecimal?, reason: String?): ManagementFeeEntity {
        val fee = managementFeeRepository.findById(feeId)
            .orElseThrow { IllegalArgumentException("관리비를 찾을 수 없습니다: $feeId") }
        if (amount == null) fee.clearOverride() else fee.overrideManually(amount, reason.orEmpty())
        return fee
    }

    /**
     * 관리자가 입금을 확인해 입금 완료로 표시하거나 취소한다. 여러 건을 한꺼번에 처리한다.
     * 입금 완료로 표시할 때의 지급액을 남겨 두어, 그 뒤 금액이 바뀌면 화면에서 차액을 알 수 있다.
     */
    @Transactional
    fun markPayments(feeIds: Collection<Long>, paid: Boolean, userId: Long?): List<ManagementFeeEntity> {
        require(feeIds.isNotEmpty()) { "관리비를 선택해 주세요." }
        val fees = managementFeeRepository.findAllById(feeIds.toSet())
        val missing = feeIds.toSet() - fees.map { it.id!! }.toSet()
        require(missing.isEmpty()) { "관리비를 찾을 수 없습니다: ${missing.joinToString()}" }
        fees.forEach { fee ->
            when {
                paid && !fee.paid -> fee.markPaid(userId)
                !paid && fee.paid -> fee.unmarkPaid()
            }
        }
        logger.info { "management fee payments paid=$paid ids=${fees.map { it.id }} by=$userId" }
        return fees
    }

    /** 관리자 확인 표시. verified=true면 지금 금액으로 확인(금액이 바뀐 건도 다시 확인), false면 취소 */
    @Transactional
    fun markVerified(feeIds: Collection<Long>, verified: Boolean, userId: Long?): List<ManagementFeeEntity> {
        require(feeIds.isNotEmpty()) { "관리비를 선택해 주세요." }
        val fees = managementFeeRepository.findAllById(feeIds.toSet())
        val missing = feeIds.toSet() - fees.map { it.id!! }.toSet()
        require(missing.isEmpty()) { "관리비를 찾을 수 없습니다: ${missing.joinToString()}" }
        fees.forEach { if (verified) it.verify(userId) else it.unverify() }
        return fees
    }

    enum class PaidBasis {
        /** 관리비가 속한 달 (입실 월) */
        FEE_MONTH,

        /** 입금 완료로 표시한 달 (실제로 돈이 나간 달) */
        PAID_MONTH,
    }

    data class PaidCell(val ownerId: Long, val month: YearMonth, val amount: BigDecimal, val count: Int)

    data class PaidOwner(val id: Long, val name: String)

    data class PaidSummary(
        val from: YearMonth,
        val to: YearMonth,
        val basis: PaidBasis,
        val months: List<YearMonth>,
        val owners: List<PaidOwner>,
        val cells: List<PaidCell>,
    )

    /**
     * 입금 완료된 관리비를 사업자(객실 명의) × 월로 모은다. 금액은 입금 완료로 표시할 때의 지급액.
     * 입금 완료가 아닌 관리비는 넣지 않는다.
     */
    @Transactional(readOnly = true)
    fun paidSummary(from: YearMonth, to: YearMonth, basis: PaidBasis): PaidSummary {
        require(!from.isAfter(to)) { "시작 월이 끝 월보다 늦습니다: $from ~ $to" }
        require(java.time.temporal.ChronoUnit.MONTHS.between(from, to) < 36) { "한 번에 36개월까지 볼 수 있습니다." }
        val fees = when (basis) {
            PaidBasis.FEE_MONTH -> managementFeeRepository.findPaidByCheckInBetween(from.atDay(1), to.atEndOfMonth())
            PaidBasis.PAID_MONTH -> managementFeeRepository.findPaidBetween(from.atDay(1).atStartOfDay(), to.atEndOfMonth().atTime(23, 59, 59))
        }
        fun monthOf(f: ManagementFeeEntity): YearMonth = when (basis) {
            PaidBasis.FEE_MONTH -> YearMonth.from(f.reservation.checkInDate!!)
            PaidBasis.PAID_MONTH -> YearMonth.from(f.paidAt!!)
        }
        val owners = fees.map { it.room.businessOwnerEntity }.distinctBy { it.id }.sortedBy { it.id }.map { PaidOwner(it.id!!, it.name) }
        val cells = fees.groupBy { it.room.businessOwnerEntity.id!! to monthOf(it) }.map { (key, list) ->
            PaidCell(key.first, key.second, list.sumOf { it.paidAmount ?: it.amount }, list.size)
        }
        val months = generateSequence(from) { it.plusMonths(1) }.takeWhile { !it.isAfter(to) }.toList()
        return PaidSummary(from, to, basis, months, owners, cells)
    }

    private fun recalculate(reservations: Collection<ReservationEntity>) {
        if (reservations.isEmpty()) return
        val ids = reservations.map { it.id!! }
        // 다중 객실 예약은 객실마다 관리비를 준다
        val roomsByReservation = reservationRoomRepository.findByReservationIds(ids).groupBy({ it.reservation.id!! }, { it.room })
        val rates = managementFeeRateRepository
            .findByRoomIdIn(reservations.flatMap { r -> roomsByReservation[r.id].orEmpty().map { it.id!! } + r.room.id!! }.toSet())
            .groupBy { it.roomId }
        val existing = managementFeeRepository.findByReservationIdIn(ids)
            .associateBy { it.reservation.id!! to it.room.id!! }
        reservations.forEach { reservation ->
            roomsByReservation[reservation.id].orEmpty().ifEmpty { listOf(reservation.room) }.forEach { room ->
                val stay = reservation.toFeeStay(room.id!!)
                val decision = ManagementFeeRules.decide(stay) { roomId, checkIn ->
                    rates[roomId]?.singleOrNull { it.appliesTo(checkIn) }?.amount
                }
                val fee = existing[stay.reservationId to stay.roomId]
                if (fee != null) {
                    fee.apply(decision)
                } else {
                    managementFeeRepository.save(
                        ManagementFeeEntity(
                            reservation = reservation,
                            room = room,
                            basis = decision.basis,
                            calculatedAmount = decision.amount,
                        )
                    )
                }
            }
        }
    }

    private fun ReservationEntity.toFeeStay(roomId: Long) = FeeStay(
        reservationId = id!!,
        roomId = roomId,
        cancelled = status == ReservationStatus.CANCELLED,
        checkInDate = checkInDate,
        nights = nights,
    )
}
