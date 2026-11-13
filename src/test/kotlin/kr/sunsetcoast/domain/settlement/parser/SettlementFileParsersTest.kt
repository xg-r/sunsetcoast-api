package kr.sunsetcoast.domain.settlement.parser

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import kr.sunsetcoast.domain.reservation.vo.OtaPlatform
import kr.sunsetcoast.domain.settlement.parser.ddnayo.DdnayoSettlementParser
import kr.sunsetcoast.domain.settlement.parser.nol.NolSettlementParser
import kr.sunsetcoast.domain.settlement.parser.yeogi.YeogiSettlementParser
import kr.sunsetcoast.domain.settlement.vo.SettlementLineType
import java.math.BigDecimal
import java.time.LocalDate

class SettlementFileParsersTest : FunSpec({
    val parsers = SettlementFileParsers(
        listOf(DdnayoSettlementParser(), YeogiSettlementParser(), NolSettlementParser())
    )

    fun money(value: Long) = BigDecimal(value).setScale(2)

    context("떠나요 정산 파일 (xls, 예약 단위)") {
        val header = listOf(
            "예약번호", "객실/자리", "예약일", "유입", "예약자", "연락처", "이용일", "이용박수",
            "매출", "정산금액", "수수료", "정산상태", "결제구분", "예약상태", "사업자번호",
        )

        test("숫자 셀 예약번호를 지수 표기 없이 읽고, 연박은 박수로 나눈다") {
            val bytes = WorkbookFixtures.xls(
                listOf(
                    header,
                    listOf(
                        72822751, "Baram 바람", "2026-08-04", "야놀자", "김**", "0503-0000-0000", "2026-08-15", 2,
                        360001, 320401, 39600, "정산완료", "야놀자", "결제완료", "000-00-00000",
                    ),
                )
            )

            val file = parsers.parse(bytes)

            file.platform shouldBe OtaPlatform.DDNAYO
            val lines = file.rows.single().lines
            lines shouldHaveSize 2
            lines.map { it.reservationNumber }.distinct() shouldBe listOf("72822751")
            lines.map { it.stayDate } shouldBe listOf(LocalDate.of(2026, 8, 15), LocalDate.of(2026, 8, 16))
            lines.map { it.amounts.gross } shouldBe listOf(BigDecimal(180000), BigDecimal(180001))
            lines.map { it.amounts.net } shouldBe listOf(BigDecimal(160200), BigDecimal(160201))
            lines.all { it.splitEstimated } shouldBe true
            lines.first().salesChannelRaw shouldBe "야놀자"
        }

        test("원본 행 보존 시 연락처는 제외한다") {
            val bytes = WorkbookFixtures.xls(
                listOf(
                    header,
                    listOf(1, "바다", "2026-08-01", "네이버펜션예약", "김**", "010-0000-0000", "2026-08-02", 1,
                        100000, 92000, 8000, "정산완료", "네이버", "결제완료", ""),
                )
            )

            parsers.parse(bytes).rows.single().payload.keys.contains("연락처") shouldBe false
        }

        test("수수료 없는 취소는 저장할 행이 없고, 위약금이 있으면 취소수수료 행이 된다") {
            val bytes = WorkbookFixtures.xls(
                listOf(
                    header,
                    listOf(1, "바다", "2026-08-01", "떠나요", "", "", "2026-08-20", 1, 170000, 0, 0, "미정산", "떠나요", "예약취소", ""),
                    listOf(2, "바다", "2026-08-01", "떠나요", "", "", "2026-08-21", 1, 222000, 68530, 6000, "완료", "떠나요", "예약취소", ""),
                )
            )

            val rows = parsers.parse(bytes).rows
            rows[0].lines shouldHaveSize 0
            rows[1].lines.single().lineType shouldBe SettlementLineType.CANCELLATION_FEE
            rows[1].lines.single().amounts.net shouldBe BigDecimal(68530)
            rows[1].lines.single().amounts.gross shouldBe BigDecimal(74530)
        }
    }

    context("여기어때 정산 파일 (xlsx, 1박 단위)") {
        val header = listOf(
            "정산일", "지급일", "지급상태", "예약번호", "구분", "상품구분", "옵션상품", "요금제 유형", "예약자", "매출인식일",
            "정산금액", "정산대상금액", "수수료율", "오버라이드 수수료율", "최종 수수료율", "수수료", "취소 수수료율",
            "파트너 부담 할인금액", "여기어때 부담 할인금액", "할인명", "제3자포인트 금액", "무상포인트 금액",
            "파트너 부담 쿠폰금액", "여기어때 부담 쿠폰금액", "제3자 부담 쿠폰금액", "쿠폰명", "결제일시", "판매채널", "B2B 여부",
        )

        fun row(type: String, date: String, net: Double, target: Double, fee: Double, partner: Double = 0.0, yeogiCoupon: Double = 0.0) =
            listOf(
                "2026-08-12", "2026-08-12", "지급완료", "2608110000AF09YE1", type, "숙박", "Villa II", "-", "김**", date,
                net, target, "15.00", "0.00", "15.00", fee, "0", partner, 0.0, "-", 0.0, 0.0, 0.0, yeogiCoupon, 0.0, "-",
                "2026-08-01 12:45:38", "여기어때", "N",
            )

        test("안내문/합계 행은 건너뛰고 구분별 행 유형을 나눈다") {
            val bytes = WorkbookFixtures.xlsx(
                listOf(
                    listOf("상호명: 테스트\n 제공자 : (주)여기어때컴퍼니"),
                    header,
                    row("입실", "2026-08-05", 98685.0, 129000.0, 19350.0, partner = 10965.0, yeogiCoupon = 6966.0),
                    row("취소수수료", "2026-08-06", 10965.0, 12900.0, 1935.0),
                    row("입실후취소", "2026-08-05", -116100.0, -129000.0, -12900.0),
                    listOf(null, null, null, null, null, null, null, null, "합계", null, 1000.0, 2000.0),
                )
            )

            val file = parsers.parse(bytes)

            file.platform shouldBe OtaPlatform.YEOGI
            file.rows shouldHaveSize 3
            file.rows.map { it.lines.single().lineType } shouldBe listOf(
                SettlementLineType.ROOM_NIGHT,
                SettlementLineType.CANCELLATION_FEE,
                SettlementLineType.ROOM_NIGHT_REVERSAL,
            )
            val night = file.rows[0].lines.single()
            night.amounts.partnerDiscount shouldBe BigDecimal(10965)
            night.amounts.platformDiscount shouldBe BigDecimal(6966)
            night.settledOn shouldBe LocalDate.of(2026, 8, 12)
        }

        test("정산금액이 맞지 않으면 그 행만 실패한다") {
            val bytes = WorkbookFixtures.xlsx(
                listOf(header, row("입실", "2026-08-05", 90000.0, 129000.0, 19350.0))
            )

            val parsedRow = parsers.parse(bytes).rows.single()
            parsedRow.error shouldNotBe null
            parsedRow.error!! shouldContain "금액 검증 실패"
        }
    }

    context("NOL 정산상세내역 (확장자 .xls지만 실제 xlsx)") {
        val header = listOf(
            "정산일", "정산 대상 기간", "예약번호", "정산구분", "예약경로", "상품유형", "객실타입", "프로모션", "판매금액",
            "수수료율", "수수료", "NOL 부스팅 서비스 이용료", "NOL 부스팅 서비스 이용 요율", "NOL 부스팅 프로그램 계약ID",
            "정산금액", "할인쿠폰 (총액)", "할인쿠폰이름", "더하기쿠폰1 (총액)", "더하기쿠폰1이름", "더하기쿠폰2 (총액)",
            "더하기쿠폰2이름", "조정사유", "프로모션 할인",
        )

        fun row(type: String, sale: String, fee: String, net: String) = listOf(
            "2026-08-27", "2026-08-21", "2608131029550192", type, "NOL 예약", "일반 예약", "Resort A 207호", "",
            sale, "13.0%", fee, "0원", "0.0%", "-", net, "1,891원", "쿠폰", "0원", "", "0원", "", "", "",
        )

        test("원 표기 금액을 읽고, 합계 행 아래 헤더를 찾는다") {
            val bytes = WorkbookFixtures.xlsx(
                listOf(
                    listOf("정산 내역 : 테스트\n정산일 기준 : 2026-08-27 ~ 2026-08-27"),
                    listOf("", "", "", "", "", "", "", "합계", "38,700원", "-", "5,031원"),
                    header,
                    row("취소수수료", "25,800원", "3,354원", "22,446원"),
                    row("취소수수료", "12,900원", "1,677원", "11,223원"),
                )
            )

            val file = parsers.parse(bytes)

            file.rows.first().lines.first().reservedAt shouldBe java.time.LocalDateTime.of(2026, 8, 13, 0, 0)
            file.platform shouldBe OtaPlatform.NOL
            file.rows shouldHaveSize 2
            val first = file.rows[0].lines.single()
            first.lineType shouldBe SettlementLineType.CANCELLATION_FEE
            first.amounts.gross shouldBe BigDecimal(25800)
            first.amounts.net shouldBe BigDecimal(22446)
            first.amounts.commissionRate shouldBe BigDecimal("13.0")
            first.amounts.platformDiscount shouldBe BigDecimal(1891)
            first.amounts.normalized().net shouldBe money(22446)
        }
    }

    test("지원하지 않는 파일은 거부한다") {
        val bytes = WorkbookFixtures.xlsx(listOf(listOf("예약번호", "객실명", "결제액")))

        shouldThrow<IllegalArgumentException> { parsers.parse(bytes) }
            .message!! shouldContain "지원하는 정산 파일 형식이 아닙니다"
    }

    test("엑셀이 아닌 파일은 거부한다") {
        shouldThrow<IllegalArgumentException> { parsers.parse("hello".toByteArray()) }
    }

    test("NOL 예약번호 앞 6자리는 예약일") {
        NolSettlementParser.reservedDateOf("2610131064116996") shouldBe java.time.LocalDateTime.of(2026, 10, 13, 0, 0)
        NolSettlementParser.reservedDateOf("PHC26032700062") shouldBe null
        NolSettlementParser.reservedDateOf("2613991064116996") shouldBe null
    }
})
