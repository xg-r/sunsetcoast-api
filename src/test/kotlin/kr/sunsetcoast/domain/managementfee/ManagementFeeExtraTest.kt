package kr.sunsetcoast.domain.managementfee

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.shouldBe
import kr.sunsetcoast.domain.managementfee.entity.ManagementFeeEntity
import kr.sunsetcoast.domain.managementfee.vo.ManagementFeeBasis
import kr.sunsetcoast.domain.managementfee.vo.ManagementFeeExtraInput
import kr.sunsetcoast.domain.managementfee.vo.ManagementFeeExtraItem.BBQ
import kr.sunsetcoast.domain.managementfee.vo.ManagementFeeExtraItem.EXTRA_PERSON
import kr.sunsetcoast.domain.managementfee.vo.ManagementFeeExtraItem.OTHER
import kr.sunsetcoast.domain.reservation.entity.ReservationEntity
import kr.sunsetcoast.domain.room.entity.RoomEntity
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal

class ManagementFeeExtraTest : FunSpec({
    fun fee(amount: Int, nights: Int = 2): ManagementFeeEntity {
        val reservation = mock(ReservationEntity::class.java)
        `when`(reservation.nights).thenReturn(nights)
        return ManagementFeeEntity(
            reservation = reservation,
            room = mock(RoomEntity::class.java),
            basis = ManagementFeeBasis.CHARGED,
            calculatedAmount = BigDecimal(amount),
        )
    }

    test("바베큐 30,000, 추가인원 1박 1인당 20,000 × 예약 박수가 기본") {
        val fee = fee(45_000, nights = 2)
        fee.replaceExtras(listOf(ManagementFeeExtraInput(BBQ, 1), ManagementFeeExtraInput(EXTRA_PERSON, 2)))
        fee.extras.map { it.amount.toInt() } shouldBe listOf(30_000, 80_000)
        fee.extras[1].nights shouldBe 2
        fee.extraAmount shouldBeEqualComparingTo BigDecimal(110_000)
        fee.amount shouldBeEqualComparingTo BigDecimal(45_000)
        fee.payableAmount shouldBeEqualComparingTo BigDecimal(155_000)
    }

    test("박수와 단가를 직접 주면 그 값으로") {
        val fee = fee(30_000, nights = 3)
        fee.replaceExtras(listOf(ManagementFeeExtraInput(EXTRA_PERSON, 1, unitPrice = BigDecimal(15_000), nights = 1)))
        fee.extraAmount shouldBeEqualComparingTo BigDecimal(15_000)
    }

    test("다시 저장하면 통째로 바뀌고, 빈 목록이면 모두 지운다") {
        val fee = fee(30_000)
        fee.replaceExtras(listOf(ManagementFeeExtraInput(BBQ, 2)))
        fee.replaceExtras(listOf(ManagementFeeExtraInput(BBQ, 1)))
        fee.extraAmount shouldBeEqualComparingTo BigDecimal(30_000)
        fee.replaceExtras(emptyList())
        fee.extras.size shouldBe 0
    }

    test("입금 완료는 관리비 + 추가요금으로, 그 뒤엔 추가요금을 고칠 수 없다") {
        val fee = fee(30_000)
        fee.replaceExtras(listOf(ManagementFeeExtraInput(BBQ, 1)))
        fee.markPaid(userId = 1)
        fee.paidAmount!! shouldBeEqualComparingTo BigDecimal(60_000)
        fee.amountChangedAfterPayment shouldBe false
        shouldThrow<IllegalStateException> { fee.replaceExtras(emptyList()) }
    }

    test("관리비 0원이어도 추가요금이 있으면 입금 완료로 표시할 수 있다") {
        val fee = fee(0)
        fee.replaceExtras(listOf(ManagementFeeExtraInput(BBQ, 1)))
        fee.markPaid(userId = 1)
        fee.paidAmount!! shouldBeEqualComparingTo BigDecimal(30_000)
    }

    test("확인한 뒤 추가요금을 넣으면 다시 확인") {
        val fee = fee(30_000)
        fee.verify(userId = 1)
        fee.replaceExtras(listOf(ManagementFeeExtraInput(EXTRA_PERSON, 1)))
        fee.amountChangedAfterVerification shouldBe true
        fee.verify(userId = 1)
        fee.amountChangedAfterVerification shouldBe false
    }

    test("잘못된 입력") {
        val fee = fee(30_000)
        shouldThrow<IllegalArgumentException> { fee.replaceExtras(listOf(ManagementFeeExtraInput(BBQ, 0))) }
        shouldThrow<IllegalArgumentException> { fee.replaceExtras(listOf(ManagementFeeExtraInput(OTHER, 1, unitPrice = BigDecimal(5_000)))) }
        shouldThrow<IllegalArgumentException> { fee.replaceExtras(listOf(ManagementFeeExtraInput(OTHER, 1, memo = "숯"))) }
        fee.replaceExtras(listOf(ManagementFeeExtraInput(OTHER, 1, unitPrice = BigDecimal(5_000), memo = "숯 추가")))
        fee.extraAmount shouldBeEqualComparingTo BigDecimal(5_000)
    }
})
