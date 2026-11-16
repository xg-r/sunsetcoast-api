package kr.sunsetcoast.domain.analytics.service

import kr.sunsetcoast.domain.reservation.vo.OtaPlatform
import kr.sunsetcoast.domain.settlement.repository.MonthlyRevenueRow
import kr.sunsetcoast.domain.settlement.repository.SettlementLineRepository
import kr.sunsetcoast.domain.settlement.vo.SettlementLineType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth

/**
 * 예약(정산 행) 기준 월 매출과 전년 동월 비교.
 *
 * - 월은 숙박일 기준 (연박은 박마다 그 달 매출). 취소수수료는 매출에 넣고 객실·박수·ADR에서는 뺀다.
 * - 매출 = 판매가(gross), 정산액 = 수수료·할인을 뺀 net. 외부 소유주 객실은 뺀다.
 * - 정산 행이 하나도 없는 달은 '데이터 없음'(null)으로 두어 0원과 구분한다 (작년 파일을 아직 안 올린 달).
 */
@Service
@Transactional(readOnly = true)
class RevenueAnalyticsService(
    private val settlementLineRepository: SettlementLineRepository,
) {
    data class MonthTotals(
        val gross: BigDecimal,
        val net: BigDecimal,
        /** 객실·박 수 (1박 행 - 입실 후 취소 행) */
        val roomNights: Long,
        val cancellationFees: BigDecimal,
        val byPlatform: Map<OtaPlatform, BigDecimal>,
    ) {
        /** 객실 1박 평균 판매가 (취소수수료 제외) */
        val adr: BigDecimal? get() =
            if (roomNights <= 0) null else (gross - cancellationFees).divide(BigDecimal(roomNights), 0, RoundingMode.HALF_UP)
    }

    data class MonthComparison(val month: YearMonth, val current: MonthTotals?, val previous: MonthTotals?) {
        val grossChange: BigDecimal? get() = change(previous?.gross, current?.gross)
        val netChange: BigDecimal? get() = change(previous?.net, current?.net)
        val roomNightsChange: BigDecimal? get() = change(previous?.roomNights?.toBigDecimal(), current?.roomNights?.toBigDecimal())
        val adrChange: BigDecimal? get() = change(previous?.adr, current?.adr)
    }

    data class YearComparison(
        val year: Int,
        val propertyId: Long?,
        val months: List<MonthComparison>,
        /** 올해와 작년 모두 데이터가 있는 달만 더한 합계 (같은 달끼리 비교) */
        val comparableMonths: List<YearMonth>,
        val comparableCurrent: MonthTotals?,
        val comparablePrevious: MonthTotals?,
    ) {
        val comparableGrossChange: BigDecimal? get() = change(comparablePrevious?.gross, comparableCurrent?.gross)
        val comparableNetChange: BigDecimal? get() = change(comparablePrevious?.net, comparableCurrent?.net)
    }

    fun compareYear(year: Int, propertyId: Long?): YearComparison {
        val rows = settlementLineRepository
            .sumRevenueByMonth(YearMonth.of(year - 1, 1).atDay(1), YearMonth.of(year, 12).atEndOfMonth())
            .filter { propertyId == null || it.propertyId == propertyId }
        val byMonth = rows.groupBy { YearMonth.of(it.year, it.month) }.mapValues { (_, list) -> totals(list) }

        val months = (1..12).map { m ->
            MonthComparison(YearMonth.of(year, m), byMonth[YearMonth.of(year, m)], byMonth[YearMonth.of(year - 1, m)])
        }
        val comparable = months.filter { it.current != null && it.previous != null }
        return YearComparison(
            year = year,
            propertyId = propertyId,
            months = months,
            comparableMonths = comparable.map { it.month },
            comparableCurrent = comparable.mapNotNull { it.current }.reduceOrNull(::plus),
            comparablePrevious = comparable.mapNotNull { it.previous }.reduceOrNull(::plus),
        )
    }

    private fun totals(rows: List<MonthlyRevenueRow>) = MonthTotals(
        gross = rows.sumOf { it.gross },
        net = rows.sumOf { it.net },
        roomNights = rows.sumOf {
            when (it.lineType) {
                SettlementLineType.ROOM_NIGHT -> it.lines
                SettlementLineType.ROOM_NIGHT_REVERSAL -> -it.lines
                SettlementLineType.CANCELLATION_FEE -> 0L
            }
        },
        cancellationFees = rows.filter { it.lineType == SettlementLineType.CANCELLATION_FEE }.sumOf { it.gross },
        byPlatform = rows.groupBy { it.source }.mapValues { (_, list) -> list.sumOf { it.gross } },
    )

    private fun plus(a: MonthTotals, b: MonthTotals) = MonthTotals(
        gross = a.gross + b.gross,
        net = a.net + b.net,
        roomNights = a.roomNights + b.roomNights,
        cancellationFees = a.cancellationFees + b.cancellationFees,
        byPlatform = (a.byPlatform.keys + b.byPlatform.keys).associateWith {
            (a.byPlatform[it] ?: BigDecimal.ZERO) + (b.byPlatform[it] ?: BigDecimal.ZERO)
        },
    )

    companion object {
        /** 증감률 (%). 비교할 수 없으면 null */
        fun change(before: BigDecimal?, now: BigDecimal?): BigDecimal? {
            if (before == null || now == null || before.signum() == 0) return null
            return (now - before).multiply(BigDecimal(100)).divide(before.abs(), 1, RoundingMode.HALF_UP)
        }
    }
}
