package kr.sunsetcoast.domain.managementfee.vo

import java.math.BigDecimal

/**
 * 추가요금 입력 한 건.
 * @param unitPrice 생략하면 항목의 기본 단가 (기타는 필수)
 * @param nights 1박 단위 요금(추가인원)의 박수. 생략하면 예약 박수
 */
data class ManagementFeeExtraInput(
    val item: ManagementFeeExtraItem,
    val quantity: Int,
    val unitPrice: BigDecimal? = null,
    val nights: Int? = null,
    val memo: String? = null,
) {
    data class Resolved(val unitPrice: BigDecimal, val nights: Int?, val amount: BigDecimal)

    fun resolve(defaultNights: Int?): Resolved {
        require(quantity > 0) { "${item.label} 수량은 1 이상이어야 합니다: $quantity" }
        val price = requireNotNull(unitPrice ?: item.defaultUnitPrice) { "${item.label} 단가를 입력해 주세요." }
        require(price.signum() > 0) { "${item.label} 단가는 0원보다 커야 합니다: $price" }
        if (item == ManagementFeeExtraItem.OTHER) require(!memo.isNullOrBlank()) { "기타 추가요금은 내용을 메모로 적어 주세요." }
        val stayNights = if (item.perNight) {
            val n = requireNotNull(nights ?: defaultNights) { "${item.label} 박수를 입력해 주세요." }
            require(n > 0) { "${item.label} 박수는 1 이상이어야 합니다: $n" }
            n
        } else {
            null
        }
        val amount = price * BigDecimal(quantity) * BigDecimal(stayNights ?: 1)
        return Resolved(price, stayNights, amount)
    }
}
