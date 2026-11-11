package kr.sunsetcoast.domain.settlement.parser.sheet

/**
 * 헤더 행 아래의 데이터 행들. 안내문/합계 행이 헤더 위에 있는 파일(여기어때, NOL)도 있어서
 * 헤더 위치는 필수 헤더가 모두 있는 첫 행으로 찾는다.
 */
class SheetTable(
    val headers: List<String>,
    val rows: List<SheetRow>,
)

class RawSheet(
    /** 행 번호(1부터) -> 셀 값 목록 */
    val rows: List<Pair<Int, List<String>>>,
) {
    fun tableWithHeaders(required: Set<String>, searchRows: Int = 10): SheetTable? {
        val headerIndex = rows.take(searchRows).indexOfFirst { (_, cells) ->
            cells.map { it.trim() }.containsAll(required)
        }
        if (headerIndex < 0) return null

        val headers = rows[headerIndex].second.map { it.trim() }
        val dataRows = rows.drop(headerIndex + 1).map { (rowNo, cells) ->
            SheetRow(
                rowNo = rowNo,
                values = headers.withIndex()
                    .filter { it.value.isNotEmpty() }
                    .associate { (i, header) -> header to cells.getOrElse(i) { "" } },
            )
        }.filterNot { it.isBlank() }

        return SheetTable(headers, dataRows)
    }
}
