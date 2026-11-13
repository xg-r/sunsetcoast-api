package kr.sunsetcoast.domain.settlement.parser.ddnayo

import kr.sunsetcoast.domain.reservation.vo.OtaPlatform
import kr.sunsetcoast.domain.settlement.parser.ParsedSettlementLine
import kr.sunsetcoast.domain.settlement.parser.SettlementFileParser
import kr.sunsetcoast.domain.settlement.parser.sheet.SheetRow
import kr.sunsetcoast.domain.settlement.parser.sheet.ratioPercent
import kr.sunsetcoast.domain.settlement.vo.SettlementAmounts
import kr.sunsetcoast.domain.settlement.vo.SettlementLineType
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 떠나요 정산 파일 (주 1회, 예약 1건 = 1행).
 *
 * 박별 금액이 없으므로 연박은 박수로 균등 분할하고 splitEstimated로 표시한다.
 * 떠나요는 선셋코스트의 네이버/야놀자/홈페이지 예약을 모아 정산하며, 실제 채널은 '유입' 컬럼에 있다.
 */
@Component
class DdnayoSettlementParser : SettlementFileParser {
    override val platform = OtaPlatform.DDNAYO

    override val requiredHeaders = setOf(
        RESERVATION_NUMBER, ROOM, USE_DATE, NIGHTS, SALES, SETTLEMENT, COMMISSION, RESERVATION_STATUS,
    )

    override fun parse(row: SheetRow): List<ParsedSettlementLine>? {
        val reservationNumber = row.textOrNull(RESERVATION_NUMBER) ?: return null

        val checkIn = row.date(USE_DATE)
        val nights = row.amount(NIGHTS).toInt()
        require(nights > 0) { "'$NIGHTS' 값이 올바르지 않습니다: ${row.text(NIGHTS)}" }

        val gross = row.amount(SALES)
        val commission = row.amount(COMMISSION)
        val net = row.amount(SETTLEMENT)
        val cancelled = row.text(RESERVATION_STATUS).contains("취소")

        val base = ParsedSettlementLine(
            reservationNumber = reservationNumber,
            roomName = row.required(ROOM),
            lineType = SettlementLineType.ROOM_NIGHT,
            stayDate = checkIn,
            settledOn = null,
            amounts = SettlementAmounts(gross = gross, net = net),
            salesChannelRaw = row.textOrNull(INFLOW),
            bookerName = row.textOrNull(BOOKER),
            reservedAt = row.dateTimeOrNull(RESERVED_AT),
            checkInDate = checkIn,
            nights = nights,
        )

        if (cancelled) {
            // 취소: 정산금액이 곧 취소수수료(위약금) 순액. 수수료 없는 무료 취소는 저장할 금액이 없다.
            if (net.signum() == 0 && commission.signum() == 0) return emptyList()
            val feeGross = net + commission
            return listOf(
                base.copy(
                    lineType = SettlementLineType.CANCELLATION_FEE,
                    amounts = SettlementAmounts(
                        gross = feeGross,
                        commissionRate = commission.ratioPercent(feeGross),
                        commission = commission,
                        net = net,
                    ),
                )
            )
        }

        val total = SettlementAmounts(
            gross = gross,
            commissionRate = commission.ratioPercent(gross),
            commission = commission,
            net = net,
        )
        require(total.isBalanced()) { "금액 검증 실패: 매출($gross) - 수수료($commission) != 정산금액($net)" }

        val grossPerNight = split(gross, nights)
        val commissionPerNight = split(commission, nights)
        val netPerNight = split(net, nights)

        return (0 until nights).map { i ->
            base.copy(
                stayDate = checkIn.plusDays(i.toLong()),
                amounts = total.copy(
                    gross = grossPerNight[i],
                    commission = commissionPerNight[i],
                    net = netPerNight[i],
                ),
                splitEstimated = nights > 1,
            )
        }
    }

    /** 원 단위로 나누고 나머지는 마지막 박에 더한다. */
    private fun split(amount: BigDecimal, nights: Int): List<BigDecimal> {
        val each = amount.divide(BigDecimal(nights), 0, RoundingMode.DOWN)
        val last = amount - each * BigDecimal(nights - 1)
        return List(nights - 1) { each } + last
    }

    companion object {
        const val RESERVATION_NUMBER = "예약번호"
        const val ROOM = "객실/자리"
        const val RESERVED_AT = "예약일"
        const val INFLOW = "유입"
        const val BOOKER = "예약자"
        const val USE_DATE = "이용일"
        const val NIGHTS = "이용박수"
        const val SALES = "매출"
        const val SETTLEMENT = "정산금액"
        const val COMMISSION = "수수료"
        const val RESERVATION_STATUS = "예약상태"
    }
}
