package kr.sunsetcoast.domain.managementfee.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kr.sunsetcoast.domain.managementfee.entity.ManagementFeeEntity
import kr.sunsetcoast.domain.managementfee.repository.ManagementFeeRateRepository
import kr.sunsetcoast.domain.managementfee.repository.ManagementFeeRepository
import kr.sunsetcoast.domain.managementfee.vo.FeeStay
import kr.sunsetcoast.domain.managementfee.vo.ManagementFeeRules
import kr.sunsetcoast.domain.reservation.entity.ReservationEntity
import kr.sunsetcoast.domain.reservation.repository.ReservationRepository
import kr.sunsetcoast.domain.reservation.repository.ReservationRoomRepository
import kr.sunsetcoast.domain.reservation.vo.ReservationStatus
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

    @Transactional(readOnly = true)
    fun findMonthly(propertyId: Long, month: YearMonth): List<ManagementFeeEntity> =
        managementFeeRepository
            .findByPropertyAndCheckInBetween(propertyId, month.atDay(1), month.atEndOfMonth())
            .sortedWith(compareBy({ it.reservation.checkInDate }, { it.room.id }, { it.reservation.id }))

    /** 관리자가 금액을 고친다. amount가 null이면 수정을 취소하고 자동 계산값으로 돌아간다. */
    @Transactional
    fun override(feeId: Long, amount: BigDecimal?, reason: String?): ManagementFeeEntity {
        val fee = managementFeeRepository.findById(feeId)
            .orElseThrow { IllegalArgumentException("관리비를 찾을 수 없습니다: $feeId") }
        if (amount == null) fee.clearOverride() else fee.overrideManually(amount, reason.orEmpty())
        return fee
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
