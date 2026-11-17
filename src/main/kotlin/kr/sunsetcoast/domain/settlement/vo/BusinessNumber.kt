package kr.sunsetcoast.domain.settlement.vo

/** 사업자등록번호. 파일·입력마다 하이픈 유무가 달라서 숫자 10자리로 맞춰 비교한다 */
object BusinessNumber {
    /** 숫자만 남긴다. 10자리가 아니면 null */
    fun normalize(value: String?): String? = value?.filter(Char::isDigit)?.takeIf { it.length == 10 }

    /** 278-27-01574 */
    fun format(digits: String): String =
        if (digits.length == 10) "${digits.take(3)}-${digits.substring(3, 5)}-${digits.takeLast(5)}" else digits
}
