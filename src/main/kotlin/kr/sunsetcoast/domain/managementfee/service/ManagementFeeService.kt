package kr.sunsetcoast.domain.managementfee.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kr.sunsetcoast.domain.managementfee.entity.ManagementFeeEntity
import kr.sunsetcoast.domain.managementfee.repository.ManagementFeeRateRepository
import kr.sunsetcoast.domain.managementfee.repository.ManagementFeeRepository
import kr.sunsetcoast.domain.managementfee.vo.FeeStay
import kr.sunsetcoast.domain.managementfee.vo.ManagementFeeRules
import kr.sunsetcoast.domain.reservation.entity.ReservationEntity
import kr.sunsetcoast.domain.reservation.repository.ReservationRepository
import kr.sunsetcoast.domain.reservation.vo.ReservationStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
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
) {
    /**
     * 바뀐 예약과, 이어진 예약 판단이 달라질 수 있는 같은 객실의 앞뒤 예약을 다시 계산한다.
     * (앞 예약이 취소되면 뒤 예약이 다시 지급 대상이 된다)
     */
    @Transactional
    fun refresh(reservations: Collection<ReservationEntity>) {
        if (reservations.isEmpty()) return
        val checkIns = reservations.mapNotNull { it.checkInDate }
        val neighbors = if (checkIns.isEmpty()) {
            emptyList()
        } else {
            reservationRepository.findByRoomsAndCheckInBetween(
                reservations.map { it.room.id!! }.toSet(),
                checkIns.min().minusDays(MAX_NIGHTS),
                checkIns.max().plusDays(MAX_NIGHTS),
            )
        }
        recalculate((reservations + neighbors).distinctBy { it.id })
    }

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
        val rates = managementFeeRateRepository.findByRoomIdIn(reservations.map { it.room.id!! }.toSet())
            .groupBy { it.roomId }
        // 다중 객실 예약이 들어오면 (예약, 객실)마다 FeeStay를 만든다.
        val stays = reservations.associateBy { it.toFeeStay() }
        val decisions = ManagementFeeRules.decide(stays.keys) { roomId, checkIn ->
            rates[roomId]?.singleOrNull { it.appliesTo(checkIn) }?.amount
        }

        val existing = managementFeeRepository.findByReservationIdIn(reservations.map { it.id!! })
            .associateBy { it.reservation.id!! to it.room.id!! }
        decisions.forEach { (stay, decision) ->
            val fee = existing[stay.reservationId to stay.roomId]
            if (fee != null) {
                fee.apply(decision)
            } else {
                val reservation = stays.getValue(stay)
                managementFeeRepository.save(
                    ManagementFeeEntity(
                        reservation = reservation,
                        room = reservation.room,
                        basis = decision.basis,
                        calculatedAmount = decision.amount,
                        consecutiveOfReservationId = decision.consecutiveOfReservationId,
                    )
                )
            }
        }
    }

    private fun ReservationEntity.toFeeStay() = FeeStay(
        reservationId = id!!,
        roomId = room.id!!,
        bookerName = bookerName,
        cancelled = status == ReservationStatus.CANCELLED,
        checkInDate = checkInDate,
        nights = nights,
    )

    companion object {
        /** 한 예약의 최대 박수 가정 (한달살기 포함). 이어진 예약을 찾을 때 입실일 검색 범위로 쓴다. */
        private const val MAX_NIGHTS = 90L
    }
}
