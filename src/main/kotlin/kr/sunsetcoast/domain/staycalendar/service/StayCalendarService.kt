package kr.sunsetcoast.domain.staycalendar.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kr.sunsetcoast.domain.reservation.entity.ReservationEntity
import kr.sunsetcoast.domain.reservation.repository.ReservationRepository
import kr.sunsetcoast.domain.settlement.entity.SettlementLineEntity
import kr.sunsetcoast.domain.settlement.repository.SettlementLineRepository
import kr.sunsetcoast.domain.settlement.vo.SettlementLineType
import kr.sunsetcoast.domain.staycalendar.entity.SeasonDayEntity
import kr.sunsetcoast.domain.staycalendar.repository.HolidayRepository
import kr.sunsetcoast.domain.staycalendar.repository.RoomListPriceRepository
import kr.sunsetcoast.domain.staycalendar.repository.SeasonDayRepository
import kr.sunsetcoast.domain.staycalendar.vo.NightAmount
import kr.sunsetcoast.domain.staycalendar.vo.ReservationStayFlags
import kr.sunsetcoast.domain.staycalendar.vo.SeasonMatcher
import kr.sunsetcoast.domain.staycalendar.vo.WeekendPolicy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

private val logger = KotlinLogging.logger {}

/**
 * 성수기/주말 피처 계산.
 *
 * 1. 예약의 1박 금액을 정가와 비교해 업소·날짜별 성수기/비수기 표를 모으고 다수결로 season_days를 정한다.
 *    한 예약만 맞아도 그날 같은 업소 전체에 적용되므로, 예약 하나하나로 판단할 때보다 훨씬 많은 날짜가 채워진다.
 * 2. 그 날짜에 걸친 예약마다 weekend / peak_season을 다시 계산한다.
 */
