package kr.sunsetcoast.domain.settlement.parser

import kr.sunsetcoast.domain.reservation.vo.OtaPlatform
import kr.sunsetcoast.domain.settlement.vo.SettlementAmounts
import kr.sunsetcoast.domain.settlement.vo.SettlementLineType
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 플랫폼 파일 한 행에서 읽은 정산 행 (플랫폼 공통 형식)
 */
data class ParsedSettlementLine(
    val reservationNumber: String,
    val roomName: String,
    val lineType: SettlementLineType,
    val stayDate: LocalDate,
    val settledOn: LocalDate?,
    val amounts: SettlementAmounts,
    val salesChannelRaw: String? = null,
    val bookerName: String? = null,
    val reservedAt: LocalDateTime? = null,
    /** 파일에 입실일/박수가 직접 있는 경우(떠나요)만 값이 있다 */
    val checkInDate: LocalDate? = null,
    val nights: Int? = null,
    val splitEstimated: Boolean = false,
)

data class ParsedRow(
    val rowNo: Int,
    val payload: Map<String, String>,
    val lines: List<ParsedSettlementLine>,
    val error: String? = null,
)

data class ParsedSettlementFile(
    val platform: OtaPlatform,
    val rows: List<ParsedRow>,
)
