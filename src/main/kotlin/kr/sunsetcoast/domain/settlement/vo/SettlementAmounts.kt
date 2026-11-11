package kr.sunsetcoast.domain.settlement.vo

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 정산 행 금액. 플랫폼별 컬럼을 아래 의미로 통일한다.
 *
 * - gross: 수수료 계산 기준 금액 (여기어때 정산대상금액, NOL 판매금액, 떠나요 매출)
 * - partnerDiscount: 우리가 부담한 할인/쿠폰 (정산금액에서 차감됨)
 * - platformDiscount: 플랫폼/제3자가 부담한 할인/쿠폰/포인트 (정산금액에 영향 없음, 참고용)
 * - commission: 플랫폼 수수료, otherFee: 그 외 차감 (NOL 부스팅 이용료 등)
 * - net: 실제 입금액
 */
data class SettlementAmounts(
    val gross: BigDecimal,
    val partnerDiscount: BigDecimal = BigDecimal.ZERO,
    val platformDiscount: BigDecimal = BigDecimal.ZERO,
    val commissionRate: BigDecimal? = null,
    val commission: BigDecimal = BigDecimal.ZERO,
    val otherFee: BigDecimal = BigDecimal.ZERO,
    val net: BigDecimal,
) {
    /** DB(DECIMAL(15,2))에서 읽은 값과 비교할 수 있도록 scale을 맞춘다. */
    fun normalized(): SettlementAmounts = copy(
        gross = gross.money(),
        partnerDiscount = partnerDiscount.money(),
        platformDiscount = platformDiscount.money(),
        commissionRate = commissionRate?.money(),
        commission = commission.money(),
        otherFee = otherFee.money(),
        net = net.money(),
    )

    /** 기준금액 - 수수료 - 기타차감 - 우리 부담 할인 = 정산금액 */
    fun isBalanced(): Boolean =
        (gross - commission - otherFee - partnerDiscount - net).abs() <= BigDecimal.ONE

    private fun BigDecimal.money(): BigDecimal = setScale(2, RoundingMode.HALF_UP)
}
