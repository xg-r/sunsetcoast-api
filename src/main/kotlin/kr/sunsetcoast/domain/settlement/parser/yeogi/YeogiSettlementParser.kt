package kr.sunsetcoast.domain.settlement.parser.yeogi

import kr.sunsetcoast.domain.reservation.vo.OtaPlatform
import kr.sunsetcoast.domain.settlement.parser.ParsedSettlementLine
import kr.sunsetcoast.domain.settlement.parser.SettlementFileParser
import kr.sunsetcoast.domain.settlement.parser.sheet.SheetRow
import kr.sunsetcoast.domain.settlement.vo.SettlementAmounts
import kr.sunsetcoast.domain.settlement.vo.SettlementLineType
import org.springframework.stereotype.Component

/**
 * 여기어때 정산 파일 (주 1회 수요일, 1박 = 1행).
 *
 * 1행은 안내문, 2행이 헤더, 마지막 행은 '합계'(정산일 비어 있음).
 * 정산금액 = 정산대상금액 - 수수료 - 파트너 부담 할인/쿠폰
 */
@Component
class YeogiSettlementParser : SettlementFileParser {
    override val platform = OtaPlatform.YEOGI

    override val requiredHeaders = setOf(
        SETTLED_ON, RESERVATION_NUMBER, TYPE, ROOM, RECOGNIZED_ON, TARGET_AMOUNT, SETTLEMENT,
    )

    override fun parse(row: SheetRow): List<ParsedSettlementLine>? {
        val reservationNumber = row.textOrNull(RESERVATION_NUMBER) ?: return null
        if (row.textOrNull(SETTLED_ON) == null) return null

        val lineType = when (val type = row.text(TYPE)) {
            "입실" -> SettlementLineType.ROOM_NIGHT
            "취소수수료" -> SettlementLineType.CANCELLATION_FEE
            "입실후취소" -> SettlementLineType.ROOM_NIGHT_REVERSAL
            else -> throw IllegalArgumentException("알 수 없는 '$TYPE' 값: $type")
        }

        val amounts = SettlementAmounts(
            gross = row.amount(TARGET_AMOUNT),
            partnerDiscount = row.amounts("파트너 부담 할인금액", "파트너 부담 쿠폰금액"),
            platformDiscount = row.amounts(
                "여기어때 부담 할인금액", "여기어때 부담 쿠폰금액", "제3자 부담 쿠폰금액", "제3자포인트 금액", "무상포인트 금액",
            ),
            commissionRate = row.percent("최종 수수료율"),
            commission = row.amount("수수료"),
            net = row.amount(SETTLEMENT),
        )
        require(amounts.isBalanced()) {
            "금액 검증 실패: 정산대상금액(${amounts.gross}) - 수수료(${amounts.commission}) - 파트너 부담(${amounts.partnerDiscount}) != 정산금액(${amounts.net})"
        }

        return listOf(
            ParsedSettlementLine(
                reservationNumber = reservationNumber,
                roomName = row.required(ROOM),
                lineType = lineType,
                stayDate = row.date(RECOGNIZED_ON),
                settledOn = row.date(SETTLED_ON),
                amounts = amounts,
                salesChannelRaw = row.textOrNull("판매채널"),
                bookerName = row.textOrNull("예약자"),
                reservedAt = row.dateTimeOrNull("결제일시"),
            )
        )
    }

    companion object {
        const val SETTLED_ON = "정산일"
        const val RESERVATION_NUMBER = "예약번호"
        const val TYPE = "구분"
        const val ROOM = "옵션상품"
        const val RECOGNIZED_ON = "매출인식일"
        const val TARGET_AMOUNT = "정산대상금액"
        const val SETTLEMENT = "정산금액"
    }
}
