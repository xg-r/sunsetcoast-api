package kr.sunsetcoast.domain.settlement.parser

import kr.sunsetcoast.domain.reservation.vo.OtaPlatform
import kr.sunsetcoast.domain.settlement.parser.sheet.SheetRow
import kr.sunsetcoast.domain.settlement.parser.sheet.WorkbookReader
import org.springframework.stereotype.Component

/**
 * 파일 내용(헤더)으로 플랫폼을 판별하고 행 단위로 파싱한다.
 * 행 하나의 오류가 파일 전체를 막지 않도록 오류는 행에 담아 돌려준다.
 */
@Component
class SettlementFileParsers(
    private val parsers: List<SettlementFileParser>,
) {
    /**
     * @throws IllegalArgumentException 엑셀 파일이 아니거나 지원하는 정산 파일 형식이 아닌 경우
     */
    fun parse(bytes: ByteArray): ParsedSettlementFile {
        val sheet = runCatching { WorkbookReader.readFirstSheet(bytes) }
            .getOrElse { throw IllegalArgumentException("엑셀 파일을 읽을 수 없습니다: ${it.message}") }

        for (parser in parsers) {
            val table = sheet.tableWithHeaders(parser.requiredHeaders) ?: continue
            val rows = table.rows.mapNotNull { row ->
                val payload = row.values.filterKeys { header -> PII_HEADERS.none { header.contains(it) } }
                try {
                    parser.parse(row)?.let { ParsedRow(row.rowNo, payload, it) }
                } catch (e: IllegalArgumentException) {
                    ParsedRow(row.rowNo, payload, emptyList(), e.message ?: "형식 오류")
                }
            }
            return ParsedSettlementFile(parser.platform, rows)
        }
        throw IllegalArgumentException("지원하는 정산 파일 형식이 아닙니다. (떠나요/여기어때/NOL 정산 파일인지 확인해 주세요)")
    }

    /**
     * 보존해 둔 원본 행(개인정보 컬럼 제외)을 다시 파싱한다. 미매핑 행을 나중에 처리할 때 쓴다.
     * @throws IllegalArgumentException 형식 오류
     */
    fun parseRow(platform: OtaPlatform, rowNo: Int, payload: Map<String, String>): List<ParsedSettlementLine> {
        val parser = parsers.single { it.platform == platform }
        return parser.parse(SheetRow(rowNo, payload)).orEmpty()
    }

    companion object {
        /** 원본 행 보존 시 제외할 개인정보 컬럼 */
        private val PII_HEADERS = listOf("연락처", "전화", "휴대폰", "이메일")
    }
}
