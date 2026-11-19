package kr.sunsetcoast.domain.settlement.vo

import java.time.LocalDate

/**
 * 정산 파일명에서 정산 계정(업소·판매자 계정)과 정산일을 읽는다. 누락 확인용.
 *
 * - 떠나요/여기어때: `정산내역_{업소명}_{yyyy-MM-dd}[_떠나요].xls(x)`
 * - 여기어때 기간 파일: `정산내역_{업소명}_{yyyy-MM-dd}_{yyyy-MM-dd}.xlsx` (한 달 치 등). 정산일은 파일의 정산 행에서 읽는다 → [period]
 * - NOL: `정산상세내역_{업소}_{판매자 계정}_{MMdd}.xls` (계정과 날짜 사이 `_`가 빠진 파일도 있다)
 *
 * 이름을 바꾼 파일은 읽지 못한다 (account·issuedOn null).
 */
data class SettlementFileName(
    val account: String?,
    val issuedOn: LocalDate?,
    /** 파일명이 아닌 파일 내용으로 추정한 정산일 (떠나요 원본 파일명) */
    val estimatedIssuedOn: LocalDate? = null,
    /** 파일 내용으로 추정한 정산일 전부 (한 달 치를 한 번에 받은 떠나요 파일은 여러 개) */
    val estimatedDates: List<LocalDate> = listOfNotNull(estimatedIssuedOn),
    /** 기간을 받은 파일의 조회 기간. 이때 issuedOn은 null (정산일이 여러 개) */
    val period: ClosedRange<LocalDate>? = null,
) {
    companion object {
        private val DATED = Regex("""^정산내역_(.+?)_(\d{4})-(\d{2})-(\d{2})(?:_(\d{4})-(\d{2})-(\d{2}))?""")
        private val NOL = Regex("""^정산상세내역_[^_]+_(.+?)_?(\d{2})(\d{2})\.xlsx?$""", RegexOption.IGNORE_CASE)

        /** @param uploadedOn NOL 파일명에는 연도가 없어서, 업로드한 날 이전의 가장 가까운 날짜로 본다 */
        fun parse(fileName: String, uploadedOn: LocalDate): SettlementFileName {
            val name = fileName.substringAfterLast('/').substringAfterLast('\\').trim()
            DATED.find(name)?.let { m ->
                val (account, y, mo, d, y2, mo2, d2) = m.destructured
                val start = dateOrNull(y.toInt(), mo.toInt(), d.toInt())
                val end = if (y2.isEmpty()) null else dateOrNull(y2.toInt(), mo2.toInt(), d2.toInt())
                if (start != null && end != null && end > start) return SettlementFileName(account.trim(), null, period = start..end)
                return SettlementFileName(account.trim(), start)
            }
            NOL.find(name)?.let { m ->
                val (account, mo, d) = m.destructured
                val thisYear = dateOrNull(uploadedOn.year, mo.toInt(), d.toInt())
                val date = if (thisYear != null && thisYear.isAfter(uploadedOn.plusDays(7))) thisYear.minusYears(1) else thisYear
                return SettlementFileName(account.trim(), date)
            }
            return SettlementFileName(null, null)
        }

        private fun dateOrNull(y: Int, m: Int, d: Int): LocalDate? = runCatching { LocalDate.of(y, m, d) }.getOrNull()
    }
}
