package kr.sunsetcoast.domain.settlement.parser

import kr.sunsetcoast.domain.reservation.vo.OtaPlatform
import kr.sunsetcoast.domain.settlement.parser.sheet.SheetRow

interface SettlementFileParser {
    val platform: OtaPlatform

    /** 이 헤더가 모두 있는 행이 있으면 이 플랫폼의 정산 파일로 판단한다. */
    val requiredHeaders: Set<String>

    /**
     * @return 정산 행 목록. 합계/안내 행처럼 무시할 행은 null.
     * @throws IllegalArgumentException 형식 오류나 금액 검증 실패
     */
    fun parse(row: SheetRow): List<ParsedSettlementLine>?
}
