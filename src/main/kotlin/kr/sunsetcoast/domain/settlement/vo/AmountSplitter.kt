package kr.sunsetcoast.domain.settlement.vo

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 다중 객실 예약의 금액을 객실별 가중치(정가) 비율로 나눈다.
 * 원 단위로 내림하고 나머지는 마지막 객실에 더해 합계가 원래 금액과 같게 한다.
 */
object AmountSplitter {
    fun split(amounts: SettlementAmounts, weights: List<BigDecimal>): List<SettlementAmounts> {
        require(weights.isNotEmpty()) { "객실이 없습니다" }
        if (weights.size == 1) return listOf(amounts)
        val gross = split(amounts.gross, weights)
        val partner = split(amounts.partnerDiscount, weights)
        val platform = split(amounts.platformDiscount, weights)
        val commission = split(amounts.commission, weights)
        val other = split(amounts.otherFee, weights)
        val net = split(amounts.net, weights)
        return weights.indices.map { i ->
            amounts.copy(
                gross = gross[i],
                partnerDiscount = partner[i],
                platformDiscount = platform[i],
                commission = commission[i],
                otherFee = other[i],
                net = net[i],
            )
        }
    }

    fun split(amount: BigDecimal, weights: List<BigDecimal>): List<BigDecimal> {
        val total = weights.fold(BigDecimal.ZERO, BigDecimal::add)
        val shares = if (total.signum() == 0) weights.map { BigDecimal.ONE } else weights
        val shareTotal = shares.fold(BigDecimal.ZERO, BigDecimal::add)
        val parts = shares.dropLast(1).map { amount.multiply(it).divide(shareTotal, 0, RoundingMode.DOWN) }
        return parts + (amount - parts.fold(BigDecimal.ZERO, BigDecimal::add))
    }
}
