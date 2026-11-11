package kr.sunsetcoast.domain.settlement.parser.sheet

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 헤더 이름으로 접근하는 엑셀 한 행. 플랫폼마다 다른 금액/날짜 표기를 여기서 흡수한다.
 *
 * rowNo는 엑셀 화면 기준 행 번호(1부터)다.
 */
class SheetRow(
    val rowNo: Int,
    val values: Map<String, String>,
) {
    fun text(header: String): String = values[header]?.trim().orEmpty()

    fun textOrNull(header: String): String? = text(header).takeIf { it.isNotEmpty() && it != "-" }

    fun required(header: String): String =
        textOrNull(header) ?: throw IllegalArgumentException("'$header' 값이 비어 있습니다.")

    /** "89,100원", "-12,900", "1.4288849E7", "-" 모두 처리. 컬럼이 없거나 비어 있으면 0 */
    fun amount(header: String): BigDecimal = parseAmount(text(header), header)

    fun amounts(vararg headers: String): BigDecimal = headers.fold(BigDecimal.ZERO) { sum, h -> sum + amount(h) }

    /** "13.0%", "15.00" -> 13.0, 15.00 */
    fun percent(header: String): BigDecimal? =
        textOrNull(header)?.removeSuffix("%")?.trim()?.toBigDecimalOrNull()

    fun date(header: String): LocalDate = parseDate(required(header), header)

    fun dateTimeOrNull(header: String): LocalDateTime? = textOrNull(header)?.let { parseDateTime(it, header) }

    fun isBlank(): Boolean = values.values.all { it.isBlank() }

    companion object {
        private val NON_NUMERIC = Regex("[^0-9.\\-]")
        private val WEEKDAY = Regex("\\([월화수목금토일]\\)")
        private val TIME = DateTimeFormatter.ofPattern("H:mm[:ss]")

        fun parseAmount(raw: String, header: String = ""): BigDecimal {
            val cleaned = raw.trim()
            if (cleaned.isEmpty() || cleaned == "-") return BigDecimal.ZERO
            return runCatching { BigDecimal(cleaned.replace(",", "").replace("원", "").trim()) }
                .recoverCatching { BigDecimal(cleaned.replace(NON_NUMERIC, "")) }
                .getOrElse { throw IllegalArgumentException("'$header' 금액 형식 오류: $raw") }
        }

        /** "2026-08-22", "2026.10.31 (토) 16:00", "2026-07-29 00:00:29", "2026-08-22T00:00" */
        fun parseDate(raw: String, header: String = ""): LocalDate {
            val normalized = normalize(raw)
            return runCatching { LocalDate.parse(normalized.take(10)) }
                .getOrElse { throw IllegalArgumentException("'$header' 날짜 형식 오류: $raw") }
        }

        fun parseDateTime(raw: String, header: String = ""): LocalDateTime {
            val normalized = normalize(raw)
            val date = parseDate(normalized, header)
            val timePart = normalized.drop(10).trim()
            if (timePart.isEmpty()) return date.atStartOfDay()
            return runCatching { LocalDateTime.of(date, LocalTime.parse(timePart, TIME)) }
                .getOrElse { throw IllegalArgumentException("'$header' 일시 형식 오류: $raw") }
        }

        private fun normalize(raw: String): String = raw.trim()
            .replace(WEEKDAY, "")
            .replace('T', ' ')
            .replace(Regex("\\s+"), " ")
            .let { if (it.length >= 10 && it[4] == '.' && it[7] == '.') it.substring(0, 10).replace('.', '-') + it.substring(10) else it }
    }
}

fun BigDecimal.ratioPercent(base: BigDecimal): BigDecimal? =
    if (base.signum() == 0) null else multiply(BigDecimal(100)).divide(base, 2, RoundingMode.HALF_UP)