@Service
class StayCalendarService(
    private val reservationRepository: ReservationRepository,
    private val settlementLineRepository: SettlementLineRepository,
    private val holidayRepository: HolidayRepository,
    private val roomListPriceRepository: RoomListPriceRepository,
    private val seasonDayRepository: SeasonDayRepository,
) {
    /** 바뀐 예약이 걸친 업소·날짜를 다시 계산한다. (정산 파일 업로드 후 호출) */
    @Transactional
    fun refresh(reservations: Collection<ReservationEntity>) {
        if (reservations.isEmpty()) return
        val lines = linesByReservation(reservations)

        // 취소로 빠진 밤도 그날의 표가 바뀌므로 다시 계산 대상에 넣는다.
        reservations
            .flatMap { r -> lines[r.id].orEmpty().filter { it.isNightLine() }.map { r.propertyId() to it.stayDate } }
            .groupBy({ it.first }, { it.second })
            .forEach { (propertyId, dates) -> recompute(propertyId, dates.toSet()) }

        reservations.filter { stayOf(lines[it.id].orEmpty()).nights.isEmpty() }
            .forEach { it.applyStayFlags(weekend = null, peakSeason = null) }
    }

    /** 전체 다시 계산. 공휴일 시드를 추가했거나 요금표를 고친 뒤 실행한다. */
    @Transactional
    fun recalculateAll(): Int {
        val reservations = reservationRepository.findAllWithRoom()
        refresh(reservations)
        logger.info { "stay calendar recalculated reservations=${reservations.size}" }
        return reservations.size
    }

    @Transactional(readOnly = true)
    fun findSeasonDays(propertyId: Long, from: LocalDate, to: LocalDate): List<SeasonDayEntity> {
        require(!from.isAfter(to)) { "조회 시작일이 종료일보다 늦습니다: $from ~ $to" }
        return seasonDayRepository.findByPropertyIdAndStayDateBetween(propertyId, from, to).sortedBy { it.stayDate }
    }

    /**
     * 관리자가 날짜의 성수기 여부를 고친다. peak가 null이면 수정을 취소하고 자동 판단으로 돌아간다.
     * 그날에 걸친 예약의 피처도 다시 계산한다.
     */
    @Transactional
    fun overrideSeason(propertyId: Long, date: LocalDate, peak: Boolean?): SeasonDayEntity {
        val day = seasonDayRepository.findByPropertyIdAndStayDate(propertyId, date)
            ?: seasonDayRepository.save(SeasonDayEntity(propertyId = propertyId, stayDate = date))
        if (peak == null) day.clearManual() else day.overrideManually(peak)
        recompute(propertyId, setOf(date))
        return day
    }

    private fun recompute(propertyId: Long, dates: Set<LocalDate>) {
        if (dates.isEmpty()) return
        val from = dates.min()
        val to = dates.max()

        val stays = reservationRepository
            .findByPropertyAndCheckInBetween(propertyId, from.minusDays(MAX_NIGHTS), to)
            .let { candidates ->
                val lines = linesByReservation(candidates)
                candidates.associateWith { stayOf(lines[it.id].orEmpty()) }
            }
            .filterValues { stay -> stay.nights.any { it.night in dates } }

        val weekendPolicy = WeekendPolicy(
            holidayRepository.findByHolidayDateBetween(from.minusDays(MAX_NIGHTS), to.plusDays(MAX_NIGHTS + 1))
                .map { it.holidayDate }.toSet()
        )
        val prices = roomListPriceRepository.findAllById(stays.keys.map { it.room.id!! }.toSet())
            .associate { it.roomId to it.toListPrice() }

        val peakVotes = HashMap<LocalDate, Int>()
        val offpeakVotes = HashMap<LocalDate, Int>()
        stays.forEach { (reservation, stay) ->
            val price = prices[reservation.room.id] ?: return@forEach
            SeasonMatcher.votes(price, stay.nights, stay.splitEstimated, weekendPolicy)
                .filterKeys { it in dates }
                .forEach { (night, peak) -> (if (peak) peakVotes else offpeakVotes).merge(night, 1, Int::plus) }
        }

        val seasonDays = seasonDayRepository
            .findByPropertyIdAndStayDateBetween(propertyId, from.minusDays(MAX_NIGHTS), to.plusDays(MAX_NIGHTS))
            .associateByTo(HashMap()) { it.stayDate }
        dates.forEach { date ->
            val day = seasonDays.getOrPut(date) {
                seasonDayRepository.save(SeasonDayEntity(propertyId = propertyId, stayDate = date))
            }
            day.applyVotes(peakVotes[date] ?: 0, offpeakVotes[date] ?: 0)
        }

        stays.forEach { (reservation, stay) ->
            val nights = stay.nights.map { it.night }
            reservation.applyStayFlags(
                weekend = ReservationStayFlags.weekend(nights, weekendPolicy),
                peakSeason = ReservationStayFlags.peakSeason(nights) { seasonDays[it]?.peak },
            )
        }
    }

    private fun linesByReservation(reservations: Collection<ReservationEntity>): Map<Long?, List<SettlementLineEntity>> {
        val ids = reservations.mapNotNull { it.id }
        if (ids.isEmpty()) return emptyMap()
        return settlementLineRepository.findByReservationIdIn(ids).groupBy { it.reservation.id }
    }

    /** 실제 투숙한 밤 (입실 후 취소로 차감된 밤 제외)과 할인 전 1박 금액 */
    private fun stayOf(lines: List<SettlementLineEntity>): Stay {
        val reversed = lines.filter { it.lineType == SettlementLineType.ROOM_NIGHT_REVERSAL }.map { it.stayDate }.toSet()
        val roomNights = lines.filter { it.lineType == SettlementLineType.ROOM_NIGHT && it.stayDate !in reversed }
        val nights = roomNights.groupBy { it.stayDate }
            .map { (night, ls) -> NightAmount(night, ls.sumOf { it.grossAmount + it.partnerDiscount + it.platformDiscount }) }
            .sortedBy { it.night }
        return Stay(nights, splitEstimated = roomNights.any { it.splitEstimated })
    }

    private fun SettlementLineEntity.isNightLine() =
        lineType == SettlementLineType.ROOM_NIGHT || lineType == SettlementLineType.ROOM_NIGHT_REVERSAL

    private fun ReservationEntity.propertyId(): Long = room.property.id!!

    private data class Stay(val nights: List<NightAmount>, val splitEstimated: Boolean)

    companion object {
        /** 한 예약의 최대 박수 가정 (한달살기 포함). 날짜에 걸친 예약을 찾을 때 입실일 검색 범위로 쓴다. */
        private const val MAX_NIGHTS = 90L
    }
}
