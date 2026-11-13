package kr.sunsetcoast.domain.settlement.vo

import java.time.LocalDate

/**
 * 정산 파일명에서 정산 계정(업소·판매자 계정)과 정산일을 읽는다. 누락 확인용.
 *
 * - 떠나요/여기어때: `정산내역_{업소명}_{yyyy-MM-dd}[_떠나요].xls(x)`
 * - NOL: `정산상세내역_{업소}_{판매자 계정}_{MMdd}.xls` (계정과 날짜 사이 `_`가 빠진 파일도 있다)
 *
 * 이름을 바꾼 파일은 읽지 못한다 (account·issuedOn null).
 */
data class SettlementFileName(val account: String?, val issuedOn: LocalDate?) {
    companion object {
        private val DATED = Regex("""^정산내역_(.+?)_(\d{4})-(\d{2})-(\d{2})""")
        private val NOL = Regex("""^정산상세내역_[^_]+_(.+?)_?(\d{2})(\d{2})\.xlsx?$""", RegexOption.IGNORE_CASE)

        /** @param uploadedOn NOL 파일명에는 연도가 없어서, 업로드한 날 이전의 가장 가까운 날짜로 본다 */
        fun parse(fileName: String, uploadedOn: LocalDate): SettlementFileName {
            val name = fileName.substringAfterLast('/').substringAfterLast('\\').trim()
            DATED.find(name)?.let { m ->
                val (account, y, mo, d) = m.destructured
                return SettlementFileName(account.trim(), dateOrNull(y.toInt(), mo.toInt(), d.toInt()))
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
