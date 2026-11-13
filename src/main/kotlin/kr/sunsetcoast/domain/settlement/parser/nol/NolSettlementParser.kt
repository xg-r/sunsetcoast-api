package kr.sunsetcoast.domain.settlement.parser.nol

import kr.sunsetcoast.domain.reservation.vo.OtaPlatform
import kr.sunsetcoast.domain.settlement.parser.ParsedSettlementLine
import kr.sunsetcoast.domain.settlement.parser.SettlementFileParser
import kr.sunsetcoast.domain.settlement.parser.sheet.SheetRow
import kr.sunsetcoast.domain.settlement.vo.SettlementAmounts
import kr.sunsetcoast.domain.settlement.vo.SettlementLineType
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * NOL(야놀자) 정산상세내역 (사업자 계정별, 주 1회, 1박 = 1행).
 *
 * 1행 안내문, 2행 합계, 3행 헤더. 금액은 "350,000원" 형식.
 * 정산금액 = 판매금액 - 수수료 - 부스팅 이용료 (할인쿠폰은 NOL 부담이라 정산금액에 영향 없음)
 */
@Component
class NolSettlementParser : SettlementFileParser {
    override val platform = OtaPlatform.NOL

    override val requiredHeaders = setOf(
        SETTLED_ON, STAY_DATE, RESERVATION_NUMBER, TYPE, ROOM, SALES, SETTLEMENT,
    )

    override fun parse(row: SheetRow): List<ParsedSettlementLine>? {
        val reservationNumber = row.textOrNull(RESERVATION_NUMBER) ?: return null

        val lineType = when (val type = row.text(TYPE)) {
            "투숙완료" -> SettlementLineType.ROOM_NIGHT
            "취소수수료" -> SettlementLineType.CANCELLATION_FEE
            else -> throw IllegalArgumentException("알 수 없는 '$TYPE' 값: $type")
        }

        val amounts = SettlementAmounts(
            gross = row.amount(SALES),
            platformDiscount = row.amounts("할인쿠폰 (총액)", "더하기쿠폰1 (총액)", "더하기쿠폰2 (총액)"),
            commissionRate = row.percent("수수료율"),
            commission = row.amount("수수료"),
            otherFee = row.amount("NOL 부스팅 서비스 이용료"),
            net = row.amount(SETTLEMENT),
        )
        require(amounts.isBalanced()) {
            "금액 검증 실패: 판매금액(${amounts.gross}) - 수수료(${amounts.commission}) - 부스팅(${amounts.otherFee}) != 정산금액(${amounts.net})"
        }

        return listOf(
            ParsedSettlementLine(
                reservationNumber = reservationNumber,
                reservedAt = reservedDateOf(reservationNumber),
                roomName = row.required(ROOM),
                lineType = lineType,
                stayDate = row.date(STAY_DATE),
                settledOn = row.date(SETTLED_ON),
                amounts = amounts,
                salesChannelRaw = row.textOrNull("예약경로"),
            )
        )
    }

    companion object {
        private val NUMBER_DATE = Regex("""^(\d{2})(\d{2})(\d{2})\d{10}$""")

        /**
         * NOL 정산 파일에는 예약 일시가 없다. 예약번호 앞 6자리가 예약일(yyMMdd)이다
         * (2026년 10월 NOL 예약 파일 59건의 '예약 일시'와 모두 일치. 뒤 자리는 시각이 아닐 때가 있어 날짜만 쓴다).
         */
        fun reservedDateOf(reservationNumber: String): LocalDateTime? {
            val (yy, mm, dd) = NUMBER_DATE.matchEntire(reservationNumber)?.destructured ?: return null
            return runCatching { LocalDate.of(2000 + yy.toInt(), mm.toInt(), dd.toInt()).atStartOfDay() }.getOrNull()
        }

        const val SETTLED_ON = "정산일"
        const val STAY_DATE = "정산 대상 기간"
        const val RESERVATION_NUMBER = "예약번호"
        const val TYPE = "정산구분"
        const val ROOM = "객실타입"
        const val SALES = "판매금액"
        const val SETTLEMENT = "정산금액"
    }
}
