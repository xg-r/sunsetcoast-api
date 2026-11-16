package kr.sunsetcoast.common.response

/**
 * 안내 문구에서 금액을 뺀다. 현장관리자에게 가는 응답·알림에 쓴다.
 * - "71681294 2026-10-22 ROOM_NIGHT 금액 변경: 정산금액 178000.00 -> 177000.00" → "71681294 2026-10-22 ROOM_NIGHT 금액 변경"
 * - "금액 검증 실패: 매출(100000) - 수수료(8000) != 정산금액(91000)" → "금액 검증 실패"
 */
object AmountText {
    private val PHRASES = listOf("금액 변경", "금액 검증 실패")

    fun redact(message: String?): String? {
        if (message == null) return null
        val phrase = PHRASES.firstOrNull { message.contains(it) } ?: return message
        return message.substringBefore(phrase) + phrase
    }
}
