package kr.sunsetcoast.domain.bank.vo

import kr.sunsetcoast.domain.settlement.parser.sheet.WorkbookReader
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 은행 거래내역 엑셀 (거래내역조회_입출식 예금).
 *
 * ```
 * 1  거래내역조회_입출식 예금
 * 2  계좌번호:000-000000-00-000 (선셋코스트)
 * 3  (번호) | 거래일시 | 출금 | 입금 | 거래후 잔액 | 거래내용 | 상대계좌번호 | 상대은행 | 메모 | 거래구분 | 수표어음금액 | CMS코드 | 상대계좌예금주명
 * 4~ 거래 (최근 거래가 위)
 * 끝 합계 행 (거래일시 칸에 '합계')
 * ```
 * 열은 이름으로 찾으므로 순서가 바뀌어도 된다.
 *
 * 2행에는 계좌번호·괄호 안 별칭·예금주명이 있다 (`계좌번호:239-…-018 (스테이디앤디) … 예금주명:스테이디앤디풀빌라`).
 * 이 줄이 없는 파일이면 업로드할 때 고른 사업자의 계좌로 넣는다.
 */
object BankStatementParser {
    data class Statement(
        /** 파일에 계좌번호 줄이 없으면 null (업로드할 때 고른 사업자의 계좌로 넣는다) */
        val accountNumber: String?,
        val holderName: String?,
        /** 예금주명 (사업자명과 같다). 고른 사업자가 맞는지 확인하는 데 쓴다 */
        val depositorName: String?,
        /** 오래된 거래부터 */
        val transactions: List<Row>,
        /** 거래후 잔액이 앞 거래와 이어지지 않는 곳 (빠진 거래가 있을 수 있다) */
        val balanceBreaks: List<String>,
    )

    data class Row(
        val rowNo: Int,
        val transactedAt: LocalDateTime,
        val withdrawal: BigDecimal,
        val deposit: BigDecimal,
        val balance: BigDecimal,
        val description: String,
        val counterpartyAccount: String?,
        val counterpartyBank: String?,
        val counterpartyName: String?,
        val memo: String?,
        val transactionType: String?,
    )

    private const val AT = "거래일시"
    private const val WITHDRAWAL = "출금"
    private const val DEPOSIT = "입금"
    private const val BALANCE = "거래후 잔액"
    private const val DESCRIPTION = "거래내용"
    private val REQUIRED = listOf(AT, WITHDRAWAL, DEPOSIT, BALANCE, DESCRIPTION)
    private val ACCOUNT = Regex("""계좌번호\s*[:：]\s*([0-9-]+)\s*(?:\((.*?)\))?""")
    private val DEPOSITOR = Regex("""예금주명\s*[:：]\s*(\S+)""")
    private val DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm[:ss]")

    fun parse(bytes: ByteArray): Statement {
        val rows = runCatching { WorkbookReader.readFirstSheet(bytes).rows }
            .getOrElse { throw IllegalArgumentException("엑셀 파일을 읽을 수 없습니다.") }

        val account = rows.firstNotNullOfOrNull { (_, cells) -> cells.firstNotNullOfOrNull { ACCOUNT.find(it) } }
        val accountNumber = account?.groupValues?.get(1)
        val holderName = account?.groupValues?.get(2)?.trim()?.ifEmpty { null }
        val depositorName = rows.firstNotNullOfOrNull { (_, cells) -> cells.firstNotNullOfOrNull { DEPOSITOR.find(it) } }?.groupValues?.get(1)
        val headerIndex = rows.indexOfFirst { (_, cells) -> REQUIRED.all { name -> cells.any { it.trim() == name } } }
        require(headerIndex >= 0) { "거래내역 머리글(${REQUIRED.joinToString()})을 찾을 수 없습니다." }
        val header = rows[headerIndex].second.map { it.trim() }
        fun col(name: String) = header.indexOf(name).takeIf { it >= 0 }

        val parsed = rows.drop(headerIndex + 1).mapNotNull { (rowNo, cells) ->
            fun cell(name: String) = col(name)?.let { cells.getOrNull(it) }?.trim()?.takeIf { it.isNotEmpty() }
            val at = cell(AT) ?: return@mapNotNull null
            if (at.contains("합계")) return@mapNotNull null
            try {
                Row(
                    rowNo = rowNo,
                    transactedAt = dateTime(at),
                    withdrawal = amount(cell(WITHDRAWAL)),
                    deposit = amount(cell(DEPOSIT)),
                    balance = amount(cell(BALANCE)),
                    description = cell(DESCRIPTION) ?: "",
                    counterpartyAccount = cell("상대계좌번호"),
                    counterpartyBank = cell("상대은행"),
                    counterpartyName = cell("상대계좌예금주명")?.let(::halfWidth),
                    memo = cell("메모"),
                    transactionType = cell("거래구분"),
                )
            } catch (e: RuntimeException) {
                throw IllegalArgumentException("${rowNo}행을 읽을 수 없습니다: ${e.message}")
            }
        }
        require(parsed.isNotEmpty()) { "거래가 없습니다." }

        // 파일은 최근 거래가 위. 같은 시각 거래도 파일 순서를 지키도록 행 번호 역순으로 둔다.
        val ordered = parsed.sortedWith(compareBy<Row> { it.transactedAt }.thenByDescending { it.rowNo })
        val breaks = ordered.zipWithNext().mapNotNull { (prev, cur) ->
            val expected = prev.balance + cur.deposit - cur.withdrawal
            if (expected.compareTo(cur.balance) == 0) null
            else "${cur.rowNo}행 ${cur.transactedAt}: 앞 거래 잔액으로 계산하면 $expected, 파일은 ${cur.balance}"
        }
        return Statement(accountNumber, holderName, depositorName, ordered, breaks)
    }

    private fun dateTime(value: String): LocalDateTime {
        val text = value.replace('T', ' ').replace('.', '-').trim()
        return LocalDateTime.parse(if (text.length == 16) "$text:00" else text.take(19), DATE_TIME)
    }

    private fun amount(value: String?): BigDecimal {
        val text = value?.replace(",", "")?.replace("원", "")?.trim().orEmpty()
        return if (text.isEmpty()) BigDecimal.ZERO else BigDecimal(text)
    }

    /** '（주）하이픈코퍼레이' 같은 전각 괄호를 반각으로 */
    private fun halfWidth(value: String) = value.replace('（', '(').replace('）', ')')
}
